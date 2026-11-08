package org.breatheless.app

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31])
@LooperMode(LooperMode.Mode.PAUSED)
class PracticeServiceTest {
    private lateinit var context:Context
    private lateinit var controller:ServiceController<PracticeService>
    private lateinit var service:PracticeService
    @Before fun prepare() {
        context=ApplicationProvider.getApplicationContext()
        Store::class.java.getDeclaredField("instance").apply{isAccessible=true;set(null,null)}
        context.filesDir.listFiles().orEmpty().forEach{it.deleteRecursively()}
        PracticeService.live=null;PracticeService.active.value=false;PracticeService.result.value=null;PracticeService.snapshot.value=null;PracticeService.lastResult=null
        Store.get(context).settings.put("sound",false).put("haptic",false);Store.get(context).save()
        controller=Robolectric.buildService(PracticeService::class.java).create();service=controller.get()
    }
    @After fun clean(){controller.destroy()}
    private fun start(p:Protocol){service.onStartCommand(Intent(context,PracticeService::class.java).setAction("START").putExtra("protocol",Codec.protocol(p).toString()),0,1)}
    @Test fun completedMorningSavesOnceAndPublishesResultAfterCue() {
        start(Presets.morning);assertEquals(State.CP_READY,PracticeService.snapshot.value!!.state)
        service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(9));service.stopCp();service.review(true)
        val entry=Store.get(context).entries.single();assertEquals(9.0,entry.cps.single().seconds,.01);assertTrue(entry.cps.single().valid)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200));assertEquals(entry.id,PracticeService.result.value)
    }
    @Test fun leavingDuringHoldExcludesMeasurement() {start(Presets.morning);service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(4));service.backgroundCp();assertEquals(State.CP_REVIEW,PracticeService.snapshot.value!!.state);service.review(true);assertFalse(Store.get(context).entries.single().cps.single().valid)}
    @Test fun quitDiscardsAlreadyReviewedCpAndNeverWritesCheckpoint() {
        start(Protocol("test","Test","",listOf(Phase(Kind.CP),Phase(Kind.REST,60))));service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(8));service.stopCp();service.review(true)
        assertEquals(1,service.cps.size);assertFalse(Store.get(context).active.baseFile.exists());service.discard();assertTrue(Store.get(context).entries.isEmpty());assertNull(PracticeService.result.value);assertNull(PracticeService.snapshot.value)
    }
    @Test fun oldInterruptedCheckpointIsDiscardedOnUpgrade() {val store=Store.get(context);Store.write(store.active,Codec.entry(Entry("partial",1760000000000,"Incomplete",100,"Interrupted")).toString());assertFalse(store.recover());assertFalse(store.active.baseFile.exists());assertTrue(store.entries.isEmpty())}
    @Test fun notificationOffersQuitWithoutSavingOrPause() {start(Protocol("test","Test","",listOf(Phase(Kind.REST,60))));assertEquals(listOf("Quit session"),shadowOf(service).lastForegroundNotification.actions.map{it.title.toString()});service.remotePrimary();assertEquals(State.TIMED,service.engine!!.state)}
    @Test fun maximumPauseIsNeverAddedToCpSeries() {start(Protocol("test","Test","",listOf(Phase(Kind.MP_LIGHT))));service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(45));service.stopCp();service.review(true);val entry=Store.get(context).entries.single();assertTrue(entry.cps.isEmpty());assertEquals(Kind.MP_LIGHT,entry.holds.single().kind);assertEquals(45.0,entry.holds.single().seconds,.01)}
    @Test fun fifteenSecondPulseConvertsBeatCountToBpm() {start(Protocol("test","Test","",listOf(Phase(Kind.PULSE,15))));service.startPulse();service.pulseBeats(16);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(15));assertEquals(State.PULSE_REVIEW,PracticeService.snapshot.value!!.state);service.recordPulse();assertEquals(64,Store.get(context).entries.single().pulses.single().bpm)}
    @Test fun quittingDuringPulseLeavesNoSession() {start(Protocol("test","Test","",listOf(Phase(Kind.PULSE,15))));service.startPulse();service.pulseBeats(15);service.discard();assertTrue(Store.get(context).entries.isEmpty())}
}
