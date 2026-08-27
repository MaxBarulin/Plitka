package com.example

import com.example.data.Snapshots
import com.example.ui.cad.CadEditorState
import com.example.ui.cad.CadObstacle
import com.example.ui.cad.OriginMode
import com.example.ui.cad.TilePattern
import com.example.ui.cad.WallProps
import com.example.ui.cad.rectangleRoom
import com.example.ui.calc.CalculatorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Введённое должно переживать закрытие приложения: снимок в JSON и обратно
 * обязан возвращать ровно те же цифры.
 */
class SnapshotPersistenceTest {

    @Test
    fun `cad state survives a json round trip`() {
        val original = CadEditorState().apply {
            vertices = rectangleRoom(4200.0, 3100.0)
            walls = mapOf(vertices[0].id to WallProps(lengthLocked = true, lockedLengthMm = 4200.0))
            obstacles = listOf(CadObstacle(x = 500.0, y = 600.0, w = 800.0, h = 400.0, name = "Венткороб"))
            tileW = 1200.0
            tileH = 600.0
            grout = 1.5
            pattern = TilePattern.HERRINGBONE
            offsetPercent = 33.0
            tileRotation = 22.5
            originMode = OriginMode.POINT
            pointX = 1234.0
            pointY = -567.0
            linStep = 5.0
            angStep = 0.5
            snapToStep = false
            showAngles = false
        }

        val json = Snapshots.cadToJson(original)
        val restored = CadEditorState()
        assertTrue(Snapshots.applyCadJson(restored, json))

        assertEquals(original.vertices.size, restored.vertices.size)
        original.vertices.forEachIndexed { i, v ->
            assertEquals(v.id, restored.vertices[i].id)
            assertEquals(v.x, restored.vertices[i].x, 1e-9)
            assertEquals(v.y, restored.vertices[i].y, 1e-9)
        }
        assertEquals(original.walls, restored.walls)
        assertEquals(1, restored.obstacles.size)
        assertEquals("Венткороб", restored.obstacles[0].name)
        assertEquals(800.0, restored.obstacles[0].w, 1e-9)
        assertEquals(1200.0, restored.tileW, 1e-9)
        assertEquals(600.0, restored.tileH, 1e-9)
        assertEquals(1.5, restored.grout, 1e-9)
        assertEquals(TilePattern.HERRINGBONE, restored.pattern)
        assertEquals(33.0, restored.offsetPercent, 1e-9)
        assertEquals(22.5, restored.tileRotation, 1e-9)
        assertEquals(OriginMode.POINT, restored.originMode)
        assertEquals(1234.0, restored.pointX, 1e-9)
        assertEquals(-567.0, restored.pointY, 1e-9)
        assertEquals(5.0, restored.linStep, 1e-9)
        assertEquals(0.5, restored.angStep, 1e-9)
        assertFalse(restored.snapToStep)
        assertFalse(restored.showAngles)
        // Площадь — главный итог плана, она обязана совпасть до миллиметра
        assertEquals(original.areaM2, restored.areaM2, 1e-9)
    }

    @Test
    fun `calculator state survives a json round trip`() {
        val original = CalculatorState().apply {
            syncWithCad = false
            areaM2 = 23.75
            perimeterM = 19.4
            openingsM = 1.8
            tileW = 300.0
            tileH = 600.0
            tileThickness = 10.5
            grout = 3.0
            wastePercent = 15.0
            piecesPerPack = 8.0
            tilePriceM2 = 2450.0
            density = 2350.0
            notchAuto = false
            notchIndex = 6
            glueKgM2 = 9.25
            levelingMm = 3.0
            backButtering = true
            gluePrice = 890.0
            groutEpoxy = true
            screedOn = true
            screedThickness = 35.0
            waterproofOn = true
            trimOn = true
            trimPrice = 640.0
            workPriceM2 = 2100.0
            tripCapacity = 45.0
        }

        val json = Snapshots.calcToJson(original)
        val restored = CalculatorState()
        assertTrue(Snapshots.applyCalcJson(restored, json))

        assertFalse(restored.syncWithCad)
        assertEquals(23.75, restored.areaM2, 1e-9)
        assertEquals(19.4, restored.perimeterM, 1e-9)
        assertEquals(1.8, restored.openingsM, 1e-9)
        assertEquals(300.0, restored.tileW, 1e-9)
        assertEquals(600.0, restored.tileH, 1e-9)
        assertEquals(10.5, restored.tileThickness, 1e-9)
        assertEquals(3.0, restored.grout, 1e-9)
        assertEquals(15.0, restored.wastePercent, 1e-9)
        assertEquals(8.0, restored.piecesPerPack, 1e-9)
        assertEquals(2450.0, restored.tilePriceM2, 1e-9)
        assertEquals(2350.0, restored.density, 1e-9)
        assertFalse(restored.notchAuto)
        assertEquals(6, restored.notchIndex)
        assertEquals(9.25, restored.glueKgM2, 1e-9)
        assertEquals(3.0, restored.levelingMm, 1e-9)
        assertTrue(restored.backButtering)
        assertEquals(890.0, restored.gluePrice, 1e-9)
        assertTrue(restored.groutEpoxy)
        assertTrue(restored.screedOn)
        assertEquals(35.0, restored.screedThickness, 1e-9)
        assertTrue(restored.waterproofOn)
        assertTrue(restored.trimOn)
        assertEquals(640.0, restored.trimPrice, 1e-9)
        assertEquals(2100.0, restored.workPriceM2, 1e-9)
        assertEquals(45.0, restored.tripCapacity, 1e-9)
    }

    @Test
    fun `broken record does not wipe the current state`() {
        val st = CadEditorState().apply { tileW = 900.0 }
        assertFalse(Snapshots.applyCadJson(st, "{это не json"))
        assertFalse(Snapshots.applyCadJson(st, null))
        assertEquals(900.0, st.tileW, 1e-9)

        val cs = CalculatorState().apply { tilePriceM2 = 3000.0 }
        assertFalse(Snapshots.applyCalcJson(cs, "не json вовсе"))
        assertEquals(3000.0, cs.tilePriceM2, 1e-9)
    }
}
