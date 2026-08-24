package com.example.ui.calc

import kotlin.math.ceil
import kotlin.math.max

// =====================================================================================
// РАСЧЁТЫ ПЛИТОЧНИКА
//
// Источники нормативов и таблиц расхода:
//  • таблица расхода клея по зубу шпателя и формату плитки;
//  • формула расхода затирки (A+B)·C·D·ρ/(A·B);
//  • типовые запасы на подрезку по схемам раскладки.
// Все значения по умолчанию можно переопределить руками — цифры у поставщиков разные.
// =====================================================================================

/** Зуб гребёнки: расход клея и рекомендуемый формат плитки. */
data class TrowelNotch(
    val notchMm: Double,
    val layerMm: String,
    val consumptionKgM2: Double,
    val tileFormat: String
)

val TROWEL_TABLE = listOf(
    TrowelNotch(4.0, "2–3 мм", 1.75, "мозаика, до 100×100"),
    TrowelNotch(6.0, "3–4 мм", 2.75, "100×100 – 200×200"),
    TrowelNotch(8.0, "4–5 мм", 3.75, "200×200 – 300×300"),
    TrowelNotch(10.0, "5–6 мм", 4.75, "300×300 – 400×400"),
    TrowelNotch(12.0, "6–8 мм", 5.75, "400×400 – 600×600"),
    TrowelNotch(15.0, "8–10 мм", 7.50, "600×600 – 600×1200"),
    TrowelNotch(20.0, "10–12 мм", 10.0, "от 800×800")
)

/** Подобрать зуб шпателя по большей стороне плитки. */
fun suggestNotch(tileMaxSideMm: Double): TrowelNotch = when {
    tileMaxSideMm <= 100 -> TROWEL_TABLE[0]
    tileMaxSideMm <= 200 -> TROWEL_TABLE[1]
    tileMaxSideMm <= 300 -> TROWEL_TABLE[2]
    tileMaxSideMm <= 400 -> TROWEL_TABLE[3]
    tileMaxSideMm <= 600 -> TROWEL_TABLE[4]
    tileMaxSideMm <= 1200 -> TROWEL_TABLE[5]
    else -> TROWEL_TABLE[6]
}

/** Типовой запас на подрезку по схеме укладки, %. */
data class LayoutWaste(val title: String, val percent: Double)

val WASTE_PRESETS = listOf(
    LayoutWaste("Прямая, простое помещение", 5.0),
    LayoutWaste("Прямая, санузел с коробами", 10.0),
    LayoutWaste("Разбежка", 10.0),
    LayoutWaste("Диагональ 45°", 15.0),
    LayoutWaste("Ёлочка / модульная", 15.0),
    LayoutWaste("Сложная геометрия, крупный формат", 20.0)
)

// -------------------------------------------------------------------------------------
// ПЛИТКА
// -------------------------------------------------------------------------------------

data class TileResult(
    val netAreaM2: Double,
    val areaWithWasteM2: Double,
    val pieces: Int,
    val piecesAreaM2: Double,
    val packs: Int,
    val cost: Double,
    val weightKg: Double
)

/**
 * @param areaM2 чистая площадь облицовки
 * @param tileWMm, tileHMm габарит плитки
 * @param groutMm ширина шва (влияет на шаг сетки, а значит на количество)
 * @param wastePercent запас на подрезку и бой
 * @param piecesPerPack штук в упаковке (0 — не считать упаковки)
 * @param pricePerM2 цена за м²; если 0 — используется [pricePerPiece]
 * @param tileThicknessMm толщина для расчёта веса
 * @param densityKgM3 плотность материала (керамогранит ≈ 2400, керамика ≈ 2000)
 */
fun calcTile(
    areaM2: Double,
    tileWMm: Double,
    tileHMm: Double,
    groutMm: Double,
    wastePercent: Double,
    piecesPerPack: Double,
    pricePerM2: Double,
    pricePerPiece: Double,
    tileThicknessMm: Double,
    densityKgM3: Double
): TileResult {
    if (areaM2 <= 0 || tileWMm <= 0 || tileHMm <= 0) return TileResult(0.0, 0.0, 0, 0.0, 0, 0.0, 0.0)

    val faceAreaM2 = (tileWMm / 1000.0) * (tileHMm / 1000.0)
    val pitchAreaM2 = ((tileWMm + groutMm) / 1000.0) * ((tileHMm + groutMm) / 1000.0)
    val withWaste = areaM2 * (1.0 + wastePercent / 100.0)
    val pieces = ceil(withWaste / pitchAreaM2).toInt()
    val piecesArea = pieces * faceAreaM2
    val packs = if (piecesPerPack > 0) ceil(pieces / piecesPerPack).toInt() else 0
    val cost = when {
        pricePerPiece > 0 -> pieces * pricePerPiece
        else -> piecesArea * pricePerM2
    }
    val weight = piecesArea * (tileThicknessMm / 1000.0) * densityKgM3
    return TileResult(areaM2, withWaste, pieces, piecesArea, packs, cost, weight)
}

