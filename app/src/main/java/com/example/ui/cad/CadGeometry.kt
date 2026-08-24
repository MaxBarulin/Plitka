package com.example.ui.cad

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// =====================================================================================
// МОДЕЛЬ ПОМЕЩЕНИЯ
// Все внутренние координаты и размеры — в МИЛЛИМЕТРАХ (Double).
// Так шаг «1 мм» остаётся точным и не накапливает погрешность float.
// =====================================================================================

/** Угол помещения. */
data class CadVertex(
    val id: String = java.util.UUID.randomUUID().toString(),
    val x: Double,
    val y: Double,
    /** Закреплённый угол не двигается решателем связей. */
    val pinned: Boolean = false
)

/** Свойства стены. В карте стен ключ — id НАЧАЛЬНОЙ вершины ребра. */
data class WallProps(
    /** Длина зафиксирована: при перемещении соседних углов она сохраняется. */
    val lengthLocked: Boolean = false,
    val lockedLengthMm: Double = 0.0,
    /** Стена не облицовывается (например, проём) — исключается из погонажа. */
    val excluded: Boolean = false
)

/** Прямоугольное препятствие: короб, колонна, инсталляция, ниша. */
data class CadObstacle(
    val id: String = java.util.UUID.randomUUID().toString(),
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val rotationDeg: Double = 0.0,
    val name: String = "Короб",
    /** true — вычитается из площади пола; false — просто отметка на плане. */
    val subtract: Boolean = true
)

/** Точка в мм. */
data class P2(val x: Double, val y: Double)

// =====================================================================================
// БАЗОВАЯ ГЕОМЕТРИЯ
// =====================================================================================

fun normDeg(d: Double): Double {
    var v = d % 360.0
    if (v < 0) v += 360.0
    // Иначе идеально горизонтальная стена из-за погрешности читалась бы как 360°, а не 0°.
    if (v > 360.0 - 1e-7) v = 0.0
    return v
}

fun distMm(a: CadVertex, b: CadVertex): Double = hypot(b.x - a.x, b.y - a.y)

/**
 * Азимут стены в «человеческих» градусах: 0° — вправо, 90° — вверх,
 * 180° — влево, 270° — вниз. Экранная ось Y смотрит вниз, поэтому знак dy инвертирован.
 */
fun azimuthDeg(from: CadVertex, to: CadVertex): Double =
    normDeg(Math.toDegrees(atan2(-(to.y - from.y), to.x - from.x)))

/** Знаковая площадь (мм²). Знак задаёт направление обхода контура. */
fun signedAreaMm2(v: List<CadVertex>): Double {
    if (v.size < 3) return 0.0
    var s = 0.0
    for (i in v.indices) {
        val j = (i + 1) % v.size
        s += v[i].x * v[j].y - v[j].x * v[i].y
    }
    return s / 2.0
}

fun polygonAreaMm2(v: List<CadVertex>): Double = abs(signedAreaMm2(v))

fun perimeterMm(v: List<CadVertex>): Double {
    if (v.size < 2) return 0.0
    var p = 0.0
    for (i in v.indices) p += distMm(v[i], v[(i + 1) % v.size])
    return p
}

/** Внутренний угол при вершине index, в градусах. */
fun interiorAngleDeg(v: List<CadVertex>, index: Int): Double {
    if (v.size < 3) return 0.0
    val n = v.size
    val prev = v[(index - 1 + n) % n]
    val cur = v[index]
    val next = v[(index + 1) % n]
    val a1 = atan2(prev.y - cur.y, prev.x - cur.x)
    val a2 = atan2(next.y - cur.y, next.x - cur.x)
    var ang = Math.toDegrees(a2 - a1)
    ang = normDeg(ang)
    // На экране ось Y направлена вниз, поэтому положительная знаковая площадь
    // соответствует обходу по часовой стрелке и угол нужно дополнить до 360°.
    return if (signedAreaMm2(v) > 0) 360.0 - ang else ang
}

