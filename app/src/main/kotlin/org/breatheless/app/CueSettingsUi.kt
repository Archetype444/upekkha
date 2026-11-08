package org.breatheless.app

import org.json.JSONObject
import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.ui.*

@Composable
fun CueSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsState()
    val config = remember(version) { JSONObject(store.settings.toString()) }
    val channel = remember(version) { store.str("fxChannel", "Alarm") }
    val feedback = remember(channel) { Feedback(context) }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { resumed++; MorningReminder.schedule(context); onPauseOrDispose { } }
    val exact = remember(resumed) { MorningReminder.exactAllowed(context) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    fun set(key: String, value: Any) { runCatching { store.settings.put(key, value); store.save(); if (key.startsWith("reminder") || key == "morningReminder") MorningReminder.schedule(context) }.onFailure { problem = it.message } }
    fun open(intent: Intent) { runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { problem = "The phone could not open this setting." } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> set("morningReminder", granted); if (!granted) problem = "Allow notifications in the phone settings to receive Morning CP reminders." }
    if(dialog=="instructions") { InstructionsScreen { dialog=null }; return }
    // Read preferences after every saved change.
    key(channel) {
        PracticePage("Sound, vibration and CP", onBack) { padding ->
            PagedColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp)) {
                item { SettingRow("Sound effects channel", channel) { dialog = "channel" } }
                item { TextMMD(text = "Bells and phase tones use this channel and its system volume. The recording and ambience use Media.", style = MaterialTheme.typography.labelSmall) }
                item { DialogButton("Test channel") { feedback.tone("bell") } }
                item { Heading("Buteyko phase cues") }
                item { SwitchRow("Sound at phase changes", config.optBoolean("sound",true)) { set("sound", it) } }
                item { SettingRow("Tone volume", "${config.optInt("cueVolume", 45)}%") { dialog = "cueVolume" } }
                item { SwitchRow("Vibration at phase changes", config.optBoolean("haptic",true)) { set("haptic", it) } }
                item { SettingRow("Vibration style", config.optString("vibration", "Double")) { dialog = "vibration" } }
                item { SettingRow("Vibration intensity", "${config.optInt("intensity", 55)}%") { dialog = "intensity" } }
                item { DialogButton("Test vibration") { feedback.vibrate() } }
                if (!feedback.hasVibrator() || !feedback.amplitudeControl()) item {
                    TextMMD(text = if (!feedback.hasVibrator()) "This phone does not report a vibration motor." else "This phone uses a fixed vibration strength. The pattern and off setting still apply.", style = MaterialTheme.typography.labelSmall)
                }
                Signals.events.forEachIndexed { index, event -> item {
                    val tone = config.optString("tone_$event", Signals.defaultTone(event))
                    SettingRow(Signals.labels[index], Signals.tones.first { it.id == tone }.name) { dialog = event }
                } }
                item { Heading("During Buteyko practice") }
                item { SettingRow("Ambience", config.optString("ambience", "Off")) { dialog = "ambience" } }
                item { SettingRow("Ambience volume", "${config.optInt("volume", 30)}%") { dialog = "volume" } }
                item { SwitchRow("Pulse before and after practice", config.optBoolean("pulsePrompt", false)) { set("pulsePrompt", it) } }
                item { SwitchRow("Hide the running CP time", config.optBoolean("hideCp",true)) { set("hideCp", it) } }
                item { SwitchRow("Volume buttons control CP", config.optBoolean("volumeKeys", false), "Either button starts or stops CP. Afterward, volume up keeps the value; volume down excludes it.") { set("volumeKeys", it) } }
                item { Heading("Morning CP reminder") }
                item { SwitchRow("Daily reminder", config.optBoolean("morningReminder", false)) { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else set("morningReminder", enabled)
                } }
                item { SettingRow("Reminder time", "%02d:%02d".format(config.optInt("reminderHour", 8), config.optInt("reminderMinute", 0))) {
                    dialog = "reminderTime"
                } }
                if (!exact) item { SettingRow("Allow exact reminders", "Without this permission Android may delay the reminder.") { open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) } }
                item { SettingRow("Notification settings", null) { open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) } }
                item { SettingRow("Instructions", null) { dialog = "instructions" } }
            }
        }
    }
    when (val choice = dialog) {
        "reminderTime" -> ReminderTimeDialog(store.settings.optInt("reminderHour",8),store.settings.optInt("reminderMinute",0),{dialog=null}) { hour,minute->set("reminderHour",hour);set("reminderMinute",minute);dialog=null }
        "channel" -> OptionsDialog("Sound effects channel", SoundRoute.names.map { it to it }, channel, { set("fxChannel", it) }, { dialog = null })
        "vibration" -> OptionsDialog("Vibration style", Signals.styles.map { it to it }, store.str("vibration", "Double"), { set("vibration", it); feedback.vibrate(it) }, { dialog = null })
        "ambience" -> OptionsDialog("Ambience", listOf("Off", "Wind", "Fireplace").map { it to it }, store.str("ambience", "Off"), { set("ambience", it) }, { dialog = null })
        "cueVolume", "intensity", "volume" -> PercentDialog(when (choice) { "cueVolume" -> "Tone volume"; "intensity" -> "Vibration intensity"; else -> "Ambience volume" }, store.int(choice, 45), { dialog = null }) {
            set(choice, it); if (choice == "intensity") feedback.vibrate(strength = it) else if (choice == "cueVolume") feedback.tone("bell"); dialog = null
        }

        in Signals.events -> {
            val event = choice!!
            val v by store.version.collectAsState()
            val selected = remember(v) { store.str("tone_$event", Signals.defaultTone(event)) }
            EInkDialog(onDismiss = { dialog = null }) {
                TextMMD(text = Signals.labels[Signals.events.indexOf(event)])
                PagedColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    Signals.tones.forEach { tone -> item { ChoiceRow(tone.name, selected == tone.id) { set("tone_$event", tone.id); feedback.tone(tone.id) } } }
                }
                DialogButton("Done") { dialog = null }
            }
        }
        else -> Unit
    }
    problem?.let { ErrorDialog(it) { problem = null } }
}

@Composable
fun PercentDialog(title: String, initial: Int, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    NumberDialog(title,initial,0..100,onDismiss,onSet)
}

@Composable
fun ReminderTimeDialog(initialHour:Int,initialMinute:Int,onDismiss:()->Unit,onSet:(Int,Int)->Unit) {
    var hour by remember { mutableIntStateOf(initialHour) };var minute by remember { mutableIntStateOf(initialMinute) };var pick by remember { mutableStateOf<String?>(null) }
    EInkDialog(onDismiss=onDismiss) {
        TextMMD(text="Reminder time")
        SettingRow("Hour","%02d".format(hour)){pick="hour"}
        SettingRow("Minute","%02d".format(minute)){pick="minute"}
        DialogButtons("Cancel","Set",onDismiss,{onSet(hour,minute)})
    }
    if(pick=="hour") OptionsDialog("Hour",(0..23).map{it to "%02d".format(it)},hour,{hour=it},{pick=null})
    if(pick=="minute") OptionsDialog("Minute",(0..59).map{it to "%02d".format(it)},minute,{minute=it},{pick=null})
}
