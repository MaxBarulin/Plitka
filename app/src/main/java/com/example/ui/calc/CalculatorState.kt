package com.example.ui.calc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Состояние калькулятора живёт во ViewModel — иначе введённые цены и настройки
 * терялись бы при каждом уходе на другую вкладку.
 *
 * Пока [syncWithCad] включено, площадь, периметр и формат плитки берутся прямо из
 * CAD-плана и обновляются на лету. Как только пользователь правит любое из этих полей
 * руками, связь разрывается (см. [detachFromCad]), а вернуть её можно переключателем.
 */
class CalculatorState {

    var syncWithCad by mutableStateOf(true)

    // ---------------------------------------------------------------- объект
    var areaM2 by mutableStateOf(10.0)
    var perimeterM by mutableStateOf(13.0)
    var openingsM by mutableStateOf(0.9)

    // ---------------------------------------------------------------- плитка
    var tileW by mutableStateOf(600.0)
    var tileH by mutableStateOf(600.0)
    var tileThickness by mutableStateOf(9.0)
    var grout by mutableStateOf(2.0)
    var wastePercent by mutableStateOf(10.0)
    var piecesPerPack by mutableStateOf(4.0)
    var tilePriceM2 by mutableStateOf(1800.0)
    var tilePricePiece by mutableStateOf(0.0)
    var density by mutableStateOf(2400.0)

    // ---------------------------------------------------------------- клей
    /** Пока true, зуб гребёнки подбирается по формату плитки автоматически. */
    var notchAuto by mutableStateOf(true)
    var notchIndex by mutableStateOf(4)
    var glueKgM2 by mutableStateOf(TROWEL_TABLE[4].consumptionKgM2)
    var levelingMm by mutableStateOf(0.0)
    var backButtering by mutableStateOf(false)
    var glueBagKg by mutableStateOf(25.0)
    var gluePrice by mutableStateOf(700.0)
    var glueReserve by mutableStateOf(10.0)

    // ---------------------------------------------------------------- затирка
    var groutDensity by mutableStateOf(1.8)
    var groutEpoxy by mutableStateOf(false)
    var groutReserve by mutableStateOf(10.0)
    var groutPackKg by mutableStateOf(2.0)
    var groutPriceKg by mutableStateOf(400.0)

    // ---------------------------------------------------------------- основание
    var screedOn by mutableStateOf(false)
    var screedThickness by mutableStateOf(20.0)
    var screedKgMm by mutableStateOf(1.6)
    var screedBagKg by mutableStateOf(20.0)
    var screedPrice by mutableStateOf(450.0)

    var primerOn by mutableStateOf(true)
    var primerKg by mutableStateOf(0.15)
    var primerLayers by mutableStateOf(2.0)
    var primerPackKg by mutableStateOf(5.0)
    var primerPrice by mutableStateOf(900.0)

    var waterproofOn by mutableStateOf(false)
    var wpKg by mutableStateOf(1.2)
    var wpLayers by mutableStateOf(2.0)
    var wpPackKg by mutableStateOf(20.0)
    var wpPrice by mutableStateOf(3500.0)

    // ---------------------------------------------------------------- расходники
    var crossesPerTile by mutableStateOf(2.0)
    var clipsPerTile by mutableStateOf(3.0)
    var dailyOutputM2 by mutableStateOf(6.0)
    var priceCross by mutableStateOf(0.6)
    var priceClip by mutableStateOf(3.5)
    var priceWedge by mutableStateOf(4.0)

    // ---------------------------------------------------------------- погонаж
    var trimOn by mutableStateOf(false)
    var trimPieceM by mutableStateOf(2.5)
    var trimReserve by mutableStateOf(10.0)
    var trimPrice by mutableStateOf(350.0)

    // ---------------------------------------------------------------- работа
    var workPriceM2 by mutableStateOf(1500.0)
    var tripCapacity by mutableStateOf(60.0)

    // ---------------------------------------------------------------- развёрнутые карточки
    val expanded = mutableStateOf(setOf("Шпаргалка: главное на объекте"))

    fun toggleExpanded(title: String) {
        expanded.value = if (title in expanded.value) expanded.value - title else expanded.value + title
    }

    /**
     * Разорвать связь с CAD-планом, зафиксировав в полях текущие значения из плана,
     * чтобы правка начиналась не со старых чисел, а с того, что человек видит на экране.
     */
    fun detachFromCad(area: Double, perimeter: Double, tw: Double, th: Double, g: Double) {
        if (!syncWithCad) return
        areaM2 = area
        perimeterM = perimeter
        tileW = tw
        tileH = th
        grout = g
        syncWithCad = false
    }
}