data class Bounds(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width get() = right - left
    val height get() = bottom - top
    val cx get() = (left + right) / 2.0
    val cy get() = (top + bottom) / 2.0
}

fun boundsOf(v: List<CadVertex>): Bounds {
    if (v.isEmpty()) return Bounds(0.0, 0.0, 1000.0, 1000.0)
    var l = v[0].x; var r = v[0].x; var t = v[0].y; var b = v[0].y
    for (p in v) {
        if (p.x < l) l = p.x
        if (p.x > r) r = p.x
        if (p.y < t) t = p.y
        if (p.y > b) b = p.y
    }
    return Bounds(l, t, r, b)
}

fun pointInPolygon(px: Double, py: Double, poly: List<P2>): Boolean {
    if (poly.size < 3) return false
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val xi = poly[i].x; val yi = poly[i].y
        val xj = poly[j].x; val yj = poly[j].y
        if ((yi > py) != (yj > py)) {
            val xCross = (xj - xi) * (py - yi) / (yj - yi) + xi
            if (px < xCross) inside = !inside
        }
        j = i
    }
    return inside
}

/** Расстояние от точки до отрезка (мм). */
fun distToSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
    val dx = bx - ax
    val dy = by - ay
    val len2 = dx * dx + dy * dy
    if (len2 < 1e-9) return hypot(px - ax, py - ay)
    var t = ((px - ax) * dx + (py - ay) * dy) / len2
    t = t.coerceIn(0.0, 1.0)
    return hypot(px - (ax + t * dx), py - (ay + t * dy))
}

// =====================================================================================
// РЕШАТЕЛЬ СВЯЗЕЙ: сохранение зафиксированных длин стен
// =====================================================================================

/**
 * Итеративно (метод Гаусса–Зейделя / position based dynamics) подтягивает вершины так,
 * чтобы стены с зафиксированной длиной сохранили свою длину.
 *
 * [frozen] — вершины, которые нельзя двигать (та, что пользователь только что задал явно).
 * Закреплённые (pinned) вершины тоже неподвижны.
 * Если у стены оба конца неподвижны — связь просто пропускается (переопределённая система).
 */
fun relaxWallLocks(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    frozen: Set<String> = emptySet(),
    iterations: Int = 96
): List<CadVertex> {
    val n = vertices.size
    if (n < 2) return vertices
    val locked = ArrayList<Triple<Int, Int, Double>>() // i, j, targetLen
    for (i in 0 until n) {
        val w = walls[vertices[i].id] ?: continue
        if (!w.lengthLocked || w.lockedLengthMm <= 0.0) continue
        locked.add(Triple(i, (i + 1) % n, w.lockedLengthMm))
    }
    if (locked.isEmpty()) return vertices

    val xs = DoubleArray(n) { vertices[it].x }
    val ys = DoubleArray(n) { vertices[it].y }

    fun solve(movable: BooleanArray) {
        repeat(iterations) {
            for ((i, j, target) in locked) {
                val mi = movable[i]
                val mj = movable[j]
                if (!mi && !mj) continue
                var dx = xs[j] - xs[i]
                var dy = ys[j] - ys[i]
                var d = hypot(dx, dy)
                if (d < 1e-6) {
                    // Вырожденная стена — растащим по X, чтобы направление стало определённым.
                    dx = 1.0; dy = 0.0; d = 1.0
                }
                val diff = (d - target) / d
                val wi = if (mi) 1.0 else 0.0
                val wj = if (mj) 1.0 else 0.0
                val sum = wi + wj
                val ki = wi / sum
                val kj = wj / sum
                xs[i] += dx * diff * ki
                ys[i] += dy * diff * ki
                xs[j] -= dx * diff * kj
                ys[j] -= dy * diff * kj
            }
        }
    }

    // Проход 1: подстраиваем соседей, не трогая только что заданную вершину.
    solve(BooleanArray(n) { !vertices[it].pinned && vertices[it].id !in frozen })
    // Проход 2: если система была переопределена (оба конца стены неподвижны),
    // остаточную ошибку гасим, разрешив двигать и заданную вершину — замок и
    // закреплённые углы важнее, чем буквальное попадание пальца.
    solve(BooleanArray(n) { !vertices[it].pinned })

    return vertices.mapIndexed { idx, v -> v.copy(x = xs[idx], y = ys[idx]) }
}

