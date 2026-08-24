package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Calculation
import com.example.ui.TileViewModel
import com.example.ui.cad.InfoRow
import com.example.ui.cad.NumberStepperField
import com.example.ui.cad.fmtNum
import com.example.ui.calc.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private fun money(v: Double): String = "%,.0f ₽".format(v).replace(',', ' ')

@Composable
fun CalculatorScreen(
    viewModel: TileViewModel,
    modifier: Modifier = Modifier
) {
    val calculations by viewModel.allCalculations.collectAsState()
    val handoff by viewModel.cadHandoff.collectAsState()

    // ------------------------------------------------------------------ Объект
    var areaM2 by remember { mutableStateOf(10.0) }
    var perimeterM by remember { mutableStateOf(13.0) }
    var openingsM by remember { mutableStateOf(0.9) }

    // ------------------------------------------------------------------ Плитка
    var tileW by remember { mutableStateOf(600.0) }
    var tileH by remember { mutableStateOf(600.0) }
    var tileThickness by remember { mutableStateOf(9.0) }
    var grout by remember { mutableStateOf(2.0) }
    var wastePercent by remember { mutableStateOf(10.0) }
    var piecesPerPack by remember { mutableStateOf(4.0) }
    var tilePriceM2 by remember { mutableStateOf(1800.0) }
    var tilePricePiece by remember { mutableStateOf(0.0) }
    var density by remember { mutableStateOf(2400.0) }

    // ------------------------------------------------------------------ Клей
    var notchIndex by remember { mutableStateOf(4) }
    var glueKgM2 by remember { mutableStateOf(TROWEL_TABLE[4].consumptionKgM2) }
    var levelingMm by remember { mutableStateOf(0.0) }
    var backButtering by remember { mutableStateOf(false) }
    var glueBagKg by remember { mutableStateOf(25.0) }
    var gluePrice by remember { mutableStateOf(700.0) }
    var glueReserve by remember { mutableStateOf(10.0) }

    // ------------------------------------------------------------------ Затирка
    var groutDensity by remember { mutableStateOf(1.8) }
    var groutEpoxy by remember { mutableStateOf(false) }
    var groutReserve by remember { mutableStateOf(10.0) }
    var groutPackKg by remember { mutableStateOf(2.0) }
    var groutPriceKg by remember { mutableStateOf(400.0) }

    // ------------------------------------------------------------------ Основание
    var screedOn by remember { mutableStateOf(false) }
    var screedThickness by remember { mutableStateOf(20.0) }
    var screedKgMm by remember { mutableStateOf(1.6) }
    var screedBagKg by remember { mutableStateOf(20.0) }
    var screedPrice by remember { mutableStateOf(450.0) }

    var primerOn by remember { mutableStateOf(true) }
    var primerKg by remember { mutableStateOf(0.15) }
    var primerLayers by remember { mutableStateOf(2.0) }
    var primerPackKg by remember { mutableStateOf(5.0) }
    var primerPrice by remember { mutableStateOf(900.0) }

    var waterproofOn by remember { mutableStateOf(false) }
    var wpKg by remember { mutableStateOf(1.2) }
    var wpLayers by remember { mutableStateOf(2.0) }
    var wpPackKg by remember { mutableStateOf(20.0) }
    var wpPrice by remember { mutableStateOf(3500.0) }

    // ------------------------------------------------------------------ Расходники
    var crossesPerTile by remember { mutableStateOf(2.0) }
    var clipsPerTile by remember { mutableStateOf(3.0) }
    var dailyOutputM2 by remember { mutableStateOf(6.0) }
    var priceCross by remember { mutableStateOf(0.6) }
    var priceClip by remember { mutableStateOf(3.5) }
    var priceWedge by remember { mutableStateOf(4.0) }

    // ------------------------------------------------------------------ Погонаж
    var trimOn by remember { mutableStateOf(false) }
    var trimPieceM by remember { mutableStateOf(2.5) }
    var trimReserve by remember { mutableStateOf(10.0) }
    var trimPrice by remember { mutableStateOf(350.0) }

    // ------------------------------------------------------------------ Работа
    var workPriceM2 by remember { mutableStateOf(1500.0) }
    var tripCapacity by remember { mutableStateOf(60.0) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var calcName by remember { mutableStateOf("") }

    // Подхват данных из CAD-редактора
    LaunchedEffect(handoff?.stamp) {
        handoff?.let {
            areaM2 = it.areaM2
            perimeterM = it.perimeterM
            tileW = it.tileWidthMm
            tileH = it.tileHeightMm
            grout = it.groutMm
            val n = suggestNotch(max(it.tileWidthMm, it.tileHeightMm))
            notchIndex = TROWEL_TABLE.indexOf(n)
            glueKgM2 = n.consumptionKgM2
            viewModel.consumeCadHandoff()
        }
    }

    // ================================================================== РАСЧЁТЫ
    val tile = calcTile(
        areaM2, tileW, tileH, grout, wastePercent, piecesPerPack,
        tilePriceM2, tilePricePiece, tileThickness, density
    )
    val glue = calcGlue(areaM2, glueKgM2, levelingMm, backButtering, glueBagKg, gluePrice, glueReserve)
    val groutRes = calcGrout(
        areaM2, tileW, tileH, grout, tileThickness, groutDensity,
        groutReserve, groutEpoxy, groutPackKg, groutPriceKg
    )
    val screed = if (screedOn) calcScreed(areaM2, screedThickness, screedKgMm, screedBagKg, screedPrice)
    else DryMixResult(0.0, 0.0, 0, 0.0)
    val primer = if (primerOn) calcCoating(areaM2, primerKg, primerLayers.toInt(), primerPackKg, primerPrice, 0.0)
    else CoatingResult(0.0, 0, 0.0, 0.0)
    val waterproof = if (waterproofOn) calcCoating(areaM2, wpKg, wpLayers.toInt(), wpPackKg, wpPrice, perimeterM)
    else CoatingResult(0.0, 0, 0.0, 0.0)
    val tileAreaM2 = (tileW / 1000.0) * (tileH / 1000.0)
    val dailyTiles = if (tileAreaM2 > 0) dailyOutputM2 / tileAreaM2 else 0.0
    val consum = calcConsumables(tile.pieces, crossesPerTile, clipsPerTile, dailyTiles, priceCross, priceClip, priceWedge)
    val trim = if (trimOn) calcTrim(perimeterM, openingsM, trimPieceM, trimReserve, trimPrice)
    else TrimResult(0.0, 0, 0.0)
    val work = calcWork(areaM2, dailyOutputM2, workPriceM2)
    val totals = calcTotals(
        materials = listOf(
            tile.cost, glue.cost, groutRes.cost, screed.cost,
            primer.cost, waterproof.cost, consum.cost, trim.cost
        ),
        laborCost = work.laborCost,
        weights = listOf(
            tile.weightKg, glue.totalKg, groutRes.totalKg,
            screed.totalKg, primer.totalKg, waterproof.totalKg
        ),
        tripCapacityKg = tripCapacity
    )

    // Быстрая раскладка по габаритам «условного прямоугольника» из площади и периметра
    val sideA = remember(areaM2, perimeterM) {
        val p = perimeterM / 2.0
        val disc = p * p - 4 * areaM2
        if (disc >= 0) (p + kotlin.math.sqrt(disc)) / 2.0 else kotlin.math.sqrt(areaM2)
    }
    val sideB = if (sideA > 0.01) areaM2 / sideA else 0.0
    val rowsA = calcRows(sideA * 1000, tileW, grout)
    val rowsB = calcRows(sideB * 1000, tileH, grout)

    // ================================================================== UI
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // ---------------------------------------------------------- Итог
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ИТОГО ПО ОБЪЕКТУ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        money(totals.grandTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    InfoRow("Материалы", money(totals.materialsCost))
                    InfoRow("Работа (${fmtNum(areaM2, 2)} м² × ${fmtNum(workPriceM2, 0)} ₽)", money(totals.laborCost))
                    InfoRow("Срок", "${fmtNum(work.days, 1)} смен")
                    InfoRow("Вес материалов", "${fmtNum(totals.totalWeightKg, 0)} кг · ${totals.trips} ходок")
                }
            }
        }

        // ---------------------------------------------------------- Шпаргалка
        item {
            CalcCard("Шпаргалка: главное на объекте", Icons.Default.Bolt, initiallyExpanded = true) {
                InfoRow("Плитки купить", "${tile.pieces} шт" + if (tile.packs > 0) " (${tile.packs} уп.)" else "", true)
                InfoRow("Плитки по площади", "${fmtNum(tile.piecesAreaM2, 2)} м² с запасом ${fmtNum(wastePercent, 0)} %")
                InfoRow("Клей", "${fmtNum(glue.totalKg, 1)} кг = ${glue.bags} меш. × ${fmtNum(glueBagKg, 0)} кг", true)
                InfoRow("Затирка", "${fmtNum(groutRes.totalKg, 2)} кг" + if (groutRes.packs > 0) " (${groutRes.packs} уп.)" else "", true)
                InfoRow("Длина швов", "${fmtNum(groutRes.jointLengthM, 0)} п.м.")
                InfoRow("Крестики / зажимы СВП", "${consum.crosses} / ${consum.clips} шт")
                InfoRow("Клинья СВП (на смену)", "${consum.wedges} шт")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    "Рядов при габарите ${fmtNum(sideA, 2)}×${fmtNum(sideB, 2)} м:",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
                InfoRow(
                    "По длине",
                    "${rowsA.fullRows} целых + подрезка ${fmtNum(rowsA.lastRowMm, 0)} мм",
                    rowsA.recommendCentering
                )
                InfoRow(
                    "По ширине",
                    "${rowsB.fullRows} целых + подрезка ${fmtNum(rowsB.lastRowMm, 0)} мм",
                    rowsB.recommendCentering
                )
                if (rowsA.recommendCentering || rowsB.recommendCentering) {
                    Text(
                        "Подрезка уже трети плитки — разложите симметрично от центра: " +
                            "тогда с обеих сторон будет по ${fmtNum(if (rowsA.recommendCentering) rowsA.centeredEdgeMm else rowsB.centeredEdgeMm, 0)} мм.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ---------------------------------------------------------- Объект
        item {
            CalcCard("Помещение", Icons.Default.SquareFoot) {
                NumberStepperField("Площадь облицовки", areaM2, { areaM2 = it }, 0.5, suffix = "м²", decimals = 2, min = 0.0, max = 10000.0)
                NumberStepperField("Периметр", perimeterM, { perimeterM = it }, 0.1, suffix = "м", decimals = 2, min = 0.0, max = 1000.0)
                NumberStepperField("Проёмы (двери) в периметре", openingsM, { openingsM = it }, 0.1, suffix = "м", decimals = 2, min = 0.0, max = 1000.0)
                Text(
                    "Площадь и периметр можно передать сюда прямо из CAD-плана: " +
                        "вкладка «Раскладка» → «Анализ» → «Отправить в калькулятор».",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------------------------------------------------- Плитка
        item {
            CalcCard("Плитка", Icons.Default.GridOn) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        200.0 to 200.0, 300.0 to 300.0, 300.0 to 600.0,
                        600.0 to 600.0, 600.0 to 1200.0, 800.0 to 800.0
                    ).forEach { (w, h) ->
                        AssistChip(
                            onClick = { tileW = w; tileH = h },
                            label = { Text("${w.toInt()}×${h.toInt()}", fontSize = 11.sp) }
                        )
                    }
                }
                NumberStepperField("Ширина плитки", tileW, { tileW = it }, 10.0, suffix = "мм", decimals = 1, min = 5.0, max = 4000.0)
                NumberStepperField("Длина плитки", tileH, { tileH = it }, 10.0, suffix = "мм", decimals = 1, min = 5.0, max = 4000.0)
                NumberStepperField("Толщина плитки", tileThickness, { tileThickness = it }, 0.5, suffix = "мм", decimals = 1, min = 2.0, max = 40.0)
                NumberStepperField("Ширина шва", grout, { grout = it }, 0.5, suffix = "мм", decimals = 1, min = 0.0, max = 30.0)

                Text("Запас на подрезку и бой:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WASTE_PRESETS.forEach { w ->
                        AssistChip(
                            onClick = { wastePercent = w.percent },
                            label = { Text("${w.title} ${w.percent.toInt()}%", fontSize = 10.sp) }
                        )
                    }
                }
                NumberStepperField("Запас", wastePercent, { wastePercent = it }, 1.0, suffix = "%", decimals = 1, min = 0.0, max = 60.0)
                NumberStepperField("Штук в упаковке", piecesPerPack, { piecesPerPack = it }, 1.0, suffix = "шт", decimals = 0, min = 0.0, max = 200.0)
                NumberStepperField("Цена за м²", tilePriceM2, { tilePriceM2 = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 500000.0)
                NumberStepperField("или цена за штуку", tilePricePiece, { tilePricePiece = it }, 10.0, suffix = "₽", decimals = 0, min = 0.0, max = 500000.0)
                NumberStepperField("Плотность материала", density, { density = it }, 50.0, suffix = "кг/м³", decimals = 0, min = 500.0, max = 4000.0)

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("Площадь с запасом", "${fmtNum(tile.areaWithWasteM2, 2)} м²")
                InfoRow("Количество", "${tile.pieces} шт", true)
                if (tile.packs > 0) InfoRow("Упаковок", "${tile.packs} шт")
                InfoRow("Вес плитки", "${fmtNum(tile.weightKg, 0)} кг")
                InfoRow("Стоимость", money(tile.cost), true)
            }
        }

        // ---------------------------------------------------------- Клей
        item {
            CalcCard("Плиточный клей", Icons.Default.Layers) {
                Text("Зуб гребёнки (задаёт базовый расход):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TROWEL_TABLE.forEachIndexed { i, n ->
                        FilterChip(
                            selected = notchIndex == i,
                            onClick = { notchIndex = i; glueKgM2 = n.consumptionKgM2 },
                            label = { Text("${n.notchMm.toInt()} мм", fontSize = 11.sp) }
                        )
                    }
                }
                val n = TROWEL_TABLE[notchIndex.coerceIn(0, TROWEL_TABLE.lastIndex)]
                Text(
                    "Зуб ${n.notchMm.toInt()} мм → слой ${n.layerMm}, база ${fmtNum(n.consumptionKgM2, 2)} кг/м², " +
                        "формат ${n.tileFormat}.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rec = suggestNotch(max(tileW, tileH))
                if (rec.notchMm != n.notchMm) {
                    Text(
                        "Для формата ${tileW.toInt()}×${tileH.toInt()} обычно берут зуб ${rec.notchMm.toInt()} мм.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.primary
                    )
                }
                NumberStepperField("Расход клея", glueKgM2, { glueKgM2 = it }, 0.25, suffix = "кг/м²", decimals = 2, min = 0.5, max = 30.0)
                NumberStepperField("Перепад основания (добор слоя)", levelingMm, { levelingMm = it }, 1.0, suffix = "мм", decimals = 1, min = 0.0, max = 50.0)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(backButtering, { backButtering = it })
                    Text("Двойное нанесение (крупный формат) ×1,6", fontSize = 12.sp)
                }
                NumberStepperField("Вес мешка", glueBagKg, { glueBagKg = it }, 5.0, suffix = "кг", decimals = 0, min = 1.0, max = 100.0)
                NumberStepperField("Цена мешка", gluePrice, { gluePrice = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)
                NumberStepperField("Запас", glueReserve, { glueReserve = it }, 5.0, suffix = "%", decimals = 0, min = 0.0, max = 50.0)

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("Фактический расход", "${fmtNum(glue.totalKgM2, 2)} кг/м²")
                InfoRow("Всего клея", "${fmtNum(glue.totalKg, 1)} кг")
                InfoRow("Мешков", "${glue.bags} шт", true)
                InfoRow("Стоимость", money(glue.cost), true)
            }
        }

        // ---------------------------------------------------------- Затирка
        item {
            CalcCard("Затирка швов", Icons.Default.BorderAll) {
                Text(
                    "Расход по формуле (A+B)·C·D·ρ/(A·B): стороны плитки, ширина шва, " +
                        "глубина шва (толщина плитки) и плотность затирки.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NumberStepperField("Плотность затирки", groutDensity, { groutDensity = it }, 0.1, suffix = "г/см³", decimals = 2, min = 1.0, max = 2.5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(groutEpoxy, { groutEpoxy = it })
                    Text("Эпоксидная (расход ×1,4)", fontSize = 12.sp)
                }
                NumberStepperField("Запас", groutReserve, { groutReserve = it }, 5.0, suffix = "%", decimals = 0, min = 0.0, max = 50.0)
                NumberStepperField("Вес упаковки", groutPackKg, { groutPackKg = it }, 0.5, suffix = "кг", decimals = 1, min = 0.0, max = 30.0)
                NumberStepperField("Цена за кг", groutPriceKg, { groutPriceKg = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("Расход", "${fmtNum(groutRes.kgPerM2, 3)} кг/м²")
                InfoRow("Всего затирки", "${fmtNum(groutRes.totalKg, 2)} кг", true)
                if (groutRes.packs > 0) InfoRow("Упаковок", "${groutRes.packs} шт")
                InfoRow("Погонаж швов", "${fmtNum(groutRes.jointLengthM, 0)} п.м.")
                InfoRow("Стоимость", money(groutRes.cost), true)
            }
        }

        // ---------------------------------------------------------- Основание
        item {
            CalcCard("Подготовка основания", Icons.Default.Foundation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(primerOn, { primerOn = it })
                    Text("Грунтовка", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(primerOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Расход на слой", primerKg, { primerKg = it }, 0.05, suffix = "кг/м²", decimals = 2, min = 0.01, max = 2.0)
                        NumberStepperField("Слоёв", primerLayers, { primerLayers = it }, 1.0, suffix = "шт", decimals = 0, min = 1.0, max = 5.0)
                        NumberStepperField("Канистра", primerPackKg, { primerPackKg = it }, 1.0, suffix = "кг", decimals = 1, min = 0.5, max = 50.0)
                        NumberStepperField("Цена канистры", primerPrice, { primerPrice = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)
                        InfoRow("Итого грунта", "${fmtNum(primer.totalKg, 2)} кг · ${primer.buckets} шт · ${money(primer.cost)}", true)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(waterproofOn, { waterproofOn = it })
                    Text("Гидроизоляция", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(waterproofOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Расход на слой", wpKg, { wpKg = it }, 0.1, suffix = "кг/м²", decimals = 2, min = 0.1, max = 5.0)
                        NumberStepperField("Слоёв", wpLayers, { wpLayers = it }, 1.0, suffix = "шт", decimals = 0, min = 1.0, max = 5.0)
                        NumberStepperField("Ведро", wpPackKg, { wpPackKg = it }, 1.0, suffix = "кг", decimals = 1, min = 1.0, max = 50.0)
                        NumberStepperField("Цена ведра", wpPrice, { wpPrice = it }, 100.0, suffix = "₽", decimals = 0, min = 0.0, max = 500000.0)
                        InfoRow("Итого состава", "${fmtNum(waterproof.totalKg, 2)} кг · ${waterproof.buckets} шт · ${money(waterproof.cost)}", true)
                        InfoRow("Гидролента по периметру", "${fmtNum(waterproof.tapeM, 1)} п.м.")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(screedOn, { screedOn = it })
                    Text("Стяжка / наливной пол", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(screedOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Толщина слоя", screedThickness, { screedThickness = it }, 1.0, suffix = "мм", decimals = 1, min = 1.0, max = 200.0)
                        NumberStepperField("Расход на 1 мм", screedKgMm, { screedKgMm = it }, 0.1, suffix = "кг/м²·мм", decimals = 2, min = 0.5, max = 3.0)
                        NumberStepperField("Вес мешка", screedBagKg, { screedBagKg = it }, 5.0, suffix = "кг", decimals = 0, min = 1.0, max = 100.0)
                        NumberStepperField("Цена мешка", screedPrice, { screedPrice = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)
                        InfoRow("Расход", "${fmtNum(screed.kgPerM2, 1)} кг/м²")
                        InfoRow("Итого смеси", "${fmtNum(screed.totalKg, 0)} кг · ${screed.bags} меш. · ${money(screed.cost)}", true)
                        Text(
                            "Ровнитель — примерно 1,5–1,8 кг/м² на каждый миллиметр слоя, " +
                                "цементно-песчаная стяжка — около 2 кг.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------- Расходники
        item {
            CalcCard("Крестики и СВП", Icons.Default.Extension) {
                NumberStepperField("Крестиков на плитку", crossesPerTile, { crossesPerTile = it }, 0.5, suffix = "шт", decimals = 1, min = 0.0, max = 8.0)
                NumberStepperField("Зажимов СВП на плитку", clipsPerTile, { clipsPerTile = it }, 0.5, suffix = "шт", decimals = 1, min = 0.0, max = 12.0)
                NumberStepperField("Выработка за смену", dailyOutputM2, { dailyOutputM2 = it }, 0.5, suffix = "м²", decimals = 1, min = 0.5, max = 100.0)
                NumberStepperField("Цена крестика", priceCross, { priceCross = it }, 0.1, suffix = "₽", decimals = 2, min = 0.0, max = 100.0)
                NumberStepperField("Цена зажима", priceClip, { priceClip = it }, 0.5, suffix = "₽", decimals = 2, min = 0.0, max = 100.0)
                NumberStepperField("Цена клина", priceWedge, { priceWedge = it }, 0.5, suffix = "₽", decimals = 2, min = 0.0, max = 100.0)

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("Крестики", "${consum.crosses} шт")
                InfoRow("Зажимы (одноразовые)", "${consum.clips} шт", true)
                InfoRow("Клинья (многоразовые)", "${consum.wedges} шт")
                InfoRow("Стоимость", money(consum.cost))
                Text(
                    "Клинья снимают на следующий день и используют повторно — их берут " +
                        "на дневную выработку, а не на весь объём.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---------------------------------------------------------- Погонаж
        item {
            CalcCard("Плинтус / профиль / уголок", Icons.Default.Straighten) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(trimOn, { trimOn = it })
                    Text("Считать погонаж", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                AnimatedVisibility(trimOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Длина элемента", trimPieceM, { trimPieceM = it }, 0.1, suffix = "м", decimals = 2, min = 0.1, max = 10.0)
                        NumberStepperField("Запас", trimReserve, { trimReserve = it }, 5.0, suffix = "%", decimals = 0, min = 0.0, max = 50.0)
                        NumberStepperField("Цена элемента", trimPrice, { trimPrice = it }, 50.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)
                        InfoRow("Длина с запасом", "${fmtNum(trim.lengthM, 2)} м")
                        InfoRow("Элементов", "${trim.pieces} шт", true)
                        InfoRow("Стоимость", money(trim.cost))
                    }
                }
            }
        }

        // ---------------------------------------------------------- Работа
        item {
            CalcCard("Работа и логистика", Icons.Default.Engineering) {
                NumberStepperField("Цена работы за м²", workPriceM2, { workPriceM2 = it }, 100.0, suffix = "₽", decimals = 0, min = 0.0, max = 100000.0)
                NumberStepperField("Выработка за смену", dailyOutputM2, { dailyOutputM2 = it }, 0.5, suffix = "м²", decimals = 1, min = 0.5, max = 100.0)
                NumberStepperField("Сколько унести за ходку", tripCapacity, { tripCapacity = it }, 10.0, suffix = "кг", decimals = 0, min = 10.0, max = 1000.0)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                InfoRow("Смен", "${fmtNum(work.days, 1)}")
                InfoRow("Работа", money(work.laborCost), true)
                InfoRow("Общий вес", "${fmtNum(totals.totalWeightKg, 0)} кг")
                InfoRow("Ходок на этаж", "${totals.trips}")
            }
        }

        // ---------------------------------------------------------- Сохранение
        item {
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Сохранить расчёт")
            }
        }

        // ---------------------------------------------------------- История
        if (calculations.isNotEmpty()) {
            item {
                Text(
                    "История расчётов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(calculations, key = { it.id }) { c ->
                HistoryCard(c) { viewModel.deleteCalculation(c.id) }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить расчёт", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = calcName,
                    onValueChange = { calcName = it },
                    label = { Text("Название (адрес, объект)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCalculation(
                        name = calcName.ifBlank { "Объект ${fmtNum(areaM2, 1)} м²" },
                        areaSqM = areaM2,
                        tileWidthCm = tileW / 10.0,
                        tileHeightCm = tileH / 10.0,
                        groutWidthMm = grout,
                        tilePricePerSqM = tilePriceM2,
                        glueConsKgPerSqM = glue.totalKgM2,
                        glueBagWeightKg = glueBagKg,
                        gluePricePerBag = gluePrice,
                        groutPricePerKg = groutPriceKg
                    )
                    calcName = ""
                    showSaveDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") } }
        )
    }
}

// =====================================================================================

@Composable
private fun CalcCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                )
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    }
}

@Composable
private fun HistoryCard(c: Calculation, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${fmtNum(c.areaSqM, 2)} м² · ${c.tileWidthCm.toInt()}×${c.tileHeightCm.toInt()} см · " +
                        "${c.calculatedTileCount} шт · ${c.calculatedGlueBagsNeeded} меш.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(sdf.format(Date(c.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                money(c.calculatedTotalMaterialCost),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
