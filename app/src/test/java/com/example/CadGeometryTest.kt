package com.example

import com.example.ui.cad.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CadGeometryTest {

    private fun rect(w: Double, h: Double) = rectangleRoom(w, h)

    @Test
    fun `area and perimeter of rectangle`() {
        val r = rect(3400.0, 2200.0)
        assertEquals(3400.0 * 2200.0, polygonAreaMm2(r), 1.0)
        assertEquals(2 * (3400.0 + 2200.0), perimeterMm(r), 1.0)
    }

    @Test
    fun `interior angles of rectangle are 90 degrees`() {
        val r = rect(3000.0, 2000.0)
        r.indices.forEach { i ->
            assertEquals("угол $i", 90.0, interiorAngleDeg(r, i), 0.01)
        }
    }

    @Test
    fun `azimuth convention right is zero and up is ninety`() {
        val a = CadVertex(x = 0.0, y = 0.0)
        val right = CadVertex(x = 1000.0, y = 0.0)
        val up = CadVertex(x = 0.0, y = -1000.0) // экранная Y вниз
        assertEquals(0.0, azimuthDeg(a, right), 0.01)
        assertEquals(90.0, azimuthDeg(a, up), 0.01)
    }

    @Test
    fun `setWallLength moves the end vertex and keeps direction`() {
        val r = rect(3000.0, 2000.0)
        val out = setWallLength(r, emptyMap(), 0, 2500.0, WallAnchor.START)
        assertEquals(2500.0, distMm(out[0], out[1]), 0.01)
        assertEquals(r[0].x, out[0].x, 0.01) // начало не сдвинулось
        assertEquals(0.0, azimuthDeg(out[0], out[1]), 0.01)
    }

    @Test
    fun `setWallLength with END anchor moves the start vertex`() {
        val r = rect(3000.0, 2000.0)
        val out = setWallLength(r, emptyMap(), 0, 2500.0, WallAnchor.END)
        assertEquals(2500.0, distMm(out[0], out[1]), 0.01)
        assertEquals(r[1].x, out[1].x, 0.01) // конец не сдвинулся
    }

    @Test
    fun `locked wall keeps its length when a neighbour corner is nudged`() {
        val r = rect(3000.0, 2000.0)
        // Фиксируем длину стены 0 (от угла 1 к углу 2) на 3000 мм
        val walls = mapOf(r[0].id to WallProps(lengthLocked = true, lockedLengthMm = 3000.0))
        // Двигаем угол №2 (конец этой стены) — решатель обязан вернуть длину
        val out = nudgeVertex(r, walls, 1, 500.0, 300.0)
        assertEquals(3000.0, distMm(out[0], out[1]), 0.5)
    }

    @Test
    fun `pinned vertex is not moved by the solver`() {
        val base = rect(3000.0, 2000.0)
        val r = base.toMutableList().also { it[0] = it[0].copy(pinned = true) }
        val walls = mapOf(r[0].id to WallProps(lengthLocked = true, lockedLengthMm = 3000.0))
        val out = nudgeVertex(r, walls, 1, 400.0, 0.0)
        assertEquals(r[0].x, out[0].x, 0.001)
        assertEquals(r[0].y, out[0].y, 0.001)
        assertEquals(3000.0, distMm(out[0], out[1]), 0.5)
    }

    @Test
    fun `unlocked wall length simply follows the corner`() {
        val r = rect(3000.0, 2000.0)
        val out = nudgeVertex(r, emptyMap(), 1, 500.0, 0.0)
        assertEquals(3500.0, distMm(out[0], out[1]), 0.01)
    }

    @Test
    fun `setWallAzimuth rotates around the start vertex`() {
        val r = rect(3000.0, 2000.0)
        val out = setWallAzimuth(r, emptyMap(), 0, 30.0, WallAnchor.START)
        assertEquals(30.0, azimuthDeg(out[0], out[1]), 0.01)
        assertEquals(3000.0, distMm(out[0], out[1]), 0.01)
    }

    @Test
    fun `appendPolarVertex places the point at the requested length and angle`() {
        val r = rect(3000.0, 2000.0)
        val out = appendPolarVertex(r, 0, 1500.0, 90.0)
        assertEquals(r.size + 1, out.size)
        assertEquals(1500.0, distMm(out[0], out[1]), 0.01)
        assertEquals(90.0, azimuthDeg(out[0], out[1]), 0.01)
    }

    @Test
    fun `splitWall inserts a midpoint`() {
        val r = rect(3000.0, 2000.0)
        val out = splitWall(r, 0)
        assertEquals(5, out.size)
        assertEquals(1500.0, out[1].x, 0.01)
        assertEquals(0.0, out[1].y, 0.01)
    }

    @Test
    fun `perfectly aligned grid gives only whole tiles`() {
        val room = rectangleRoom(1000.0, 1000.0)
        val spec = TileSpec(
            widthMm = 500.0, heightMm = 500.0, groutMm = 0.0,
            pattern = TilePattern.STRAIGHT, rotationDeg = 0.0,
            originXMm = 0.0, originYMm = 0.0
        )
        val layout = generateLayout(room, emptyList(), spec)
        assertEquals(4, layout.stats.wholeCount)
        assertEquals(0, layout.stats.cutCount)
    }

    @Test
    fun `partial column is reported as cut tiles`() {
        val room = rectangleRoom(1200.0, 1000.0)
        val spec = TileSpec(
            widthMm = 500.0, heightMm = 500.0, groutMm = 0.0,
            pattern = TilePattern.STRAIGHT, rotationDeg = 0.0,
            originXMm = 0.0, originYMm = 0.0
        )
        val layout = generateLayout(room, emptyList(), spec)
        assertEquals(4, layout.stats.wholeCount)
        assertEquals(2, layout.stats.cutCount)
        assertEquals(6, layout.stats.totalCount)
    }

    @Test
    fun `obstacle removes tiles fully inside it`() {
        val room = rectangleRoom(1000.0, 1000.0)
        val obstacle = CadObstacle(x = 0.0, y = 0.0, w = 500.0, h = 500.0)
        val spec = TileSpec(
            widthMm = 500.0, heightMm = 500.0, groutMm = 0.0,
            pattern = TilePattern.STRAIGHT, rotationDeg = 0.0,
            originXMm = 0.0, originYMm = 0.0
        )
        val layout = generateLayout(room, listOf(obstacle), spec)
        assertEquals(3, layout.stats.totalCount)
        assertEquals(3, layout.stats.wholeCount)
    }

    @Test
    fun `herringbone lattice tiles the plane without overlaps`() {
        val room = rectangleRoom(4000.0, 3000.0)
        val spec = TileSpec(
            widthMm = 300.0, heightMm = 600.0, groutMm = 0.0,
            pattern = TilePattern.HERRINGBONE, rotationDeg = 0.0,
            originXMm = 0.0, originYMm = 0.0
        )
        val layout = generateLayout(room, emptyList(), spec)
        assertTrue("ёлочка должна дать плитки", layout.stats.totalCount > 0)
        // Суммарная площадь целых плиток не может превысить площадь помещения
        val wholeArea = layout.stats.wholeCount * 300.0 * 600.0
        assertTrue("площадь целых плиток превысила комнату", wholeArea <= 4000.0 * 3000.0)
        // И должна покрывать заметную её часть — значит решётка без дыр
        assertTrue("ёлочка покрыла слишком мало", wholeArea > 4000.0 * 3000.0 * 0.6)
    }

    @Test
    fun `grid rotation is consistent both ways`() {
        val spec = TileSpec(rotationDeg = 37.0, originXMm = 1234.0, originYMm = -567.0)
        val g = worldToGrid(2000.0, 900.0, spec)
        val w = gridToWorld(g.x, g.y, spec)
        assertTrue(abs(w.x - 2000.0) < 0.001)
        assertTrue(abs(w.y - 900.0) < 0.001)
    }

    @Test
    fun `orthogonalize snaps a slightly skewed wall to 90 degrees`() {
        val skewed = listOf(
            CadVertex(x = 0.0, y = 0.0),
            CadVertex(x = 3000.0, y = 40.0),
            CadVertex(x = 3000.0, y = 2000.0),
            CadVertex(x = 0.0, y = 2000.0)
        )
        val out = orthogonalize(skewed)
        assertEquals(0.0, azimuthDeg(out[0], out[1]), 0.01)
    }
}