// =====================================================================================
// РЕДАКТИРУЮЩИЕ ОПЕРАЦИИ
// =====================================================================================

enum class WallAnchor { START, END, CENTER }

/** Сдвиг угла на dx/dy мм с последующим восстановлением зафиксированных длин. */
fun nudgeVertex(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    index: Int,
    dxMm: Double,
    dyMm: Double
): List<CadVertex> {
    if (index !in vertices.indices) return vertices
    val moved = vertices.toMutableList()
    val v = moved[index]
    moved[index] = v.copy(x = v.x + dxMm, y = v.y + dyMm)
    return relaxWallLocks(moved, walls, frozen = setOf(v.id))
}

/** Явная установка координат угла. */
fun setVertexPosition(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    index: Int,
    xMm: Double,
    yMm: Double
): List<CadVertex> {
    if (index !in vertices.indices) return vertices
    val moved = vertices.toMutableList()
    val v = moved[index]
    moved[index] = v.copy(x = xMm, y = yMm)
    return relaxWallLocks(moved, walls, frozen = setOf(v.id))
}

/**
 * Установка длины стены [edgeIndex]. Направление стены сохраняется,
 * двигается один из концов (или оба симметрично).
 */
fun setWallLength(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    edgeIndex: Int,
    targetMm: Double,
    anchor: WallAnchor
): List<CadVertex> {
    val n = vertices.size
    if (n < 2 || edgeIndex !in 0 until n || targetMm <= 0.0) return vertices
    val i = edgeIndex
    val j = (edgeIndex + 1) % n
    val a = vertices[i]
    val b = vertices[j]
    var dx = b.x - a.x
    var dy = b.y - a.y
    var len = hypot(dx, dy)
    if (len < 1e-6) { dx = 1.0; dy = 0.0; len = 1.0 }
    val ux = dx / len
    val uy = dy / len
    val delta = targetMm - len

    val out = vertices.toMutableList()
    val frozen = HashSet<String>()
    when (anchor) {
        WallAnchor.START -> {
            out[j] = b.copy(x = a.x + ux * targetMm, y = a.y + uy * targetMm)
            frozen += a.id; frozen += b.id
        }
        WallAnchor.END -> {
            out[i] = a.copy(x = b.x - ux * targetMm, y = b.y - uy * targetMm)
            frozen += a.id; frozen += b.id
        }
        WallAnchor.CENTER -> {
            out[i] = a.copy(x = a.x - ux * delta / 2.0, y = a.y - uy * delta / 2.0)
            out[j] = b.copy(x = b.x + ux * delta / 2.0, y = b.y + uy * delta / 2.0)
            frozen += a.id; frozen += b.id
        }
    }
    return relaxWallLocks(out, walls, frozen = frozen)
}

/** Поворот стены вокруг одного из концов до заданного азимута. */
fun setWallAzimuth(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    edgeIndex: Int,
    azimuth: Double,
    pivot: WallAnchor
): List<CadVertex> {
    val n = vertices.size
    if (n < 2 || edgeIndex !in 0 until n) return vertices
    val i = edgeIndex
    val j = (edgeIndex + 1) % n
    val a = vertices[i]
    val b = vertices[j]
    val len = distMm(a, b).let { if (it < 1e-6) 1000.0 else it }
    val rad = Math.toRadians(azimuth)
    val ux = cos(rad)
    val uy = -sin(rad) // экранная Y вниз

    val out = vertices.toMutableList()
    val frozen = HashSet<String>()
    if (pivot == WallAnchor.END) {
        out[i] = a.copy(x = b.x - ux * len, y = b.y - uy * len)
    } else {
        out[j] = b.copy(x = a.x + ux * len, y = a.y + uy * len)
    }
    frozen += a.id; frozen += b.id
    return relaxWallLocks(out, walls, frozen = frozen)
}

