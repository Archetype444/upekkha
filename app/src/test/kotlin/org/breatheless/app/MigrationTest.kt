package org.breatheless.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.nikka.data.Store as MeditationStore
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.sit.Sitting
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31],manifest=Config.NONE)
class MigrationTest {
    private lateinit var context: Context
    private val sample=Entry("old-entry",1760000000000L,"Morning CP",315,"Completed",listOf(CpValue(14.8,1760000000000L,true,"CP 1"),CpValue(40.0,1760000000000L,false,"Excluded")),listOf(PulseValue(62,1760000000000L,"Before practice")),listOf(WalkValue(5,3.5,"Manual")),2,"Original note","Morning")
    @Before fun reset() {
        context=ApplicationProvider.getApplicationContext()
        for (clazz in listOf(Store::class.java,MeditationStore::class.java)) {
            val field=clazz.getDeclaredField("instance");field.isAccessible=true
            (field.get(null) as? android.database.sqlite.SQLiteOpenHelper)?.close();field.set(null,null)
        }
        context.databaseList().forEach { context.deleteDatabase(it) }
        context.filesDir.listFiles().orEmpty().forEach { it.deleteRecursively() }
        context.getSharedPreferences("nikka",0).edit().clear().commit()
        PracticeService.live=null;PracticeService.active.value=false;PracticeService.result.value=null
        Sitting.load(context)
    }
    private fun legacy()=JSONObject().put("format","BreatheLess").put("schemaVersion",1).put("entries",JSONArray().put(Codec.entry(sample))).put("protocols",JSONArray()).put("settings",JSONObject().put("brightness",5).put("sound",false))
    @Test fun oldOnDiskDataLoadsWithoutLosingMeasurements() {
        File(context.filesDir,"breatheless.json").writeText(legacy().toString())
        val s=Store.get(context);assertEquals(sample,s.entries.single());assertFalse(s.settings.has("brightness"));assertFalse(s.bool("sound"))
    }
    @Test fun combinedBackupRestoresBothStoresAndPreferences() {
        val s=Store.get(context);s.upsert(sample);s.settings.put("fxChannel","Notification").put("freestyle",Codec.freestyle(Freestyle(rest=37))).put("morningReminder",true).put("reminderHour",6).put("reminderMinute",17);s.save()
        val m=MeditationStore.get(context)
        m.addSession(sample.timestamp,1200,1199,"Historic sitting note")
        val habit=m.addHabit("Read",4,"After breakfast")
        m.setTick(habit,LocalDate.of(2026,11,1),true)
        Prefs(context).minutes=37;Prefs(context).breathCue=2
        val raw=UnifiedBackup.write(context)
        m.replaceAll(com.wanderwildwood.nikka.data.Backup.Contents(listOf(com.wanderwildwood.nikka.data.Backup.HabitWithTicks(com.wanderwildwood.nikka.data.Habit(0,"",7,true,0),emptySet())),emptyList()))
        s.entries.clear();s.save();Prefs(context).minutes=20
        UnifiedBackup.restore(context,raw)
        assertEquals(sample,s.entries.single());assertEquals(1199,m.sessions().single().satSec);assertTrue(m.habits().any{it.name=="Read" && it.cue=="After breakfast"});assertEquals(37,s.freestyle().rest);assertEquals(37,Prefs(context).minutes);assertEquals(2,Prefs(context).breathCue);assertEquals("Notification",s.str("fxChannel","Alarm"));assertEquals(17,s.settings.getInt("reminderMinute"))
    }
    @Test fun oldBackupMergesWithoutErasingMeditationOrDuplicatingEntries() {
        val m=MeditationStore.get(context);m.addSession(sample.timestamp,60,60,"")
        UnifiedBackup.restore(context,legacy().toString());UnifiedBackup.restore(context,legacy().toString())
        assertEquals(1,Store.get(context).entries.size);assertEquals(1,m.sessions().size)
    }
    @Test fun malformedCombinedBackupLeavesBothStoresUnchanged() {
        val m=MeditationStore.get(context);m.addSession(sample.timestamp,60,60,"")
        val s=Store.get(context);s.upsert(sample)
        val raw=JSONObject(UnifiedBackup.write(context));raw.getJSONObject("practice").getJSONArray("sittings").getJSONObject(0).put("sat_seconds",-1)
        try { UnifiedBackup.restore(context,raw.toString());fail("Malformed backup accepted") } catch (_:IllegalArgumentException) { }
        assertEquals(sample,s.entries.single());assertEquals(60,m.sessions().single().satSec)
    }
}
