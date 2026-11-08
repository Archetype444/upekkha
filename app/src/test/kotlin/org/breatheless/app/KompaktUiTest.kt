package org.breatheless.app

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
    @Before fun prepare() {
        compose.runOnIdle {
            PracticeService.result.value=null;PracticeService.active.value=false;PracticeService.snapshot.value=null;PracticeService.live=null
            val store=Store.get(compose.activity);store.protocols.clear();store.entries.clear();store.settings.remove("freestyleV4");store.settings.remove("freestyle");store.settings.put("sound",false).put("haptic",false).put("pulsePrompt",false);store.save()
        }
        compose.onNodeWithText("Buteyko",exact=true).performClick()
    }
    private fun screenshot(name:String) {compose.runOnIdle {
        val folder=System.getProperty("upekkha.screenshot.dir") ?: return@runOnIdle
        val view=compose.activity.window.decorView
        val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap));java.io.File(folder).mkdirs();java.io.File(folder,name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
    }}
    @Test fun homeShowsNewModesWithoutTodayAndHidesStatusBar() {
        listOf("Morning CP","Guided Buteyko","Diminished Breathing","Classical Buteyko","Freestyle").forEach{compose.onNodeWithText(it).assertIsDisplayed()}
        listOf("Today","Diary","Learn","Mindfulness Buteyko").forEach{compose.onNodeWithText(it,exact=true).assertDoesNotExist()}
        compose.runOnIdle {assertEquals(-1f,compose.activity.window.attributes.screenBrightness,0f);assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN!=0)}
        screenshot("v4-home.png")
    }
    @Test fun freestyleDurationIsAListAndNeverOpensKeyboard() {
        compose.onNodeWithText("Freestyle",exact=true).performClick()
        compose.onNodeWithText("1. Initial rest").assertIsDisplayed().performClick()
        compose.onNodeWithText("Duration",exact=true).performClick()
        compose.onNodeWithText("15 sec",exact=true).assertIsDisplayed().performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("15 sec",exact=true).assertIsDisplayed()
        compose.runOnIdle {assertEquals(15,Store.get(compose.activity).freestylePlan().phases.first().seconds)}
        screenshot("v4-duration.png")
    }
    @Test fun stepCanBeMovedAndRemovedWithoutDrag() {
        compose.onNodeWithText("Freestyle",exact=true).performClick();compose.onNodeWithText("1. Initial rest").performClick()
        compose.onNodeWithText("Move down",exact=true).performClick()
        compose.runOnIdle {assertEquals(Kind.CP,Store.get(compose.activity).freestylePlan().phases.first().kind)}
        compose.onNodeWithText("Remove step",exact=true).performClick()
        compose.runOnIdle {assertFalse(Store.get(compose.activity).freestylePlan().phases.any{it.kind==Kind.REST})}
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
    @Test fun sittingOffersShortIntervalsAndPersistsSeconds() {
        compose.onNodeWithText("Sit",exact=true).performClick()
        compose.onNodeWithText("Interval bell",exact=true).performClick()
        compose.onNodeWithText("Every 15 sec",exact=true).assertIsDisplayed().performClick()
        compose.onNodeWithText("Every 15 sec",exact=true).assertIsDisplayed()
        compose.runOnIdle {assertEquals(15,Prefs(compose.activity).intervalSec)}
        screenshot("v4-sit.png")
    }
    @Test fun trackingCanDeleteLegacyButeykoRecords() {
        compose.runOnIdle {Store.get(compose.activity).upsert(Entry("delete-this",System.currentTimeMillis(),"A completed practice",120,"Completed",listOf(CpValue(22.0,System.currentTimeMillis(),true,"CP 1"))))}
        compose.onNodeWithText("Track",exact=true).performClick();compose.onNodeWithText("Practice history",exact=true).performClick()
        compose.onNodeWithText("A completed practice",exact=true).performClick();compose.onNodeWithText("Delete session",exact=true).performClick();compose.onNodeWithText("Delete",exact=true).performClick()
        compose.runOnIdle {assertTrue(Store.get(compose.activity).entries.isEmpty())}
        compose.onNodeWithText("A completed practice",exact=true).assertDoesNotExist()
    }
    @Test fun instructionsAreAvailableFromSettings() {
        compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Instructions",exact=true).performClick()
        compose.onNodeWithText("Source instructions from Adam Shand’s breathe-less and the course linked below.",exact=true).assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        screenshot("v4-instructions.png")
    }
    private fun reach(text:String):SemanticsNodeInteraction {
        repeat(12) {
            val node=compose.onNodeWithText(text,exact=true)
            if(runCatching{node.assertIsDisplayed()}.isSuccess)return node
            compose.onRoot().performTouchInput { swipeUp() }
        }
        error("Could not reach $text")
    }
    @Test fun savedPracticeCanBeRenamedAndDeletedFromItsHomeButton() {
        compose.runOnIdle { Store.get(compose.activity).saveTemplate(Protocol("saved-ui","Morning ease","",listOf(Phase(Kind.CP),Phase(Kind.RECOVER,15)),true)) }
        reach("Edit").performClick();reach("Rename practice").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Evening ease");compose.onNodeWithText("Save",exact=true).performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        reach("Evening ease").assertIsDisplayed();reach("Edit").performClick();reach("Delete practice").performClick();compose.onNodeWithText("Delete",exact=true).performClick()
        compose.runOnIdle { assertTrue(Store.get(compose.activity).protocols.isEmpty()) }
    }
    @Test fun phaseToneSelectionUpdatesItsVisibleRow() {
        compose.onNodeWithContentDescription("Settings",useUnmergedTree=true).performClick();compose.onNodeWithText("Sound, vibration and CP",exact=true).performClick()
        reach("Rest begins").performClick();reach("Bell").performClick();compose.onNodeWithText("Done",exact=true).performClick()
        compose.onNode(hasText("Rest begins").and(hasText("Bell"))).assertIsDisplayed()
        compose.runOnIdle { assertEquals("bell",Store.get(compose.activity).str("tone_REST","")) }
    }

}