// -------------------------------------------------------------------------------------
// КЛЕЙ
// -------------------------------------------------------------------------------------

data class GlueResult(
    val baseKgM2: Double,
    val totalKgM2: Double,
    val totalKg: Double,
    val bags: Int,
    val cost: Double
)

/**
 * @param notchKgM2 базовый расход по зубу шпателя, кг/м²
 * @param levelingMm средний перепад основания, мм (добавляет ≈1,4 кг/м² на каждый мм)
 * @param backButtering true — двойное нанесение (крупный формат, керамогранит): ×1,6
 */
fun calcGlue(
    areaM2: Double,
    notchKgM2: Double,
    levelingMm: Double,
    backButtering: Boolean,
    bagWeightKg: Double,
    pricePerBag: Double,
    reservePercent: Double
): GlueResult {
    if (areaM2 <= 0 || notchKgM2 <= 0) return GlueResult(0.0, 0.0, 0.0, 0, 0.0)
    var perM2 = notchKgM2 + max(0.0, levelingMm) * 1.4
    if (backButtering) perM2 *= 1.6
    val total = areaM2 * perM2 * (1.0 + reservePercent / 100.0)
    val bags = if (bagWeightKg > 0) ceil(total / bagWeightKg).toInt() else 0
    return GlueResult(notchKgM2, perM2, total, bags, bags * pricePerBag)
}

// -------------------------------------------------------------------------------------
// ЗАТИРКА
// -------------------------------------------------------------------------------------

data class GroutResult(
    val kgPerM2: Double,
    val totalKg: Double,
    val packs: Int,
    val cost: Double,
    val jointLengthM: Double
)

/**
 * Классическая формула расхода: (A + B) · C · D · ρ / (A · B), кг/м².
 * A, B — стороны плитки (мм); C — ширина шва (мм); D — глубина шва = толщина плитки (мм);
 * ρ — насыпная плотность затирки, г/см³ (цементная 1,5–1,8; эпоксидная плотнее).
 */
fun calcGrout(
    areaM2: Double,
    tileWMm: Double,
    tileHMm: Double,
    groutMm: Double,
    tileThicknessMm: Double,
    densityGcm3: Double,
    reservePercent: Double,
    epoxy: Boolean,
    packWeightKg: Double,
    pricePerKg: Double
): GroutResult {
    if (areaM2 <= 0 || tileWMm <= 0 || tileHMm <= 0 || groutMm <= 0) {
        return GroutResult(0.0, 0.0, 0, 0.0, 0.0)
    }
    var perM2 = (tileWMm + tileHMm) * groutMm * tileThicknessMm * densityGcm3 / (tileWMm * tileHMm)
    if (epoxy) perM2 *= 1.4
    perM2 *= (1.0 + reservePercent / 100.0)
    val total = areaM2 * perM2
    val packs = if (packWeightKg > 0) ceil(total / packWeightKg).toInt() else 0
    // Погонаж швов: на 1 м² приходится (1/A + 1/B) метров шва (A, B — в метрах)
    val jointLen = areaM2 * (1000.0 / tileWMm + 1000.0 / tileHMm)
    return GroutResult(perM2, total, packs, total * pricePerKg, jointLen)
}

// -------------------------------------------------------------------------------------
// ПОДГОТОВКА ОСНОВАНИЯ
// -------------------------------------------------------------------------------------

data class DryMixResult(val kgPerM2: Double, val totalKg: Double, val bags: Int, val cost: Double)

/** Наливной пол / ровнитель / стяжка: расход = толщина(мм) × плотность(кг/м² на 1 мм). */
fun calcScreed(
    areaM2: Double,
    thicknessMm: Double,
    kgPerM2PerMm: Double,
    bagWeightKg: Double,
    pricePerBag: Double
): DryMixResult {
    if (areaM2 <= 0 || thicknessMm <= 0) return DryMixResult(0.0, 0.0, 0, 0.0)
    val perM2 = thicknessMm * kgPerM2PerMm
    val total = areaM2 * perM2
    val bags = if (bagWeightKg > 0) ceil(total / bagWeightKg).toInt() else 0
    return DryMixResult(perM2, total, bags, bags * pricePerBag)
}

data class CoatingResult(val totalKg: Double, val buckets: Int, val cost: Double, val tapeM: Double)

/** Грунтовка / обмазочная гидроизоляция: расход на слой × число слоёв. */
fun calcCoating(
    areaM2: Double,
    kgPerM2PerLayer: Double,
    layers: Int,
    packWeightKg: Double,
    pricePerPack: Double,
    perimeterM: Double,
    tapeReservePercent: Double = 10.0
): CoatingResult {
    if (areaM2 <= 0 || kgPerM2PerLayer <= 0 || layers <= 0) return CoatingResult(0.0, 0, 0.0, 0.0)
    val total = areaM2 * kgPerM2PerLayer * layers
    val packs = if (packWeightKg > 0) ceil(total / packWeightKg).toInt() else 0
    return CoatingResult(total, packs, packs * pricePerPack, perimeterM * (1.0 + tapeReservePercent / 100.0))
}

