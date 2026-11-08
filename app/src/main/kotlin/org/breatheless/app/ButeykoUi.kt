package org.breatheless.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.nikka.sit.Breathing
import com.wanderwildwood.nikka.sit.Sitting
import com.wanderwildwood.nikka.ui.*
import kotlinx.coroutines.delay
import java.util.Locale

object ButeykoActions {
    fun start(context: Context, protocol: Protocol, pulse: Int? = null) {
        check(PracticeService.live == null && Sitting.running.value == null && Breathing.running.value == null) { "A practice is already running." }
        check(Store.get(context).error == null) { Store.get(context).error ?: "Stored data is unavailable." }
        Codec.readProtocol(Codec.protocol(protocol))
        PracticeService.failure = null
        context.startForegroundService(Intent(context, PracticeService::class.java).setAction("START")
            .putExtra("protocol", Codec.protocol(protocol).toString()).putExtra("pulse", pulse ?: 0))
    }
}

fun secondsLabel(value: Int) = when { value == 0 -> "None"; value % 60 == 0 -> "${value / 60} min"; else -> "$value sec" }
fun cpLabel(value: Double) = String.format(Locale.getDefault(), "%.1f s", value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticePage(title: String, onBack: (() -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    if (onBack != null) BackHandler(onBack = onBack)
    Scaffold(containerColor = MaterialTheme.colorScheme.surface, topBar = {
        TopAppBarMMD(title = { TextMMD(text = title) }, navigationIcon = { if (onBack != null) BarButton(Icons.Back, "Back", onBack) })
    }, content = content)
}

@Composable
fun ButeykoTab() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    var page by remember { mutableStateOf("home") }
    var pending by remember { mutableStateOf<Protocol?>(null) }
    var safety by remember { mutableStateOf(false) }
    var pulse by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    fun launch(protocol: Protocol, bpm: Int? = null) { runCatching { ButeykoActions.start(context, protocol, bpm) }.onFailure { problem = it.message } }
    fun afterSafety() { if (pending?.id != "morning" && store.bool("pulsePrompt", false)) pulse = true else pending?.let { launch(it) } }
    fun request(protocol: Protocol) { pending = protocol; if (!store.bool("safetyAccepted", false)) safety = true else afterSafety() }
    BackHandler(enabled = page != "home") { page = "home" }
    when (page) {
        "freestyle" -> FreestyleScreen(onBack = { page = "home" }, onStart = ::request, onWalking = { page = "walking" })
        "walking" -> WalkingScreen { page = "freestyle" }
        else -> PagedColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp)) {
            item { SettingRow("Morning CP", null) { request(Presets.morning) } }
            item { DottedRule() }
            item { SettingRow("Mindfulness Buteyko", null) { request(Presets.listening) } }
            item { DottedRule() }
            item { SettingRow("Freestyle", "Set up your practice") { page = "freestyle" } }
            if (store.error != null) item { TextMMD(text = store.error.orEmpty()) }
        }
    }
    if (safety) SafetyDialog(onDismiss = { safety = false }) { store.settings.put("safetyAccepted", true); store.save(); safety = false; afterSafety() }
    if (pulse) PulseDialog(onDismiss = { pulse = false }) { bpm -> pulse = false; pending?.let { launch(it, bpm) } }
    problem?.let { ErrorDialog(it) { problem = null } }
}

@Composable
fun FreestyleScreen(onBack: () -> Unit, onStart: (Protocol) -> Unit, onWalking: () -> Unit) {
    val store = Store.get(LocalContext.current)
    var config by remember { mutableStateOf(store.freestyle()) }
    var edit by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    fun set(next: Freestyle) { runCatching { next.protocol(); config = next; store.settings.put("freestyle", Codec.freestyle(next)); store.save() }.onFailure { problem = it.message ?: "Check the practice length." } }
    PagedColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp)) {
        item { SettingRow("Back", null, onBack) }
        item { Heading("Freestyle") }
        item { DialogButton("Start practice", filled = true) { onStart(config.protocol()) } }
        item { SettingRow("Initial rest", secondsLabel(config.rest)) { edit = "Initial rest" } }
        item { SwitchRow("Initial CP", config.initialCp) { set(config.copy(initialCp = it)) } }
        item { SettingRow("Reduced breathing", secondsLabel(config.reduce)) { edit = "Reduced breathing" } }
        item { SettingRow("Rounds", config.rounds.toString()) { edit = "Rounds" } }
        item { SettingRow("Recovery between rounds", secondsLabel(config.recovery)) { edit = "Recovery between rounds" } }
        item { SwitchRow("CP between rounds", config.betweenCp) { set(config.copy(betweenCp = it)) } }
        item { SettingRow("Final recovery", secondsLabel(config.finalRecovery)) { edit = "Final recovery" } }
        item { SwitchRow("Final CP", config.finalCp) { set(config.copy(finalCp = it)) } }
        item { DottedRule() }
        item { SettingRow("Walking exercise", "Count steps during gentle holds", onWalking) }
    }
    val field = edit
    if (field == "Rounds") OptionsDialog("Rounds", (1..12).map { it to it.toString() }, config.rounds, { set(config.copy(rounds = it)) }, { edit = null })
    else if (field != null) {
        val current = when (field) { "Initial rest" -> config.rest; "Reduced breathing" -> config.reduce; "Final recovery" -> config.finalRecovery; else -> config.recovery }
        SecondsDialog(field, current, field != "Reduced breathing", { edit = null }) { seconds ->
            set(when (field) { "Initial rest" -> config.copy(rest = seconds); "Reduced breathing" -> config.copy(reduce = seconds); "Final recovery" -> config.copy(finalRecovery = seconds); else -> config.copy(recovery = seconds) }); edit = null
        }
    }
    problem?.let { ErrorDialog(it) { problem = null } }
}

