package org.breatheless.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.wanderwildwood.nikka.data.Prefs
import com.wanderwildwood.nikka.data.Store as MeditationStore
import com.wanderwildwood.nikka.sit.Sitting
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31],manifest=Config.NONE)
class Version4StorageTest {
    private lateinit var context:Context
    @Before fun prepare() {
        context=ApplicationProvider.getApplicationContext()
        for(clazz in listOf(Store::class.java,MeditationStore::class.java)){val field=clazz.getDeclaredField("instance");field.isAccessible=true;(field.get(null) as? android.database.sqlite.SQLiteOpenHelper)?.close();field.set(null,null)}
        context.filesDir.listFiles().orEmpty().forEach{it.deleteRecursively()};context.databaseList().forEach{context.deleteDatabase(it)}
        context.getSharedPreferences("nikka",0).edit().clear().commit();PracticeService.live=null;Sitting.load(context)
    }
    @Test fun v3FreestyleMigratesWithoutChangingItsSequence() {val s=Store.get(context);val old=Freestyle(30,600,15,300,2,true,true,true);s.settings.put("freestyle",Codec.freestyle(old));s.save();assertEquals(old.protocol().phases,s.freestylePlan().phases)}
    @Test fun customPracticeCanBeSavedEditedRenamedAndDeleted() {val s=Store.get(context);val p=Protocol("custom","Original","",listOf(Phase(Kind.MP_MEDIUM),Phase(Kind.RECOVER,15)),true,2);s.saveTemplate(p);val edit=p.copy(name="Evening",phases=p.phases.reversed());s.saveTemplate(edit);assertEquals(edit,s.protocols.single());assertEquals(edit,Codec.parse(s.json().toString()).second.single());s.deleteTemplate(p.id);assertTrue(s.protocols.isEmpty())}
    @Test fun deletedSessionIsRemovedFromDiskAsWellAsMemory() {val s=Store.get(context);s.upsert(Entry("remove",1760000000000,"Test",30,"Completed"));s.deleteEntry("remove");Store::class.java.getDeclaredField("instance").apply{isAccessible=true;set(null,null)};assertTrue(Store.get(context).entries.isEmpty())}
    @Test fun fullBackupPreservesNewHoldsTemplateOrderRoundsAndShortBell() {
        val s=Store.get(context);val p=Protocol("saved","Afternoon","",listOf(Phase(Kind.MP_FULL),Phase(Kind.VERY_SHALLOW,300),Phase(Kind.PULSE,15)),true,2);s.saveTemplate(p);s.saveFreestyle(p)
        val e=Entry("hold",1760000000000,"Afternoon",100,"Completed",holds=listOf(HoldValue(Kind.MP_FULL,45.0,1760000000000,true,"Full effort")));s.upsert(e);Prefs(context).intervalSec=15
        val raw=UnifiedBackup.write(context);assertEquals(4,JSONObject(raw).getInt("schemaVersion"));s.deleteTemplate(p.id);s.deleteEntry(e.id);Prefs(context).intervalSec=600
        UnifiedBackup.restore(context,raw);assertEquals(p,s.protocols.single());assertEquals(e,s.entries.single());assertEquals(2,s.freestylePlan().rounds);assertEquals(15,Prefs(context).intervalSec)
    }
    @Test fun olderCombinedBackupVersionStillRestores() {val s=Store.get(context);s.upsert(Entry("old",1760000000000,"Old practice",30,"Completed"));val raw=JSONObject(UnifiedBackup.write(context)).put("schemaVersion",3).toString();s.entries.clear();s.save();UnifiedBackup.restore(context,raw);assertEquals("old",s.entries.single().id)}
    @Test fun oldMinuteIntervalMigratesToSeconds() {context.getSharedPreferences("nikka",0).edit().putInt("interval",5).commit();assertEquals(300,Prefs(context).intervalSec);Prefs(context).intervalSec=30;assertEquals(30,Prefs(context).intervalSec)}
}
