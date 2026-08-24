package com.example

import android.app.Application
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TileViewModel
import com.example.ui.screens.CadLayoutScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Дымовые тесты: экраны должны собираться и переживать переключение вкладок
 * без падений — на устройстве это первое, что ломается.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreenSmokeTest {

    @get:Rule
    val rule = createComposeRule()

    private fun vm() = TileViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun `cad editor renders and switches tabs`() {
        rule.setContent { MyApplicationTheme { CadLayoutScreen(viewModel = vm()) } }

        rule.onNodeWithText("1. Контур").assertExists()
        rule.onNodeWithText("Стены").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Форма").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("2. Раскладка").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Старт").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Анализ").performClick()
        rule.waitForIdle()
    }

    @Test
    fun `cad editor opens the spec dialog`() {
        rule.setContent { MyApplicationTheme { CadLayoutScreen(viewModel = vm()) } }
        rule.onNodeWithText("2. Раскладка").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Анализ").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Отправить в калькулятор").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Отправлено в калькулятор ✓").assertExists()
    }

    @Test
    fun `calculator renders and expands sections`() {
        rule.setContent { MyApplicationTheme { CalculatorScreen(viewModel = vm()) } }

        rule.onNodeWithText("ИТОГО ПО ОБЪЕКТУ").assertExists()
        // Карточки лежат в LazyColumn — до них нужно долистать. Первый прокручиваемый
        // узел и есть список: остальные — горизонтальные ленты чипов внутри карточек.
        listOf("Плитка", "Плиточный клей", "Затирка швов", "Крестики и СВП", "Работа и логистика")
            .forEach { title ->
                rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(title))
                rule.onNodeWithText(title).performClick()
                rule.waitForIdle()
            }
    }
}
