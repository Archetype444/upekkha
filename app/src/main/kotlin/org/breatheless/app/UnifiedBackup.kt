package org.breatheless.app

import android.content.Context
import com.wanderwildwood.nikka.data.Backup as MeditationBackup
import com.wanderwildwood.nikka.data.Store as MeditationStore
import com.wanderwildwood.nikka.sit.Breathing
import com.wanderwildwood.nikka.sit.Sitting
import org.json.JSONObject
import java.io.InputStream

object UnifiedBackup {
    private const val MAX_BYTES = 32 * 1024 * 1024
    private val integers = mapOf("minutes" to 1..600, "warmup" to 0..120, "interval" to 0..30,
        "breath_in" to 1..20, "breath_hold" to 0..20, "breath_out" to 1..20, "breath_rest" to 0..20,
        "breath_minutes" to 1..15, "breath_cue" to 0..3, "breathe_first" to 0..5)
    private val booleans = setOf("breathe", "show_streaks", "dnd")
    fun readBounded(input: InputStream): String {
        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val count = input.read(buffer); if (count < 0) break; require(out.size() + count <= MAX_BYTES) { "Backup exceeds 32 MB." }; out.write(buffer, 0, count) }
        return out.toString("UTF-8")
    }
    private fun validatePreferences(source: JSONObject): JSONObject {
        val result = JSONObject()
        for ((key, range) in integers) if (source.has(key)) { val n = source.getInt(key); require(n in range) { "Invalid $key." }; result.put(key, n) }
        for (key in booleans) if (source.has(key)) { require(source.get(key) is Boolean); result.put(key, source.getBoolean(key)) }
        return result
    }
    fun write(context: Context): String {
        val prefs = context.getSharedPreferences("nikka", Context.MODE_PRIVATE)
        val settings = JSONObject()
        integers.keys.forEach { if (prefs.contains(it)) settings.put(it, prefs.getInt(it, 0)) }
        booleans.forEach { if (prefs.contains(it)) settings.put(it, prefs.getBoolean(it, false)) }
        return JSONObject().put("format", "Upekkha").put("schemaVersion", 3)
            .put("practice", JSONObject(MeditationBackup.write(MeditationStore.get(context))))
            .put("buteyko", Store.get(context).json()).put("preferences", settings).toString(2)
    }
    fun restore(context: Context, text: String) {
        require(text.toByteArray().size <= MAX_BYTES) { "Backup exceeds 32 MB." }
        check(PracticeService.live == null && Sitting.running.value == null && Breathing.running.value == null) { "End the current practice before restoring a backup." }
        val root = JSONObject(text)
        val legacy = Store.get(context); val meditation = MeditationStore.get(context)
        when (root.optString("format")) {
            "BreatheLess" -> { legacy.restore(text); MorningReminder.schedule(context); return }
            "nikka-backup" -> { meditation.replaceAll(MeditationBackup.read(text)); return }
            "Upekkha" -> require(root.getInt("schemaVersion") == 3) { "Unsupported Upekkhā backup version." }
            else -> error("This is not a supported practice backup.")
        }
        // Parse every section before either store is changed.
        val practice = MeditationBackup.read(root.getJSONObject("practice").toString())
        val buteyko = root.getJSONObject("buteyko").toString()
        Codec.parse(buteyko)
        Codec.settings(JSONObject(buteyko).getJSONObject("settings"), JSONObject())
        val preferences = validatePreferences(root.optJSONObject("preferences") ?: JSONObject())
        val oldPractice = MeditationBackup.read(MeditationBackup.write(meditation))
        val oldButeyko = legacy.json().toString()
        try {
            meditation.replaceAll(practice)
            legacy.replace(buteyko)
        } catch (e: Exception) {
            runCatching { meditation.replaceAll(oldPractice) }; runCatching { legacy.replace(oldButeyko) }
            throw e
        }
        val editor = context.getSharedPreferences("nikka", Context.MODE_PRIVATE).edit()
        integers.keys.forEach { if (preferences.has(it)) editor.putInt(it, preferences.getInt(it)) else editor.remove(it) }
        booleans.forEach { if (preferences.has(it)) editor.putBoolean(it, preferences.getBoolean(it)) else editor.remove(it) }
        editor.remove("just_ended").apply()
        MorningReminder.schedule(context)
    }
}
