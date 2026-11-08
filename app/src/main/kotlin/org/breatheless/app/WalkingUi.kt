package org.breatheless.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.nikka.ui.*
import kotlinx.coroutines.delay

@Composable
fun WalkingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    var permitted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) }
    var sensorActive by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    val values = remember { mutableStateListOf<WalkValue>() }
    val began = remember { SystemClock.elapsedRealtime() }
    val timestamp = remember { System.currentTimeMillis() }
    val id = remember { Store.id() }
    var holding by remember { mutableStateOf(false) }
    var holdStart by remember { mutableLongStateOf(0) }
    var steps by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(began) }
    var saved by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    fun release() {
        if (!holding) return
        values += WalkValue(steps, (SystemClock.elapsedRealtime() - holdStart) / 1000.0, if (sensorActive) "Step sensor" else "Manual")
        holding = false
    }
    fun save(): Boolean {
        if (saved) return true
        release()
        return runCatching {
            if (values.isNotEmpty()) store.upsert(Entry(id, timestamp, "Walking exercise", ((SystemClock.elapsedRealtime() - began) / 1000).toInt(), "Saved", walks = values.toList(), context = "Walking"))
            saved = true
            true
        }.getOrElse { problem = it.message; false }
    }
    BackHandler { if (save()) onBack() }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = SystemClock.elapsedRealtime(); if (now - began >= 12 * 60 * 1000 && !saved) { if (save()) onBack() } } }
    DisposableEffect(sensor, permitted) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { if (holding && !saved) steps++ }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }
        sensorActive = sensor != null && permitted && runCatching { sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL) }.getOrDefault(false)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    DisposableEffect(Unit) { onDispose { if (!saved && values.isNotEmpty()) runCatching { store.upsert(Entry(id, timestamp, "Walking exercise", ((SystemClock.elapsedRealtime() - began) / 1000).toInt(), "Interrupted", walks = values.toList(), context = "Walking")) } } }
    PagedColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) {
        item { Heading("Walking exercise") }
        item { TextMMD(text = "Walk on level ground. After a normal exhalation, begin a gentle hold and count steps. Release at the first urge to breathe, then recover fully before another hold. Stop if you feel unwell.", style = MaterialTheme.typography.bodySmall) }
        item { TextMMD(text = PracticeService.clock((now - began) / 1000), modifier = Modifier.padding(vertical = 12.dp)) }
        item { TextMMD(text = if (holding) "$steps steps" else "${values.size} holds completed", style = MaterialTheme.typography.headlineMedium) }
        if (sensor != null && !permitted) item { DialogButton("Enable step sensor") { permission.launch(Manifest.permission.ACTIVITY_RECOGNITION) } }
        item { TextMMD(text = if (sensorActive) "Steps are counted by the phone." else "Count each step with the button below.", style = MaterialTheme.typography.labelSmall) }
        if (holding) {
            if (!sensorActive) item { DialogButton("Add step", true) { steps++ } }
            item { DialogButton("Release and recover", true, ::release) }
        } else item { DialogButton("Begin hold", true) { steps = 0; holdStart = SystemClock.elapsedRealtime(); holding = true } }
        values.forEachIndexed { index, walk -> item { SettingRow("Hold ${index + 1}", "${walk.steps} steps · ${cpLabel(walk.seconds)}", null) } }
        problem?.let { item { TextMMD(text = it) } }
        item { DialogButton("End and save") { if (save()) onBack() } }
    }
}
