package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Order
import com.example.ui.TileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: TileViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.allOrders.collectAsState()
    val context = LocalContext.current

    // Tabs / Filters
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PLANNED, IN_PROGRESS, DONE
    val filterOptions = listOf(
        "ALL" to "Все",
        "PLANNED" to "План",
        "IN_PROGRESS" to "В работе",
        "DONE" to "Готово"
    )

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == "ALL") orders else orders.filter { it.status == selectedFilter }
    }

    // New Order Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var orderTitle by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var orderAddress by remember { mutableStateOf("") }
    var orderCost by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }
    var orderStatus by remember { mutableStateOf("PLANNED") }
    
    // Simple calendar pick state
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Selected order for full view/editing
    var detailOrder by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Reset fields
                    orderTitle = ""
                    clientName = ""
                    clientPhone = ""
                    orderAddress = ""
                    orderCost = ""
                    orderNotes = ""
                    orderStatus = "PLANNED"
                    selectedDate = Calendar.getInstance()
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 80.dp) // Offset above the bottom tab bar
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заказ")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Filter Row ---
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                filterOptions.forEachIndexed { index, (filterCode, label) ->
                    SegmentedButton(
                        selected = selectedFilter == filterCode,
                        onClick = { selectedFilter = filterCode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filterOptions.size)
                    ) {
                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // --- Empty State ---
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Заказов пока нет",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Нажмите + внизу экрана, чтобы запланировать новый заказ на укладку плитки.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // --- Orders List ---
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
                ) {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onClick = { detailOrder = order },
                            onStatusChange = { newStatus ->
                                viewModel.updateOrder(order.copy(status = newStatus))
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Order Add Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый заказ") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = orderTitle,
                            onValueChange = { orderTitle = it },
                            label = { Text("Что укладываем (например: Санузел 6м²)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = { clientName = it },
                            label = { Text("Имя клиента") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = clientPhone,
                            onValueChange = { clientPhone = it },
                            label = { Text("Телефон") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = orderAddress,
                            onValueChange = { orderAddress = it },
                            label = { Text("Адрес") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = orderCost,
                            onValueChange = { orderCost = it },
                            label = { Text("Стоимость работ (₽)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
                        )
                    }
                    item {
                        // Date info block
                        var showDatePicker by remember { mutableStateOf(false) }
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Дата начала укладки", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(selectedDate.time), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState(
                                initialSelectedDateMillis = selectedDate.timeInMillis
                            )
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            val cal = Calendar.getInstance()
                                            cal.timeInMillis = it
                                            selectedDate = cal
                                        }
                                        showDatePicker = false
                                    }) {
                                        Text("ОК")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("Отмена")
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = { orderNotes = it },
                            label = { Text("Дополнительные заметки (сложность, заусовка...)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (orderTitle.isNotBlank() && clientName.isNotBlank()) {
                            viewModel.addOrder(
                                title = orderTitle,
                                clientName = clientName,
                                clientPhone = clientPhone,
                                dateMillis = selectedDate.timeInMillis,
                                address = orderAddress,
                                notes = orderNotes,
                                totalCost = orderCost.toDoubleOrNull() ?: 0.0,
                                status = "PLANNED"
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // --- Order Detail Dialog (Full info + Actions) ---
    detailOrder?.let { order ->
        var editMode by remember { mutableStateOf(false) }
        var editTitle by remember { mutableStateOf(order.title) }
        var editClientName by remember { mutableStateOf(order.clientName) }
        var editClientPhone by remember { mutableStateOf(order.clientPhone) }
        var editAddress by remember { mutableStateOf(order.address) }
        var editCost by remember { mutableStateOf(order.totalCost.toString()) }
        var editNotes by remember { mutableStateOf(order.notes) }
        var editStatus by remember { mutableStateOf(order.status) }

        AlertDialog(
            onDismissRequest = { detailOrder = null },
            title = {
                Text(if (editMode) "Редактирование заказа" else order.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (editMode) {
                        item {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Объект укладки") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editClientName,
                                onValueChange = { editClientName = it },
                                label = { Text("Имя клиента") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editClientPhone,
                                onValueChange = { editClientPhone = it },
                                label = { Text("Телефон") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editAddress,
                                onValueChange = { editAddress = it },
                                label = { Text("Адрес") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editCost,
                                onValueChange = { editCost = it },
                                label = { Text("Цена (₽)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editNotes,
                                onValueChange = { editNotes = it },
                                label = { Text("Заметки") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Статус заказа:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("PLANNED" to "План", "IN_PROGRESS" to "В работе", "DONE" to "Готово").forEach { (code, lbl) ->
                                    FilterChip(
                                        selected = editStatus == code,
                                        onClick = { editStatus = code },
                                        label = { Text(lbl) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        // View mode
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
                        
                        item {
                            // Date row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dateFormat.format(Date(order.dateMillis)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        item {
                            // Status row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(status = order.status)
                            }
                        }

                        item {
                            HorizontalDivider()
                        }

                        item {
                            // Client details
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Заказчик:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.clientName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    if (order.clientPhone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.clientPhone}"))
                                                context.startActivity(intent)
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Позвонить", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                                if (order.clientPhone.isNotBlank()) {
                                    Text(order.clientPhone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (order.address.isNotBlank()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Адрес работы:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(order.address, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Бюджет / Стоимость:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%,.0f ₽".format(order.totalCost), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }

                        if (order.notes.isNotBlank()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Заметки мастера:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            order.notes,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (editMode) {
                    Button(
                        onClick = {
                            viewModel.updateOrder(
                                order.copy(
                                    title = editTitle,
                                    clientName = editClientName,
                                    clientPhone = editClientPhone,
                                    address = editAddress,
                                    totalCost = editCost.toDoubleOrNull() ?: 0.0,
                                    notes = editNotes,
                                    status = editStatus
                                )
                            )
                            detailOrder = null
                        }
                    ) {
                        Text("Сохранить")
                    }
                } else {
                    Button(
                        onClick = { editMode = true }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Изменить")
                    }
                }
            },
            dismissButton = {
                if (editMode) {
                    TextButton(onClick = { editMode = false }) {
                        Text("Назад")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.deleteOrder(order.id)
                                detailOrder = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                        TextButton(onClick = { detailOrder = null }) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM", Locale("ru"))
    val dateFullFormat = SimpleDateFormat("yyyy", Locale("ru"))
    val date = Date(order.dateMillis)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Calendar Sheet Display (Left Side) ---
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(60.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val cal = Calendar.getInstance().apply { time = date }
                    Text(
                        text = cal.get(Calendar.DAY_OF_MONTH).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = SimpleDateFormat("MMM", Locale("ru")).format(date).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // --- Order Body ---
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (order.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = order.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(status = order.status)
                    Text(
                        text = "%,.0f ₽".format(order.totalCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // --- Arrow details ---
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, bgColor, textColor) = when (status) {
        "PLANNED" -> Triple("Запланирован", Color(0xFFE3F2FD), Color(0xFF1565C0))
        "IN_PROGRESS" -> Triple("В процессе", Color(0xFFFFF3E0), Color(0xFFE65100))
        "DONE" -> Triple("Завершен", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple("Неизвестно", Color(0xFFECEFF1), Color(0xFF37474F))
    }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
