package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.data.Calculation
import com.example.ui.TileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: TileViewModel,
    modifier: Modifier = Modifier
) {
    val calculations by viewModel.allCalculations.collectAsState()
    val focusManager = LocalFocusManager.current

    // Inputs State
    var areaInput by remember { mutableStateOf("10") }
    var tileWidthInput by remember { mutableStateOf("30") }
    var tileHeightInput by remember { mutableStateOf("60") }
    var groutWidthInput by remember { mutableStateOf("2") }
    var tilePriceInput by remember { mutableStateOf("1500") }
    var glueConsInput by remember { mutableStateOf("4.5") }
    var glueBagWeightInput by remember { mutableStateOf("25") }
    var gluePriceInput by remember { mutableStateOf("600") }
    var groutPriceInput by remember { mutableStateOf("300") }

    // Dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var calcNameInput by remember { mutableStateOf("") }

    // Parse values safely
    val area = areaInput.toDoubleOrNull() ?: 0.0
    val tileWidth = tileWidthCmInputParser(tileWidthInput)
    val tileHeight = tileWidthCmInputParser(tileHeightInput)
    val groutWidth = groutWidthInputParser(groutWidthInput)
    val tilePrice = tilePriceInput.toDoubleOrNull() ?: 0.0
    val glueCons = glueConsInput.toDoubleOrNull() ?: 0.0
    val glueBagWeight = glueBagWeightInput.toDoubleOrNull() ?: 25.0
    val gluePrice = gluePriceInput.toDoubleOrNull() ?: 0.0
    val groutPrice = groutPriceInput.toDoubleOrNull() ?: 0.0

    // Compute live results
    val results = viewModel.runCalculation(
        areaSqM = area,
        tileWidthCm = tileWidth,
        tileHeightCm = tileHeight,
        groutWidthMm = groutWidth,
        tilePricePerSqM = tilePrice,
        glueConsKgPerSqM = glueCons,
        glueBagWeightKg = glueBagWeight,
        gluePricePerBag = gluePrice,
        groutPricePerKg = groutPrice
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
    ) {
        // --- Total Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ОБЩАЯ СТОИМОСТЬ МАТЕРИАЛОВ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%,.0f ₽".format(results.totalCost),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Площадь: $area м²",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Плитка: ${tileWidth.toInt()}x${tileHeight.toInt()} см",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // --- Inputs Grid ---
        item {
            Text(
                text = "Параметры укладки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Area and Grout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = areaInput,
                            onValueChange = { areaInput = it },
                            label = { Text("Площадь (м²)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = groutWidthInput,
                            onValueChange = { groutWidthInput = it },
                            label = { Text("Шов (мм)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Row 2: Tile dimensions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tileWidthInput,
                            onValueChange = { tileWidthInput = it },
                            label = { Text("Шир. плитки (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tileHeightInput,
                            onValueChange = { tileHeightInput = it },
                            label = { Text("Выс. плитки (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Row 3: Tile price
                    OutlinedTextField(
                        value = tilePriceInput,
                        onValueChange = { tilePriceInput = it },
                        label = { Text("Цена плитки за м² (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
                    )

                    // Expandable / Advanced params header
                    var showAdvanced by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAdvanced) "Скрыть доп. параметры" else "Настройки клея и затирки")
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = glueConsInput,
                                    onValueChange = { glueConsInput = it },
                                    label = { Text("Расход клея (кг/м²)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = glueBagWeightInput,
                                    onValueChange = { glueBagWeightInput = it },
                                    label = { Text("Вес мешка (кг)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = gluePriceInput,
                                    onValueChange = { gluePriceInput = it },
                                    label = { Text("Цена клея (₽/мешок)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = groutPriceInput,
                                    onValueChange = { groutPriceInput = it },
                                    label = { Text("Цена затирки (₽/кг)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Action Buttons ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        calcNameInput = "Расчет ${area}м² (${tileWidth.toInt()}x${tileHeight.toInt()})"
                        showSaveDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить расчет")
                }
            }
        }

        // --- Interactive Tile Layout Visualizer ---
        item {
            TileLayoutVisualizer(
                tileWidthCm = tileWidth,
                tileHeightCm = tileHeight,
                groutWidthMm = groutWidth
            )
        }

        // --- Detailed Results Breakdown ---
        item {
            Text(
                text = "Детализация расчетов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Tiles Card
                MaterialBreakdownCard(
                    title = "Керамогранит / Плитка (+10% запас)",
                    mainQuantity = "%d шт.".format(results.tileCount),
                    subDetails = "Площадь с запасом: %.2f м²".format(results.tileSqMNeededWithMargin),
                    cost = results.tileCost,
                    icon = Icons.Default.Dashboard
                )

                // Glue Card
                MaterialBreakdownCard(
                    title = "Плиточный клей",
                    mainQuantity = "%d меш. (%d кг)".format(results.glueBagsNeeded, (results.glueBagsNeeded * glueBagWeight).toInt()),
                    subDetails = "Расход: $glueCons кг/м²",
                    cost = results.glueCost,
                    icon = Icons.Default.Layers
                )

                // Grout Card
                MaterialBreakdownCard(
                    title = "Затирка для швов",
                    mainQuantity = "%.1f кг".format(results.groutKgNeeded),
                    subDetails = "Шов: $groutWidth мм, глубина: 8мм",
                    cost = results.groutCost,
                    icon = Icons.Default.GridOn
                )
            }
        }

        // --- Calculation History ---
        if (calculations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "История расчетов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(calculations) { calc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusManager.clearFocus()
                            // Load saved calculation back into active fields
                            areaInput = calc.areaSqM.toString()
                            tileWidthInput = calc.tileWidthCm.toString()
                            tileHeightInput = calc.tileHeightCm.toString()
                            groutWidthInput = calc.groutWidthMm.toString()
                            tilePriceInput = calc.tilePricePerSqM.toString()
                            glueConsInput = calc.glueConsKgPerSqM.toString()
                            glueBagWeightInput = calc.glueBagWeightKg.toString()
                            gluePriceInput = calc.gluePricePerBag.toString()
                            groutPriceInput = calc.groutPricePerKg.toString()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = calc.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Плитка ${calc.tileWidthCm.toInt()}x${calc.tileHeightCm.toInt()} см | Шов ${calc.groutWidthMm} мм",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Материалы: %,.0f ₽".format(calc.calculatedTotalMaterialCost),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteCalculation(calc.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Удалить расчет",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Save Dialog ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить этот расчет") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Введите название для быстрого поиска в истории:")
                    OutlinedTextField(
                        value = calcNameInput,
                        onValueChange = { calcNameInput = it },
                        label = { Text("Название расчета") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (calcNameInput.isNotBlank()) {
                            viewModel.saveCalculation(
                                name = calcNameInput,
                                areaSqM = area,
                                tileWidthCm = tileWidth,
                                tileHeightCm = tileHeight,
                                groutWidthMm = groutWidth,
                                tilePricePerSqM = tilePrice,
                                glueConsKgPerSqM = glueCons,
                                glueBagWeightKg = glueBagWeight,
                                gluePricePerBag = gluePrice,
                                groutPricePerKg = groutPrice
                            )
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun MaterialBreakdownCard(
    title: String,
    mainQuantity: String,
    subDetails: String,
    cost: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mainQuantity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "%,.0f ₽".format(cost),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// Helpers for input parsers
private fun tileWidthCmInputParser(input: String): Double {
    val clean = input.replace(",", ".")
    return clean.toDoubleOrNull() ?: 0.0
}

private fun groutWidthInputParser(input: String): Double {
    val clean = input.replace(",", ".")
    return clean.toDoubleOrNull() ?: 0.0
}

enum class LayoutPattern {
    GRID,
    BRICK_50,
    BRICK_33,
    DIAGONAL
}

@Composable
fun TileLayoutVisualizer(
    tileWidthCm: Double,
    tileHeightCm: Double,
    groutWidthMm: Double,
    modifier: Modifier = Modifier
) {
    var pattern by remember { mutableStateOf(LayoutPattern.GRID) }
    var shiftX by remember { mutableStateOf(0f) } // in percent (0 to 100)
    var shiftY by remember { mutableStateOf(0f) } // in percent (0 to 100)

    val validTileWidth = if (tileWidthCm > 0) tileWidthCm else 30.0
    val validTileHeight = if (tileHeightCm > 0) tileHeightCm else 60.0
    val validGroutWidth = groutWidthMm.coerceAtLeast(0.0)

    val tileColor = MaterialTheme.colorScheme.primaryContainer
    val groutColor = MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Интерактивная раскладка (Пол / Стены)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Настройте центровку швов с помощью ползунков, чтобы избежать узких полосок-обрезков у стен.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Pattern Selector tabs
            ScrollableTabRow(
                selectedTabIndex = pattern.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                LayoutPattern.values().forEach { pat ->
                    val selected = pattern == pat
                    Tab(
                        selected = selected,
                        onClick = { pattern = pat },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                        text = {
                            Text(
                                text = when (pat) {
                                    LayoutPattern.GRID -> "Сетка"
                                    LayoutPattern.BRICK_50 -> "Разбежка 50%"
                                    LayoutPattern.BRICK_33 -> "Разбежка 33%"
                                    LayoutPattern.DIAGONAL -> "Диагональ"
                                },
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasW = size.width
                    val canvasH = size.height

                    // Simulated floor region is 240cm x 160cm
                    val simW = 240f
                    val simH = 160f

                    // Aspect ratio fitting
                    val scaleX = canvasW / simW
                    val scaleY = canvasH / simH
                    val scale = minOf(scaleX, scaleY)

                    val roomW = simW * scale
                    val roomH = simH * scale

                    // Centering floor rectangle in the Canvas
                    val startX = (canvasW - roomW) / 2f
                    val startY = (canvasH - roomH) / 2f

                    // Draw outer floor rect fill background (darker background for joint lines)
                    drawRect(
                        color = groutColor.copy(alpha = 0.3f),
                        topLeft = Offset(startX, startY),
                        size = Size(roomW, roomH)
                    )

                    clipRect(
                        left = startX,
                        top = startY,
                        right = startX + roomW,
                        bottom = startY + roomH
                    ) {
                        // Calculate shift offset in cm based on slider percentage
                        val tileWPlusGrout = (validTileWidth + (validGroutWidth / 10.0)).toFloat()
                        val tileHPlusGrout = (validTileHeight + (validGroutWidth / 10.0)).toFloat()

                        val actualShiftX = (shiftX / 100f) * tileWPlusGrout
                        val actualShiftY = (shiftY / 100f) * tileHPlusGrout

                        val drawTiling = {
                            // Start tiling from negative coordinates to fully cover shift range
                            val minCol = -5
                            val maxCol = (simW / tileWPlusGrout).toInt() + 5
                            val minRow = -5
                            val maxRow = (simH / tileHPlusGrout).toInt() + 5

                            for (row in minRow..maxRow) {
                                for (col in minCol..maxCol) {
                                    // Base tile positions in cm
                                    var tileX = col * tileWPlusGrout + actualShiftX
                                    val tileY = row * tileHPlusGrout + actualShiftY

                                    // Apply offset depending on pattern
                                    if (pattern == LayoutPattern.BRICK_50) {
                                        tileX += (kotlin.math.abs(row) % 2) * 0.5f * tileWPlusGrout
                                    } else if (pattern == LayoutPattern.BRICK_33) {
                                        tileX += (kotlin.math.abs(row) % 3) * 0.33f * tileWPlusGrout
                                    }

                                    // Map to canvas pixel space
                                    val pxX = startX + tileX * scale
                                    val pxY = startY + tileY * scale
                                    val pxW = validTileWidth.toFloat() * scale
                                    val pxH = validTileHeight.toFloat() * scale

                                    // Draw tile face
                                    drawRect(
                                        color = tileColor,
                                        topLeft = Offset(pxX, pxY),
                                        size = Size(pxW, pxH)
                                    )

                                    // Draw tile grout borders
                                    drawRect(
                                        color = groutColor,
                                        topLeft = Offset(pxX, pxY),
                                        size = Size(pxW, pxH),
                                        style = Stroke(width = (validGroutWidth.toFloat() / 10f * scale).coerceAtLeast(1.5f))
                                    )
                                }
                            }
                        }

                        if (pattern == LayoutPattern.DIAGONAL) {
                            withTransform({
                                // Rotate around center of simulated room
                                rotate(45f, pivot = Offset(startX + roomW / 2f, startY + roomH / 2f))
                            }) {
                                drawTiling()
                            }
                        } else {
                            drawTiling()
                        }
                    }

                    // Draw outer border of the room
                    drawRect(
                        color = groutColor,
                        topLeft = Offset(startX, startY),
                        size = Size(roomW, roomH),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Sliders for Centering adjustments
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Смещение швов по горизонтали",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${shiftX.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = shiftX,
                    onValueChange = { shiftX = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Смещение швов по вертикали",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${shiftY.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = shiftY,
                    onValueChange = { shiftY = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Legend / Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Имитация зоны: 2.4 x 1.6 м",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Плитка: ${validTileWidth.toInt()}x${validTileHeight.toInt()} см",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
