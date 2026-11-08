package org.breatheless.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.wanderwildwood.nikka.MainActivity
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31],qualifiers="w320dp-h533dp-hdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class KompaktUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun homeFitsCompactScreenAndUsesSystemBrightness() {
        compose.onNodeWithText("Morning CP").assertIsDisplayed()
        compose.onNodeWithText("Mindfulness Buteyko").assertIsDisplayed()
        compose.onNodeWithText("Freestyle").assertIsDisplayed()
        compose.onNodeWithText("Diary").assertDoesNotExist();compose.onNodeWithText("Learn").assertDoesNotExist()
        compose.runOnIdle { assertEquals(-1f,compose.activity.window.attributes.screenBrightness,0f) }
    }
    @Test fun freestyleSavesExactSecondsWithoutTemplateName() {
        compose.onNodeWithText("Freestyle").performClick()
        compose.onNodeWithText("Initial rest").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("37")
        compose.onNodeWithText("Set",useUnmergedTree=true).performClick()
        compose.onNodeWithText("37 sec").assertIsDisplayed()
        compose.runOnIdle { assertEquals(37,Store.get(compose.activity).freestyle().rest) }
        compose.onNodeWithText("Template name").assertDoesNotExist()
    }
}