/** Добавить угол в середину стены [edgeIndex]. */
fun splitWall(vertices: List<CadVertex>, edgeIndex: Int): List<CadVertex> {
    val n = vertices.size
    if (n < 2 || edgeIndex !in 0 until n) return vertices
    val a = vertices[edgeIndex]
    val b = vertices[(edgeIndex + 1) % n]
    val mid = CadVertex(x = (a.x + b.x) / 2.0, y = (a.y + b.y) / 2.0)
    val out = vertices.toMutableList()
    out.add(edgeIndex + 1, mid)
    return out
}

/**
 * Добавить угол «полярно»: от конца стены [edgeIndex] отложить отрезок
 * длиной [lengthMm] под азимутом [azimuth].
 */
fun appendPolarVertex(
    vertices: List<CadVertex>,
    afterIndex: Int,
    lengthMm: Double,
    azimuth: Double
): List<CadVertex> {
    if (vertices.isEmpty()) return vertices
    val idx = afterIndex.coerceIn(0, vertices.size - 1)
    val base = vertices[idx]
    val rad = Math.toRadians(azimuth)
    val p = CadVertex(x = base.x + cos(rad) * lengthMm, y = base.y - sin(rad) * lengthMm)
    val out = vertices.toMutableList()
    out.add(idx + 1, p)
    return out
}

/** Прямоугольная комната по габаритам. */
fun rectangleRoom(widthMm: Double, heightMm: Double, originX: Double = 0.0, originY: Double = 0.0): List<CadVertex> =
    listOf(
        CadVertex(x = originX, y = originY),
        CadVertex(x = originX + widthMm, y = originY),
        CadVertex(x = originX + widthMm, y = originY + heightMm),
        CadVertex(x = originX, y = originY + heightMm)
    )

/** Выровнять все стены по ортогонали (90°), сохранив габарит. */
fun orthogonalize(vertices: List<CadVertex>, toleranceDeg: Double = 25.0): List<CadVertex> {
    val n = vertices.size
    if (n < 3) return vertices
    val out = vertices.toMutableList()
    for (i in 0 until n) {
        val j = (i + 1) % n
        val a = out[i]
        val b = out[j]
        val az = azimuthDeg(a, b)
        val snapped = normDeg(Math.round(az / 90.0) * 90.0)
        if (abs(normDeg(az - snapped).let { if (it > 180) 360 - it else it }) <= toleranceDeg) {
            val len = distMm(a, b)
            val rad = Math.toRadians(snapped)
            out[j] = b.copy(x = a.x + cos(rad) * len, y = a.y - sin(rad) * len)
        }
    }
    return out
}

// =====================================================================================
// РАСКЛАДКА ПЛИТКИ
// =====================================================================================

enum class TilePattern { STRAIGHT, OFFSET, HERRINGBONE, DIAGONAL }

fun tilePatternTitle(p: TilePattern): String = when (p) {
    TilePattern.STRAIGHT -> "Прямая (шов в шов)"
    TilePattern.OFFSET -> "Разбежка"
    TilePattern.HERRINGBONE -> "Ёлочка"
    TilePattern.DIAGONAL -> "Диагональ 45°"
}

/** Откуда стартует сетка плитки. */
enum class OriginMode { CORNER, POINT, CENTER }

data class TileSpec(
    val widthMm: Double = 600.0,
    val heightMm: Double = 600.0,
    val groutMm: Double = 2.0,
    val pattern: TilePattern = TilePattern.STRAIGHT,
    /** Смещение ряда в долях ширины плитки: 0.5 = 50 %, 0.333 = 1/3. */
    val offsetFraction: Double = 0.5,
    val rotationDeg: Double = 0.0,
    val originXMm: Double = 0.0,
    val originYMm: Double = 0.0
) {
    val effectiveRotation: Double get() = if (pattern == TilePattern.DIAGONAL) rotationDeg + 45.0 else rotationDeg
}

