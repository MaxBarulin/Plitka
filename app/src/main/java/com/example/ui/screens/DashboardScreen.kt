package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TileTab
import com.example.data.Order
import com.example.data.OrderStatus
import com.example.ui.TileViewModel
import com.example.ui.cad.fmtNum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val TELEGRAM_HANDLE = "@Cvela_siren"
const val TELEGRAM_URL = "https://t.me/Cvela_siren"

private fun money(v: Double): String = "%,.0f ₽".format(v).replace(',', ' ')

@Composable
fun DashboardScreen(
    viewModel: TileViewModel,
    onNavigateToTab: (TileTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.allOrders.collectAsState()
    val calculations by viewModel.allCalculations.collectAsState()
    val cad = viewModel.cadState

    val activeOrders = remember(orders) { orders.filter { OrderStatus.isActive(it.status) } }
    val nextOrder = remember(activeOrders) { activeOrders.minByOrNull { it.dateMillis } }
    val lastCalculation = calculations.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(2.dp))

        Header(activeOrders.size)

        if (nextOrder == null) {
            EmptyOrdersCard { onNavigateToTab(TileTab.ORDERS) }
        } else {
            NextOrderCard(nextOrder) { onNavigateToTab(TileTab.ORDERS) }
        }

        CurrentPlanCard(
            corners = cad.vertices.size,
            areaM2 = cad.areaM2,
            perimeterM = cad.perimeterM,
            tileW = cad.tileW,
            tileH = cad.tileH,
            groutMm = cad.grout,
            rotationDeg = cad.tileRotation,
            onClick = { onNavigateToTab(TileTab.CAD_PLAN) }
        )

        LastEstimateCard(
            name = lastCalculation?.name,
            total = lastCalculation?.totalCost,
            areaM2 = lastCalculation?.areaM2,
            onClick = { onNavigateToTab(TileTab.CALCULATOR) }
        )

        CalendarCard(activeOrders.size) { onNavigateToTab(TileTab.ORDERS) }

        AuthorSignature()

        Spacer(Modifier.height(8.dp))
    }
}

// =====================================================================================

@Composable
private fun Header(activeCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "ПроПлитка",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = if (activeCount > 0)
                    plural(activeCount, "активный заказ", "активных заказа", "активных заказов")
                else
                    "Свободный график",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("app_mark"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Каркас плитки бенто: одинаковые отступы, рамка и заголовок у всех карточек. */
@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    accent: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (accent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            contentColor = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            content?.invoke(this)
        }
    }
}

/** Три равные ячейки со значением и подписью. */
@Composable
private fun StatRow(stats: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyOrdersCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("bento_no_orders"),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Text("Заказов пока нет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Добавьте первый объект во вкладке «Заказы» — он появится здесь",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NextOrderCard(order: Order, onClick: () -> Unit) {
    val dateString = remember(order.dateMillis) {
        SimpleDateFormat("d MMMM", Locale("ru")).format(Date(order.dateMillis))
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bento_closest_order"),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ближайший заказ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$dateString · ${dueLabel(order.dateMillis)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = order.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (order.address.isNotBlank()) {
                Text(
                    text = order.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            // Никакого «процента готовности»: приложение его не знает.
            // Показываем только то, что действительно записано в заказе.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = OrderStatus.label(order.status),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (order.clientName.isNotBlank()) {
                    Text(
                        text = order.clientName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (order.totalCost > 0) {
                    Text(
                        text = money(order.totalCost),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentPlanCard(
    corners: Int,
    areaM2: Double,
    perimeterM: Double,
    tileW: Double,
    tileH: Double,
    groutMm: Double,
    rotationDeg: Double,
    onClick: () -> Unit
) {
    BentoCard(
        title = "Текущий план",
        subtitle = "${plural(corners, "угол", "угла", "углов")} · плитка " +
            "${tileW.toInt()}×${tileH.toInt()} мм · шов ${fmtNum(groutMm, 1)} мм",
        icon = Icons.Default.CropFree,
        onClick = onClick,
        testTag = "bento_current_plan"
    ) {
        StatRow(
            listOf(
                "Площадь" to "${fmtNum(areaM2, 2)} м²",
                "Периметр" to "${fmtNum(perimeterM, 2)} м",
                "Угол" to "${fmtNum(rotationDeg, 0)}°"
            )
        )
    }
}

@Composable
private fun LastEstimateCard(
    name: String?,
    total: Double?,
    areaM2: Double?,
    onClick: () -> Unit
) {
    BentoCard(
        title = "Смета материалов",
        subtitle = if (name != null && total != null && areaM2 != null)
            "$name · ${fmtNum(areaM2, 2)} м² · ${money(total)}"
        else
            "Клей, затирка, СВП, стяжка, работа",
        icon = Icons.Default.Calculate,
        onClick = onClick,
        testTag = "bento_estimate"
    )
}

@Composable
private fun CalendarCard(activeCount: Int, onClick: () -> Unit) {
    BentoCard(
        title = "Календарь заказов",
        subtitle = if (activeCount > 0)
            plural(activeCount, "активный объект", "активных объекта", "активных объектов") + " в графике"
        else
            "График свободен",
        icon = Icons.Default.CalendarMonth,
        onClick = onClick,
        testTag = "bento_calendar"
    )
}

/** Подпись автора: тап открывает телеграм. */
@Composable
fun AuthorSignature(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                // Если телеграма нет, ссылку подхватит браузер; в худшем случае
                // открывать нечем — тогда просто ничего не произойдёт.
                runCatching { uriHandler.openUri(TELEGRAM_URL) }
            }
            .padding(vertical = 12.dp)
            .testTag("author_signature"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "By Max B",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = TELEGRAM_HANDLE,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Когда заказ: «сегодня», «через 3 дня», «просрочен на 2 дня». */
fun dueLabel(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val dayMs = 24L * 60 * 60 * 1000
    // Считаем в календарных днях: заказ «завтра» не должен зависеть от часа записи.
    val days = Math.floorDiv(dateMillis, dayMs) - Math.floorDiv(nowMillis, dayMs)
    return when {
        days == 0L -> "сегодня"
        days == 1L -> "завтра"
        days == -1L -> "был вчера"
        days > 1L -> "через ${plural(days.toInt(), "день", "дня", "дней")}"
        else -> "просрочен на ${plural((-days).toInt(), "день", "дня", "дней")}"
    }
}

/** Число со склонением: 1 угол, 2 угла, 5 углов. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod100 = n % 100
    val mod10 = n % 10
    val word = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$n $word"
}
