package com.example.data

import com.example.ui.cad.CadEditorState
import com.example.ui.cad.CadObstacle
import com.example.ui.cad.CadVertex
import com.example.ui.cad.OriginMode
import com.example.ui.cad.TilePattern
import com.example.ui.cad.WallProps
import com.example.ui.calc.CalculatorState
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

// =====================================================================================
// Снимки рабочего состояния.
//
// Мастер посмотрел смету, убрал телефон, вернулся через неделю — всё введённое должно
// быть на месте. Поэтому состояние экранов сериализуется в JSON и хранится в базе:
// одна строка «рабочий стол» (автосохранение) плюс по строке на каждый сохранённый расчёт.
//
// Перечисления пишем строками: так переименование в коде не ломает старые записи молча —
// неизвестное значение просто откатится к значению по умолчанию.
// =====================================================================================

@JsonClass(generateAdapter = true)
data class VertexDto(val id: String, val x: Double, val y: Double, val pinned: Boolean)

@JsonClass(generateAdapter = true)
data class WallDto(val id: String, val lengthLocked: Boolean, val lockedLengthMm: Double, val excluded: Boolean)

@JsonClass(generateAdapter = true)
data class ObstacleDto(
    val id: String,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val rotationDeg: Double,
    val name: String,
    val subtract: Boolean
)

@JsonClass(generateAdapter = true)
data class CadSnapshot(
    val vertices: List<VertexDto>,
    val walls: List<WallDto>,
    val obstacles: List<ObstacleDto>,
    val tileW: Double,
    val tileH: Double,
    val grout: Double,
    val pattern: String,
    val offsetPercent: Double,
    val tileRotation: Double,
    val originMode: String,
    val originCorner: Int,
    val originOffX: Double,
    val originOffY: Double,
    val pointX: Double,
    val pointY: Double,
    val linStep: Double,
    val angStep: Double,
    val snapToStep: Boolean,
    val showTiles: Boolean,
    val highlightCuts: Boolean,
    val showLabels: Boolean,
    val showAngles: Boolean
)

@JsonClass(generateAdapter = true)
data class CalcSnapshot(
    val syncWithCad: Boolean,
    val areaM2: Double,
    val perimeterM: Double,
    val openingsM: Double,
    val tileW: Double,
    val tileH: Double,
    val tileThickness: Double,
    val grout: Double,
    val wastePercent: Double,
    val piecesPerPack: Double,
    val tilePriceM2: Double,
    val tilePricePiece: Double,
    val density: Double,
    val notchAuto: Boolean,
    val notchIndex: Int,
    val glueKgM2: Double,
    val levelingMm: Double,
    val backButtering: Boolean,
    val glueBagKg: Double,
    val gluePrice: Double,
    val glueReserve: Double,
    val groutDensity: Double,
    val groutEpoxy: Boolean,
    val groutReserve: Double,
    val groutPackKg: Double,
    val groutPriceKg: Double,
    val screedOn: Boolean,
    val screedThickness: Double,
    val screedKgMm: Double,
    val screedBagKg: Double,
    val screedPrice: Double,
    val primerOn: Boolean,
    val primerKg: Double,
    val primerLayers: Double,
    val primerPackKg: Double,
    val primerPrice: Double,
    val waterproofOn: Boolean,
    val wpKg: Double,
    val wpLayers: Double,
    val wpPackKg: Double,
    val wpPrice: Double,
    val crossesPerTile: Double,
    val clipsPerTile: Double,
    val dailyOutputM2: Double,
    val priceCross: Double,
    val priceClip: Double,
    val priceWedge: Double,
    val trimOn: Boolean,
    val trimPieceM: Double,
    val trimReserve: Double,
    val trimPrice: Double,
    val workPriceM2: Double,
    val tripCapacity: Double
)

object Snapshots {
    private val moshi: Moshi = Moshi.Builder().build()
    private val cadAdapter = moshi.adapter(CadSnapshot::class.java)
    private val calcAdapter = moshi.adapter(CalcSnapshot::class.java)

    // ------------------------------------------------------------------ CAD

