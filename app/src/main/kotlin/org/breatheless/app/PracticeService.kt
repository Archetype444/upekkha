package org.breatheless.app

import android.app.*
import android.content.*
import android.media.*
import android.media.session.*
import android.os.*
import android.view.KeyEvent
import com.wanderwildwood.nikka.MainActivity
import com.wanderwildwood.nikka.R
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject

class PracticeService : Service() {
    var engine: Engine? = null; private set
    val cps = mutableListOf<CpValue>()
    val holds = mutableListOf<HoldValue>()
    val pulses = mutableListOf<PulseValue>()
    var note = ""; private set
    var audioPosition = 0; private set
    var audioDuration = 1227168; private set
    var ending = false; private set
    private lateinit var store: Store
    private lateinit var feedback: Feedback
    private lateinit var media: MediaSession
    private val handler = Handler(Looper.getMainLooper())
    private var voice: MediaPlayer? = null
    private var ambience: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var wake: PowerManager.WakeLock? = null
    private var recordId = ""
    private var began = 0L
    private var savedId: String? = null
    private var lastNotification = ""
    private val audioManager get() = getSystemService(AudioManager::class.java)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        store = Store.get(this); feedback = Feedback(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("practice", "Practice controls", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null); enableVibration(false) })
        media = MediaSession(this, "Upekkhā").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { remotePrimary() }
                override fun onPause() { if (engine?.state in measurementStates) remotePrimary() }
                override fun onStop() { discard() }
                override fun onSkipToNext() { if (engine?.state in reviewStates) review(false) }
                override fun onMediaButtonEvent(intent: Intent): Boolean {
                    val key = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) else @Suppress("DEPRECATION") intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (key?.action == KeyEvent.ACTION_DOWN && key.repeatCount == 0 && key.keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK)) { remotePrimary(); return true }
                    return super.onMediaButtonEvent(intent)
                }
            }); isActive = true
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                if (engine != null) return START_NOT_STICKY
                startForeground(19, notification())
                try {
                    val protocol = Codec.readProtocol(JSONObject(intent.getStringExtra("protocol")!!))
                    recordId = Store.id(); began = System.currentTimeMillis(); engine = Engine(protocol, SystemClock.elapsedRealtime())
                    store.active.delete(); live = this; result.value = null; active.value = true
                    val pre = intent.getIntExtra("pulse", 0)
                    if (pre in 20..250) pulses += PulseValue(pre, began, "Before practice")
                    holdWake(); phaseChanged(); handler.post(ticker)
                } catch (e: Exception) { failure = "Practice could not start: ${e.message}"; discard() }
            }
            "PRIMARY" -> remotePrimary()
            "END" -> discard()
            "EXCLUDE" -> review(false)
        }
        return START_NOT_STICKY
    }
    private val ticker = object : Runnable {
        override fun run() {
            val e = engine ?: return
            if (ending) return
            val now = SystemClock.elapsedRealtime()
            if (e.tick(now)) when (e.state) {
                State.CP_REVIEW, State.HOLD_REVIEW -> { note = "The pause reached the software limit. This measurement is excluded."; feedback.signal(if (e.phase.kind == Kind.CP) "CP_END" else "HOLD_END") }
                State.PULSE_REVIEW -> feedback.signal("PULSE_END")
                else -> phaseChanged()
            }
            if (ending) return
            if (e.state == State.FINISHED) { complete(); return }
            if (e.total(now) > 21600) { discard(); return }
            voice?.let { runCatching { audioPosition = it.currentPosition } }
            publish()
            val content = notificationText()
            if (content != lastNotification) { lastNotification = content; getSystemService(NotificationManager::class.java).notify(19, notification()) }
            handler.postDelayed(this, if (e.state in listOf(State.TIMED, State.PULSE_RUNNING)) e.remaining(now).coerceIn(1, 1000) else 1000)
        }
    }
    private fun publish() {
        val e = engine ?: return
        snapshot.value = e.snapshot(SystemClock.elapsedRealtime(), note, ending)
        updates.value++
        val actions = PlaybackState.ACTION_STOP or when (e.state) { in measurementStates -> PlaybackState.ACTION_PLAY_PAUSE; State.PAUSED -> PlaybackState.ACTION_PLAY; else -> 0L }
        media.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, e.phase.title).putString(MediaMetadata.METADATA_KEY_ARTIST, "Upekkhā").build())
        media.setPlaybackState(PlaybackState.Builder().setActions(actions).setState(if (ending) PlaybackState.STATE_STOPPED else PlaybackState.STATE_PLAYING, 0, 1f).build())
    }
    private fun phaseChanged() {
        val e = engine ?: return
        if (e.state == State.FINISHED) { complete(); return }
        voice?.release(); voice = null; audioPosition = 0; note = ""
        if (e.phase.kind == Kind.LISTEN) { ambience?.release(); ambience = null; startRecording() } else configureAmbience()
        cue(e.phase.kind.name); publish()
        getSystemService(NotificationManager::class.java).notify(19, notification())
    }
    internal fun recordingCompleted() {
        if (!ending && engine?.state == State.LISTENING) { engine?.advance(SystemClock.elapsedRealtime()); phaseChanged() }
    }
    private fun startRecording() {
        try {
            val granted = requestFocus()
            voice = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                assets.openFd("audio/mindfulness.mp3").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setWakeMode(this@PracticeService, PowerManager.PARTIAL_WAKE_LOCK)
                setOnCompletionListener { recordingCompleted() }
                setOnErrorListener { failed, _, _ ->
                    failed.release(); if (voice === failed) voice = null
                    if (engine?.state != State.PAUSED) engine?.pause(SystemClock.elapsedRealtime())
                    releaseWake(); note = "The recording stopped. Resume to retry, or quit the practice."; publish(); true
                }
                prepare(); audioDuration = duration; if (granted) start()
            }
            if (!granted) { engine?.pause(SystemClock.elapsedRealtime()); releaseWake(); note = "Audio focus is unavailable. Resume when other playback has finished." }
        } catch (e: Exception) {
            voice?.release(); voice = null; engine?.pause(SystemClock.elapsedRealtime()); releaseWake()
            note = "The recording could not be opened. Resume to retry. ${e.message.orEmpty()}"
        }
    }
    private fun requestFocus(): Boolean {
        if (focus == null) focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (engine?.state !in reviewStates + listOf(State.PAUSED, State.FINISHED)) {
                        pause(); if (engine?.state == State.PAUSED) note = "Playback interrupted. Resume when you are ready."; publish()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { voice?.setVolume(.3f, .3f); ambience?.setVolume(.1f, .1f) }
                    AudioManager.AUDIOFOCUS_GAIN -> { voice?.setVolume(1f, 1f); val volume = store.int("volume", 30) / 100f; ambience?.setVolume(volume, volume) }
                }
            }.build()
        return audioManager.requestAudioFocus(focus!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    private fun holdWake() { if (wake?.isHeld != true) wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Upekkhā:practice").apply { acquire(6 * 60 * 60 * 1000L) } }
    private fun releaseWake() { if (wake?.isHeld == true) wake?.release(); wake = null }
    private fun cue(event: String) { runCatching { voice?.setVolume(.3f, .3f) }; feedback.signal(event); handler.postDelayed({ runCatching { voice?.setVolume(1f, 1f) } }, 1500) }
    fun pause() {
        val e = engine ?: return
        if (ending || e.state in reviewStates || e.state == State.FINISHED) return
        if (e.state == State.PAUSED) {
            e.resume(SystemClock.elapsedRealtime()); holdWake(); note = ""
            if (e.phase.kind == Kind.LISTEN) {
                if (voice == null) startRecording() else if (requestFocus()) voice?.start() else { e.pause(SystemClock.elapsedRealtime()); releaseWake(); note = "Audio focus is unavailable." }
            } else runCatching { ambience?.start() }
        } else {
            e.pause(SystemClock.elapsedRealtime()); runCatching { voice?.pause() }; runCatching { ambience?.pause() }; releaseWake()
            if (e.state in reviewStates) { note = "The pause was interrupted. Its measurement will be excluded."; feedback.signal(if (e.phase.kind == Kind.CP) "CP_END" else "HOLD_END") }
        }
        publish()
    }
    fun startCp() { val e = engine ?: return; if (ending || e.state !in listOf(State.CP_READY, State.HOLD_READY)) return; e.startCp(SystemClock.elapsedRealtime()); note = ""; publish() }
    fun stopCp() { val e = engine ?: return; if (ending || e.state !in listOf(State.CP_RUNNING, State.HOLD_RUNNING)) return; e.stopCp(SystemClock.elapsedRealtime()); cue(if (e.phase.kind == Kind.CP) "CP_END" else "HOLD_END"); publish() }
    fun backgroundCp() {
        val e = engine ?: return
        if (e.state in listOf(State.CP_RUNNING, State.HOLD_RUNNING)) { e.stopCp(SystemClock.elapsedRealtime(), true); note = "The app left the screen during the pause. This value is excluded."; feedback.signal(if (e.phase.kind == Kind.CP) "CP_END" else "HOLD_END"); publish() }
        if (e.state == State.PULSE_RUNNING) { e.pause(SystemClock.elapsedRealtime()); note = "Pulse count interrupted. Begin a new count."; publish() }
    }
    fun review(valid: Boolean) {
        val e = engine ?: return
        if (ending || e.state !in reviewStates) return
        if (e.pendingCp > 0) {
            val included = valid && !e.interrupted
            if (e.phase.kind == Kind.CP) cps += CpValue(e.pendingCp, System.currentTimeMillis(), included, "CP ${cps.size + 1}")
            else holds += HoldValue(e.phase.kind, e.pendingCp, System.currentTimeMillis(), included, e.phase.title)
        }
        e.advance(SystemClock.elapsedRealtime()); holdWake(); phaseChanged()
    }
    fun startPulse() { val e = engine ?: return; if (ending || e.state != State.PULSE_READY) return; e.startPulse(SystemClock.elapsedRealtime()); feedback.signal("PULSE"); note = ""; publish() }
    fun pulseBeats(beats: Int) { val e = engine ?: return; if (e.state !in listOf(State.PULSE_RUNNING, State.PULSE_REVIEW) || ending) return; e.setPulseBeats(beats); publish() }
    fun recordPulse(skip: Boolean = false) {
        val e = engine ?: return
        if (ending || e.state !in listOf(State.PULSE_READY, State.PULSE_REVIEW)) return
        val bpm = (e.pulseBeats * 60.0 / e.phase.seconds).toInt()
        if (!skip && bpm !in 20..250) { note = "Adjust the beat count, or skip this pulse measurement."; publish(); return }
        if (!skip) pulses += PulseValue(bpm, System.currentTimeMillis(), if (e.index < e.protocol.phases.size / 2) "Before practice" else "After practice")
        e.advance(SystemClock.elapsedRealtime()); holdWake(); phaseChanged()
    }
    fun remotePrimary() { when (engine?.state) {
        State.CP_READY, State.HOLD_READY -> startCp(); State.CP_RUNNING, State.HOLD_RUNNING -> stopCp(); in reviewStates -> review(engine?.interrupted != true)
        State.PULSE_READY -> startPulse(); State.PULSE_RUNNING -> pulseBeats((engine?.pulseBeats ?: 0) + 1); State.PULSE_REVIEW -> recordPulse(); State.PAUSED -> pause(); else -> Unit
    } }
    private fun complete() {
        val e = engine ?: return
        if (ending || e.state != State.FINISHED) return
        val entry = Entry(recordId, began, e.protocol.name, e.total(SystemClock.elapsedRealtime()), "Completed", cps.toList(), pulses.toList(), context = if (e.protocol.id == "morning") "Morning" else "Practice", holds = holds.toList())
        try { store.upsert(entry); store.active.delete(); savedId = entry.id; lastResult = entry.id }
        catch (error: Exception) { note = "Practice finished, but saving failed. ${error.message.orEmpty()}"; failure = note; publish(); return }
        ending = true; stopPlayback(); feedback.signal("END"); publish()
        handler.removeCallbacks(ticker)
        handler.postDelayed({
            if (live === this) { live = null; snapshot.value = null; active.value = false; result.value = savedId }
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }, 2200)
    }
    fun finish(status: String) { if (status == "Completed") complete() else discard() }
    fun retrySave() { complete() }
    fun discard() {
        if (ending && savedId != null) return
        ending = true; handler.removeCallbacksAndMessages(null); store.active.delete(); stopPlayback()
        if (live === this) { live = null; snapshot.value = null; active.value = false; result.value = null; lastResult = null }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    private fun stopPlayback() { voice?.release(); voice = null; ambience?.release(); ambience = null; focus?.let { audioManager.abandonAudioFocusRequest(it) }; focus = null; releaseWake() }
    fun configureAmbience() {
        ambience?.release(); ambience = null
        val e = engine ?: return
        if (e.phase.kind == Kind.LISTEN || ending || store.str("ambience", "Off") == "Off") return
        try {
            if (!requestFocus()) return
            val id = if (store.str("ambience", "Off") == "Wind") R.raw.wind else R.raw.fireplace
            ambience = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                resources.openRawResourceFd(id).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }; isLooping = true; prepare()
                val volume = store.int("volume", 30) / 100f; setVolume(volume, volume); if (e.state != State.PAUSED) start()
            }
        } catch (error: Exception) { note = "Ambience is unavailable. ${error.message.orEmpty()}" }
    }
    private fun notificationText(): String {
        if (ending) return "Practice saved"
        val e = engine ?: return "Starting practice"
        return e.phase.title + " · " + when (e.state) {
            State.TIMED, State.PULSE_RUNNING -> clock((e.remaining(SystemClock.elapsedRealtime()) + 999) / 1000)
            State.LISTENING -> "Playing"; State.CP_READY, State.HOLD_READY, State.PULSE_READY -> "Ready"
            State.CP_RUNNING, State.HOLD_RUNNING -> "Measuring"; State.CP_REVIEW, State.HOLD_REVIEW, State.PULSE_REVIEW -> "Review measurement"
            State.PAUSED -> "Interrupted"; State.FINISHED -> "Finished"
        }
    }
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(name: String, number: Int) = PendingIntent.getService(this, number, Intent(this, PracticeService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = when (engine?.state) {
            State.CP_READY -> "Start CP"; State.HOLD_READY -> "Start pause"; State.CP_RUNNING, State.HOLD_RUNNING -> "Stop pause"; State.CP_REVIEW, State.HOLD_REVIEW -> "Continue"
            State.PULSE_READY -> "Start pulse count"; State.PULSE_REVIEW -> "Record pulse"; State.PAUSED -> "Continue practice"; else -> null
        }
        val builder = Notification.Builder(this, "practice").setSmallIcon(R.drawable.ic_notification).setContentTitle("Upekkhā").setContentText(notificationText()).setContentIntent(open).setOngoing(!ending).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE)
        if (title != null && !ending) builder.addAction(Notification.Action.Builder(null, title, action("PRIMARY", 1)).build())
        if (!ending) builder.addAction(Notification.Action.Builder(null, "Quit session", action("END", 2)).build())
        return builder.build()
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null); stopPlayback(); media.release(); feedback.release()
        if (live === this) { live = null; snapshot.value = null; active.value = false; if (savedId != null) result.value = savedId }
        super.onDestroy()
    }
    companion object {
        val active = MutableStateFlow(false)
        val result = MutableStateFlow<String?>(null)
        val snapshot = MutableStateFlow<PracticeSnapshot?>(null)
        val updates = MutableStateFlow(0)
        val reviewStates = setOf(State.CP_REVIEW, State.HOLD_REVIEW)
        val measurementStates = setOf(State.CP_READY, State.CP_RUNNING, State.CP_REVIEW, State.HOLD_READY, State.HOLD_RUNNING, State.HOLD_REVIEW, State.PULSE_READY, State.PULSE_RUNNING, State.PULSE_REVIEW)
        var live: PracticeService? = null
        var lastResult: String? = null
        var failure: String? = null
        fun clock(seconds: Long) = "%02d:%02d".format(seconds.coerceAtLeast(0) / 60, seconds.coerceAtLeast(0) % 60)
    }
}
