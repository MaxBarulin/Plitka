package com.example

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TileViewModel
import com.example.ui.cad.rectangleRoom
import com.example.ui.screens.CadLayoutScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Дымовые тесты: экраны должны собираться, переживать переключение вкладок
 * и обмениваться данными — на устройстве это первое, что ломается.
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
    fun `cad state survives leaving and reopening the screen`() {
        val viewModel = vm()
        var visible by mutableStateOf(true)
        rule.setContent {
            MyApplicationTheme {
                if (visible) CadLayoutScreen(viewModel = viewModel) else Text("другая вкладка")
            }
        }

        rule.onNodeWithText("2. Раскладка").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Старт").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Итоговая точка старта").assertExists()

        // Уходим на другой экран и возвращаемся
        rule.runOnIdle { visible = false }
        rule.onNodeWithText("другая вкладка").assertExists()
        rule.runOnIdle { visible = true }
        rule.waitForIdle()

        // Редактор вернулся в том же режиме, а не сбросился на «Контур»
        rule.onNodeWithText("Итоговая точка старта").assertExists()
    }

    @Test
    fun `calculator takes area from the cad plan automatically`() {
        val viewModel = vm()
        rule.setContent { MyApplicationTheme { CalculatorScreen(viewModel = viewModel) } }
        // Восстановление прошлого сеанса идёт корутиной и иначе перезапишет
        // то, что задаёт тест.
        rule.waitUntil(5_000) { viewModel.workspaceLoaded.value }

        rule.runOnIdle { viewModel.cadState.vertices = rectangleRoom(4000.0, 3000.0) } // 12 м²
        rule.waitForIdle()
        rule.onNodeWithText("Работа (12 м² × 1500 ₽)").assertExists()

        // Правка чертежа тут же меняет смету — без всякого «перенести вручную»
        rule.runOnIdle { viewModel.cadState.vertices = rectangleRoom(2000.0, 1000.0) } // 2 м²
        rule.waitForIdle()
        rule.onNodeWithText("Работа (2 м² × 1500 ₽)").assertExists()
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

    @Test
    fun `dashboard shows an empty state while there are no orders`() {
        rule.setContent {
            MyApplicationTheme { DashboardScreen(viewModel = vm(), onNavigateToTab = {}) }
        }

        rule.onNodeWithText("Заказов пока нет").assertExists()
    }

    @Test
    fun `dashboard carries the author signature`() {
        rule.setContent {
            MyApplicationTheme { DashboardScreen(viewModel = vm(), onNavigateToTab = {}) }
        }

        rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("By Max B"))
        rule.onNodeWithText("By Max B").assertExists()
        rule.onNodeWithText("@Cvela_siren").assertExists()
    }
}
