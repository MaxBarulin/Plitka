package com.example.ui.cad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Палитра «тёмного CAD»
object CadColors {
    val Background = Color(0xFF1B1F27)
    val GridMinor = Color(0xFF262C37)
    val GridMajor = Color(0xFF39414F)
    val Axis = Color(0xFF4E5B70)
    val Floor = Color(0xFF2A303B)
    val Wall = Color(0xFFFFD8C7)
    val WallLocked = Color(0xFF6EE7B7)
    val WallSelected = Color(0xFFFFC400)
    val Vertex = Color(0xFFFF6B35)
    val VertexSelected = Color(0xFFFFC400)
    val VertexPinned = Color(0xFF60A5FA)
    val TileWhole = Color(0xFFE8E0D6)
    val TileCut = Color(0xFFC9A227)
    val TileEdge = Color(0x33000000)
    val Obstacle = Color(0xFF12151B)
    val ObstacleEdge = Color(0xFFFF9AA2)
    val Origin = Color(0xFFFF3D00)
    val Label = Color(0xFFF2F5F9)
    val LabelBg = Color(0xE0111419)
}

/** Преобразование мм <-> пиксели канвы. */
data class CadTransform(
    val centerPx: Offset,
    val viewCenterX: Double,
    val viewCenterY: Double,
    val pxPerMm: Float
) {
    fun toScreen(x: Double, y: Double): Offset = Offset(
        centerPx.x + ((x - viewCenterX) * pxPerMm).toFloat(),
        centerPx.y + ((y - viewCenterY) * pxPerMm).toFloat()
    )

    fun toWorldX(px: Float): Double = viewCenterX + (px - centerPx.x) / pxPerMm
    fun toWorldY(py: Float): Double = viewCenterY + (py - centerPx.y) / pxPerMm
}

