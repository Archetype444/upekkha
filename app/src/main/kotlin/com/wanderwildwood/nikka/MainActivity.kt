package com.wanderwildwood.nikka

import android.os.Bundle
import android.content.Intent
import android.view.KeyEvent
import org.breatheless.app.ButeykoActions
import org.breatheless.app.MorningReminder
import org.breatheless.app.PracticeService
import org.breatheless.app.Presets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.nikka.data.Backup
import com.wanderwildwood.nikka.data.Store
import com.wanderwildwood.nikka.sit.Sitting
import com.wanderwildwood.nikka.ui.houseTypography
import java.io.File
import com.wanderwildwood.nikka.ui.PracticeApp
import com.wanderwildwood.nikka.ui.monochrome
import com.wanderwildwood.nikka.ui.ArmedScope

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PracticeService.live == null) runCatching { org.breatheless.app.Store.get(this).recover() }
        MorningReminder.schedule(this)
        openMorningCp(intent)
        Sitting.load(this)
        setContent {
            // A steady cursor: a blinking one is a repaint twice a second on an e-ink panel
            // for as long as a field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome, typography = houseTypography) {
                    ArmedScope {
                        // A debug build names its rows for the emulator tests; a release names none.
                        Box(Modifier.semantics { testTagsAsResourceId = BuildConfig.DEBUG }) {
                            PracticeApp()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openMorningCp(intent)
    }
    private fun openMorningCp(intent: Intent?) {
        if (intent?.getBooleanExtra("morning_cp", false) != true) return
        intent.removeExtra("morning_cp")
        if (PracticeService.live == null && Sitting.running.value == null && com.wanderwildwood.nikka.sit.Breathing.running.value == null)
            runCatching { ButeykoActions.start(this, Presets.morning) }
    }
    override fun onPause() {
        PracticeService.live?.backgroundCp()
        super.onPause()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val service=PracticeService.live
        if (event.repeatCount==0 && service!=null && org.breatheless.app.Store.get(this).bool("volumeKeys",false) && keyCode in listOf(KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN)) {
            when(service.engine?.state) {
                org.breatheless.app.State.CP_READY -> service.startCp()
                org.breatheless.app.State.CP_RUNNING -> service.stopCp()
                org.breatheless.app.State.CP_REVIEW -> service.review(keyCode==KeyEvent.KEYCODE_VOLUME_UP && service.engine?.interrupted!=true)
                else -> return super.onKeyDown(keyCode,event)
            }
            return true
        }
        return super.onKeyDown(keyCode,event)
    }
}
