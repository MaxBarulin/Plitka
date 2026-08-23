package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R

// Data model for portfolio items (either predefined or custom added)
data class PortfolioWork(
    val id: Int,
    val title: String,
    val category: String, // BATHROOM, KITCHEN, FLOOR
    val description: String,
    val specs: String, // Tile size, grout, alignment system
    val durationDays: Int,
    val price: Double,
    val drawableResId: Int // Resource ID of our generated images
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    modifier: Modifier = Modifier
) {
    // Hardcoded generated image IDs from drawable
    val imgMarble = R.drawable.img_portfolio_marble_1787497478640
    val imgHexagon = R.drawable.img_portfolio_hexagon_1787497492756
    val imgTerrazzo = R.drawable.img_portfolio_terrazzo_1787497506170

    // Initial pre-loaded portfolio list using our stunning generated photos
    val initialWorks = remember {
        listOf(
            PortfolioWork(
                id = 1,
                title = "Элитный санузел под мрамор",
                category = "BATHROOM",
                description = "Полная облицовка ванной комнаты крупноформатным керамогранитом 60х120 см. Выполнена идеальная заусовка (запил углов под 45 градусов), установлена скрытая ревизионная ниша под плитку, эпоксидная затирка Litokol.",
                specs = "Керамогранит 60x120 | Затирка эпоксидная | Углы 45° | СВП",
                durationDays = 14,
                price = 145000.0,
                drawableResId = imgMarble
            ),
            PortfolioWork(
                id = 2,
                title = "Геометрический фартук на кухне",
                category = "KITCHEN",
                description = "Монтаж кухонного фартука из мелкоформатной плитки типа 'гексагон' (шестиугольники). Очень сложная раскладка с идеальным сведением швов в плоскости. Затирка контрастных оттенков.",
                specs = "Гексагоны 15x15 | Цементная затирка | Сложный контур",
                durationDays = 4,
                price = 32000.0,
                drawableResId = imgHexagon
            ),
            PortfolioWork(
                id = 3,
                title = "Бесшовный пол в гостиной",
                category = "FLOOR",
                description = "Укладка ректифицированного керамогранита с текстурой терраццо на водяной теплый пол. Шов 1.5 мм. Использовалась профессиональная система выравнивания плитки (СВП), компенсационные швы по периметру.",
                specs = "Керамогранит 80x80 | Теплый пол | Шов 1.5мм | СВП",
                durationDays = 6,
                price = 68000.0,
                drawableResId = imgTerrazzo
            )
        )
    }

    var worksList by remember { mutableStateOf(initialWorks) }
    var selectedCategory by remember { mutableStateOf("ALL") } // ALL, BATHROOM, KITCHEN, FLOOR

    val filteredWorks = remember(worksList, selectedCategory) {
        if (selectedCategory == "ALL") worksList else worksList.filter { it.category == selectedCategory }
    }

    // New Work Form Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newSpecs by remember { mutableStateOf("") }
    var newDuration by remember { mutableStateOf("5") }
    var newPrice by remember { mutableStateOf("45000") }
    var newCategory by remember { mutableStateOf("BATHROOM") }
    var selectedImageRes by remember { mutableStateOf(imgMarble) } // default choice

    // Active detail item popup
    var activeDetailWork by remember { mutableStateOf<PortfolioWork?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newTitle = ""
                    newDesc = ""
                    newSpecs = ""
                    newDuration = "5"
                    newPrice = "45000"
                    newCategory = "BATHROOM"
                    selectedImageRes = imgMarble
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Добавить в портфолио")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Welcome Profile Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handyman,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Мастер-Облицовщик",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Профессиональное портфолио для показа клиентам",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- Category Filters ---
            ScrollableTabRow(
                selectedTabIndex = when (selectedCategory) {
                    "ALL" -> 0
                    "BATHROOM" -> 1
                    "KITCHEN" -> 2
                    "FLOOR" -> 3
                    else -> 0
                },
                edgePadding = 16.dp,
                divider = {},
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = selectedCategory == "ALL",
                    onClick = { selectedCategory = "ALL" },
                    text = { Text("Все работы") }
                )
                Tab(
                    selected = selectedCategory == "BATHROOM",
                    onClick = { selectedCategory = "BATHROOM" },
                    text = { Text("Ванные комнаты") }
                )
                Tab(
                    selected = selectedCategory == "KITCHEN",
                    onClick = { selectedCategory = "KITCHEN" },
                    text = { Text("Кухонные фартуки") }
                )
                Tab(
                    selected = selectedCategory == "FLOOR",
                    onClick = { selectedCategory = "FLOOR" },
                    text = { Text("Полы и крупный формат") }
                )
            }

            // --- Gallery list ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
            ) {
                items(filteredWorks) { work ->
                    PortfolioCard(
                        work = work,
                        onClick = { activeDetailWork = work }
                    )
                }
            }
        }
    }

    // --- Add Portfolio Work Dialog ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить работу в портфолио") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Название работы (например: Санузел в стиле Лофт)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        Text("Категория работы:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("BATHROOM" to "Ванная", "KITCHEN" to "Кухня", "FLOOR" to "Пол").forEach { (code, lbl) ->
                                FilterChip(
                                    selected = newCategory == code,
                                    onClick = { newCategory = code },
                                    label = { Text(lbl) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = newSpecs,
                            onValueChange = { newSpecs = it },
                            label = { Text("Характеристики (плитка, затирка, швы)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newDuration,
                                onValueChange = { newDuration = it },
                                label = { Text("Срок (дней)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newPrice,
                                onValueChange = { newPrice = it },
                                label = { Text("Стоимость (₽)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Подробное описание работы") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                    }
                    item {
                        Text("Выберите демонстрационное фото:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                imgMarble to "Мрамор",
                                imgHexagon to "Мозаика",
                                imgTerrazzo to "Терраццо"
                            ).forEach { (resId, name) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedImageRes == resId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedImageRes = resId }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Gray)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (selectedImageRes == resId) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val newItem = PortfolioWork(
                                id = worksList.size + 1,
                                title = newTitle,
                                category = newCategory,
                                description = newDesc,
                                specs = newSpecs,
                                durationDays = newDuration.toIntOrNull() ?: 5,
                                price = newPrice.toDoubleOrNull() ?: 40000.0,
                                drawableResId = selectedImageRes
                            )
                            worksList = listOf(newItem) + worksList
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

    // --- Active Detail Portfolio Work View ---
    activeDetailWork?.let { work ->
        AlertDialog(
            onDismissRequest = { activeDetailWork = null },
            title = {
                Text(work.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = work.drawableResId),
                                contentDescription = work.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    item {
                        Text(
                            text = when (work.category) {
                                "BATHROOM" -> "Категория: Ванная комната"
                                "KITCHEN" -> "Категория: Кухня / Фартук"
                                "FLOOR" -> "Категория: Полы и крупный формат"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        HorizontalDivider()
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Подробное описание работы:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(work.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Технические характеристики:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    work.specs,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Срок выполнения:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${work.durationDays} дн.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column {
                                Text("Стоимость работ:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("%,.0f ₽".format(work.price), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeDetailWork = null }) {
                    Text("Отлично")
                }
            }
        )
    }
}

@Composable
fun PortfolioCard(
    work: PortfolioWork,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero image with visual gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = work.drawableResId),
                    contentDescription = work.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Linear gradient top to bottom to make text highly readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                
                // Badges
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f))
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "%,.0f ₽".format(work.price),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }

                // Title overlay at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = work.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (work.category) {
                            "BATHROOM" -> "Ванные"
                            "KITCHEN" -> "Кухни"
                            "FLOOR" -> "Полы"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Description block
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = work.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = work.specs,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${work.durationDays} дн.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
