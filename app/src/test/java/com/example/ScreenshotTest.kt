package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TileViewModel
import com.example.ui.screens.CadLayoutScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Снимки основных экранов — быстрый способ увидеть регресс тёмной темы и вёрстки. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    private fun vm() = TileViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun cad_room_mode() {
        rule.setContent { MyApplicationTheme { CadLayoutScreen(viewModel = vm()) } }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cad_room.png")
    }

    @Test
    fun cad_tile_mode() {
        rule.setContent { MyApplicationTheme { CadLayoutScreen(viewModel = vm()) } }
        rule.onNodeWithText("2. Раскладка").performClick()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/cad_tiles.png")
    }

    @Test
    fun calculator() {
        rule.setContent { MyApplicationTheme { CalculatorScreen(viewModel = vm()) } }
        rule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calculator.png")
    }
}
