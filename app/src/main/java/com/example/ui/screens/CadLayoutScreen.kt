package com.example.ui.screens

import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TileViewModel
import kotlin.math.*

enum class CadMode {
    ROOM_PERIMETER, // Editing walls, vertices, obstacles
    TILE_LAYOUT     // Positioning tile grid, rotating, analyzing cuts
}

enum class TilePatternType {
    STRAIGHT,       // Шов в шов
    OFFSET_HALF,    // Разбежка 50%
    OFFSET_THIRD,   // Разбежка 33%
    HERRINGBONE,    // Елочка
    DIAGONAL_45     // Диагональ 45°
}

data class RoomVertex(
    val id: String = java.util.UUID.randomUUID().toString(),
    var x: Float, // in meters
    var y: Float  // in meters
)

data class RoomObstacle(
    val id: String = java.util.UUID.randomUUID().toString(),
    var x: Float,      // top-left x in meters
    var y: Float,      // top-left y in meters
    var width: Float,  // width in meters
    var height: Float, // height in meters
    val name: String = "Короб/Колонна"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadLayoutScreen(
    viewModel: TileViewModel,
    modifier: Modifier = Modifier
) {
    var cadMode by remember { mutableStateOf(CadMode.ROOM_PERIMETER) }
    
    // Room polygon vertices (in meters)
    var vertices by remember {
        mutableStateOf(
            listOf(
                RoomVertex(x = 0.5f, y = 0.5f),
                RoomVertex(x = 3.5f, y = 0.5f),
                RoomVertex(x = 3.5f, y = 2.0f),
                RoomVertex(x = 2.3f, y = 2.0f),
                RoomVertex(x = 2.3f, y = 3.2f),
                RoomVertex(x = 0.5f, y = 3.2f)
            )
        )
    }

    // Box obstacles (e.g. duct, column, riser) in meters
    var obstacles by remember {
        mutableStateOf(
            listOf(
                RoomObstacle(x = 2.7f, y = 0.5f, width = 0.6f, height = 0.5f, name = "Венткороб")
            )
        )
    }

    // Selected vertex for manual coordinate editing or deletion
    var selectedVertexIndex by remember { mutableStateOf<Int?>(null) }
    var selectedObstacleId by remember { mutableStateOf<String?>(null) }

    // Tile parameters
    var tileWidthCm by remember { mutableStateOf(60f) }
    var tileHeightCm by remember { mutableStateOf(60f) }
    var groutWidthMm by remember { mutableStateOf(2.0f) }
    var tilePattern by remember { mutableStateOf(TilePatternType.STRAIGHT) }

    // Grid placement in meters and degrees
    var gridOriginX by remember { mutableStateOf(0.5f) } // meters
    var gridOriginY by remember { mutableStateOf(0.5f) } // meters
    var gridRotationDeg by remember { mutableStateOf(0f) }

    // Canvas viewport transformations (pan and zoom)
    var scale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Info sheet / bottom sheet
    var showStatsDialog by remember { mutableStateOf(false) }

    // Calculate room area and perimeter
    val roomAreaSqM = remember(vertices, obstacles) {
        calculatePolygonArea(vertices) - obstacles.sumOf { (it.width * it.height).toDouble() }.toFloat().coerceAtLeast(0f)
    }
    val roomPerimeterM = remember(vertices) {
        calculatePolygonPerimeter(vertices)
    }

    // Tile estimation count
    val tileAreaSqM = (tileWidthCm / 100f) * (tileHeightCm / 100f)
    val approxTileCount = if (tileAreaSqM > 0) ceil(roomAreaSqM / tileAreaSqM * 1.12f).toInt() else 0
    val totalGroutLinearMeters = if (tileAreaSqM > 0) {
        val totalTiles = roomAreaSqM / tileAreaSqM
        val tilePerimeter = 2 * (tileWidthCm + tileHeightCm) / 100f
        ((totalTiles * tilePerimeter) / 2f).roundToInt()
    } else 0

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CAD План & Раскладка",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "S = ${"%.2f".format(roomAreaSqM)} м²  •  P = ${"%.2f".format(roomPerimeterM)} м",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showStatsDialog = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = "Анализ подрезки",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Switcher Tab
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { cadMode = CadMode.ROOM_PERIMETER },
                            selected = cadMode == CadMode.ROOM_PERIMETER,
                            icon = {
                                Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        ) {
                            Text("1. Контур комнаты", fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { cadMode = CadMode.TILE_LAYOUT },
                            selected = cadMode == CadMode.TILE_LAYOUT,
                            icon = {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        ) {
                            Text("2. Сетка плитки", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cadMode == CadMode.ROOM_PERIMETER) {
                    CadRoomControls(
                        vertices = vertices,
                        onVerticesChange = { vertices = it },
                        selectedVertexIndex = selectedVertexIndex,
                        onSelectVertex = { selectedVertexIndex = it },
                        obstacles = obstacles,
                        onObstaclesChange = { obstacles = it }
                    )
                } else {
                    CadTileControls(
                        tileWidthCm = tileWidthCm,
                        onTileWidthChange = { tileWidthCm = it },
                        tileHeightCm = tileHeightCm,
                        onTileHeightChange = { tileHeightCm = it },
                        groutWidthMm = groutWidthMm,
                        onGroutWidthChange = { groutWidthMm = it },
                        tilePattern = tilePattern,
                        onTilePatternChange = { tilePattern = it },
                        gridRotationDeg = gridRotationDeg,
                        onGridRotationChange = { gridRotationDeg = it },
                        onAlignToCenter = {
                            val bounds = getPolygonBoundingBox(vertices)
                            gridOriginX = (bounds.left + bounds.right) / 2f
                            gridOriginY = (bounds.top + bounds.bottom) / 2f
                        },
                        onAlignToOrigin = {
                            if (vertices.isNotEmpty()) {
                                gridOriginX = vertices[0].x
                                gridOriginY = vertices[0].y
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E222A)) // High contrast dark CAD canvas
        ) {
            val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(0.5f, 4.0f)
                panOffset += panChange
            }

            // Primary CAD Drawing Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformableState)
                    .pointerInput(cadMode, vertices, obstacles, scale, panOffset) {
                        if (cadMode == CadMode.ROOM_PERIMETER) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                                    val baseMetersPerPx = 0.008f / scale
                                    val metersX = (tapOffset.x - canvasCenter.x - panOffset.x) * baseMetersPerPx + 2.0f
                                    val metersY = (tapOffset.y - canvasCenter.y - panOffset.y) * baseMetersPerPx + 2.0f

                                    // Check if tapped near any vertex to select it
                                    var hitIndex: Int? = null
                                    vertices.forEachIndexed { index, v ->
                                        val dist = sqrt((v.x - metersX).pow(2) + (v.y - metersY).pow(2))
                                        if (dist < 0.25f) {
                                            hitIndex = index
                                        }
                                    }
                                    selectedVertexIndex = hitIndex
                                }
                            )
                        }
                    }
                    .pointerInput(cadMode, selectedVertexIndex, vertices, scale, panOffset) {
                        if (cadMode == CadMode.ROOM_PERIMETER && selectedVertexIndex != null) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val baseMetersPerPx = 0.008f / scale
                                val index = selectedVertexIndex ?: return@detectDragGestures
                                if (index in vertices.indices) {
                                    val updated = vertices.toMutableList()
                                    val current = updated[index]
                                    val newX = (current.x + dragAmount.x * baseMetersPerPx).coerceIn(0f, 10f)
                                    val newY = (current.y + dragAmount.y * baseMetersPerPx).coerceIn(0f, 10f)
                                    // Snap to 5cm grid
                                    val snappedX = (round(newX * 20) / 20f)
                                    val snappedY = (round(newY * 20) / 20f)
                                    updated[index] = current.copy(x = snappedX, y = snappedY)
                                    vertices = updated
                                }
                            }
                        } else if (cadMode == CadMode.TILE_LAYOUT) {
                            // Drag tile grid origin
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val baseMetersPerPx = 0.008f / scale
                                gridOriginX += dragAmount.x * baseMetersPerPx
                                gridOriginY += dragAmount.y * baseMetersPerPx
                            }
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height
                val center = Offset(canvasW / 2f, canvasH / 2f)

                // Coordinate conversion: 1 meter = pxPerMeter pixels
                val basePxPerMeter = 125f * scale

                fun metersToCanvas(x: Float, y: Float): Offset {
                    val px = center.x + panOffset.x + (x - 2.0f) * basePxPerMeter
                    val py = center.y + panOffset.y + (y - 2.0f) * basePxPerMeter
                    return Offset(px, py)
                }

                // 1. Draw CAD Architectural Grid Background (10cm minor, 1m major)
                drawCadGrid(
                    canvasW = canvasW,
                    canvasH = canvasH,
                    center = center,
                    panOffset = panOffset,
                    basePxPerMeter = basePxPerMeter
                )

                if (vertices.size >= 3) {
                    // Create room boundary path
                    val roomPath = Path().apply {
                        val first = metersToCanvas(vertices[0].x, vertices[0].y)
                        moveTo(first.x, first.y)
                        for (i in 1 until vertices.size) {
                            val pt = metersToCanvas(vertices[i].x, vertices[i].y)
                            lineTo(pt.x, pt.y)
                        }
                        close()
                    }

                    // 2. Draw Tiling Grid inside Room Path (using clipPath)
                    clipPath(roomPath) {
                        // Background fill for floor
                        drawPath(
                            path = roomPath,
                            color = Color(0xFF2C313C)
                        )

                        // If in tile layout mode or previewing tiles
                        drawTileGridEngine(
                            vertices = vertices,
                            obstacles = obstacles,
                            tileWidthCm = tileWidthCm,
                            tileHeightCm = tileHeightCm,
                            groutWidthMm = groutWidthMm,
                            pattern = tilePattern,
                            originX = gridOriginX,
                            originY = gridOriginY,
                            rotationDeg = gridRotationDeg,
                            pxPerMeter = basePxPerMeter,
                            metersToCanvas = ::metersToCanvas
                        )

                        // Draw obstacles cutouts inside room
                        obstacles.forEach { obs ->
                            val tl = metersToCanvas(obs.x, obs.y)
                            val br = metersToCanvas(obs.x + obs.width, obs.y + obs.height)
                            val rect = Rect(tl.x, tl.y, br.x, br.y)

                            drawRect(
                                color = Color(0xFF14171D),
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width, rect.height)
                            )
                            drawRect(
                                color = Color(0xFFFFB4A2),
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width, rect.height),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // 3. Draw Room Perimeter Wall Lines and Dimension Markers
                    drawRoomWallsAndDimensions(
                        vertices = vertices,
                        selectedVertexIndex = selectedVertexIndex,
                        metersToCanvas = ::metersToCanvas,
                        pxPerMeter = basePxPerMeter,
                        isPerimeterMode = cadMode == CadMode.ROOM_PERIMETER
                    )

                    // 4. Draw Tile Origin Anchor indicator when in layout mode
                    if (cadMode == CadMode.TILE_LAYOUT) {
                        val originPt = metersToCanvas(gridOriginX, gridOriginY)
                        drawCircle(
                            color = Color(0xFFFF5722),
                            radius = 9.dp.toPx(),
                            center = originPt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = originPt
                        )
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(originPt.x - 14.dp.toPx(), originPt.y),
                            end = Offset(originPt.x + 14.dp.toPx(), originPt.y),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFFFF5722),
                            start = Offset(originPt.x, originPt.y - 14.dp.toPx()),
                            end = Offset(originPt.x, originPt.y + 14.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // Floating CAD Zoom / Reset Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scale = 1.0f
                        panOffset = Offset.Zero
                    },
                    modifier = Modifier.size(42.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "Сброс вида", modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { scale = (scale * 1.25f).coerceAtMost(4.0f) },
                    modifier = Modifier.size(42.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Приблизить", modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(42.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Отдалить", modifier = Modifier.size(20.dp))
                }
            }

            // Floating Hint Chip
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (cadMode == CadMode.ROOM_PERIMETER) Icons.Default.TouchApp else Icons.Default.OpenWith,
                        contentDescription = null,
                        tint = Color(0xFFFFDCD2),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (cadMode == CadMode.ROOM_PERIMETER)
                            "Тап по углу для перетаскивания"
                        else
                            "Тяните пальцем для сдвига швов",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Detailed Stats & Cut Tile Breakdown Dialog
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = {
                Text("Спецификация раскладки", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Площадь помещения: ${"%.2f".format(roomAreaSqM)} м²")
                    Text("• Периметр стен: ${"%.2f".format(roomPerimeterM)} п.м.")
                    Text("• Размер плитки: ${tileWidthCm.toInt()} x ${tileHeightCm.toInt()} см")
                    Text("• Ширина шва: $groutWidthMm мм")
                    Text("• Примерное кол-во плитки (+12% подрезка): $approxTileCount шт.")
                    Text("• Суммарная длина швов: ~$totalGroutLinearMeters п.м.")
                    Text(
                        "• Расход затирки: ~${"%.2f".format(totalGroutLinearMeters * groutWidthMm * 0.015f)} кг",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showStatsDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// CONTROL PANELS FOR BOTH MODES
// -------------------------------------------------------------

@Composable
fun CadRoomControls(
    vertices: List<RoomVertex>,
    onVerticesChange: (List<RoomVertex>) -> Unit,
    selectedVertexIndex: Int?,
    onSelectVertex: (Int?) -> Unit,
    obstacles: List<RoomObstacle>,
    onObstaclesChange: (List<RoomObstacle>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Форма помещения",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Button to add a new vertex
            FilledTonalButton(
                onClick = {
                    if (vertices.size >= 3) {
                        val last = vertices.last()
                        val first = vertices.first()
                        val newV = RoomVertex(x = (last.x + first.x) / 2f + 0.3f, y = (last.y + first.y) / 2f + 0.3f)
                        onVerticesChange(vertices + newV)
                    }
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить угол", fontSize = 12.sp)
            }
        }

        // Room Shape Templates Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                PresetRoomChip("Прямоугольник (3х2м)") {
                    onVerticesChange(
                        listOf(
                            RoomVertex(x = 0.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 2.5f),
                            RoomVertex(x = 0.5f, y = 2.5f)
                        )
                    )
                }
            }
            item {
                PresetRoomChip("Г-образная (L)") {
                    onVerticesChange(
                        listOf(
                            RoomVertex(x = 0.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 2.0f),
                            RoomVertex(x = 2.0f, y = 2.0f),
                            RoomVertex(x = 2.0f, y = 3.5f),
                            RoomVertex(x = 0.5f, y = 3.5f)
                        )
                    )
                }
            }
            item {
                PresetRoomChip("Т-образная") {
                    onVerticesChange(
                        listOf(
                            RoomVertex(x = 0.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 0.5f),
                            RoomVertex(x = 3.5f, y = 1.5f),
                            RoomVertex(x = 2.5f, y = 1.5f),
                            RoomVertex(x = 2.5f, y = 3.2f),
                            RoomVertex(x = 1.5f, y = 3.2f),
                            RoomVertex(x = 1.5f, y = 1.5f),
                            RoomVertex(x = 0.5f, y = 1.5f)
                        )
                    )
                }
            }
            item {
                PresetRoomChip("+ Короб инсталляции") {
                    onObstaclesChange(
                        obstacles + RoomObstacle(
                            x = 0.5f,
                            y = 0.5f,
                            width = 0.8f,
                            height = 0.4f,
                            name = "Инсталляция"
                        )
                    )
                }
            }
        }

        // Selected Vertex Details / Delete
        if (selectedVertexIndex != null && selectedVertexIndex in vertices.indices) {
            val v = vertices[selectedVertexIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Угол #${selectedVertexIndex + 1}: X=${"%.2f".format(v.x)}м, Y=${"%.2f".format(v.y)}м",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                if (vertices.size > 3) {
                    IconButton(
                        onClick = {
                            val updated = vertices.toMutableList()
                            updated.removeAt(selectedVertexIndex)
                            onVerticesChange(updated)
                            onSelectVertex(null)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить угол", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun CadTileControls(
    tileWidthCm: Float,
    onTileWidthChange: (Float) -> Unit,
    tileHeightCm: Float,
    onTileHeightChange: (Float) -> Unit,
    groutWidthMm: Float,
    onGroutWidthChange: (Float) -> Unit,
    tilePattern: TilePatternType,
    onTilePatternChange: (TilePatternType) -> Unit,
    gridRotationDeg: Float,
    onGridRotationChange: (Float) -> Unit,
    onAlignToCenter: () -> Unit,
    onAlignToOrigin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tile Dimensions and Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Формат: ${tileWidthCm.toInt()}x${tileHeightCm.toInt()} см, шов $groutWidthMm мм",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = {
                        onTileWidthChange(60f)
                        onTileHeightChange(60f)
                    },
                    label = { Text("60x60") }
                )
                AssistChip(
                    onClick = {
                        onTileWidthChange(60f)
                        onTileHeightChange(120f)
                    },
                    label = { Text("60x120") }
                )
                AssistChip(
                    onClick = {
                        onTileWidthChange(30f)
                        onTileHeightChange(60f)
                    },
                    label = { Text("30x60") }
                )
            }
        }

        // Pattern selection
        ScrollableTabRow(
            selectedTabIndex = tilePattern.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            TilePatternType.values().forEach { pat ->
                val isSelected = tilePattern == pat
                Tab(
                    selected = isSelected,
                    onClick = { onTilePatternChange(pat) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    text = {
                        Text(
                            text = when (pat) {
                                TilePatternType.STRAIGHT -> "Прямая"
                                TilePatternType.OFFSET_HALF -> "Разбежка 50%"
                                TilePatternType.OFFSET_THIRD -> "Разбежка 33%"
                                TilePatternType.HERRINGBONE -> "Елочка"
                                TilePatternType.DIAGONAL_45 -> "Диагональ"
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        // Quick Alignment buttons and Rotation slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onAlignToCenter,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В центр", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onAlignToOrigin,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("От угла", fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Угол: ${gridRotationDeg.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { onGridRotationChange((gridRotationDeg + 45f) % 360f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Поворот +45°", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun PresetRoomChip(name: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp)
    )
}

// -------------------------------------------------------------
// CANVAS DRAWING ENGINES & MATHEMATICS
// -------------------------------------------------------------

fun DrawScope.drawCadGrid(
    canvasW: Float,
    canvasH: Float,
    center: Offset,
    panOffset: Offset,
    basePxPerMeter: Float
) {
    // 10cm grid lines (minor) and 1m grid lines (major)
    val minorPx = basePxPerMeter * 0.1f // 10cm
    val majorPx = basePxPerMeter * 1.0f // 1m

    // Start offsets relative to center
    val originScreenX = center.x + panOffset.x
    val originScreenY = center.y + panOffset.y

    // Minor lines
    val startX = (originScreenX % minorPx)
    val startY = (originScreenY % minorPx)

    var curX = startX
    while (curX < canvasW) {
        drawLine(
            color = Color(0xFF282D37),
            start = Offset(curX, 0f),
            end = Offset(curX, canvasH),
            strokeWidth = 1f
        )
        curX += minorPx
    }

    var curY = startY
    while (curY < canvasH) {
        drawLine(
            color = Color(0xFF282D37),
            start = Offset(0f, curY),
            end = Offset(canvasW, curY),
            strokeWidth = 1f
        )
        curY += minorPx
    }

    // Major 1-meter lines
    val majorStartX = (originScreenX % majorPx)
    val majorStartY = (originScreenY % majorPx)

    var mX = majorStartX
    while (mX < canvasW) {
        drawLine(
            color = Color(0xFF3B4252),
            start = Offset(mX, 0f),
            end = Offset(mX, canvasH),
            strokeWidth = 1.5f
        )
        mX += majorPx
    }

    var mY = majorStartY
    while (mY < canvasH) {
        drawLine(
            color = Color(0xFF3B4252),
            start = Offset(0f, mY),
            end = Offset(canvasW, mY),
            strokeWidth = 1.5f
        )
        mY += majorPx
    }
}

fun DrawScope.drawTileGridEngine(
    vertices: List<RoomVertex>,
    obstacles: List<RoomObstacle>,
    tileWidthCm: Float,
    tileHeightCm: Float,
    groutWidthMm: Float,
    pattern: TilePatternType,
    originX: Float,
    originY: Float,
    rotationDeg: Float,
    pxPerMeter: Float,
    metersToCanvas: (Float, Float) -> Offset
) {
    val bounds = getPolygonBoundingBox(vertices)
    val tileW_m = tileWidthCm / 100f
    val tileH_m = tileHeightCm / 100f
    val grout_m = groutWidthMm / 1000f

    val stepX = tileW_m + grout_m
    val stepY = tileH_m + grout_m

    // Expand bounding box to guarantee full coverage even after rotation
    val maxRadius = sqrt((bounds.right - bounds.left).pow(2) + (bounds.bottom - bounds.top).pow(2)) + 1.0f
    val originCenterCanvas = metersToCanvas(originX, originY)

    val tileFaceColor = Color(0xFFE5DDD5) // Realistic ceramic cream tone
    val groutLineColor = Color(0xFF5A4A42) // Dark grout line

    withTransform({
        rotate(rotationDeg, pivot = originCenterCanvas)
    }) {
        val numCols = (maxRadius * 2 / stepX).toInt() + 4
        val numRows = (maxRadius * 2 / stepY).toInt() + 4

        for (row in -numRows / 2..numRows / 2) {
            for (col in -numCols / 2..numCols / 2) {
                var localTileX = col * stepX
                val localTileY = row * stepY

                // Pattern Offset shifts
                when (pattern) {
                    TilePatternType.OFFSET_HALF -> {
                        localTileX += (abs(row) % 2) * 0.5f * stepX
                    }
                    TilePatternType.OFFSET_THIRD -> {
                        localTileX += (abs(row) % 3) * 0.333f * stepX
                    }
                    TilePatternType.DIAGONAL_45 -> {
                        // Handled by rotation or 45 offset
                    }
                    TilePatternType.HERRINGBONE -> {
                        // Staggered pattern
                        if (row % 2 == 0) {
                            localTileX += 0.25f * stepX
                        }
                    }
                    TilePatternType.STRAIGHT -> {}
                }

                val worldX = originX + localTileX
                val worldY = originY + localTileY

                val ptScreen = metersToCanvas(worldX, worldY)
                val widthPx = tileW_m * pxPerMeter
                val heightPx = tileH_m * pxPerMeter
                val groutPx = (grout_m * pxPerMeter).coerceAtLeast(1.5f)

                // 1. Draw Tile Face
                drawRect(
                    color = tileFaceColor,
                    topLeft = ptScreen,
                    size = Size(widthPx, heightPx)
                )

                // 2. Draw Grout Line (Stroke)
                drawRect(
                    color = groutLineColor,
                    topLeft = ptScreen,
                    size = Size(widthPx, heightPx),
                    style = Stroke(width = groutPx)
                )
            }
        }
    }
}

fun DrawScope.drawRoomWallsAndDimensions(
    vertices: List<RoomVertex>,
    selectedVertexIndex: Int?,
    metersToCanvas: (Float, Float) -> Offset,
    pxPerMeter: Float,
    isPerimeterMode: Boolean
) {
    val wallColor = Color(0xFFFFDCD2) // Bento peach wall accent
    val vertexColor = Color(0xFFFF5722)

    for (i in vertices.indices) {
        val nextIdx = (i + 1) % vertices.size
        val p1 = vertices[i]
        val p2 = vertices[nextIdx]

        val pt1 = metersToCanvas(p1.x, p1.y)
        val pt2 = metersToCanvas(p2.x, p2.y)

        // Draw Thick Wall line
        drawLine(
            color = wallColor,
            start = pt1,
            end = pt2,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Calculate wall segment length in meters
        val lengthMeters = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        val midPoint = Offset((pt1.x + pt2.x) / 2f, (pt1.y + pt2.y) / 2f)

        // Draw dimension background badge & tick
        drawCircle(
            color = Color.Black.copy(alpha = 0.75f),
            radius = 12.dp.toPx(),
            center = midPoint
        )
    }

    // Draw draggable vertex nodes
    if (isPerimeterMode) {
        vertices.forEachIndexed { index, v ->
            val pt = metersToCanvas(v.x, v.y)
            val isSelected = index == selectedVertexIndex

            drawCircle(
                color = if (isSelected) Color(0xFFFF9800) else vertexColor,
                radius = if (isSelected) 10.dp.toPx() else 7.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                center = pt
            )
        }
    }
}

// -------------------------------------------------------------
// GEOMETRY & POLYGON MATH HELPERS
// -------------------------------------------------------------

fun calculatePolygonArea(vertices: List<RoomVertex>): Float {
    if (vertices.size < 3) return 0f
    var area = 0.0
    for (i in vertices.indices) {
        val j = (i + 1) % vertices.size
        area += (vertices[i].x * vertices[j].y) - (vertices[j].x * vertices[i].y)
    }
    return abs(area / 2.0).toFloat()
}

fun calculatePolygonPerimeter(vertices: List<RoomVertex>): Float {
    if (vertices.size < 2) return 0f
    var perimeter = 0f
    for (i in vertices.indices) {
        val j = (i + 1) % vertices.size
        val dx = vertices[j].x - vertices[i].x
        val dy = vertices[j].y - vertices[i].y
        perimeter += sqrt(dx * dx + dy * dy)
    }
    return perimeter
}

data class PolygonBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

fun getPolygonBoundingBox(vertices: List<RoomVertex>): PolygonBounds {
    if (vertices.isEmpty()) return PolygonBounds(0f, 0f, 1f, 1f)
    var minX = vertices[0].x
    var maxX = vertices[0].x
    var minY = vertices[0].y
    var maxY = vertices[0].y

    for (v in vertices) {
        if (v.x < minX) minX = v.x
        if (v.x > maxX) maxX = v.x
        if (v.y < minY) minY = v.y
        if (v.y > maxY) maxY = v.y
    }
    return PolygonBounds(minX, minY, maxX, maxY)
}
