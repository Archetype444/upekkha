package org.breatheless.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.data.Session
import com.wanderwildwood.nikka.ui.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TrackingRow(val time: Long, val legacy: Entry? = null, val meditation: Session? = null)

@Composable
fun TrackingTab(c: AppState) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsState()
    val entries = remember(version) { store.entries.sortedByDescending { it.timestamp } }
    val sessions = remember(c.version) { c.store.sessions() }
    var days by remember { mutableIntStateOf(30) }
    var morning by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<TrackingRow?>(null) }
    var remove by remember { mutableStateOf<Entry?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var add by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val start = if (days == 0) 0L else c.today.minusDays(days - 1L).atStartOfDay(zone).toInstant().toEpochMilli()
    val visible = entries.filter { it.timestamp >= start }
    val meditation = sessions.filter { it.start >= start }
    val cpEntries = if (morning) visible.filter { it.context == "Morning" } else visible
    val cps = cpEntries.flatMap { it.cps }
    val stats = Analysis.stats(cps) { Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay() }
    val rows = remember(entries, sessions) { (entries.map { TrackingRow(it.timestamp, legacy = it) } + sessions.map { TrackingRow(it.start, meditation = it) }).sortedByDescending { it.time } }
    val format = remember { DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.getDefault()) }
    BackHandler(enabled = history) { history = false }
    PagedColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp)) {
        if (history) {
            item { SettingRow("Back to tracking", null) { history = false } }
            item { Heading("Practice history") }
            item { DialogButton("Add a sitting") { add = true } }
            if (rows.isEmpty()) item { TextMMD(text = "Your completed practices will appear here.") }
            rows.forEach { row -> item {
                SettingRow(row.legacy?.name ?: if (row.meditation?.breathing == true) "Breathing" else "Sitting", format.format(Instant.ofEpochMilli(row.time).atZone(zone))) { selected = row }
            } }
        } else {
            item { Spacer(Modifier.height(10.dp)); HouseChoiceRow(listOf("7", "30", "90", "All"), listOf(7,30,90,0).indexOf(days), { days = listOf(7,30,90,0)[it] }, compact = true) }
            item { TextMMD(text = "${visible.size + meditation.size} practices · ${(visible.sumOf { it.duration } + meditation.sumOf { it.satSec }) / 60} min", modifier = Modifier.padding(vertical = 14.dp)) }
            item { SettingRow("Practice history", "${rows.size} saved practices") { history = true } }
            item { Heading("Control Pause") }
            item { SwitchRow("Morning CP only", morning) { morning = it } }
            if (stats == null) item { TextMMD(text = "Record a CP to see your trend. Excluded measurements are kept in history.", style = MaterialTheme.typography.bodySmall) }
            else {
                item { TextMMD(text = "Average ${cpLabel(stats.average)}\nLowest ${cpLabel(stats.low)} · Best ${cpLabel(stats.best)}") }
                item { TextMMD(text = stats.change?.let { change -> "Trend ${if (change > 0) "+" else ""}${cpLabel(change)}" } ?: "Trend needs measurements on two or more days.", style = MaterialTheme.typography.labelSmall) }
                item { key(stats.points) { AndroidView(factory = { Trend(it, stats.points) }, modifier = Modifier.fillMaxWidth().height(180.dp)) } }
                item { TextMMD(text = "${stats.count} included measurements · daily averages", style = MaterialTheme.typography.labelSmall) }
            }
            val pulses = visible.flatMap { it.pulses }
            if (pulses.isNotEmpty()) item { SettingRow("Average pulse", "${pulses.map { it.bpm }.average().toInt()} bpm", null) }
            val symptoms = visible.mapNotNull { it.symptom }
            if (symptoms.isNotEmpty()) item { SettingRow("Average symptom score", String.format(Locale.getDefault(), "%.1f / 10", symptoms.average()), null) }
            val walks = visible.flatMap { it.walks }
            if (walks.isNotEmpty()) item { SettingRow("Walking", "${walks.size} holds · ${walks.sumOf { it.steps }} steps", null) }
        }
    }
    if (add) SessionDialog(c, null) { add = false }
    selected?.let { row ->
        if (row.meditation != null) SessionDialog(c, row.meditation) { selected = null }
        else row.legacy?.let { entry -> EInkDialog(onDismiss = { selected = null }) {
            TextMMD(text = entry.name, style = MaterialTheme.typography.titleMedium)
            PagedColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                item { TextMMD(text = format.format(Instant.ofEpochMilli(entry.timestamp).atZone(zone)), style = MaterialTheme.typography.labelSmall) }
                item { SettingRow(entry.status, "${entry.duration / 60} min ${entry.duration % 60} sec", null) }
                entry.cps.forEach { cp -> item { SettingRow(cp.label, cpLabel(cp.seconds) + if (cp.valid) "" else " · excluded", null) } }
                entry.holds.forEach { hold -> item { SettingRow(hold.label, cpLabel(hold.seconds) + if(hold.valid) "" else " · excluded", null) } }
                entry.pulses.forEach { pulse -> item { SettingRow(pulse.label, "${pulse.bpm} bpm", null) } }
                entry.walks.forEachIndexed { index, walk -> item { SettingRow("Hold ${index + 1}", "${walk.steps} steps · ${cpLabel(walk.seconds)}", null) } }
                entry.symptom?.let { item { SettingRow("Symptom score", "$it / 10", null) } }
            }
            DialogButton("Delete session") { remove=entry;selected=null }
            DialogButton("Close") { selected = null }
        } }
    }
    remove?.let { entry -> EInkDialog(onDismiss={remove=null}) {
        TextMMD(text="Delete this ${entry.name} session? Its measurements will be removed from tracking.")
        DialogButtons("Cancel","Delete",{remove=null},{runCatching{store.deleteEntry(entry.id);remove=null}.onFailure{problem=it.message}})
    } }
    problem?.let { ErrorDialog(it) { problem=null } }

}
