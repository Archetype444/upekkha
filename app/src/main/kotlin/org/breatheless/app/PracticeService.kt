package org.breatheless.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.media.session.*
import android.os.*
import android.view.KeyEvent
import org.json.JSONObject

class PracticeService:Service(){
    var engine:Engine?=null;private set
    val cps=mutableListOf<CpValue>();val pulses=mutableListOf<PulseValue>()
    var note="";private set
    var audioPosition=0;private set
    var audioDuration=1227168;private set
    var ending=false;private set
    private lateinit var store:Store;private lateinit var feedback:Feedback
    private val handler=Handler(Looper.getMainLooper())
    private var voice:MediaPlayer?=null;private var ambience:MediaPlayer?=null;private var focus:AudioFocusRequest?=null
    private var wake:PowerManager.WakeLock?=null;private lateinit var media:MediaSession
    private var recordId="";private var began=0L;private var checkpointTime=0L;private var lastNotification="";private var finished=false
    private var activePhase=-1
    private val audioManager get()=getSystemService(AudioManager::class.java)
    override fun onBind(i:Intent?):IBinder?=null
    override fun onCreate(){super.onCreate();store=Store.get(this);feedback=Feedback(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("practice","Practice controls",NotificationManager.IMPORTANCE_LOW).apply{setSound(null,null);enableVibration(false);description="Session controls; phase sounds are configured in the app."})
        media=MediaSession(this,"Upekkhā").apply{setCallback(object:MediaSession.Callback(){
            override fun onPlay(){if(engine?.state in listOf(State.CP_READY,State.CP_RUNNING,State.CP_REVIEW))remotePrimary()else if(engine?.state==State.PAUSED)pause()}
            override fun onPause(){if(engine?.state==State.LISTENING || engine?.state==State.TIMED)pause()else remotePrimary()}
            override fun onStop(){finish("Ended early")}
            override fun onSkipToNext(){if(engine?.state==State.CP_REVIEW)review(false)}
            override fun onSeekTo(pos:Long){seek(pos.toInt())}
            override fun onMediaButtonEvent(intent:Intent):Boolean{val k=if(Build.VERSION.SDK_INT>=33)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT,KeyEvent::class.java)else @Suppress("DEPRECATION") intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT);if(k?.action==KeyEvent.ACTION_DOWN && k.repeatCount==0 && k.keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_HEADSETHOOK)){remotePrimary();return true};return super.onMediaButtonEvent(intent)}
        });isActive=true}
    }
    override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{when(i?.action){
        "START"->{if(engine!=null)return START_NOT_STICKY;startForeground(19,notification());try{
            val p=Codec.readProtocol(JSONObject(i.getStringExtra("protocol")!!));recordId=Store.id();began=System.currentTimeMillis();engine=Engine(p,SystemClock.elapsedRealtime());live=this
            val pre=i.getIntExtra("pulse",0);if(pre in 20..250)pulses.add(PulseValue(pre,began,"Before practice"))
            store.settings.put("lastProtocol",p.id);store.save();holdWake();phaseChanged();handler.post(ticker)
        }catch(e:Exception){failure="Session could not start: ${e.message}";stopSelf()}}
        "PRIMARY"->remotePrimary();"PAUSE"->pause();"END"->finish("Ended early");"EXCLUDE"->review(false)
    };return START_NOT_STICKY}
    private val ticker=object:Runnable{override fun run(){val e=engine ?:return;if(ending)return
        val now=SystemClock.elapsedRealtime();if(e.tick(now)){if(e.state==State.CP_REVIEW){note="The CP timer was stopped at the software limit. This value is excluded.";feedback.signal("CP_END");checkpoint()}else phaseChanged()}
        if(e.state==State.FINISHED){finish("Completed");return}
        if(e.total(now)>21600){finish("Ended early");return}
        voice?.let{try{audioPosition=it.currentPosition}catch(_:Exception){}}
        if(now-checkpointTime>=10000){checkpoint();checkpointTime=now}
        val content=notificationText();if(content!=lastNotification){getSystemService(NotificationManager::class.java).notify(19,notification());lastNotification=content;updateMedia()}
        handler.postDelayed(this,200)
    }}
    private fun holdWake(){if(wake?.isHeld!=true)wake=(getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Upekkhā:practice").apply{acquire(6*60*60*1000L)}}
    private fun releaseWake(){if(wake?.isHeld==true)wake?.release();wake=null}
    private fun requestFocus():Boolean{if(focus==null)focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setOnAudioFocusChangeListener{change->if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){try{ambience?.pause()}catch(_:Exception){};if(engine?.state !in listOf(State.PAUSED,State.CP_REVIEW,State.FINISHED)){pause();note="Playback interrupted. Resume when you are ready."}}else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){voice?.setVolume(.3f,.3f);ambience?.setVolume(.1f,.1f)}else if(change==AudioManager.AUDIOFOCUS_GAIN){voice?.setVolume(1f,1f)}}.build();return audioManager.requestAudioFocus(focus!!)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED}
    private fun phaseChanged(){val e=engine ?:return
        if(e.state==State.FINISHED){finish("Completed");return}
        activePhase=e.index;voice?.release();voice=null;audioPosition=0;note=""
        if(e.phase.kind==Kind.LISTEN){ambience?.release();ambience=null;startRecording()}else configureAmbience()
        cue(e.phase.kind.name);checkpoint();updateMedia();getSystemService(NotificationManager::class.java).notify(19,notification())
    }
    private fun startRecording(){try{
        val granted=requestFocus()
        voice=MediaPlayer().apply{setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());assets.openFd("audio/mindfulness.mp3").use{setDataSource(it.fileDescriptor,it.startOffset,it.length)};setWakeMode(this@PracticeService,PowerManager.PARTIAL_WAKE_LOCK);setOnCompletionListener{if(!ending && engine?.state==State.LISTENING){engine?.advance(SystemClock.elapsedRealtime());phaseChanged()}};setOnErrorListener{_,_,_->pause();note="The recording could not be played. Resume to retry, or end the session.";true};prepare();audioDuration=duration;setVolume(1f,1f);if(granted)start()}
        if(!granted){engine?.pause(SystemClock.elapsedRealtime());releaseWake();note="Audio focus is unavailable. Resume when other playback has finished."}
    }catch(e:Exception){voice?.release();voice=null;engine?.pause(SystemClock.elapsedRealtime());releaseWake();note="The recording could not be opened: ${e.message}"}}
    private fun cue(event:String){voice?.let{try{it.setVolume(.3f,.3f)}catch(_:Exception){}};feedback.signal(event);handler.postDelayed({voice?.let{try{it.setVolume(1f,1f)}catch(_:Exception){}}},1500)}
    fun pause(){val e=engine ?:return;if(ending || e.state==State.FINISHED || e.state==State.CP_REVIEW)return
        if(e.state==State.PAUSED){e.resume(SystemClock.elapsedRealtime());holdWake();if(e.phase.kind==Kind.LISTEN){if(voice==null)startRecording()else if(requestFocus())voice?.start()else{e.pause(SystemClock.elapsedRealtime());note="Audio focus is unavailable.";releaseWake()}}else ambience?.start()}
        else{e.pause(SystemClock.elapsedRealtime());try{voice?.pause()}catch(_:Exception){};try{ambience?.pause()}catch(_:Exception){};releaseWake();if(e.state==State.CP_REVIEW){note="CP interrupted. The value will be excluded.";cue("CP_END")}}
        checkpoint();updateMedia()
    }
    fun startCp(){val e=engine ?:return;if(e.state!=State.CP_READY || ending)return;e.startCp(SystemClock.elapsedRealtime());note="";checkpoint();updateMedia()}
    fun stopCp(){val e=engine ?:return;if(e.state!=State.CP_RUNNING)return;e.stopCp(SystemClock.elapsedRealtime());cue("CP_END");checkpoint();updateMedia()}
    fun backgroundCp(){val e=engine ?:return;if(e.state==State.CP_RUNNING){e.stopCp(SystemClock.elapsedRealtime(),true);note="The app left the screen during CP. The value is excluded.";cue("CP_END");checkpoint();updateMedia()}}
    fun review(valid:Boolean){val e=engine ?:return;if(e.state!=State.CP_REVIEW || ending)return;if(e.pendingCp>0)cps.add(CpValue(e.pendingCp,System.currentTimeMillis(),valid && !e.interrupted,"CP ${cps.size+1}"));e.advance(SystemClock.elapsedRealtime());holdWake();phaseChanged()}
    fun skip(){val e=engine ?:return;if(e.state==State.CP_RUNNING || ending)return;if(e.state==State.PAUSED)e.resume(SystemClock.elapsedRealtime());e.advance(SystemClock.elapsedRealtime());holdWake();phaseChanged()}
    fun seek(position:Int){if(engine?.phase?.kind!=Kind.LISTEN || voice==null || ending)return;val p=position.coerceIn(0,(audioDuration-500).coerceAtLeast(0));voice?.seekTo(p);audioPosition=p;checkpoint()}
    fun remotePrimary(){when(engine?.state){State.CP_READY->startCp();State.CP_RUNNING->stopCp();State.CP_REVIEW->review(!engine!!.interrupted);State.TIMED,State.LISTENING,State.PAUSED->pause();else->{}}}
    fun addPulse(bpm:Int){require(bpm in 20..250);pulses.add(PulseValue(bpm,System.currentTimeMillis(),engine?.phase?.title ?:"Practice"));checkpoint()}
    private fun entry(status:String):Entry{val e=engine!!;val values=cps.toMutableList();if(e.state==State.CP_REVIEW && e.pendingCp>0)values.add(CpValue(e.pendingCp,System.currentTimeMillis(),false,"Unreviewed CP"));return Entry(recordId,began,e.protocol.name,e.total(SystemClock.elapsedRealtime()),status,values,pulses.toList(),comment=if(e.protocol.phases.any{it.kind==Kind.LISTEN})"Recording: Mindfulness Buteyko — Marilleke Vrancken. Post-recording rest and wait are separate protocol phases."else"",context=if(e.protocol.id=="morning")"Morning"else"Practice")}
    private fun checkpoint(){if(engine!=null && !finished)try{Store.write(store.active,Codec.entry(entry("Interrupted")).toString())}catch(e:Exception){failure="Could not save checkpoint: ${e.message}"}}
    fun finish(status:String){if(ending || engine==null)return;ending=true
        try{val data=entry(status);store.upsert(data);finished=true;store.active.delete();lastResult=data.id}catch(e:Exception){failure="Could not save session: ${e.message}";ending=false;checkpoint();ending=true}
        voice?.release();voice=null;ambience?.release();ambience=null;focus?.let{audioManager.abandonAudioFocusRequest(it)};releaseWake();handler.removeCallbacks(ticker);feedback.signal("END");updateMedia();getSystemService(NotificationManager::class.java).notify(19,notification());handler.postDelayed({if(live===this)live=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()},2200)
    }
    fun configureAmbience(){ambience?.release();ambience=null;val e=engine ?:return;if(e.phase.kind==Kind.LISTEN || ending)return;val selected=store.str("ambience","Off");if(selected=="Off")return
        try{if(!requestFocus())return;val id=resources.getIdentifier(if(selected=="Wind")"wind"else"fireplace","raw",packageName);ambience=MediaPlayer().apply{setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());resources.openRawResourceFd(id).use{setDataSource(it.fileDescriptor,it.startOffset,it.length)};isLooping=true;prepare();val v=store.int("volume",30)/100f;setVolume(v,v);if(e.state!=State.PAUSED)start()}}catch(ex:Exception){note="Ambience is unavailable. ${ex.message}"}
    }
    private fun notificationText():String{if(ending)return "Session saved";val e=engine ?:return "Starting session";return e.phase.title+" · "+when(e.state){State.TIMED->clock((e.remaining(SystemClock.elapsedRealtime())+999)/1000);State.LISTENING->clock((audioDuration-audioPosition).coerceAtLeast(0)/1000L);State.CP_READY->"Ready";State.CP_RUNNING->"End at the first urge";State.CP_REVIEW->"Review measurement";State.PAUSED->"Paused";else->"Finished"}}
    private fun updateMedia(){val e=engine;val running=e?.state in listOf(State.TIMED,State.LISTENING,State.CP_RUNNING);media.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,e?.phase?.title ?:"Upekkhā").putString(MediaMetadata.METADATA_KEY_ARTIST,"Upekkhā").putLong(MediaMetadata.METADATA_KEY_DURATION,if(e?.phase?.kind==Kind.LISTEN)audioDuration.toLong()else -1L).build());media.setPlaybackState(PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SEEK_TO).setState(if(ending)PlaybackState.STATE_STOPPED else if(running)PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,if(e?.phase?.kind==Kind.LISTEN)audioPosition.toLong()else 0,if(running)1f else 0f).build())}
    private fun notification():Notification{
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(s:String,n:Int)=PendingIntent.getService(this,n,Intent(this,PracticeService::class.java).setAction(s),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val e=engine;val title=when(e?.state){State.CP_READY->"Start CP";State.CP_RUNNING->"Stop CP";State.CP_REVIEW->if(e.interrupted)"Continue"else"Keep CP";State.PAUSED->"Resume";else->"Pause"}
        return Notification.Builder(this,"practice").setSmallIcon(resources.getIdentifier("ic_notification","drawable",packageName)).setContentTitle("Upekkhā").setContentText(notificationText()).setContentIntent(open).setOngoing(!ending).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).setStyle(Notification.MediaStyle().setMediaSession(media.sessionToken).setShowActionsInCompactView(0,1)).addAction(Notification.Action.Builder(null,title,action("PRIMARY",1)).build()).addAction(Notification.Action.Builder(null,if(e?.state==State.CP_REVIEW)"Exclude"else"End",action(if(e?.state==State.CP_REVIEW)"EXCLUDE"else"END",2)).build()).build()
    }
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);if(!finished)checkpoint();voice?.release();ambience?.release();focus?.let{audioManager.abandonAudioFocusRequest(it)};releaseWake();media.release();feedback.release();if(live===this)live=null;super.onDestroy()}
    companion object{var live:PracticeService?=null;var lastResult:String?=null;var failure:String?=null;fun clock(s:Long)="%02d:%02d".format(s.coerceAtLeast(0)/60,s.coerceAtLeast(0)%60)}
}
