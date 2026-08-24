package com.example

import com.example.ui.calc.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileMathTest {

    @Test
    fun `grout consumption matches the reference table`() {
        // Справочник: 200x200x8, шов 2 мм -> ~0,32 кг/м²
        val r = calcGrout(
            areaM2 = 1.0, tileWMm = 200.0, tileHMm = 200.0, groutMm = 2.0,
            tileThicknessMm = 8.0, densityGcm3 = 1.8, reservePercent = 10.0,
            epoxy = false, packWeightKg = 2.0, pricePerKg = 0.0
        )
        assertEquals(0.32, r.kgPerM2, 0.02)
    }

    @Test
    fun `grout consumption for large format is much lower`() {
        // Справочник: 600x600x10, шов 2 мм -> ~0,13 кг/м²
        val r = calcGrout(
            areaM2 = 1.0, tileWMm = 600.0, tileHMm = 600.0, groutMm = 2.0,
            tileThicknessMm = 10.0, densityGcm3 = 1.8, reservePercent = 10.0,
            epoxy = false, packWeightKg = 2.0, pricePerKg = 0.0
        )
        assertEquals(0.13, r.kgPerM2, 0.02)
    }

    @Test
    fun `epoxy grout costs 40 percent more material`() {
        val base = calcGrout(1.0, 300.0, 600.0, 3.0, 9.0, 1.8, 0.0, false, 2.0, 0.0)
        val epoxy = calcGrout(1.0, 300.0, 600.0, 3.0, 9.0, 1.8, 0.0, true, 2.0, 0.0)
        assertEquals(base.kgPerM2 * 1.4, epoxy.kgPerM2, 1e-6)
    }

    @Test
    fun `tile count accounts for grout pitch and waste`() {
        // 10 м², плитка 500x500 без шва, запас 0 % -> ровно 40 шт
        val r = calcTile(10.0, 500.0, 500.0, 0.0, 0.0, 0.0, 1000.0, 0.0, 9.0, 2400.0)
        assertEquals(40, r.pieces)
        // С запасом 10 % — 44 шт
        val r2 = calcTile(10.0, 500.0, 500.0, 0.0, 10.0, 0.0, 1000.0, 0.0, 9.0, 2400.0)
        assertEquals(44, r2.pieces)
    }

    @Test
    fun `packs are rounded up`() {
        val r = calcTile(10.0, 600.0, 600.0, 2.0, 10.0, 4.0, 1000.0, 0.0, 9.0, 2400.0)
        assertTrue(r.packs * 4 >= r.pieces)
        assertTrue((r.packs - 1) * 4 < r.pieces)
    }

    @Test
    fun `tile weight uses thickness and density`() {
        // 1 м² керамогранита 10 мм при 2400 кг/м³ = 24 кг
        val r = calcTile(1.0, 1000.0, 1000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 2400.0)
        assertEquals(24.0, r.weightKg, 0.01)
    }

    @Test
    fun `notch suggestion follows the tile format`() {
        assertEquals(6.0, suggestNotch(200.0).notchMm, 0.01)
        assertEquals(12.0, suggestNotch(600.0).notchMm, 0.01)
        assertEquals(15.0, suggestNotch(1200.0).notchMm, 0.01)
        assertEquals(20.0, suggestNotch(1800.0).notchMm, 0.01)
    }

    @Test
    fun `glue accounts for base unevenness and back buttering`() {
        val plain = calcGlue(10.0, 5.75, 0.0, false, 25.0, 700.0, 0.0)
        assertEquals(5.75, plain.totalKgM2, 1e-6)
        assertEquals(57.5, plain.totalKg, 1e-6)
        assertEquals(3, plain.bags)

        val uneven = calcGlue(10.0, 5.75, 2.0, false, 25.0, 700.0, 0.0)
        assertEquals(5.75 + 2.8, uneven.totalKgM2, 1e-6)

        val doubled = calcGlue(10.0, 5.75, 0.0, true, 25.0, 700.0, 0.0)
        assertEquals(5.75 * 1.6, doubled.totalKgM2, 1e-6)
    }

    @Test
    fun `screed consumption is thickness times density`() {
        val r = calcScreed(10.0, 20.0, 1.6, 20.0, 450.0)
        assertEquals(32.0, r.kgPerM2, 1e-6)
        assertEquals(320.0, r.totalKg, 1e-6)
        assertEquals(16, r.bags)
    }

    @Test
    fun `rows calculation reports the last cut`() {
        // 2400 мм стены, плитка 600 + шов 0 -> ровно 4 ряда, подрезки нет
        val exact = calcRows(2400.0, 600.0, 0.0)
        assertEquals(4, exact.fullRows)
        assertEquals(0.0, exact.lastRowMm, 0.01)

        // 2500 мм -> 4 целых и 100 мм подрезки, это меньше трети плитки
        val cut = calcRows(2500.0, 600.0, 0.0)
        assertEquals(4, cut.fullRows)
        assertEquals(100.0, cut.lastRowMm, 0.01)
        assertTrue(cut.recommendCentering)
        // При симметричной раскладке край с каждой стороны шире
        assertTrue(cut.centeredEdgeMm > cut.lastRowMm)
    }

    @Test
    fun `consumables scale with the tile count`() {
        val r = calcConsumables(100, 2.0, 3.0, 20.0, 0.6, 3.5, 4.0)
        assertEquals(200, r.crosses)
        assertEquals(300, r.clips)
        assertEquals(60, r.wedges) // клинья только на дневную выработку
    }

    @Test
    fun `trim subtracts openings and rounds pieces up`() {
        val r = calcTrim(perimeterM = 13.0, openingsM = 1.0, pieceLengthM = 2.5, reservePercent = 0.0, pricePerPiece = 350.0)
        assertEquals(12.0, r.lengthM, 1e-6)
        assertEquals(5, r.pieces)
    }

    @Test
    fun `totals sum materials labor and weight`() {
        val t = calcTotals(listOf(1000.0, 500.0), 3000.0, listOf(100.0, 25.0), 60.0)
        assertEquals(1500.0, t.materialsCost, 1e-6)
        assertEquals(4500.0, t.grandTotal, 1e-6)
        assertEquals(125.0, t.totalWeightKg, 1e-6)
        assertEquals(3, t.trips)
    }

    @Test
    fun `zero inputs never crash`() {
        assertEquals(0, calcTile(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0).pieces)
        assertEquals(0.0, calcGlue(0.0, 0.0, 0.0, false, 0.0, 0.0, 0.0).totalKg, 1e-9)
        assertEquals(0.0, calcGrout(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, 0.0, 0.0).totalKg, 1e-9)
        assertEquals(0, calcRows(0.0, 0.0, 0.0).fullRows)
    }
}