private fun DrawScope.labelAt(
    tm: TextMeasurer,
    text: String,
    center: Offset,
    fontSize: Float = 11f,
    bg: Color = CadColors.LabelBg,
    fg: Color = CadColors.Label
) {
    val style = TextStyle(color = fg, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    val res = tm.measure(AnnotatedString(text), style)
    val w = res.size.width.toFloat()
    val h = res.size.height.toFloat()
    drawRoundRect(
        color = bg,
        topLeft = Offset(center.x - w / 2 - 5f, center.y - h / 2 - 2f),
        size = androidx.compose.ui.geometry.Size(w + 10f, h + 4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
    )
    drawText(res, color = fg, topLeft = Offset(center.x - w / 2, center.y - h / 2))
}

/** Сетка: мелкая — [minorMm], крупная — [majorMm]. */
fun DrawScope.drawCadGrid(t: CadTransform, minorMm: Double, majorMm: Double) {
    val w = size.width
    val h = size.height
    val minorPx = (minorMm * t.pxPerMm).toFloat()
    val majorPx = (majorMm * t.pxPerMm).toFloat()

    if (minorPx >= 6f) {
        val x0 = t.toScreen(0.0, 0.0).x
        var k = kotlin.math.floor((0f - x0) / minorPx)
        var x = x0 + k * minorPx
        while (x < w) {
            drawLine(CadColors.GridMinor, Offset(x, 0f), Offset(x, h), 1f)
            x += minorPx
        }
        val y0 = t.toScreen(0.0, 0.0).y
        k = kotlin.math.floor((0f - y0) / minorPx)
        var y = y0 + k * minorPx
        while (y < h) {
            drawLine(CadColors.GridMinor, Offset(0f, y), Offset(w, y), 1f)
            y += minorPx
        }
    }

    if (majorPx >= 10f) {
        val x0 = t.toScreen(0.0, 0.0).x
        var k = kotlin.math.floor((0f - x0) / majorPx)
        var x = x0 + k * majorPx
        while (x < w) {
            drawLine(CadColors.GridMajor, Offset(x, 0f), Offset(x, h), 1.4f)
            x += majorPx
        }
        val y0 = t.toScreen(0.0, 0.0).y
        k = kotlin.math.floor((0f - y0) / majorPx)
        var y = y0 + k * majorPx
        while (y < h) {
            drawLine(CadColors.GridMajor, Offset(0f, y), Offset(w, y), 1.4f)
            y += majorPx
        }
    }

    // Оси нуля
    val origin = t.toScreen(0.0, 0.0)
    if (origin.x in 0f..w) drawLine(CadColors.Axis, Offset(origin.x, 0f), Offset(origin.x, h), 1.8f)
    if (origin.y in 0f..h) drawLine(CadColors.Axis, Offset(0f, origin.y), Offset(w, origin.y), 1.8f)
}

fun roomPath(vertices: List<CadVertex>, t: CadTransform): Path {
    val p = Path()
    if (vertices.size < 3) return p
    val first = t.toScreen(vertices[0].x, vertices[0].y)
    p.moveTo(first.x, first.y)
    for (i in 1 until vertices.size) {
        val s = t.toScreen(vertices[i].x, vertices[i].y)
        p.lineTo(s.x, s.y)
    }
    p.close()
    return p
}

/** Отрисовка раскладки плитки (плитки приходят в координатах сетки). */
fun DrawScope.drawTiles(layout: TileLayout, spec: TileSpec, t: CadTransform, highlightCuts: Boolean) {
    if (layout.tiles.isEmpty()) return
    val rad = Math.toRadians(-spec.effectiveRotation)
    val c = cos(rad)
    val s = sin(rad)

    fun toScreen(gx: Double, gy: Double): Offset {
        val wx = gx * c - gy * s + spec.originXMm
        val wy = gx * s + gy * c + spec.originYMm
        return t.toScreen(wx, wy)
    }

    val whole = Path()
    val cut = Path()
    for (tile in layout.tiles) {
        val path = if (tile.status == TileStatus.WHOLE) whole else cut
        val p1 = toScreen(tile.x, tile.y)
        val p2 = toScreen(tile.x + tile.w, tile.y)
        val p3 = toScreen(tile.x + tile.w, tile.y + tile.h)
        val p4 = toScreen(tile.x, tile.y + tile.h)
        path.moveTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
        path.lineTo(p3.x, p3.y)
        path.lineTo(p4.x, p4.y)
        path.close()
    }
    drawPath(whole, CadColors.TileWhole)
    drawPath(cut, if (highlightCuts) CadColors.TileCut else CadColors.TileWhole)
    drawPath(whole, CadColors.TileEdge, style = Stroke(width = 0.8f))
    drawPath(cut, CadColors.TileEdge, style = Stroke(width = 0.8f))
}

fun DrawScope.drawObstacles(obstacles: List<CadObstacle>, selectedId: String?, t: CadTransform) {
    obstacles.forEach { o ->
        val pts = obstacleCorners(o).map { t.toScreen(it.x, it.y) }
        val p = Path()
        p.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) p.lineTo(pts[i].x, pts[i].y)
        p.close()
        drawPath(p, CadColors.Obstacle)
        drawPath(
            p,
            if (o.id == selectedId) CadColors.WallSelected else CadColors.ObstacleEdge,
            style = Stroke(width = if (o.id == selectedId) 3.5f else 2f)
        )
    }
}

/** Стены с подписями длин и углов. */
fun DrawScope.drawWalls(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    selectedWall: Int?,
    selectedVertex: Int?,
    t: CadTransform,
    tm: TextMeasurer,
    showLabels: Boolean,
    showAngles: Boolean,
    editMode: Boolean
) {
    if (vertices.size < 2) return
    val n = vertices.size
    for (i in 0 until n) {
        val a = vertices[i]
        val b = vertices[(i + 1) % n]
        val pa = t.toScreen(a.x, a.y)
        val pb = t.toScreen(b.x, b.y)
        val props = walls[a.id]
        val isLocked = props?.lengthLocked == true
        val color = when {
            selectedWall == i -> CadColors.WallSelected
            isLocked -> CadColors.WallLocked
            else -> CadColors.Wall
        }
        drawLine(
            color = color,
            start = pa,
            end = pb,
            strokeWidth = if (selectedWall == i) 7f else 5f,
            cap = StrokeCap.Round
        )
        if (props?.excluded == true) {
            drawLine(
                color = CadColors.Background,
                start = pa,
                end = pb,
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        if (showLabels) {
            val lenMm = distMm(a, b)
            val mid = Offset((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f)
            // Сдвигаем подпись перпендикулярно стене, чтобы не наезжала на линию
            val dx = pb.x - pa.x
            val dy = pb.y - pa.y
            val l = hypot(dx, dy).coerceAtLeast(1f)
            val nx = -dy / l
            val ny = dx / l
            val labelPos = Offset(mid.x + nx * 18f, mid.y + ny * 18f)
            val lockMark = if (isLocked) " 🔒" else ""
            labelAt(
                tm,
                "${fmtMm(lenMm)}$lockMark",
                labelPos,
                fontSize = 11f,
                bg = if (selectedWall == i) Color(0xE0553B00) else CadColors.LabelBg,
                fg = if (isLocked) CadColors.WallLocked else CadColors.Label
            )
        }
    }

    if (showAngles && vertices.size >= 3) {
        for (i in vertices.indices) {
            val ang = interiorAngleDeg(vertices, i)
            val v = vertices[i]
            val p = t.toScreen(v.x, v.y)
            // Смещение подписи угла внутрь помещения (к центроиду)
            val cx = vertices.sumOf { it.x } / vertices.size
            val cy = vertices.sumOf { it.y } / vertices.size
            val cp = t.toScreen(cx, cy)
            val dx = cp.x - p.x
            val dy = cp.y - p.y
            val l = hypot(dx, dy).coerceAtLeast(1f)
            labelAt(
                tm,
                "${fmtDeg(ang)}°",
                Offset(p.x + dx / l * 34f, p.y + dy / l * 34f),
                fontSize = 10f,
                fg = if (abs(ang - 90.0) < 0.15) Color(0xFF9CE3B4) else CadColors.Label
            )
        }
    }

    if (editMode) {
        vertices.forEachIndexed { index, v ->
            val p = t.toScreen(v.x, v.y)
            val sel = index == selectedVertex
            val col = when {
                sel -> CadColors.VertexSelected
                v.pinned -> CadColors.VertexPinned
                else -> CadColors.Vertex
            }
            drawCircle(col, radius = if (sel) 13f else 9f, center = p)
            drawCircle(Color.White, radius = if (sel) 5f else 3f, center = p)
            if (v.pinned) {
                drawCircle(CadColors.VertexPinned, radius = 17f, center = p, style = Stroke(width = 2f))
            }
        }
    }
}

/** Маркер точки старта раскладки. */
fun DrawScope.drawOriginMarker(x: Double, y: Double, rotationDeg: Double, t: CadTransform) {
    val p = t.toScreen(x, y)
    drawCircle(CadColors.Origin, radius = 10f, center = p)
    drawCircle(Color.White, radius = 4f, center = p)
    val rad = Math.toRadians(-rotationDeg)
    val ux = cos(rad).toFloat()
    val uy = sin(rad).toFloat()
    val len = 46f
    // Ось X сетки
    drawLine(CadColors.Origin, p, Offset(p.x + ux * len, p.y + uy * len), 3f, cap = StrokeCap.Round)
    // Ось Y сетки (перпендикуляр)
    drawLine(Color(0xFF4FC3F7), p, Offset(p.x - uy * len, p.y + ux * len), 3f, cap = StrokeCap.Round)
}

/** Масштабная линейка в углу канвы. */
fun DrawScope.drawScaleBar(t: CadTransform, tm: TextMeasurer) {
    val targetPx = 120f
    val rawMm = targetPx / t.pxPerMm
    val nice = listOf(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0)
    val mm = nice.minByOrNull { abs(it - rawMm) } ?: 1000.0
    val px = (mm * t.pxPerMm).toFloat()
    val y = size.height - 26f
    val x0 = 18f
    drawLine(CadColors.Label, Offset(x0, y), Offset(x0 + px, y), 2.5f)
    drawLine(CadColors.Label, Offset(x0, y - 6f), Offset(x0, y + 6f), 2.5f)
    drawLine(CadColors.Label, Offset(x0 + px, y - 6f), Offset(x0 + px, y + 6f), 2.5f)
    val text = if (mm >= 1000) "${fmtNum(mm / 1000.0, 2)} м" else "${fmtMm(mm)} мм"
    labelAt(tm, text, Offset(x0 + px / 2f, y - 18f), fontSize = 10f)
}
