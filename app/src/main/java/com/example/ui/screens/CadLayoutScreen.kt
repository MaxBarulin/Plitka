package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TileViewModel
import com.example.ui.cad.*
import kotlin.math.max
import kotlin.math.min

private val LINEAR_STEPS = listOf(1.0, 5.0, 10.0, 25.0, 50.0, 100.0, 500.0)
private val ANGLE_STEPS = listOf(0.1, 0.5, 1.0, 5.0, 15.0, 45.0, 90.0)

@Composable
fun CadLayoutScreen(
    viewModel: TileViewModel,
    modifier: Modifier = Modifier
) {
    // Всё состояние чертежа лежит во ViewModel — переключение вкладок его не сбрасывает.
    val st = viewModel.cadState
    val calc = viewModel.calcState

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showStats by remember { mutableStateOf(false) }
    var showRectDialog by remember { mutableStateOf(false) }

    fun fitView() {
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        val b = boundsOf(st.vertices)
        val w = max(b.width, 500.0)
        val h = max(b.height, 500.0)
        val sx = canvasSize.width / (w * 1.25)
        val sy = canvasSize.height / (h * 1.25)
        st.pxPerMm = min(sx, sy).toFloat().coerceIn(0.005f, 6f)
        st.viewCX = b.cx
        st.viewCY = b.cy
    }

    LaunchedEffect(canvasSize) {
        if (!st.didFit && canvasSize.width > 0) { fitView(); st.didFit = true }
    }

    val spec = st.tileSpec
    val layout = remember(st.vertices, st.obstacles, spec, st.showTiles) {
        if (st.showTiles) generateLayout(st.vertices, st.obstacles, spec)
        else TileLayout(emptyList(), LayoutStats(0, 0, 0, polygonAreaMm2(st.vertices), 0.0, emptyList(), false))
    }
    val areaMm2 = st.areaM2 * 1_000_000.0
    val perimMm = st.perimeterM * 1000.0

    val textMeasurer = rememberTextMeasurer()

    // =================================================================================
    Column(modifier = modifier.fillMaxSize()) {

        // ------------------------------------------------------------------ Шапка
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "S = ${fmtArea(areaMm2)} м²   P = ${fmtNum(st.perimeterM, 2)} м   " +
                                "углов: ${st.vertices.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (st.mode == CadMode.ROOM)
                                "Шаг ${fmtNum(st.linStep, 1)} мм / ${fmtNum(st.angStep, 1)}°"
                            else
                                "${fmtNum(st.tileW, 0)}×${fmtNum(st.tileH, 0)} мм, шов ${fmtNum(st.grout, 1)} мм, " +
                                    "${fmtNum(st.tileRotation, 1)}°",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ToolIconButton(Icons.Default.Undo, "Отменить", { st.undo() }, enabled = st.undoStack.isNotEmpty())
                    Spacer(Modifier.width(4.dp))
                    ToolIconButton(Icons.Default.CenterFocusStrong, "Вписать в экран", { fitView() })
                    Spacer(Modifier.width(4.dp))
                    ToolIconButton(Icons.Default.Assessment, "Спецификация", { showStats = true })
                }

                Spacer(Modifier.height(6.dp))

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        onClick = { st.mode = CadMode.ROOM },
                        selected = st.mode == CadMode.ROOM
                    ) { Text("1. Контур", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        onClick = { st.mode = CadMode.TILE },
                        selected = st.mode == CadMode.TILE
                    ) { Text("2. Раскладка", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // ------------------------------------------------------------------ Канва
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CadColors.Background)
                .onSizeChanged { canvasSize = it }
        ) {
            val transform = CadTransform(
                centerPx = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                viewCenterX = st.viewCX,
                viewCenterY = st.viewCY,
                pxPerMm = st.pxPerMm
            )
            // Жесты читают состояние напрямую: пересоздавать детекторы на каждый
            // сдвиг вида нельзя — жест обрывался бы посреди движения.
            fun screenToWorldX(px: Float) = st.viewCX + (px - canvasSize.width / 2f) / st.pxPerMm
            fun screenToWorldY(py: Float) = st.viewCY + (py - canvasSize.height / 2f) / st.pxPerMm

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(st.dragMode) {
                        if (st.dragMode == DragMode.PAN) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (st.pxPerMm * zoom).coerceIn(0.004f, 8f)
                                st.pxPerMm = newScale
                                st.viewCX -= pan.x / newScale
                                st.viewCY -= pan.y / newScale
                            }
                        }
                    }
                    .pointerInput(st.dragMode, st.mode) {
                        if (st.dragMode == DragMode.EDIT) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                val dxMm = drag.x / st.pxPerMm
                                val dyMm = drag.y / st.pxPerMm
                                if (st.mode == CadMode.ROOM) {
                                    val vi = st.selVertex
                                    val oi = st.selObstacle
                                    if (vi != null && vi in st.vertices.indices) {
                                        val cur = st.vertices[vi]
                                        val nx = st.snapVal(cur.x + dxMm)
                                        val ny = st.snapVal(cur.y + dyMm)
                                        st.vertices = setVertexPosition(st.vertices, st.walls, vi, nx, ny)
                                    } else if (oi != null) {
                                        st.obstacles = st.obstacles.map {
                                            if (it.id == oi) it.copy(x = st.snapVal(it.x + dxMm), y = st.snapVal(it.y + dyMm)) else it
                                        }
                                    }
                                } else {
                                    if (st.originMode == OriginMode.POINT) {
                                        st.pointX = st.snapVal(st.pointX + dxMm)
                                        st.pointY = st.snapVal(st.pointY + dyMm)
                                    } else {
                                        st.originOffX = st.snapVal(st.originOffX + dxMm)
                                        st.originOffY = st.snapVal(st.originOffY + dyMm)
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(st.mode) {
                        detectTapGestures { tap ->
                            val wx = screenToWorldX(tap.x)
                            val wy = screenToWorldY(tap.y)
                            val hitMm = 22f / st.pxPerMm

                            if (st.mode == CadMode.ROOM) {
                                var bestV: Int? = null
                                var bestD = hitMm.toDouble()
                                st.vertices.forEachIndexed { i, v ->
                                    val d = kotlin.math.hypot(v.x - wx, v.y - wy)
                                    if (d < bestD) { bestD = d; bestV = i }
                                }
                                if (bestV != null) {
                                    st.selVertex = bestV; st.selWall = null; st.selObstacle = null; st.roomTab = 0
                                    return@detectTapGestures
                                }
                                var bestW: Int? = null
                                var bestWD = hitMm.toDouble()
                                val n = st.vertices.size
                                for (i in 0 until n) {
                                    val a = st.vertices[i]
                                    val b = st.vertices[(i + 1) % n]
                                    val d = distToSegment(wx, wy, a.x, a.y, b.x, b.y)
                                    if (d < bestWD) { bestWD = d; bestW = i }
                                }
                                if (bestW != null) {
                                    st.selWall = bestW; st.selVertex = null; st.selObstacle = null; st.roomTab = 1
                                    return@detectTapGestures
                                }
                                val obs = st.obstacles.firstOrNull {
                                    pointInPolygon(wx, wy, obstacleCorners(it))
                                }
                                if (obs != null) {
                                    st.selObstacle = obs.id; st.selVertex = null; st.selWall = null; st.roomTab = 2
                                } else {
                                    st.selVertex = null; st.selWall = null; st.selObstacle = null
                                }
                            } else {
                                if (st.originMode == OriginMode.POINT) {
                                    st.pointX = st.snapVal(wx); st.pointY = st.snapVal(wy)
                                }
                            }
                        }
                    }
            ) {
                drawCadGrid(transform, minorMm = 100.0, majorMm = 1000.0)

                if (st.vertices.size >= 3) {
                    val rp = roomPath(st.vertices, transform)
                    drawPath(rp, CadColors.Floor)
                    if (st.showTiles) {
                        clipPath(rp) { drawTiles(layout, spec, transform, st.highlightCuts) }
                    }
                    clipPath(rp) { drawObstacles(st.obstacles, st.selObstacle, transform) }
                }

                drawWalls(
                    vertices = st.vertices,
                    walls = st.walls,
                    selectedWall = st.selWall,
                    selectedVertex = st.selVertex,
                    t = transform,
                    tm = textMeasurer,
                    showLabels = st.showLabels,
                    showAngles = st.showAngles && st.mode == CadMode.ROOM,
                    editMode = st.mode == CadMode.ROOM
                )

                if (st.mode == CadMode.TILE) {
                    drawOriginMarker(st.originPoint.x, st.originPoint.y, spec.effectiveRotation, transform)
                }

                drawScaleBar(transform, textMeasurer)
            }

            // Панель навигации поверх канвы
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { st.dragMode = if (st.dragMode == DragMode.PAN) DragMode.EDIT else DragMode.PAN },
                    containerColor = if (st.dragMode == DragMode.EDIT)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        if (st.dragMode == DragMode.EDIT) Icons.Default.OpenWith else Icons.Default.PanTool,
                        contentDescription = if (st.dragMode == DragMode.EDIT) "Режим правки" else "Режим навигации",
                        modifier = Modifier.size(18.dp)
                    )
                }
                SmallFloatingActionButton(
                    onClick = { st.pxPerMm = (st.pxPerMm * 1.3f).coerceAtMost(8f) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { Icon(Icons.Default.Add, "Приблизить", modifier = Modifier.size(18.dp)) }
                SmallFloatingActionButton(
                    onClick = { st.pxPerMm = (st.pxPerMm / 1.3f).coerceAtLeast(0.004f) },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { Icon(Icons.Default.Remove, "Отдалить", modifier = Modifier.size(18.dp)) }
                SmallFloatingActionButton(
                    onClick = { st.panelExpanded = !st.panelExpanded },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        if (st.panelExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        "Свернуть панель",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = when {
                        st.dragMode == DragMode.PAN -> "Навигация: тяните карту, щипок — масштаб"
                        st.mode == CadMode.ROOM -> "Правка: тапните угол/стену, тяните для сдвига"
                        else -> "Правка: тяните — двигать старт раскладки"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        // ------------------------------------------------------------------ Панель управления
        if (st.panelExpanded) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp)
            ) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    if (st.mode == CadMode.ROOM) {
                        RoomTabs(st.roomTab) { st.roomTab = it }
                        Spacer(Modifier.height(8.dp))
                        when (st.roomTab) {
                            0 -> VertexPanel(
                                vertices = st.vertices,
                                selected = st.selVertex,
                                linStep = st.linStep,
                                onStepChange = { st.linStep = it },
                                snap = st.snapToStep,
                                onSnapChange = { st.snapToStep = it },
                                onSelect = { st.selVertex = it },
                                onBefore = { st.snapshot() },
                                onVerticesChange = { st.vertices = it },
                                walls = st.walls
                            )
                            1 -> WallPanel(
                                vertices = st.vertices,
                                walls = st.walls,
                                selected = st.selWall,
                                linStep = st.linStep,
                                angStep = st.angStep,
                                onLinStep = { st.linStep = it },
                                onAngStep = { st.angStep = it },
                                onSelect = { st.selWall = it },
                                onBefore = { st.snapshot() },
                                onVerticesChange = { st.vertices = it },
                                onWallsChange = { st.walls = it }
                            )
                            2 -> ObstaclePanel(
                                obstacles = st.obstacles,
                                selectedId = st.selObstacle,
                                linStep = st.linStep,
                                angStep = st.angStep,
                                onSelect = { st.selObstacle = it },
                                onBefore = { st.snapshot() },
                                onChange = { st.obstacles = it },
                                defaultPos = { val b = boundsOf(st.vertices); P2(b.left + 200, b.top + 200) }
                            )
                            else -> ShapePanel(
                                onBefore = { st.snapshot() },
                                onRect = { showRectDialog = true },
                                onOrtho = { st.vertices = orthogonalize(st.vertices) },
                                onClearLocks = { st.walls = st.walls.mapValues { it.value.copy(lengthLocked = false) } },
                                showLabels = st.showLabels,
                                onShowLabels = { st.showLabels = it },
                                showAngles = st.showAngles,
                                onShowAngles = { st.showAngles = it }
                            )
                        }
                    } else {
                        TileTabs(st.tileTab) { st.tileTab = it }
                        Spacer(Modifier.height(8.dp))
                        when (st.tileTab) {
                            0 -> TileParamPanel(
                                tileW = st.tileW, onTileW = { st.tileW = it },
                                tileH = st.tileH, onTileH = { st.tileH = it },
                                grout = st.grout, onGrout = { st.grout = it },
                                pattern = st.pattern, onPattern = { st.pattern = it },
                                offsetPercent = st.offsetPercent, onOffsetPercent = { st.offsetPercent = it },
                                rotation = st.tileRotation, onRotation = { st.tileRotation = it },
                                linStep = st.linStep, onLinStep = { st.linStep = it },
                                angStep = st.angStep, onAngStep = { st.angStep = it }
                            )
                            1 -> OriginPanel(
                                vertices = st.vertices,
                                originMode = st.originMode, onOriginMode = { st.originMode = it },
                                corner = st.originCorner, onCorner = { st.originCorner = it },
                                offX = st.originOffX, onOffX = { st.originOffX = it },
                                offY = st.originOffY, onOffY = { st.originOffY = it },
                                pointX = st.pointX, onPointX = { st.pointX = it },
                                pointY = st.pointY, onPointY = { st.pointY = it },
                                linStep = st.linStep, onLinStep = { st.linStep = it },
                                resolved = st.originPoint
                            )
                            else -> AnalysisPanel(
                                layout = layout,
                                spec = spec,
                                areaMm2 = areaMm2,
                                perimeterM = st.perimeterM,
                                showTiles = st.showTiles, onShowTiles = { st.showTiles = it },
                                highlightCuts = st.highlightCuts, onHighlightCuts = { st.highlightCuts = it },
                                syncWithCalculator = calc.syncWithCad,
                                onSyncWithCalculator = { calc.syncWithCad = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Диалоги
    if (showStats) {
        StatsDialog(
            onDismiss = { showStats = false },
            vertices = st.vertices,
            walls = st.walls,
            areaMm2 = areaMm2,
            perimMm = perimMm,
            layout = layout,
            spec = spec
        )
    }

    if (showRectDialog) {
        RectRoomDialog(
            onDismiss = { showRectDialog = false },
            onApply = { w, h ->
                st.snapshot()
                st.vertices = rectangleRoom(w, h)
                st.walls = emptyMap()
                st.selVertex = null; st.selWall = null
                showRectDialog = false
                fitView()
            }
        )
    }
}


// =====================================================================================
// ВКЛАДКИ
// =====================================================================================

@Composable
private fun RoomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val titles = listOf("Углы", "Стены", "Короба", "Форма")
    TabRowCompact(titles, selected, onSelect)
}

@Composable
private fun TileTabs(selected: Int, onSelect: (Int) -> Unit) {
    val titles = listOf("Плитка", "Старт", "Анализ")
    TabRowCompact(titles, selected, onSelect)
}

@Composable
private fun TabRowCompact(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        titles.forEachIndexed { i, title ->
            FilterChip(
                selected = selected == i,
                onClick = { onSelect(i) },
                label = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }
    }
}

// =====================================================================================
// ПАНЕЛЬ УГЛОВ
// =====================================================================================

@Composable
private fun VertexPanel(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    selected: Int?,
    linStep: Double,
    onStepChange: (Double) -> Unit,
    snap: Boolean,
    onSnapChange: (Boolean) -> Unit,
    onSelect: (Int?) -> Unit,
    onBefore: () -> Unit,
    onVerticesChange: (List<CadVertex>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StepChooser("Шаг перемещения", LINEAR_STEPS, linStep, onStepChange, "мм", 1)

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            vertices.indices.forEach { i ->
                FilterChip(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    label = { Text("№${i + 1}", fontSize = 11.sp) }
                )
            }
        }

        val idx = selected
        if (idx == null || idx !in vertices.indices) {
            Text(
                "Выберите угол на плане или чипом выше. Координаты можно ввести вручную " +
                    "или добрать стрелками с выбранным шагом.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val v = vertices[idx]
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberStepperField(
                        label = "X угла №${idx + 1}",
                        value = v.x,
                        onValueChange = { onVerticesChange(setVertexPosition(vertices, walls, idx, it, v.y)) },
                        step = linStep, suffix = "мм", decimals = 1
                    )
                    NumberStepperField(
                        label = "Y угла №${idx + 1}",
                        value = v.y,
                        onValueChange = { onVerticesChange(setVertexPosition(vertices, walls, idx, v.x, it)) },
                        step = linStep, suffix = "мм", decimals = 1
                    )
                }
                DPad(
                    step = linStep,
                    unit = "мм",
                    decimals = 1,
                    onMove = { dx, dy -> onVerticesChange(nudgeVertex(vertices, walls, idx, dx, dy)) }
                )
            }

            InfoRow("Внутренний угол", "${fmtDeg(interiorAngleDeg(vertices, idx))}°")

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = snap,
                    onClick = { onSnapChange(!snap) },
                    label = { Text("Привязка к шагу", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = v.pinned,
                    onClick = {
                        onBefore()
                        onVerticesChange(vertices.toMutableList().also { it[idx] = v.copy(pinned = !v.pinned) })
                    },
                    label = { Text(if (v.pinned) "Закреплён" else "Закрепить", fontSize = 11.sp) }
                )
                AssistChip(
                    onClick = {
                        onBefore()
                        onVerticesChange(splitWall(vertices, idx))
                    },
                    label = { Text("+ угол после", fontSize = 11.sp) }
                )
                if (vertices.size > 3) {
                    AssistChip(
                        onClick = {
                            onBefore()
                            onVerticesChange(vertices.toMutableList().also { it.removeAt(idx) })
                            onSelect(null)
                        },
                        label = { Text("Удалить", fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

// =====================================================================================
// ПАНЕЛЬ СТЕН
// =====================================================================================

@Composable
private fun WallPanel(
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    selected: Int?,
    linStep: Double,
    angStep: Double,
    onLinStep: (Double) -> Unit,
    onAngStep: (Double) -> Unit,
    onSelect: (Int?) -> Unit,
    onBefore: () -> Unit,
    onVerticesChange: (List<CadVertex>) -> Unit,
    onWallsChange: (Map<String, WallProps>) -> Unit
) {
    var anchor by remember { mutableStateOf(WallAnchor.START) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            vertices.indices.forEach { i ->
                val locked = walls[vertices[i].id]?.lengthLocked == true
                FilterChip(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    label = { Text(if (locked) "С${i + 1} 🔒" else "С${i + 1}", fontSize = 11.sp) }
                )
            }
        }

        val idx = selected
        if (idx == null || idx !in vertices.indices || vertices.size < 2) {
            Text(
                "Выберите стену на плане. Можно задать точную длину, азимут, " +
                    "зафиксировать длину замком — тогда при движении соседних углов она не изменится.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val a = vertices[idx]
        val b = vertices[(idx + 1) % vertices.size]
        val len = distMm(a, b)
        val az = azimuthDeg(a, b)
        val props = walls[a.id] ?: WallProps()

        Text(
            "Стена №${idx + 1}: угол ${idx + 1} → угол ${(idx + 1) % vertices.size + 1}",
            fontSize = 12.sp, fontWeight = FontWeight.Bold
        )

        StepChooser("Шаг длины", LINEAR_STEPS, linStep, onLinStep, "мм", 1)

        NumberStepperField(
            label = "Длина стены",
            value = len,
            onValueChange = { newLen ->
                onVerticesChange(setWallLength(vertices, walls, idx, newLen, anchor))
                if (props.lengthLocked) {
                    onWallsChange(walls + (a.id to props.copy(lockedLengthMm = newLen)))
                }
            },
            step = linStep, suffix = "мм", decimals = 1, min = 1.0, max = 100_000.0
        )

        Text("Что двигать при изменении длины:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(0, 3),
                onClick = { anchor = WallAnchor.START },
                selected = anchor == WallAnchor.START
            ) { Text("конец", fontSize = 11.sp) }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(1, 3),
                onClick = { anchor = WallAnchor.END },
                selected = anchor == WallAnchor.END
            ) { Text("начало", fontSize = 11.sp) }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(2, 3),
                onClick = { anchor = WallAnchor.CENTER },
                selected = anchor == WallAnchor.CENTER
            ) { Text("оба", fontSize = 11.sp) }
        }

        StepChooser("Шаг угла", ANGLE_STEPS, angStep, onAngStep, "°", 1)

        NumberStepperField(
            label = "Азимут стены (0°→вправо, 90°→вверх)",
            value = az,
            onValueChange = {
                onVerticesChange(
                    setWallAzimuth(
                        vertices, walls, idx, normDeg(it),
                        if (anchor == WallAnchor.END) WallAnchor.END else WallAnchor.START
                    )
                )
            },
            step = angStep, suffix = "°", decimals = 1, min = -720.0, max = 1080.0
        )

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = props.lengthLocked,
                onClick = {
                    onBefore()
                    onWallsChange(
                        walls + (a.id to props.copy(
                            lengthLocked = !props.lengthLocked,
                            lockedLengthMm = len
                        ))
                    )
                },
                label = { Text(if (props.lengthLocked) "Длина закреплена 🔒" else "Закрепить длину", fontSize = 11.sp) }
            )
            FilterChip(
                selected = props.excluded,
                onClick = {
                    onBefore()
                    onWallsChange(walls + (a.id to props.copy(excluded = !props.excluded)))
                },
                label = { Text("Проём (не считать)", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onBefore(); onVerticesChange(splitWall(vertices, idx)) },
                label = { Text("Разделить", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = {
                    onBefore()
                    val snapped = Math.round(az / 90.0) * 90.0
                    onVerticesChange(setWallAzimuth(vertices, walls, idx, snapped, WallAnchor.START))
                },
                label = { Text("Выровнять 90°", fontSize = 11.sp) }
            )
        }

        // Полярный ввод новой стены
        PolarAppend(
            onAppend = { l, angle ->
                onBefore()
                onVerticesChange(appendPolarVertex(vertices, idx, l, angle))
            },
            defaultAzimuth = az
        )
    }
}

@Composable
private fun PolarAppend(onAppend: (Double, Double) -> Unit, defaultAzimuth: Double) {
    var l by remember { mutableStateOf(1000.0) }
    var ang by remember { mutableStateOf(defaultAzimuth) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text(
            "Добавить угол полярно (от начала выбранной стены)",
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStepperField("Длина", l, { l = it }, 10.0, Modifier.weight(1f), "мм", 0, 1.0, 100_000.0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberStepperField("Азимут", ang, { ang = it }, 1.0, Modifier.weight(1f), "°", 1, -720.0, 1080.0)
        }
        Button(onClick = { onAppend(l, ang) }, modifier = Modifier.fillMaxWidth()) {
            Text("Отложить отрезок", fontSize = 12.sp)
        }
    }
}

// =====================================================================================
// ПАНЕЛЬ КОРОБОВ
// =====================================================================================

@Composable
private fun ObstaclePanel(
    obstacles: List<CadObstacle>,
    selectedId: String?,
    linStep: Double,
    angStep: Double,
    onSelect: (String?) -> Unit,
    onBefore: () -> Unit,
    onChange: (List<CadObstacle>) -> Unit,
    defaultPos: () -> P2
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            obstacles.forEachIndexed { i, o ->
                FilterChip(
                    selected = selectedId == o.id,
                    onClick = { onSelect(o.id) },
                    label = { Text("${o.name} ${i + 1}", fontSize = 11.sp) }
                )
            }
            AssistChip(
                onClick = {
                    onBefore()
                    val p = defaultPos()
                    val n = CadObstacle(x = p.x, y = p.y, w = 600.0, h = 400.0)
                    onChange(obstacles + n)
                    onSelect(n.id)
                },
                label = { Text("+ короб", fontSize = 11.sp) }
            )
        }

        val o = obstacles.firstOrNull { it.id == selectedId }
        if (o == null) {
            Text(
                "Короба, колонны, инсталляции и ниши вычитаются из площади и учитываются в подрезке.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        fun upd(f: (CadObstacle) -> CadObstacle) = onChange(obstacles.map { if (it.id == o.id) f(it) else it })

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NumberStepperField("X", o.x, { v -> upd { it.copy(x = v) } }, linStep, suffix = "мм", decimals = 1)
                NumberStepperField("Y", o.y, { v -> upd { it.copy(y = v) } }, linStep, suffix = "мм", decimals = 1)
            }
            DPad(linStep, "мм", { dx, dy -> upd { it.copy(x = it.x + dx, y = it.y + dy) } }, decimals = 1)
        }
        NumberStepperField("Ширина", o.w, { v -> upd { it.copy(w = v) } }, linStep, suffix = "мм", decimals = 1, min = 1.0)
        NumberStepperField("Глубина", o.h, { v -> upd { it.copy(h = v) } }, linStep, suffix = "мм", decimals = 1, min = 1.0)
        NumberStepperField("Поворот", o.rotationDeg, { v -> upd { it.copy(rotationDeg = v) } }, angStep, suffix = "°", decimals = 1, min = -360.0, max = 360.0)

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = o.subtract,
                onClick = { upd { it.copy(subtract = !it.subtract) } },
                label = { Text("Вычитать из площади", fontSize = 11.sp) }
            )
            AssistChip(
                onClick = { onBefore(); onChange(obstacles.filter { it.id != o.id }); onSelect(null) },
                label = { Text("Удалить", fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error)
            )
        }
    }
}

// =====================================================================================
// ПАНЕЛЬ ФОРМЫ
// =====================================================================================

@Composable
private fun ShapePanel(
    onBefore: () -> Unit,
    onRect: () -> Unit,
    onOrtho: () -> Unit,
    onClearLocks: () -> Unit,
    showLabels: Boolean,
    onShowLabels: (Boolean) -> Unit,
    showAngles: Boolean,
    onShowAngles: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRect, modifier = Modifier.fillMaxWidth()) {
            Text("Задать прямоугольник по размерам", fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onBefore(); onOrtho() }, modifier = Modifier.weight(1f)) {
                Text("Выпрямить по 90°", fontSize = 11.sp)
            }
            OutlinedButton(onClick = { onBefore(); onClearLocks() }, modifier = Modifier.weight(1f)) {
                Text("Снять все замки", fontSize = 11.sp)
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(showLabels, { onShowLabels(!showLabels) }, { Text("Размеры", fontSize = 11.sp) })
            FilterChip(showAngles, { onShowAngles(!showAngles) }, { Text("Углы", fontSize = 11.sp) })
        }
        Text(
            "Замок на стене (вкладка «Стены») сохраняет её длину: при перемещении соседних углов " +
                "решатель связей возвращает стене заданный размер. Закреплённый угол (вкладка «Углы») " +
                "остаётся на месте.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================================================
// ПАНЕЛЬ ПАРАМЕТРОВ ПЛИТКИ
// =====================================================================================

@Composable
private fun TileParamPanel(
    tileW: Double, onTileW: (Double) -> Unit,
    tileH: Double, onTileH: (Double) -> Unit,
    grout: Double, onGrout: (Double) -> Unit,
    pattern: TilePattern, onPattern: (TilePattern) -> Unit,
    offsetPercent: Double, onOffsetPercent: (Double) -> Unit,
    rotation: Double, onRotation: (Double) -> Unit,
    linStep: Double, onLinStep: (Double) -> Unit,
    angStep: Double, onAngStep: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                200.0 to 200.0, 300.0 to 300.0, 300.0 to 600.0, 600.0 to 600.0,
                600.0 to 1200.0, 800.0 to 800.0, 1200.0 to 2780.0, 100.0 to 300.0
            ).forEach { (w, h) ->
                AssistChip(
                    onClick = { onTileW(w); onTileH(h) },
                    label = { Text("${w.toInt()}×${h.toInt()}", fontSize = 11.sp) }
                )
            }
        }

        NumberStepperField("Ширина плитки", tileW, onTileW, linStep, suffix = "мм", decimals = 1, min = 5.0, max = 4000.0)
        NumberStepperField("Длина плитки", tileH, onTileH, linStep, suffix = "мм", decimals = 1, min = 5.0, max = 4000.0)
        NumberStepperField("Ширина шва", grout, onGrout, 0.5, suffix = "мм", decimals = 1, min = 0.0, max = 30.0)

        HorizontalDivider()

        Text("Схема раскладки", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TilePattern.entries.forEach { p ->
                FilterChip(
                    selected = pattern == p,
                    onClick = { onPattern(p) },
                    label = { Text(tilePatternTitle(p), fontSize = 11.sp) }
                )
            }
        }

        if (pattern == TilePattern.OFFSET) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(50.0, 33.3, 25.0, 20.0).forEach { p ->
                    AssistChip(onClick = { onOffsetPercent(p) }, label = { Text("${fmtNum(p, 1)} %", fontSize = 11.sp) })
                }
            }
            NumberStepperField(
                "Смещение ряда", offsetPercent, onOffsetPercent, 1.0,
                suffix = "%", decimals = 1, min = 0.0, max = 90.0
            )
        }

        if (pattern == TilePattern.HERRINGBONE) {
            Text(
                "Ёлочка строится из пары «длинная + короткая» плитка. Классика — формат 2:1 " +
                    "(600×300, 1200×600), но раскладка корректно собирается и для других пропорций.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        StepChooser("Шаг угла поворота", ANGLE_STEPS, angStep, onAngStep, "°", 1)
        NumberStepperField(
            "Угол раскладки", rotation, onRotation, angStep,
            suffix = "°", decimals = 1, min = -360.0, max = 360.0
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.0, 15.0, 22.5, 30.0, 45.0, 60.0, 90.0).forEach { a ->
                AssistChip(onClick = { onRotation(a) }, label = { Text("${fmtNum(a, 1)}°", fontSize = 11.sp) })
            }
        }
        StepChooser("Шаг размеров", LINEAR_STEPS, linStep, onLinStep, "мм", 1)
    }
}

// =====================================================================================
// ПАНЕЛЬ ТОЧКИ СТАРТА
// =====================================================================================

@Composable
private fun OriginPanel(
    vertices: List<CadVertex>,
    originMode: OriginMode, onOriginMode: (OriginMode) -> Unit,
    corner: Int, onCorner: (Int) -> Unit,
    offX: Double, onOffX: (Double) -> Unit,
    offY: Double, onOffY: (Double) -> Unit,
    pointX: Double, onPointX: (Double) -> Unit,
    pointY: Double, onPointY: (Double) -> Unit,
    linStep: Double, onLinStep: (Double) -> Unit,
    resolved: P2
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(0, 3),
                onClick = { onOriginMode(OriginMode.CORNER) },
                selected = originMode == OriginMode.CORNER
            ) { Text("От угла", fontSize = 11.sp) }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(1, 3),
                onClick = { onOriginMode(OriginMode.POINT) },
                selected = originMode == OriginMode.POINT
            ) { Text("От точки", fontSize = 11.sp) }
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(2, 3),
                onClick = { onOriginMode(OriginMode.CENTER) },
                selected = originMode == OriginMode.CENTER
            ) { Text("От центра", fontSize = 11.sp) }
        }

        StepChooser("Шаг перемещения точки", LINEAR_STEPS, linStep, onLinStep, "мм", 1)

        when (originMode) {
            OriginMode.CORNER -> {
                Text("Какой угол — начало раскладки:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vertices.indices.forEach { i ->
                        FilterChip(
                            selected = corner == i,
                            onClick = { onCorner(i) },
                            label = { Text("№${i + 1}", fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Отступ по X", offX, onOffX, linStep, suffix = "мм", decimals = 1)
                        NumberStepperField("Отступ по Y", offY, onOffY, linStep, suffix = "мм", decimals = 1)
                    }
                    DPad(linStep, "мм", { dx, dy -> onOffX(offX + dx); onOffY(offY + dy) }, decimals = 1)
                }
            }
            OriginMode.CENTER -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("Смещение X", offX, onOffX, linStep, suffix = "мм", decimals = 1)
                        NumberStepperField("Смещение Y", offY, onOffY, linStep, suffix = "мм", decimals = 1)
                    }
                    DPad(linStep, "мм", { dx, dy -> onOffX(offX + dx); onOffY(offY + dy) }, decimals = 1)
                }
            }
            OriginMode.POINT -> {
                Text(
                    "Тапните по плану, чтобы поставить точку, затем доводите стрелками с выбранным шагом.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberStepperField("X точки", pointX, onPointX, linStep, suffix = "мм", decimals = 1)
                        NumberStepperField("Y точки", pointY, onPointY, linStep, suffix = "мм", decimals = 1)
                    }
                    DPad(linStep, "мм", { dx, dy -> onPointX(pointX + dx); onPointY(pointY + dy) }, decimals = 1)
                }
            }
        }

        InfoRow("Итоговая точка старта", "X ${fmtMm(resolved.x)} / Y ${fmtMm(resolved.y)} мм", highlight = true)
    }
}