enum class TileStatus { WHOLE, CUT }

/** Плитка в системе координат сетки (оси совпадают со швами). */
data class GridTile(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val status: TileStatus
)

data class LayoutStats(
    val wholeCount: Int,
    val cutCount: Int,
    val totalCount: Int,
    val roomAreaMm2: Double,
    /** Наименьшая подрезка по краям габарита, мм (полезно, чтобы не было «сопливой» подрезки). */
    val minEdgeCutMm: Double,
    val edgeCutsMm: List<Double>,
    val truncated: Boolean
)

data class TileLayout(
    val tiles: List<GridTile>,
    val stats: LayoutStats
)

private const val MAX_TILES = 6000

/** Мир → координаты сетки (поворот вокруг точки старта). */
fun worldToGrid(x: Double, y: Double, spec: TileSpec): P2 {
    val rad = Math.toRadians(-spec.effectiveRotation)
    val dx = x - spec.originXMm
    val dy = y - spec.originYMm
    // Поворот на -angle с учётом экранной Y вниз
    val c = cos(rad); val s = sin(rad)
    return P2(dx * c + dy * s, -dx * s + dy * c)
}

/** Координаты сетки → мир. */
fun gridToWorld(x: Double, y: Double, spec: TileSpec): P2 {
    val rad = Math.toRadians(-spec.effectiveRotation)
    val c = cos(rad); val s = sin(rad)
    // Обратное преобразование к worldToGrid
    val wx = x * c - y * s
    val wy = x * s + y * c
    return P2(wx + spec.originXMm, wy + spec.originYMm)
}

private fun segIntersectsRect(
    ax: Double, ay: Double, bx: Double, by: Double,
    l: Double, t: Double, r: Double, bo: Double
): Boolean {
    // Быстрое отсечение
    if (max(ax, bx) < l || min(ax, bx) > r || max(ay, by) < t || min(ay, by) > bo) return false
    // Если хотя бы один конец внутри — пересекает
    if (ax in l..r && ay in t..bo) return true
    if (bx in l..r && by in t..bo) return true
    // Проверка со сторонами прямоугольника
    fun segSeg(
        p0x: Double, p0y: Double, p1x: Double, p1y: Double,
        q0x: Double, q0y: Double, q1x: Double, q1y: Double
    ): Boolean {
        fun cross(ox: Double, oy: Double, x1: Double, y1: Double, x2: Double, y2: Double) =
            (x1 - ox) * (y2 - oy) - (y1 - oy) * (x2 - ox)
        val d1 = cross(p0x, p0y, p1x, p1y, q0x, q0y)
        val d2 = cross(p0x, p0y, p1x, p1y, q1x, q1y)
        val d3 = cross(q0x, q0y, q1x, q1y, p0x, p0y)
        val d4 = cross(q0x, q0y, q1x, q1y, p1x, p1y)
        return ((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0))
    }
    return segSeg(ax, ay, bx, by, l, t, r, t) ||
        segSeg(ax, ay, bx, by, r, t, r, bo) ||
        segSeg(ax, ay, bx, by, r, bo, l, bo) ||
        segSeg(ax, ay, bx, by, l, bo, l, t)
}