// -------------------------------------------------------------------------------------
// РАСХОДНИКИ: КРЕСТИКИ И СВП
// -------------------------------------------------------------------------------------

data class ConsumablesResult(
    val crosses: Int,
    val clips: Int,
    val wedges: Int,
    val cost: Double
)

/**
 * @param perTileCrosses крестиков на плитку (обычно 2 при прямой раскладке)
 * @param perTileClips зажимов СВП на плитку: 2 для среднего формата, 3–4 для крупного
 * @param dailyOutputTiles сколько плиток кладётся за смену — столько клиньев нужно
 *                         физически (клинья снимаются на следующий день и идут в оборот)
 */
fun calcConsumables(
    tiles: Int,
    perTileCrosses: Double,
    perTileClips: Double,
    dailyOutputTiles: Double,
    priceCross: Double,
    priceClip: Double,
    priceWedge: Double
): ConsumablesResult {
    if (tiles <= 0) return ConsumablesResult(0, 0, 0, 0.0)
    val crosses = ceil(tiles * perTileCrosses).toInt()
    val clips = ceil(tiles * perTileClips).toInt()
    val wedges = if (dailyOutputTiles > 0) ceil(dailyOutputTiles * perTileClips).toInt() else clips
    val cost = crosses * priceCross + clips * priceClip + wedges * priceWedge
    return ConsumablesResult(crosses, clips, wedges, cost)
}

// -------------------------------------------------------------------------------------
// ПОГОНАЖ: ПЛИНТУС, ПРОФИЛЬ, УГОЛОК
// -------------------------------------------------------------------------------------

data class TrimResult(val lengthM: Double, val pieces: Int, val cost: Double)

fun calcTrim(
    perimeterM: Double,
    openingsM: Double,
    pieceLengthM: Double,
    reservePercent: Double,
    pricePerPiece: Double
): TrimResult {
    val len = max(0.0, perimeterM - openingsM) * (1.0 + reservePercent / 100.0)
    val pieces = if (pieceLengthM > 0) ceil(len / pieceLengthM).toInt() else 0
    return TrimResult(len, pieces, pieces * pricePerPiece)
}

// -------------------------------------------------------------------------------------
// РАСКЛАДКА: РЯДЫ И ПОДРЕЗКА (быстрый расчёт по прямоугольнику)
// -------------------------------------------------------------------------------------

data class RowResult(
    val fullRows: Int,
    val lastRowMm: Double,
    val centeredEdgeMm: Double,
    val recommendCentering: Boolean
)

/**
 * Сколько целых плиток укладывается по стороне и какой остаётся край.
 * [centeredEdgeMm] — какой была бы подрезка, если разложить симметрично от центра
 * (одинаковая с обеих сторон). Если край меньше трети плитки — лучше центрировать.
 */
fun calcRows(sideMm: Double, tileMm: Double, groutMm: Double): RowResult {
    val pitch = tileMm + groutMm
    if (sideMm <= 0 || pitch <= 0) return RowResult(0, 0.0, 0.0, false)
    val full = kotlin.math.floor(sideMm / pitch).toInt()
    val last = sideMm - full * pitch
    val centered = (sideMm - (full - 1).coerceAtLeast(0) * pitch) / 2.0
    return RowResult(full, last, centered, last in 0.1..(tileMm / 3.0))
}

// -------------------------------------------------------------------------------------
// ТРУДОЗАТРАТЫ И ИТОГ
// -------------------------------------------------------------------------------------

data class WorkResult(val days: Double, val laborCost: Double)

fun calcWork(areaM2: Double, outputM2PerDay: Double, pricePerM2: Double): WorkResult {
    val days = if (outputM2PerDay > 0) areaM2 / outputM2PerDay else 0.0
    return WorkResult(days, areaM2 * pricePerM2)
}

data class TotalResult(
    val materialsCost: Double,
    val laborCost: Double,
    val grandTotal: Double,
    val totalWeightKg: Double,
    val trips: Int
)

/** @param tripCapacityKg сколько мастер уносит за одну ходку (по умолчанию 60 кг). */
fun calcTotals(
    materials: List<Double>,
    laborCost: Double,
    weights: List<Double>,
    tripCapacityKg: Double = 60.0
): TotalResult {
    val mat = materials.sum()
    val weight = weights.sum()
    return TotalResult(
        materialsCost = mat,
        laborCost = laborCost,
        grandTotal = mat + laborCost,
        totalWeightKg = weight,
        trips = if (tripCapacityKg > 0) ceil(weight / tripCapacityKg).toInt() else 0
    )
}