    fun capture(st: CadEditorState) = CadSnapshot(
        vertices = st.vertices.map { VertexDto(it.id, it.x, it.y, it.pinned) },
        walls = st.walls.map { (id, w) -> WallDto(id, w.lengthLocked, w.lockedLengthMm, w.excluded) },
        obstacles = st.obstacles.map {
            ObstacleDto(it.id, it.x, it.y, it.w, it.h, it.rotationDeg, it.name, it.subtract)
        },
        tileW = st.tileW,
        tileH = st.tileH,
        grout = st.grout,
        pattern = st.pattern.name,
        offsetPercent = st.offsetPercent,
        tileRotation = st.tileRotation,
        originMode = st.originMode.name,
        originCorner = st.originCorner,
        originOffX = st.originOffX,
        originOffY = st.originOffY,
        pointX = st.pointX,
        pointY = st.pointY,
        linStep = st.linStep,
        angStep = st.angStep,
        snapToStep = st.snapToStep,
        showTiles = st.showTiles,
        highlightCuts = st.highlightCuts,
        showLabels = st.showLabels,
        showAngles = st.showAngles
    )

    fun restore(st: CadEditorState, s: CadSnapshot) {
        if (s.vertices.size >= 3) {
            st.vertices = s.vertices.map { CadVertex(it.id, it.x, it.y, it.pinned) }
            st.walls = s.walls.associate { it.id to WallProps(it.lengthLocked, it.lockedLengthMm, it.excluded) }
            st.obstacles = s.obstacles.map {
                CadObstacle(it.id, it.x, it.y, it.w, it.h, it.rotationDeg, it.name, it.subtract)
            }
        }
        st.tileW = s.tileW
        st.tileH = s.tileH
        st.grout = s.grout
        st.pattern = TilePattern.entries.firstOrNull { it.name == s.pattern } ?: TilePattern.STRAIGHT
        st.offsetPercent = s.offsetPercent
        st.tileRotation = s.tileRotation
        st.originMode = OriginMode.entries.firstOrNull { it.name == s.originMode } ?: OriginMode.CORNER
        st.originCorner = s.originCorner
        st.originOffX = s.originOffX
        st.originOffY = s.originOffY
        st.pointX = s.pointX
        st.pointY = s.pointY
        st.linStep = s.linStep
        st.angStep = s.angStep
        st.snapToStep = s.snapToStep
        st.showTiles = s.showTiles
        st.highlightCuts = s.highlightCuts
        st.showLabels = s.showLabels
        st.showAngles = s.showAngles
        st.selVertex = null
        st.selWall = null
        st.selObstacle = null
        st.undoStack.clear()
        st.didFit = false // вписать восстановленный план в экран
    }

    // ------------------------------------------------------------------ Калькулятор

    fun capture(cs: CalculatorState) = CalcSnapshot(
        syncWithCad = cs.syncWithCad,
        areaM2 = cs.areaM2,
        perimeterM = cs.perimeterM,
        openingsM = cs.openingsM,
        tileW = cs.tileW,
        tileH = cs.tileH,
        tileThickness = cs.tileThickness,
        grout = cs.grout,
        wastePercent = cs.wastePercent,
        piecesPerPack = cs.piecesPerPack,
        tilePriceM2 = cs.tilePriceM2,
        tilePricePiece = cs.tilePricePiece,
        density = cs.density,
        notchAuto = cs.notchAuto,
        notchIndex = cs.notchIndex,
        glueKgM2 = cs.glueKgM2,
        levelingMm = cs.levelingMm,
        backButtering = cs.backButtering,
        glueBagKg = cs.glueBagKg,
        gluePrice = cs.gluePrice,
        glueReserve = cs.glueReserve,
        groutDensity = cs.groutDensity,
        groutEpoxy = cs.groutEpoxy,
        groutReserve = cs.groutReserve,
        groutPackKg = cs.groutPackKg,
        groutPriceKg = cs.groutPriceKg,
        screedOn = cs.screedOn,
        screedThickness = cs.screedThickness,
        screedKgMm = cs.screedKgMm,
        screedBagKg = cs.screedBagKg,
        screedPrice = cs.screedPrice,
        primerOn = cs.primerOn,
        primerKg = cs.primerKg,
        primerLayers = cs.primerLayers,
        primerPackKg = cs.primerPackKg,
        primerPrice = cs.primerPrice,
        waterproofOn = cs.waterproofOn,
        wpKg = cs.wpKg,
        wpLayers = cs.wpLayers,
        wpPackKg = cs.wpPackKg,
        wpPrice = cs.wpPrice,
        crossesPerTile = cs.crossesPerTile,
        clipsPerTile = cs.clipsPerTile,
        dailyOutputM2 = cs.dailyOutputM2,
        priceCross = cs.priceCross,
        priceClip = cs.priceClip,
        priceWedge = cs.priceWedge,
        trimOn = cs.trimOn,
        trimPieceM = cs.trimPieceM,
        trimReserve = cs.trimReserve,
        trimPrice = cs.trimPrice,
        workPriceM2 = cs.workPriceM2,
        tripCapacity = cs.tripCapacity
    )

