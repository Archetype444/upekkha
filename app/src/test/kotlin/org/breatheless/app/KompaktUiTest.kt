package org.breatheless.app

import android.media.MediaPlayer
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import android.os.Looper
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.wanderwildwood.nikka.MainActivity
import com.wanderwildwood.nikka.data.Prefs
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31],qualifiers="w320dp-h533dp-hdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KompaktUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private var controller:ServiceController<PracticeService>?=null
    private var recording:MediaPlayer?=null
    @Before fun prepare() {
        compose.runOnIdle {
            ShadowMediaPlayer.setMediaInfoProvider { ShadowMediaPlayer.MediaInfo(1227168,0) }
            ShadowMediaPlayer.setCreateListener { player, _ -> recording=player }
            PracticeService.result.value=null;PracticeService.active.value=false;PracticeService.snapshot.value=null;PracticeService.live=null
            val store=Store.get(compose.activity);store.protocols.clear();store.entries.clear();store.settings.remove("freestyleV4");store.settings.remove("freestyle");store.settings.put("sound",false).put("haptic",false).put("pulsePrompt",false);store.save()
        }
        compose.onNodeWithText("Buteyko").performClick()
    }
    private fun screenshot(name:String) {compose.runOnIdle {
        val folder=System.getProperty("upekkha.screenshot.dir") ?: return@runOnIdle
        val view=compose.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap));java.io.File(folder).mkdirs();java.io.File(folder,name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }}
    @Test fun homeShowsNewModesWithoutTodayAndHidesStatusBar() {
        listOf("Morning CP","Guided Buteyko","Diminished Breathing","Classical Buteyko","Freestyle").forEach{compose.onNodeWithText(it).assertIsDisplayed()}
        listOf("Today","Diary","Learn","Mindfulness Buteyko").forEach{compose.onNodeWithText(it).assertDoesNotExist()}
        compose.runOnIdle {assertEquals(-1f,compose.activity.window.attributes.screenBrightness,0f);assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN!=0)}
        screenshot("v4-home.png")
    }
    @Test fun freestyleDurationIsAListAndNeverOpensKeyboard() {
        compose.onNodeWithText("Freestyle").performClick()
        compose.onNodeWithText("1. Initial rest").assertIsDisplayed().performClick()
        compose.onNodeWithText("Duration").performClick()
        compose.onNodeWithText("15 sec").assertIsDisplayed().performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNode(hasText("Duration").and(hasText("15 sec"))).assertIsDisplayed()
        compose.runOnIdle {assertEquals(15,Store.get(compose.activity).freestylePlan().phases.first().seconds)}
        screenshot("v4-duration.png")
    }
    @Test fun stepCanBeMovedAndRemovedWithoutDrag() {
        compose.onNodeWithText("Freestyle").performClick();compose.onNodeWithText("1. Initial rest").performClick()
        compose.onNodeWithText("Move down").performClick()
        compose.runOnIdle {assertEquals(Kind.CP,Store.get(compose.activity).freestylePlan().phases.first().kind)}
        compose.onNodeWithText("Remove step").performClick()
        compose.runOnIdle {assertFalse(Store.get(compose.activity).freestylePlan().phases.any{it.kind==Kind.REST})}
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
    @Test fun sittingOffersShortIntervalsAndPersistsSeconds() {
        compose.onNodeWithText("Sit").performClick()
        compose.onNodeWithText("Interval bell").performClick()
        compose.onNodeWithText("Every 15 sec").assertIsDisplayed().performClick()
        compose.onNodeWithText("Every 15 sec").assertIsDisplayed()
        compose.runOnIdle {assertEquals(15,Prefs(compose.activity).intervalSec)}
        screenshot("v4-sit.png")
    }
    @Test fun trackingCanDeleteLegacyButeykoRecords() {
        compose.runOnIdle {Store.get(compose.activity).upsert(Entry("delete-this",System.currentTimeMillis(),"A completed practice",120,"Completed",listOf(CpValue(22.0,System.currentTimeMillis(),true,"CP 1"))))}
        compose.onNodeWithText("Track").performClick();compose.onNodeWithText("Practice history").performClick()
        compose.onNodeWithText("A completed practice").performClick();compose.onNodeWithText("Delete session").performClick();compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle {assertTrue(Store.get(compose.activity).entries.isEmpty())}
        compose.onNodeWithText("A completed practice").assertDoesNotExist()
    }
    @Test fun instructionsAreAvailableFromSettings() {
        compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Instructions").performClick()
        compose.onNodeWithText("Source instructions from Adam Shand’s breathe-less and the course linked below.").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        screenshot("v4-instructions.png")
    }
    private fun reach(text:String):SemanticsNodeInteraction {
        repeat(12) {
            val node=compose.onNodeWithText(text)
            if(runCatching{node.assertIsDisplayed()}.isSuccess)return node
            compose.onRoot().performTouchInput { swipeUp() }
        }
        error("Could not reach $text")
    }
    @Test fun savedPracticeCanBeRenamedAndDeletedFromItsHomeButton() {
        compose.runOnIdle { Store.get(compose.activity).saveTemplate(Protocol("saved-ui","Morning ease","",listOf(Phase(Kind.CP),Phase(Kind.RECOVER,15)),true)) }
        reach("Edit").performClick();reach("Rename practice").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Evening ease");compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        reach("Evening ease").assertIsDisplayed();reach("Edit").performClick();reach("Delete practice").performClick();compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertTrue(Store.get(compose.activity).protocols.isEmpty()) }
    }
    @Test fun phaseToneSelectionUpdatesItsVisibleRow() {
        compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick();compose.onNodeWithText("Sound, vibration and CP").performClick()
        reach("Rest begins").performClick();reach("Bell").performClick();compose.onNodeWithText("Done").performClick()
        compose.onNode(hasText("Rest begins").and(hasText("Bell"))).assertIsDisplayed()
        compose.runOnIdle { assertEquals("bell",Store.get(compose.activity).str("tone_REST","")) }
    }

    @After fun cleanSession() { compose.runOnIdle { controller?.destroy();PracticeService.result.value=null;PracticeService.active.value=false;PracticeService.snapshot.value=null;PracticeService.live=null } }
    private fun beginGuided() {
        compose.onNodeWithText("Guided Buteyko").performClick()
        compose.onNodeWithText("Before practising").assertDoesNotExist()
        compose.runOnIdle {
            val intent=shadowOf(compose.activity).nextStartedService
            assertNotNull(intent)
            controller=Robolectric.buildService(PracticeService::class.java).create()
            controller!!.get().onStartCommand(intent,0,1)
        }
        compose.onNodeWithText("Start CP").assertIsDisplayed()
    }
    private fun takeCp() {
        compose.onNodeWithText("Start CP").performClick()
        compose.onNodeWithText("Stop CP").assertIsDisplayed()
        compose.runOnIdle { ShadowSystemClock.advanceBy(Duration.ofSeconds(4)) }
        compose.onNodeWithText("Stop CP").performClick()
        compose.onNodeWithText("Keep measurement").assertIsDisplayed().performClick()
    }
    @Test fun guidedTouchControlsFollowAudioRecoveryAndFinalCp() {
        beginGuided();takeCp()
        compose.onNodeWithText("Recording").assertIsDisplayed();compose.onNodeWithText("2 of 4").assertIsDisplayed();compose.onNodeWithText("Playing").assertIsDisplayed()
        screenshot("v4-guided-audio.png")
        compose.runOnIdle { shadowOf(recording!!).invokeCompletionListener() }
        compose.onNodeWithText("Recovery").assertIsDisplayed();compose.onNodeWithText("3 of 4").assertIsDisplayed()
        compose.runOnIdle { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(300)) }
        compose.onNodeWithText("4 of 4").assertIsDisplayed();takeCp()
        compose.runOnIdle { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200));val entry=Store.get(compose.activity).entries.single();assertEquals(2,entry.cps.size);assertTrue(entry.cps.all{it.valid}) }
        compose.onNodeWithText("Save and close").assertIsDisplayed()
    }
    @Test fun quittingGuidedAfterItsFirstCpDiscardsEverything() {
        beginGuided();takeCp();compose.onNodeWithText("Recording").assertIsDisplayed()
        reach("Quit session").performClick();compose.onNodeWithText("Quit").performClick()
        compose.runOnIdle { assertTrue(Store.get(compose.activity).entries.isEmpty());assertNull(PracticeService.result.value) }
        compose.onNodeWithText("Guided Buteyko").assertIsDisplayed()
    }

}
