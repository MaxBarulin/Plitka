package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TileViewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тема светлая — значки системных панелей должны быть тёмными.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

enum class TileTab {
    DASHBOARD,
    CAD_PLAN,
    CALCULATOR,
    ORDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: TileViewModel = viewModel()
    var currentTab by remember { mutableStateOf(TileTab.DASHBOARD) }
    val workspaceLoaded by viewModel.workspaceLoaded.collectAsState()

    // Введённое сохраняем при каждом уходе из приложения: человек мог просто
    // убрать телефон в карман, и вернуться он хочет к тем же цифрам.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.persistWorkspace()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            TileTab.DASHBOARD -> "Рабочий кабинет"
                            TileTab.CAD_PLAN -> "CAD Чертеж & Раскладка"
                            TileTab.CALCULATOR -> "Материалы"
                            TileTab.ORDERS -> "Календарь заказов"
                        },
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == TileTab.DASHBOARD,
                    onClick = { currentTab = TileTab.DASHBOARD },
                    label = { Text("Главная") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Главная панель"
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentTab == TileTab.CAD_PLAN,
                    onClick = { currentTab = TileTab.CAD_PLAN },
                    label = { Text("CAD План") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "CAD План помещения"
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentTab == TileTab.CALCULATOR,
                    onClick = { currentTab = TileTab.CALCULATOR },
                    label = { Text("Калькулятор") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Калькулятор материалов"
                        )
                    }
                )
                NavigationBarItem(
                    selected = currentTab == TileTab.ORDERS,
                    onClick = { currentTab = TileTab.ORDERS },
                    label = { Text("Заказы") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Заказы и задачи"
                        )
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!workspaceLoaded) {
                // Чтение одной строки из базы занимает миллисекунды, но показывать
                // пустые поля и затем подменять их значениями — плохо.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Box
            }
            when (currentTab) {
                TileTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { currentTab = it }
                    )
                }
                TileTab.CAD_PLAN -> {
                    com.example.ui.screens.CadLayoutScreen(viewModel = viewModel)
                }
                TileTab.CALCULATOR -> {
                    CalculatorScreen(viewModel = viewModel)
                }
                TileTab.ORDERS -> {
                    OrdersScreen(viewModel = viewModel)
                }
            }
        }
    }
}