@Composable
fun SecondsDialog(title: String, current: Int, allowZero: Boolean, onDismiss: () -> Unit, onSet: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    var error by remember { mutableStateOf(false) }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = title, fontWeight = FontWeight.Bold)
        Field("Seconds", value, { value = it.filter(Char::isDigit).take(4) }, number = true)
        TextMMD(text = if (allowZero) "5–3600 seconds, or 0 to leave this block out." else "5–3600 seconds.", style = MaterialTheme.typography.labelSmall)
        if (error) TextMMD(text = "Check the duration.")
        DialogButtons("Cancel", "Set", onDismiss, {
            val n = value.toIntOrNull()
            if (n == null || !(n in 5..3600 || allowZero && n == 0)) error = true else onSet(n)
        })
    }
}

@Composable
fun ButeykoSessionScreen() {
    val update by PracticeService.updates.collectAsState()
    val service = PracticeService.live
    val engine = remember(service, update) { service?.engine }
    if (service == null || engine == null) { TextMMD(text = "Starting practice…"); return }
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var end by remember { mutableStateOf(false) }
    LaunchedEffect(service) { while (true) { delay(1000); now = SystemClock.elapsedRealtime() } }
    BackHandler { (context as? Activity)?.moveTaskToBack(true) }
    DisposableEffect(engine.state) {
        val window = (context as? Activity)?.window
        if (engine.state == State.CP_RUNNING) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    PracticePage(engine.protocol.name) { padding ->
        PagedColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
            item { TextMMD(text = engine.phase.title, style = MaterialTheme.typography.titleLarge) }
            item { TextMMD(text = "${engine.index + 1} of ${engine.protocol.phases.size}", style = MaterialTheme.typography.labelSmall) }
            item {
                val face = when (engine.state) {
                    State.CP_READY -> "Ready"
                    State.CP_RUNNING -> if (store.bool("hideCp")) "End at the first urge" else cpLabel(engine.cpElapsed(now))
                    State.CP_REVIEW -> cpLabel(engine.pendingCp)
                    State.LISTENING -> "Playing"
                    State.TIMED -> PracticeService.clock((engine.remaining(now) + 999) / 1000)
                    State.PAUSED -> "Interrupted"
                    State.FINISHED -> "Finished"
                }
                TextMMD(text = face, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp))
            }
            item { TextMMD(text = engine.phase.guidance, style = MaterialTheme.typography.bodySmall) }
            if (service.note.isNotBlank()) item { TextMMD(text = service.note, modifier = Modifier.padding(vertical = 12.dp)) }
            when (engine.state) {
                State.CP_READY -> item { DialogButton("Start CP", true) { service.startCp() } }
                State.CP_RUNNING -> item { DialogButton("Stop CP", true) { service.stopCp() } }
                State.CP_REVIEW -> {
                    item { TextMMD(text = if (engine.interrupted) "This interrupted measurement will be excluded." else "The next breath should feel easy and natural. Exclude the value if you had to gasp or strain.", style = MaterialTheme.typography.bodySmall) }
                    if (!engine.interrupted) item { DialogButton("Keep measurement", true) { service.review(true) } }
                    item { DialogButton(if (engine.interrupted) "Continue" else "Exclude measurement") { service.review(false) } }
                }
                State.PAUSED -> item { DialogButton(if (engine.phase.kind == Kind.LISTEN) "Resume recording" else "Continue practice", true) { service.pause() } }
                else -> Unit
            }
            item { Spacer(Modifier.height(18.dp)); DialogButton("End and save") { end = true } }
        }
    }
    if (end) EInkDialog(onDismiss = { end = false }) {
        TextMMD(text = "End this practice and save the measurements taken so far?")
        DialogButtons("Continue", "End and save", { end = false }, { end = false; service.finish("Ended early") })
    }
}