private fun classify(
    x: Double, y: Double, w: Double, h: Double,
    room: List<P2>,
    holes: List<List<P2>>
): TileStatus? {
    // Пробы берём чуть внутрь плитки: если шов идеально совпал с гранью стены,
    // точка ровно на границе полигона иначе давала бы ложную «подрезку».
    val eps = (min(w, h) * 0.02).coerceIn(0.05, 2.0)
    val l = x + eps; val t = y + eps; val r = x + w - eps; val b = y + h - eps
    val corners = listOf(P2(l, t), P2(r, t), P2(r, b), P2(l, b))
    var insideCount = 0
    for (c in corners) if (pointInPolygon(c.x, c.y, room)) insideCount++

    var crossesRoom = false
    for (i in room.indices) {
        val a = room[i]
        val bb = room[(i + 1) % room.size]
        if (segIntersectsRect(a.x, a.y, bb.x, bb.y, l, t, r, b)) { crossesRoom = true; break }
    }

    if (insideCount == 0 && !crossesRoom) return null // плитка целиком вне помещения

    var touchesHole = false
    for (hole in holes) {
        var cornersIn = 0
        for (c in corners) if (pointInPolygon(c.x, c.y, hole)) cornersIn++
        var cross = false
        for (i in hole.indices) {
            val a = hole[i]
            val bb = hole[(i + 1) % hole.size]
            if (segIntersectsRect(a.x, a.y, bb.x, bb.y, l, t, r, b)) { cross = true; break }
        }
        if (cornersIn == 4 && !cross) return null // плитка целиком внутри короба
        if (cross || cornersIn > 0) touchesHole = true
    }

    return if (insideCount == 4 && !crossesRoom && !touchesHole)
        TileStatus.WHOLE else TileStatus.CUT
}

/**
 * Генерация раскладки. Плитки возвращаются в координатах сетки —
 * отрисовка накладывает поворот и перенос через [gridToWorld].
 */
