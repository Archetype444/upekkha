package org.breatheless.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
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
    private lateinit var context: Context
    private lateinit var controller: ServiceController<PracticeService>
    private lateinit var service: PracticeService
    @Before fun prepare() {
        context=ApplicationProvider.getApplicationContext()
        Store::class.java.getDeclaredField("instance").apply { isAccessible=true;set(null,null) }
        context.filesDir.listFiles().orEmpty().forEach{it.deleteRecursively()}
        PracticeService.live=null;PracticeService.active.value=false;PracticeService.result.value=null;PracticeService.lastResult=null
        val store=Store.get(context);store.settings.put("sound",false).put("haptic",false);store.save()
        controller=Robolectric.buildService(PracticeService::class.java).create();service=controller.get()
    }
    @After fun clean() { controller.destroy() }
    private fun start(protocol:Protocol) { service.onStartCommand(Intent(context,PracticeService::class.java).setAction("START").putExtra("protocol",Codec.protocol(protocol).toString()),0,1) }
    @Test fun morningStartsReadyAndSavesReviewedMeasurement() {
        start(Presets.morning);assertEquals(State.CP_READY,service.engine!!.state)
        service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(9));service.stopCp();service.review(true)
        val e=Store.get(context).entries.single();assertEquals("Morning",e.context);assertEquals(9.0,e.cps.single().seconds,0.01);assertTrue(e.cps.single().valid);assertEquals(e.id,PracticeService.result.value)
    }
    @Test fun leavingDuringHoldExcludesTheMeasurement() {
        start(Presets.morning);service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(4));service.backgroundCp()
        assertEquals(State.CP_REVIEW,service.engine!!.state);assertTrue(service.engine!!.interrupted)
        service.review(true);assertFalse(Store.get(context).entries.single().cps.single().valid)
    }
    @Test fun timedNotificationsHaveEndAndSaveWithoutPause() {
        start(Protocol("test","Test","",listOf(Phase(Kind.REST,60))))
        val n=shadowOf(service).lastForegroundNotification
        assertEquals(listOf("End and save"),n.actions.map{it.title.toString()})
        service.remotePrimary();assertEquals(State.TIMED,service.engine!!.state)
    }
    @Test fun interruptedCheckpointRecoversWithoutResumingHold() {
        start(Presets.morning);service.startCp();ShadowSystemClock.advanceBy(Duration.ofSeconds(6));service.stopCp()
        assertTrue(Store.get(context).active.baseFile.exists())
        assertTrue(Store.get(context).recover())
        val e=Store.get(context).entries.single();assertEquals("Interrupted",e.status);assertEquals(6.0,e.cps.single().seconds,0.01);assertFalse(e.cps.single().valid)
    }
}