// =====================================================================================
// ПАНЕЛЬ АНАЛИЗА
// =====================================================================================

@Composable
private fun AnalysisPanel(
    layout: TileLayout,
    spec: TileSpec,
    areaMm2: Double,
    perimeterM: Double,
    showTiles: Boolean, onShowTiles: (Boolean) -> Unit,
    highlightCuts: Boolean, onHighlightCuts: (Boolean) -> Unit,
    syncWithCalculator: Boolean,
    onSyncWithCalculator: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(showTiles, { onShowTiles(!showTiles) }, { Text("Показывать плитку", fontSize = 11.sp) })
            FilterChip(highlightCuts, { onHighlightCuts(!highlightCuts) }, { Text("Подсветить подрезку", fontSize = 11.sp) })
        }
        InfoRow("Площадь пола", "${fmtArea(areaMm2)} м²")
        InfoRow("Периметр", "${fmtNum(perimeterM, 2)} м")
        InfoRow("Плитка целиком", "${layout.stats.wholeCount} шт")
        InfoRow("Плитка в подрезку", "${layout.stats.cutCount} шт")
        InfoRow("Всего задействовано", "${layout.stats.totalCount} шт", highlight = true)
        val tileArea = spec.widthMm * spec.heightMm / 1_000_000.0
        InfoRow("Площадь по плиткам", "${fmtNum(layout.stats.totalCount * tileArea, 2)} м²")
        if (layout.stats.edgeCutsMm.isNotEmpty()) {
            InfoRow(
                "Подрезка у стен (мин)",
                "${fmtMm(layout.stats.minEdgeCutMm)} мм",
                highlight = layout.stats.minEdgeCutMm < min(spec.widthMm, spec.heightMm) / 3.0
            )
            if (layout.stats.minEdgeCutMm < min(spec.widthMm, spec.heightMm) / 3.0) {
                Text(
                    "Узкая подрезка (< 1/3 плитки) — сдвиньте старт раскладки, чтобы подрезка " +
                        "у противоположных стен была одинаковой и шире.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (layout.stats.truncated) {
            Text(
                "Показана часть раскладки: слишком много плиток для отрисовки. " +
                    "Увеличьте формат плитки для предпросмотра.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        if (syncWithCalculator) {
            Text(
                "Калькулятор связан с планом: площадь, периметр, формат плитки и шов " +
                    "уходят в смету автоматически, вручную ничего переносить не нужно.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                "Связь с калькулятором разорвана — там значения введены вручную.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSyncWithCalculator, modifier = Modifier.fillMaxWidth()) {
                Text("Снова брать данные из плана", fontSize = 12.sp)
            }
        }
    }
}

// =====================================================================================
// ДИАЛОГИ
// =====================================================================================

@Composable
private fun RectRoomDialog(onDismiss: () -> Unit, onApply: (Double, Double) -> Unit) {
    var w by remember { mutableStateOf(3400.0) }
    var h by remember { mutableStateOf(2200.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Прямоугольное помещение", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberStepperField("Ширина", w, { w = it }, 10.0, suffix = "мм", decimals = 0, min = 100.0, max = 50_000.0)
                NumberStepperField("Длина", h, { h = it }, 10.0, suffix = "мм", decimals = 0, min = 100.0, max = 50_000.0)
                Text("Площадь: ${fmtNum(w * h / 1_000_000.0, 2)} м²", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(onClick = { onApply(w, h) }) { Text("Построить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun StatsDialog(
    onDismiss: () -> Unit,
    vertices: List<CadVertex>,
    walls: Map<String, WallProps>,
    areaMm2: Double,
    perimMm: Double,
    layout: TileLayout,
    spec: TileSpec
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Спецификация плана", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow("Площадь пола", "${fmtArea(areaMm2)} м²", highlight = true)
                InfoRow("Периметр (без проёмов)", "${fmtNum(perimMm / 1000.0, 3)} м")
                InfoRow("Углов", "${vertices.size}")
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Стены", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                val n = vertices.size
                for (i in 0 until n) {
                    val a = vertices[i]
                    val b = vertices[(i + 1) % n]
                    val p = walls[a.id]
                    val marks = buildString {
                        if (p?.lengthLocked == true) append(" 🔒")
                        if (p?.excluded == true) append(" (проём)")
                    }
                    InfoRow("С${i + 1}$marks", "${fmtMm(distMm(a, b))} мм / ${fmtDeg(azimuthDeg(a, b))}°")
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Углы помещения", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                vertices.indices.forEach { i ->
                    InfoRow("Угол ${i + 1}", "${fmtDeg(interiorAngleDeg(vertices, i))}°")
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Раскладка", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                InfoRow("Формат", "${fmtMm(spec.widthMm)}×${fmtMm(spec.heightMm)} мм")
                InfoRow("Шов", "${fmtNum(spec.groutMm, 1)} мм")
                InfoRow("Схема", tilePatternTitle(spec.pattern))
                InfoRow("Угол", "${fmtDeg(spec.effectiveRotation)}°")
                InfoRow("Целых плиток", "${layout.stats.wholeCount} шт")
                InfoRow("В подрезку", "${layout.stats.cutCount} шт")
                InfoRow("Итого", "${layout.stats.totalCount} шт", highlight = true)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}