@Composable
fun ButeykoEndScreen(id: String) {
    val store = Store.get(LocalContext.current)
    val version by store.version.collectAsState()
    val entry = remember(id, version) { store.entries.find { it.id == id } }
    var pulse by remember(id) { mutableStateOf("") }
    var symptom by remember(id) { mutableStateOf(entry?.symptom?.toString().orEmpty()) }
    var counter by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun save() {
        val bpm = pulse.trim().toIntOrNull(); val score = symptom.trim().toIntOrNull()
        if (pulse.isNotBlank() && (bpm == null || bpm !in 20..250)) { error = "Pulse must be 20–250 bpm."; return }
        if (symptom.isNotBlank() && (score == null || score !in 0..10)) { error = "Symptom score must be 0–10."; return }
        if (entry != null) runCatching { store.upsert(entry.copy(pulses = entry.pulses + listOfNotNull(bpm?.let { PulseValue(it, System.currentTimeMillis(), "After practice") }), symptom = score)) }.onFailure { error = it.message; return }
        PracticeService.lastResult = null; PracticeService.result.value = null
    }
    BackHandler { save() }
    PracticePage("Practice saved") { padding ->
        PagedColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
            item { TextMMD(text = entry?.name ?: "Practice", style = MaterialTheme.typography.titleLarge) }
            entry?.cps?.forEach { cp -> item { SettingRow(cp.label, cpLabel(cp.seconds) + if (cp.valid) "" else " · excluded", null) } }
            if (store.bool("pulsePrompt", false)) {
                item { Field("Pulse after practice (optional)", pulse, { pulse = it.filter(Char::isDigit).take(3) }, number = true) }
                item { DialogButton("Count pulse") { counter = true } }
            }
            item { Field("Symptom score (optional, 0–10)", symptom, { symptom = it.filter(Char::isDigit).take(2) }, number = true) }
            item { TextMMD(text = "0 means no symptoms; 10 means the worst symptoms for you.", style = MaterialTheme.typography.labelSmall) }
            error?.let { item { TextMMD(text = it) } }
            item { DialogButton("Save and close", true, ::save) }
        }
    }
    if (counter) PulseCounter(onDismiss = { counter = false }) { pulse = it.toString(); counter = false }
}

@Composable
fun PulseDialog(onDismiss: () -> Unit, onDone: (Int?) -> Unit) {
    var pulse by remember { mutableStateOf("") }; var counter by remember { mutableStateOf(false) }; var error by remember { mutableStateOf(false) }
    EInkDialog(onDismiss = onDismiss) {
        Field("Pulse before practice (optional)", pulse, { pulse = it.filter(Char::isDigit).take(3) }, number = true)
        DialogButton("Count pulse") { counter = true }
        if (error) TextMMD(text = "Pulse must be 20–250 bpm.")
        DialogButtons("Cancel", "Begin", onDismiss, {
            val bpm = pulse.toIntOrNull()
            if (pulse.isNotBlank() && (bpm == null || bpm !in 20..250)) error = true else onDone(bpm)
        })
    }
    if (counter) PulseCounter(onDismiss = { counter = false }) { pulse = it.toString(); counter = false }
}

@Composable
fun PulseCounter(onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var began by remember { mutableLongStateOf(0) }; var beats by remember { mutableIntStateOf(0) }; var left by remember { mutableIntStateOf(30) }
    LaunchedEffect(began) { if (began > 0) while (left > 0) { delay(250); left = (30 - (SystemClock.elapsedRealtime() - began) / 1000).toInt().coerceAtLeast(0) } }
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = if (began == 0L) "Tap once for each pulse beat. The first tap begins a 30-second count." else "$left seconds · $beats beats")
        if (left > 0) DialogButton("Beat", true) { if (began == 0L) began = SystemClock.elapsedRealtime(); beats++ }
        else { TextMMD(text = "${beats * 2} bpm"); if (beats * 2 in 20..250) DialogButton("Use pulse", true) { onDone(beats * 2) } else TextMMD(text = "The count is outside 20–250 bpm. Close and try again.") }
        DialogButton("Close", onClick = onDismiss)
    }
}

@Composable
fun SafetyDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(text = "Before practising", fontWeight = FontWeight.Bold)
        PagedColumn(Modifier.fillMaxWidth().heightIn(max = 230.dp)) {
            item { TextMMD(text = "A Control Pause ends at the first clear urge to breathe, swallow or involuntary movement after a normal exhalation. The next breath should be easy. It is not a maximum breath hold.", style = MaterialTheme.typography.bodySmall) }
            item { TextMMD(text = "Keep reduced breathing gentle. Stop and breathe normally if you feel dizzy, distressed, faint or short of breath. Never practise holds in water, while driving or near a fall hazard.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
            item { TextMMD(text = "Ask your clinician before breath-hold or reduced-breathing practice if you are pregnant, have a heart or lung condition, seizures, fainting episodes or another serious illness. Follow your asthma action plan and prescribed treatment. CP is a personal tracking measure, not a diagnosis or an oxygen/CO₂ measurement.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
        }
        DialogButtons("Back", "Understood", onDismiss, onAccept)
    }
}

@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) { EInkDialog(onDismiss = onDismiss) { TextMMD(text = message); DialogButton("Close", onClick = onDismiss) } }