fun generateLayout(
    vertices: List<CadVertex>,
    obstacles: List<CadObstacle>,
    spec: TileSpec
): TileLayout {
    val empty = LayoutStats(0, 0, 0, 0.0, 0.0, emptyList(), false)
    if (vertices.size < 3 || spec.widthMm <= 1 || spec.heightMm <= 1) return TileLayout(emptyList(), empty)

    val room = vertices.map { val p = worldToGrid(it.x, it.y, spec); P2(p.x, p.y) }
    val holes = obstacles.map { o -> obstacleCorners(o).map { val p = worldToGrid(it.x, it.y, spec); P2(p.x, p.y) } }

    var minX = room[0].x; var maxX = room[0].x; var minY = room[0].y; var maxY = room[0].y
    for (p in room) {
        minX = min(minX, p.x); maxX = max(maxX, p.x)
        minY = min(minY, p.y); maxY = max(maxY, p.y)
    }

    val g = spec.groutMm
    val tw = spec.widthMm
    val th = spec.heightMm
    val stepX = tw + g
    val stepY = th + g

    val tiles = ArrayList<GridTile>()
    var whole = 0
    var cut = 0
    var truncated = false

    fun add(x: Double, y: Double, w: Double, h: Double) {
        if (tiles.size >= MAX_TILES) { truncated = true; return }
        val st = classify(x, y, w, h, room, holes) ?: return
        tiles.add(GridTile(x, y, w, h, st))
        if (st == TileStatus.WHOLE) whole++ else cut++
    }

    if (spec.pattern == TilePattern.HERRINGBONE) {
        // Ёлочка: элементарная фигура из двух плиток (горизонтальная + вертикальная),
        // размножается по решётке u = (L+S, S-L), v = (S, S), где L — длинная сторона.
        val lng = max(tw, th) + g
        val shrt = min(tw, th) + g
        val ux = lng + shrt; val uy = shrt - lng
        val vx = shrt; val vy = shrt
        val det = ux * vy - uy * vx
        if (abs(det) < 1e-6) return TileLayout(emptyList(), empty)
        // Диапазон индексов решётки, покрывающий габарит
        val cornersBb = listOf(P2(minX, minY), P2(maxX, minY), P2(maxX, maxY), P2(minX, maxY))
        var iMin = Int.MAX_VALUE; var iMax = Int.MIN_VALUE
        var jMin = Int.MAX_VALUE; var jMax = Int.MIN_VALUE
        for (c in cornersBb) {
            val i = (c.x * vy - c.y * vx) / det
            val j = (ux * c.y - uy * c.x) / det
            iMin = min(iMin, floor(i).toInt()); iMax = max(iMax, ceil(i).toInt())
            jMin = min(jMin, floor(j).toInt()); jMax = max(jMax, ceil(j).toInt())
        }
        val pad = 3
        for (i in (iMin - pad)..(iMax + pad)) {
            for (j in (jMin - pad)..(jMax + pad)) {
                val ox = i * ux + j * vx
                val oy = i * uy + j * vy
                // Горизонтальная плитка
                add(ox + g / 2, oy + g / 2, lng - g, shrt - g)
                // Вертикальная плитка
                add(ox + lng + g / 2, oy + shrt - lng + g / 2, shrt - g, lng - g)
                if (truncated) break
            }
            if (truncated) break
        }
    } else {
        val col0 = floor((minX - stepX * 2) / stepX).toInt()
        val col1 = ceil((maxX + stepX * 2) / stepX).toInt()
        val row0 = floor((minY - stepY * 2) / stepY).toInt()
        val row1 = ceil((maxY + stepY * 2) / stepY).toInt()
        for (row in row0..row1) {
            // Разбежка: период ряда = 1 / доля смещения (0,5 -> через 2 ряда, 1/3 -> через 3).
            val shift = if (spec.pattern == TilePattern.OFFSET && spec.offsetFraction > 0.001) {
                val period = Math.round(1.0 / spec.offsetFraction).toInt().coerceIn(2, 12)
                (((row % period) + period) % period) * spec.offsetFraction * stepX
            } else 0.0
            for (col in (col0 - 2)..(col1 + 2)) {
                val x = col * stepX + shift
                val y = row * stepY
                add(x + g / 2, y + g / 2, tw, th)
                if (truncated) break
            }
            if (truncated) break
        }
    }

    // Подрезка по краям габарита: расстояние от границы до ближайшего шва.
    val edgeCuts = if (spec.pattern == TilePattern.HERRINGBONE) emptyList() else listOf(
        cutAt(minX, stepX), cutAt(maxX, stepX), cutAt(minY, stepY), cutAt(maxY, stepY)
    )

    val roomArea = polygonAreaMm2(vertices) -
        obstacles.filter { it.subtract }.sumOf { it.w * it.h }

    return TileLayout(
        tiles = tiles,
        stats = LayoutStats(
            wholeCount = whole,
            cutCount = cut,
            totalCount = whole + cut,
            roomAreaMm2 = max(0.0, roomArea),
            minEdgeCutMm = edgeCuts.minOrNull() ?: 0.0,
            edgeCutsMm = edgeCuts,
            truncated = truncated
        )
    )
}

private fun cutAt(coord: Double, step: Double): Double {
    val m = ((coord % step) + step) % step
    return min(m, step - m)
}

fun obstacleCorners(o: CadObstacle): List<P2> {
    val rad = Math.toRadians(-o.rotationDeg)
    val c = cos(rad); val s = sin(rad)
    val cx = o.x + o.w / 2
    val cy = o.y + o.h / 2
    val hw = o.w / 2; val hh = o.h / 2
    return listOf(
        P2(-hw, -hh), P2(hw, -hh), P2(hw, hh), P2(-hw, hh)
    ).map { P2(cx + it.x * c - it.y * s, cy + it.x * s + it.y * c) }
}

// =====================================================================================
// ФОРМАТИРОВАНИЕ
// =====================================================================================

/** мм → «2 450 мм» / «2,45 м» в зависимости от режима. */
fun fmtMm(mm: Double): String = "%.0f".format(mm)

fun fmtM(mm: Double): String = "%.3f".format(mm / 1000.0)

fun fmtArea(mm2: Double): String = "%.2f".format(mm2 / 1_000_000.0)

fun fmtDeg(d: Double): String = "%.1f".format(d)
