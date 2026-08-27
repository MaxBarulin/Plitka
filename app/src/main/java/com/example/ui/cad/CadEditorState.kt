package com.example.ui.cad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max

enum class CadMode { ROOM, TILE }

enum class DragMode { PAN, EDIT }

data class RoomSnapshot(
    val vertices: List<CadVertex>,
    val walls: Map<String, WallProps>,
    val obstacles: List<CadObstacle>
)

/**
 * Состояние CAD-редактора живёт во ViewModel, а не в композиции: при переключении
 * вкладок экран уничтожается, и чертёж иначе сбрасывался бы на дефолтный прямоугольник.
 */
class CadEditorState {

    // ----------------------------------------------------------------- модель
    var vertices by mutableStateOf(rectangleRoom(3400.0, 2200.0))
    var walls by mutableStateOf<Map<String, WallProps>>(emptyMap())
    var obstacles by mutableStateOf<List<CadObstacle>>(emptyList())

    val undoStack = mutableStateListOf<RoomSnapshot>()

    fun snapshot() {
        undoStack.add(RoomSnapshot(vertices, walls, obstacles))
        if (undoStack.size > 40) undoStack.removeAt(0)
    }

    fun undo() {
        val s = undoStack.removeLastOrNull() ?: return
        vertices = s.vertices
        walls = s.walls
        obstacles = s.obstacles
    }

    // ----------------------------------------------------------------- выбор и режимы
    var mode by mutableStateOf(CadMode.ROOM)
    var dragMode by mutableStateOf(DragMode.PAN)
    var selVertex by mutableStateOf<Int?>(null)
    var selWall by mutableStateOf<Int?>(null)
    var selObstacle by mutableStateOf<String?>(null)
    var roomTab by mutableStateOf(0)
    var tileTab by mutableStateOf(0)
    var panelExpanded by mutableStateOf(true)
    /** Подсказку над чертежом можно убрать тапом — она нужна только первое время. */
    var hintDismissed by mutableStateOf(false)

    // ----------------------------------------------------------------- шаги
    var linStep by mutableStateOf(10.0)
    var angStep by mutableStateOf(1.0)
    var snapToStep by mutableStateOf(true)

    // ----------------------------------------------------------------- плитка
    var tileW by mutableStateOf(600.0)
    var tileH by mutableStateOf(600.0)
    var grout by mutableStateOf(2.0)
    var pattern by mutableStateOf(TilePattern.STRAIGHT)
    var offsetPercent by mutableStateOf(50.0)
    var tileRotation by mutableStateOf(0.0)
    var originMode by mutableStateOf(OriginMode.CORNER)
    var originCorner by mutableStateOf(0)
    var originOffX by mutableStateOf(0.0)
    var originOffY by mutableStateOf(0.0)
    var pointX by mutableStateOf(0.0)
    var pointY by mutableStateOf(0.0)
    var showTiles by mutableStateOf(true)
    var highlightCuts by mutableStateOf(true)
    var showLabels by mutableStateOf(true)
    var showAngles by mutableStateOf(true)

    // ----------------------------------------------------------------- вид
    var pxPerMm by mutableStateOf(0.09f)
    var viewCX by mutableStateOf(1700.0)
    var viewCY by mutableStateOf(1100.0)
    var didFit by mutableStateOf(false)

    // ----------------------------------------------------------------- производные
    /** Площадь пола за вычетом коробов, м². */
    val areaM2: Double
        get() = max(0.0, polygonAreaMm2(vertices) - obstacles.filter { it.subtract }.sumOf { it.w * it.h }) / 1_000_000.0

    /** Периметр без стен, помеченных как проём, м. */
    val perimeterM: Double
        get() {
            var p = 0.0
            val n = vertices.size
            for (i in 0 until n) {
                if (walls[vertices[i].id]?.excluded == true) continue
                p += distMm(vertices[i], vertices[(i + 1) % n])
            }
            return p / 1000.0
        }

    val originPoint: P2
        get() = when (originMode) {
            OriginMode.CORNER -> {
                val v = vertices.getOrNull(originCorner.coerceIn(0, max(0, vertices.size - 1)))
                if (v == null) P2(0.0, 0.0) else P2(v.x + originOffX, v.y + originOffY)
            }
            OriginMode.CENTER -> {
                val b = boundsOf(vertices)
                P2(b.cx + originOffX, b.cy + originOffY)
            }
            OriginMode.POINT -> P2(pointX, pointY)
        }

    val tileSpec: TileSpec
        get() = TileSpec(
            widthMm = tileW,
            heightMm = tileH,
            groutMm = grout,
            pattern = pattern,
            offsetFraction = (offsetPercent / 100.0).coerceIn(0.0, 0.9),
            rotationDeg = tileRotation,
            originXMm = originPoint.x,
            originYMm = originPoint.y
        )

    fun snapVal(v: Double): Double =
        if (snapToStep && linStep > 0) Math.round(v / linStep) * linStep else v
}