    fun restore(cs: CalculatorState, s: CalcSnapshot) {
        cs.syncWithCad = s.syncWithCad
        cs.areaM2 = s.areaM2
        cs.perimeterM = s.perimeterM
        cs.openingsM = s.openingsM
        cs.tileW = s.tileW
        cs.tileH = s.tileH
        cs.tileThickness = s.tileThickness
        cs.grout = s.grout
        cs.wastePercent = s.wastePercent
        cs.piecesPerPack = s.piecesPerPack
        cs.tilePriceM2 = s.tilePriceM2
        cs.tilePricePiece = s.tilePricePiece
        cs.density = s.density
        cs.notchAuto = s.notchAuto
        cs.notchIndex = s.notchIndex
        cs.glueKgM2 = s.glueKgM2
        cs.levelingMm = s.levelingMm
        cs.backButtering = s.backButtering
        cs.glueBagKg = s.glueBagKg
        cs.gluePrice = s.gluePrice
        cs.glueReserve = s.glueReserve
        cs.groutDensity = s.groutDensity
        cs.groutEpoxy = s.groutEpoxy
        cs.groutReserve = s.groutReserve
        cs.groutPackKg = s.groutPackKg
        cs.groutPriceKg = s.groutPriceKg
        cs.screedOn = s.screedOn
        cs.screedThickness = s.screedThickness
        cs.screedKgMm = s.screedKgMm
        cs.screedBagKg = s.screedBagKg
        cs.screedPrice = s.screedPrice
        cs.primerOn = s.primerOn
        cs.primerKg = s.primerKg
        cs.primerLayers = s.primerLayers
        cs.primerPackKg = s.primerPackKg
        cs.primerPrice = s.primerPrice
        cs.waterproofOn = s.waterproofOn
        cs.wpKg = s.wpKg
        cs.wpLayers = s.wpLayers
        cs.wpPackKg = s.wpPackKg
        cs.wpPrice = s.wpPrice
        cs.crossesPerTile = s.crossesPerTile
        cs.clipsPerTile = s.clipsPerTile
        cs.dailyOutputM2 = s.dailyOutputM2
        cs.priceCross = s.priceCross
        cs.priceClip = s.priceClip
        cs.priceWedge = s.priceWedge
        cs.trimOn = s.trimOn
        cs.trimPieceM = s.trimPieceM
        cs.trimReserve = s.trimReserve
        cs.trimPrice = s.trimPrice
        cs.workPriceM2 = s.workPriceM2
        cs.tripCapacity = s.tripCapacity
    }

    // ------------------------------------------------------------------ JSON

    fun cadToJson(st: CadEditorState): String = cadAdapter.toJson(capture(st))
    fun calcToJson(cs: CalculatorState): String = calcAdapter.toJson(capture(cs))

    /** Повреждённая или устаревшая запись не должна ронять запуск — просто игнорируем её. */
    fun applyCadJson(st: CadEditorState, json: String?): Boolean {
        val snap = json?.let { runCatching { cadAdapter.fromJson(it) }.getOrNull() } ?: return false
        restore(st, snap)
        return true
    }

    fun applyCalcJson(cs: CalculatorState, json: String?): Boolean {
        val snap = json?.let { runCatching { calcAdapter.fromJson(it) }.getOrNull() } ?: return false
        restore(cs, snap)
        return true
    }
}
