package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Calculation
import com.example.data.Snapshots
import com.example.data.TileRepository
import com.example.ui.TileViewModel
import com.example.ui.cad.CadEditorState
import com.example.ui.cad.rectangleRoom
import com.example.ui.calc.CalculatorState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StorageTest {

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    private fun repo(): TileRepository {
        val db = AppDatabase.getDatabase(app)
        return TileRepository(db.orderDao(), db.calculationDao(), db.workspaceDao())
    }

    private fun sampleCalculation(total: Double) = Calculation(
        name = "Ванная на Ленина",
        areaM2 = 12.5, perimeterM = 14.2,
        tileWidthMm = 300.0, tileHeightMm = 600.0, tileThicknessMm = 9.0,
        groutMm = 2.0, wastePercent = 12.0,
        tilePieces = 78, tilePacks = 10, tileAreaM2 = 14.04,
        tilePricePerM2 = 1990.0, tileCost = 27939.6,
        glueKgPerM2 = 5.75, glueKg = 79.06, glueBags = 4,
        glueBagKg = 25.0, gluePricePerBag = 700.0, glueCost = 2800.0,
        groutKgPerM2 = 0.21, groutKg = 2.63, groutPacks = 2,
        groutPricePerKg = 400.0, groutCost = 1050.0,
        extrasCost = 3120.0,
        materialsCost = 34909.6, laborPricePerM2 = 1500.0, laborCost = 18750.0,
        totalCost = total, totalWeightKg = 412.0, workDays = 2.1
    )

    @Test
    fun `saved calculation keeps the exact numbers it was given`() = runBlocking {
        val r = repo()
        val calc = sampleCalculation(total = 53659.6)
        r.insertCalculation(calc)

        val stored = r.allCalculations.first().first { it.name == "Ванная на Ленина" }
        // Ничего не пересчитывается заново — что положили, то и лежит
        assertEquals(53659.6, stored.totalCost, 1e-6)
        assertEquals(78, stored.tilePieces)
        assertEquals(4, stored.glueBags)
        assertEquals(2.63, stored.groutKg, 1e-6)
        assertEquals(34909.6, stored.materialsCost, 1e-6)
        assertEquals(18750.0, stored.laborCost, 1e-6)

        r.deleteCalculationById(stored.id)
    }

    @Test
    fun `workspace survives a restart`() = runBlocking {
        val cad = CadEditorState().apply {
            vertices = rectangleRoom(5000.0, 4000.0) // 20 м²
            tileW = 800.0
            tileH = 800.0
        }
        val calc = CalculatorState().apply {
            tilePriceM2 = 2777.0
            workPriceM2 = 1900.0
        }

        val r = repo()
        r.saveWorkspace(Snapshots.cadToJson(cad), Snapshots.calcToJson(calc))

        // Новый запуск приложения: состояния создаются с нуля и наполняются с диска
        val saved = r.loadWorkspace()
        assertNotNull(saved)
        val freshCad = CadEditorState()
        val freshCalc = CalculatorState()
        assertTrue(Snapshots.applyCadJson(freshCad, saved!!.cadJson))
        assertTrue(Snapshots.applyCalcJson(freshCalc, saved.calcJson))

        assertEquals(20.0, freshCad.areaM2, 1e-6)
        assertEquals(800.0, freshCad.tileW, 1e-6)
        assertEquals(2777.0, freshCalc.tilePriceM2, 1e-6)
        assertEquals(1900.0, freshCalc.workPriceM2, 1e-6)
    }

    @Test
    fun `opening a saved calculation brings back the plan and the prices`() {
        val vm = TileViewModel(app)
        vm.cadState.vertices = rectangleRoom(5000.0, 4000.0) // 20 м²
        vm.cadState.tileW = 1200.0
        vm.calcState.tilePriceM2 = 3333.0
        vm.calcState.workPriceM2 = 2500.0

        val saved = sampleCalculation(total = 1.0).copy(
            cadJson = Snapshots.cadToJson(vm.cadState),
            calcJson = Snapshots.calcToJson(vm.calcState)
        )

        // Человек ушёл считать другой объект
        vm.cadState.vertices = rectangleRoom(1000.0, 1000.0)
        vm.cadState.tileW = 200.0
        vm.calcState.tilePriceM2 = 10.0
        vm.calcState.workPriceM2 = 20.0

        // ...и вернулся к сохранённому
        assertTrue(vm.loadCalculation(saved))
        assertEquals(20.0, vm.cadState.areaM2, 1e-6)
        assertEquals(1200.0, vm.cadState.tileW, 1e-6)
        assertEquals(3333.0, vm.calcState.tilePriceM2, 1e-6)
        assertEquals(2500.0, vm.calcState.workPriceM2, 1e-6)
    }

    @Test
    fun `old record without a snapshot cannot be opened but does not crash`() {
        val vm = TileViewModel(app)
        assertEquals(false, vm.loadCalculation(sampleCalculation(total = 100.0)))
    }
}
