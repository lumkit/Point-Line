package io.github.lumkit.pline

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.lumkit.pline.graphics.*
import io.github.lumkit.pline.tool.ToolType
import io.github.lumkit.pline.util.Math
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
/**
 * 记忆化并可保存/恢复的绘制状态。
 * @return 绘制状态对象
 */
fun rememberDrawingState(): DrawingState = rememberSaveable(
    saver = DrawingState.Saver
) {
    DrawingState()
}

/**
 * 绘制状态，包含帧几何、交互参数、笔划集合与离屏缓存等。
 *
 * 约定：
 * - 所有笔划与点使用帧坐标（像素）；
 * - 视图交互（缩放/平移/旋转）仅影响显示与输入映射；
 * - 大画布采用分块离屏（Tiles）与合成（Composite）以优化性能。
 *
 * @property frame 画布帧参数（尺寸/背景色）
 * @property drawMode 工作模式（拖拽/绘制）
 * @property baseSize 基准尺寸，影响适配缩放
 * @property minScale 最小缩放
 * @property maxScale 最大缩放
 * @property brushColor 当前画笔颜色
 * @property brushWidth 当前画笔宽度（像素）
 * @property eraserWidth 橡皮擦宽度（像素）
 * @property brushPreset 画笔预设参数
 * @property strokes 已提交笔划集合
 * @property layers 图层标识集合
 * @property smoothLine 是否启用平滑路径
 * @property rotationRad 视图旋转（弧度）
 * @property rotationSensitivity 旋转灵敏度
 * @property panSensitivity 平移灵敏度
 * @property currentTool 当前工具
 * @property canUndo 是否可撤销
 * @property canRedo 是否可重做
 * @property commitMinSamples 提交笔划的最少采样点数
 * @property commitMinDistancePx 提交笔划的最小长度（像素）
 * @property drawTapSlopViewPx 绘制启动抖动阈值（视图像素）
 * @property backingBitmap 合成后的离屏位图（可为空）
 * @property tileSize 分块尺寸（像素）
 * @property tiles 分块列表
 * @property checkpointInterval 合成检查点间隔（笔划数）
 * @property maxCheckpoints 最大检查点数量
 */
@Stable
class DrawingState {
    var frame by mutableStateOf(Frame.Transparent)
    var drawMode by mutableStateOf(DrawMode.Draw)
    var baseSize by mutableStateOf(300f)
    var minScale by mutableStateOf(0.3f)
    var maxScale by mutableStateOf(150f)
    var brushColor by mutableStateOf(0xFF000000L)
    var brushWidth by mutableStateOf(16f)
    var eraserWidth by mutableStateOf(24f)
    var brushPreset: BrushPreset by mutableStateOf(
        BrushPreset(
            shape = BrushShapeType.Square,
            mode = StrokeModel.Path,
            spacing = 0f,
            opacity = 1f,
            velocitySpacingScale = 0f,
        )
    )

    internal var pendingStroke: Stroke? by mutableStateOf(null)
    var strokes by mutableStateOf(listOf<Stroke>())
    var layers by mutableStateOf(listOf<String>())
    // 缩放因子
    internal var scaleFactor by mutableStateOf(1f)
    // 偏移量
    internal var offset by mutableStateOf(Offset.Zero)
    var smoothLine: Boolean by mutableStateOf(true)
    var rotationRad: Float by mutableStateOf(0f)
    var rotationSensitivity: Float by mutableStateOf(1.0f)
    var panSensitivity: Float by mutableStateOf(1.0f)
    var currentTool: ToolType by mutableStateOf(ToolType.Brush)
    var canUndo: Boolean by mutableStateOf(false)
    var canRedo: Boolean by mutableStateOf(false)
    var commitMinSamples: Int by mutableStateOf(3)
    var commitMinDistancePx: Float by mutableStateOf(3f)
    var drawTapSlopViewPx: Float by mutableStateOf(6f)

    var backingBitmap: ImageBitmap? by mutableStateOf(null)
    private var backingCanvas: Canvas? = null
    data class Tile(
        val col: Int,
        val row: Int,
        val originX: Int,
        val originY: Int,
        val width: Int,
        val height: Int,
        val bitmap: ImageBitmap,
        val canvas: Canvas,
    )
    var tileSize: Int = 1024
    var tiles: List<Tile> by mutableStateOf(emptyList())
    private var tileCols: Int = 0
    private var tileRows: Int = 0
    private data class BackingCheckpoint(val strokeCount: Int, val bitmap: ImageBitmap)
    private val checkpoints = mutableListOf<BackingCheckpoint>()
    var checkpointInterval: Int = 50
    var maxCheckpoints: Int = 3

    private fun tileAt(col: Int, row: Int): Tile {
        return tiles[row * tileCols + col]
    }

    private fun ensureBacking() {
        val bmp = backingBitmap
        if (bmp == null || bmp.width != frame.width || bmp.height != frame.height) {
            val newBmp = ImageBitmap(frame.width, frame.height)
            backingBitmap = newBmp
            backingCanvas = Canvas(newBmp)
        }
    }

    private fun clearBacking() {
        ensureBacking()
        val newBmp = ImageBitmap(frame.width, frame.height)
        backingBitmap = newBmp
        backingCanvas = Canvas(newBmp)
    }

    private fun ensureTiles() {
        val cols = ceil(frame.width / tileSize.toDouble()).toInt().coerceAtLeast(1)
        val rows = ceil(frame.height / tileSize.toDouble()).toInt().coerceAtLeast(1)
        if (tiles.isNotEmpty() && cols == tileCols && rows == tileRows) return
        tileCols = cols
        tileRows = rows
        val list = mutableListOf<Tile>()
        var r = 0
        while (r < rows) {
            var c = 0
            while (c < cols) {
                val ox = c * tileSize
                val oy = r * tileSize
                val w = min(tileSize, frame.width - ox)
                val h = min(tileSize, frame.height - oy)
                val bmp = ImageBitmap(w, h)
                val cvs = Canvas(bmp)
                list.add(Tile(col = c, row = r, originX = ox, originY = oy, width = w, height = h, bitmap = bmp, canvas = cvs))
                c++
            }
            r++
        }
        tiles = list
    }

    private fun clearTiles() {
        ensureTiles()
        tiles.forEach { t ->
            val ds = CanvasDrawScope()
            ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = t.canvas, size = Size(t.width.toFloat(), t.height.toFloat())) {
                clipRect(0f, 0f, t.width.toFloat(), t.height.toFloat()) {
                    drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
                }
            }
        }
    }

    private fun snapshotBacking(): ImageBitmap {
        val newBmp = ImageBitmap(frame.width, frame.height)
        val canvas = Canvas(newBmp)
        val ds = CanvasDrawScope()
        ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = Size(frame.width.toFloat(), frame.height.toFloat())) {
            if (tiles.isNotEmpty()) {
                tiles.forEach { t ->
                    drawImage(t.bitmap, topLeft = Offset(t.originX.toFloat(), t.originY.toFloat()))
                }
            } else {
                ensureBacking()
                val src = backingBitmap ?: return@draw
                drawImage(src)
            }
        }
        return newBmp
    }

    private fun createCheckpointIfNeeded() {
        if (checkpointInterval <= 0) return
        val count = strokes.size
        if (count == 0) return
        if (count % checkpointInterval == 0) {
            snapshotBacking()?.let { bmp ->
                checkpoints.add(BackingCheckpoint(count, bmp))
                while (checkpoints.size > maxCheckpoints) checkpoints.removeAt(0)
            }
        }
    }

    @Serializable
    sealed class Operation {
        @Serializable
        data class AddStroke(val stroke: Stroke) : Operation()
        @Serializable
        data class RemoveStroke(val id: String, val backup: Stroke?) : Operation()
        @Serializable
        data class AddLayer(val id: String) : Operation()
        @Serializable
        data class RemoveLayer(val id: String) : Operation()
    }

    private val undoStack = mutableListOf<Operation>()
    private val redoStack = mutableListOf<Operation>()

    private fun apply(op: Operation) {
        when (op) {
            is Operation.AddStroke -> {
                strokes = strokes + op.stroke
            }
            is Operation.RemoveStroke -> {
                strokes = strokes.filterNot { it.id == op.id }
            }
            is Operation.AddLayer -> {
                if (!layers.contains(op.id)) layers = layers + op.id
            }
            is Operation.RemoveLayer -> {
                layers = layers.filterNot { it == op.id }
            }
        }
    }

    private fun revert(op: Operation) {
        when (op) {
            is Operation.AddStroke -> {
                strokes = strokes.filterNot { it.id == op.stroke.id }
            }
            is Operation.RemoveStroke -> {
                if (op.backup != null) strokes = strokes + op.backup
            }
            is Operation.AddLayer -> {
                layers = layers.filterNot { it == op.id }
            }
            is Operation.RemoveLayer -> {
                if (!layers.contains(op.id)) layers = layers + op.id
            }
        }
    }

    fun pushOp(op: Operation) {
        undoStack.add(op)
        redoStack.clear()
        canUndo = undoStack.isNotEmpty()
        canRedo = false
    }

    /** 撤销一次操作 */
    fun undo() {
        if (undoStack.isEmpty()) return
        val op = undoStack.removeAt(undoStack.lastIndex)
        revert(op)
        redoStack.add(op)
        canUndo = undoStack.isNotEmpty()
        canRedo = true
        val b = when (op) {
            is Operation.AddStroke -> strokeBounds(op.stroke)
            is Operation.RemoveStroke -> op.backup?.let { strokeBounds(it) }
            else -> null
        }
        rebuildBackingForBounds(b)
    }

    /** 重做一次操作 */
    fun redo() {
        if (redoStack.isEmpty()) return
        val op = redoStack.removeAt(redoStack.lastIndex)
        apply(op)
        undoStack.add(op)
        canUndo = true
        canRedo = redoStack.isNotEmpty()
        val b = when (op) {
            is Operation.AddStroke -> strokeBounds(op.stroke)
            is Operation.RemoveStroke -> op.backup?.let { strokeBounds(it) }
            else -> null
        }
        rebuildBackingForBounds(b)
    }

    /** 新增图层标识 */
    fun addLayer(id: String) {
        layers = layers + id
        pushOp(Operation.AddLayer(id))
    }

    /** 移除图层标识（存在时） */
    fun removeLayer(id: String) {
        val exists = layers.contains(id)
        if (!exists) return
        layers = layers.filterNot { it == id }
        pushOp(Operation.RemoveLayer(id))
    }

    /**
     * 按笔划 ID 擦除（支持撤销恢复）。
     */
    fun eraseStroke(id: String) {
        val backup = strokes.find { it.id == id }
        if (backup != null) {
            strokes = strokes.filterNot { it.id == id }
            pushOp(Operation.RemoveStroke(id, backup))
        }
    }

    /**
     * 几何擦除：将与橡皮路径接触的笔划拆分并移除接触段。
     * @param eraserPath 橡皮路径（帧坐标）
     * @param threshold 判定阈值（像素）
     */
    fun eraseGeometry(eraserPath: List<Point>, threshold: Float = 12f) {
        val ops = mutableListOf<Operation>()
        if (eraserPath.size < 2) return
        val exs = eraserPath.map { it.x.toFloat() }
        val eys = eraserPath.map { it.y.toFloat() }
        val eb = Rect(
            (exs.minOrNull() ?: 0f) - threshold,
            (eys.minOrNull() ?: 0f) - threshold,
            (exs.maxOrNull() ?: 0f) + threshold,
            (eys.maxOrNull() ?: 0f) + threshold,
        )

        strokes.forEach { s ->
            val pts = s.points
            if (pts.size < 2) return@forEach
            // Fast reject by bbox
            val sb = strokeBounds(s)
            if (sb.right < eb.left || sb.left > eb.right || sb.bottom < eb.top || sb.top > eb.bottom) return@forEach

            val touched = BooleanArray(pts.size)
            for (i in 0 until pts.size) {
                val p = pts[i]
                var hit = false
                var j = 1
                while (j < eraserPath.size) {
                    val a = eraserPath[j - 1]
                    val b = eraserPath[j]
                    val dist2 = Math.pointSegmentDistanceSquared(p.x, p.y, a.x, a.y, b.x, b.y)
                    if (dist2 <= (threshold * threshold)) { hit = true; break }
                    j++
                }
                touched[i] = hit
            }

            val newSegments = mutableListOf<Stroke>()
            var i = 0
            while (i < pts.size) {
                while (i < pts.size && touched[i]) i++
                val seg = mutableListOf<Point>()
                while (i < pts.size && !touched[i]) { seg.add(pts[i]); i++ }
                if (seg.size >= 2) {
                    newSegments.add(
                        s.copy(
                            id = Random.nextLong().toString(),
                            points = seg,
                        )
                    )
                }
            }
            ops.add(Operation.RemoveStroke(s.id, s))
            newSegments.forEach { ops.add(Operation.AddStroke(it)) }
        }

        ops.forEach { op ->
            when (op) {
                is Operation.RemoveStroke -> strokes = strokes.filterNot { it.id == op.id }
                is Operation.AddStroke -> strokes = strokes + op.stroke
                else -> {}
            }
        }
        ops.forEach { pushOp(it) }
        val rects = mutableListOf<Rect>()
        ops.forEach {
            when (it) {
                is Operation.RemoveStroke -> it.backup?.let { s -> rects.add(strokeBounds(s)) }
                is Operation.AddStroke -> rects.add(strokeBounds(it.stroke))
                else -> {}
            }
        }
        val union = if (rects.isEmpty()) null else Rect(
            rects.minOf { it.left },
            rects.minOf { it.top },
            rects.maxOf { it.right },
            rects.maxOf { it.bottom }
        )
        rebuildBackingForBounds(union)
    }

    /** 导出快照（用于持久化/跨平台存储） */
    fun toSnapshot(): Snapshot {
        val state = this
        val preset = state.brushPreset
        return Snapshot(
            scaleFactor = state.scaleFactor,
            offsetX = state.offset.x,
            offsetY = state.offset.y,
            baseSize = state.baseSize,
            minScale = state.minScale,
            maxScale = state.maxScale,
            rotationRad = state.rotationRad,
            frame = state.frame,
            drawModeName = state.drawMode.name,
            brushColor = state.brushColor,
            brushWidth = state.brushWidth,
            eraserWidth = state.eraserWidth,
            presetShapeName = preset.shape.name,
            presetModeName = preset.mode.name,
            presetSpacing = preset.spacing,
            presetOpacity = preset.opacity,
            presetVelocitySpacingScale = preset.velocitySpacingScale,
            presetBlendModeName = preset.blendMode.toString(),
            presetMaterialShaderKey = preset.materialShaderKey,
            presetSoftEdge = preset.softEdge,
            presetSoftEdgeFeather = preset.softEdgeFeather,
            presetAngleFollow = preset.angleFollow,
            presetAngleSmooth = preset.angleSmooth,
            smoothLine = state.smoothLine,
            rotationSensitivity = state.rotationSensitivity,
            panSensitivity = state.panSensitivity,
            currentToolName = state.currentTool.name,
            commitMinSamples = state.commitMinSamples,
            commitMinDistancePx = state.commitMinDistancePx,
            drawTapSlopViewPx = state.drawTapSlopViewPx,
            tileSize = state.tileSize,
            checkpointInterval = state.checkpointInterval,
            maxCheckpoints = state.maxCheckpoints,
            strokes = state.strokes,
            layers = state.layers,
            pendingStroke = state.pendingStroke,
            undoOps = state.undoStack.toList(),
            redoOps = state.redoStack.toList(),
        )
    }

    companion object {

        @Serializable
        data class Snapshot(
            val scaleFactor: Float,
            val offsetX: Float,
            val offsetY: Float,
            val baseSize: Float,
            val minScale: Float,
            val maxScale: Float,
            val rotationRad: Float,
            val frame: Frame,
            val drawModeName: String,
            val brushColor: Long,
            val brushWidth: Float,
            val eraserWidth: Float,
            val presetShapeName: String,
            val presetModeName: String,
            val presetSpacing: Float,
            val presetOpacity: Float,
            val presetVelocitySpacingScale: Float,
            val presetBlendModeName: String,
            val presetMaterialShaderKey: String?,
            val presetSoftEdge: Boolean,
            val presetSoftEdgeFeather: Float,
            val presetAngleFollow: Boolean,
            val presetAngleSmooth: Float,
            val smoothLine: Boolean,
            val rotationSensitivity: Float,
            val panSensitivity: Float,
            val currentToolName: String,
            val commitMinSamples: Int,
            val commitMinDistancePx: Float,
            val drawTapSlopViewPx: Float,
            val tileSize: Int,
            val checkpointInterval: Int,
            val maxCheckpoints: Int,
            val strokes: List<Stroke>,
            val layers: List<String>,
            val pendingStroke: Stroke?,
            val undoOps: List<Operation>,
            val redoOps: List<Operation>,
        )

        /** 状态保存/恢复器（配合 rememberSaveable 使用） */
        internal val Saver: Saver<DrawingState, String> = Saver(
            save = { state ->
                state.export()
            },
            restore = { data ->
                DrawingState().apply {
                    import(data)
                }
            }
        )
    }

    /** 导出可序列化字符串 */
    fun export(): String {
        return Json.encodeToString(this.toSnapshot())
    }

    /** 从序列化字符串导入状态 */
    fun import(data: String) {
        val snapshot = try {
            Json.decodeFromString<Snapshot>(data)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        // geometry & view
        frame = snapshot.frame
        scaleFactor = snapshot.scaleFactor
        offset = Offset(snapshot.offsetX, snapshot.offsetY)
        baseSize = snapshot.baseSize
        minScale = snapshot.minScale
        maxScale = snapshot.maxScale
        rotationRad = snapshot.rotationRad
        // modes
        drawMode = DrawMode.valueOf(snapshot.drawModeName)
        currentTool = ToolType.valueOf(snapshot.currentToolName)
        // brush state
        brushColor = snapshot.brushColor
        brushWidth = snapshot.brushWidth
        eraserWidth = snapshot.eraserWidth
        brushPreset = BrushPreset(
            shape = BrushShapeType.valueOf(snapshot.presetShapeName),
            mode = StrokeModel.valueOf(snapshot.presetModeName),
            spacing = snapshot.presetSpacing,
            opacity = snapshot.presetOpacity,
            velocitySpacingScale = snapshot.presetVelocitySpacingScale,
            blendMode = BlendMode.SrcOver,
            materialShaderKey = snapshot.presetMaterialShaderKey,
            softEdge = snapshot.presetSoftEdge,
            softEdgeFeather = snapshot.presetSoftEdgeFeather,
            angleFollow = snapshot.presetAngleFollow,
            angleSmooth = snapshot.presetAngleSmooth,
        )
        smoothLine = snapshot.smoothLine
        rotationSensitivity = snapshot.rotationSensitivity
        panSensitivity = snapshot.panSensitivity
        commitMinSamples = snapshot.commitMinSamples
        commitMinDistancePx = snapshot.commitMinDistancePx
        drawTapSlopViewPx = snapshot.drawTapSlopViewPx
        // tiles & checkpoints config
        tileSize = snapshot.tileSize
        checkpointInterval = snapshot.checkpointInterval
        maxCheckpoints = snapshot.maxCheckpoints
        // content
        strokes = snapshot.strokes
        layers = snapshot.layers
        pendingStroke = snapshot.pendingStroke
        // undo/redo
        undoStack.clear(); undoStack.addAll(snapshot.undoOps)
        redoStack.clear(); redoStack.addAll(snapshot.redoOps)
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
        // rebuild backing for display
        rebuildBackingFromStrokes()
    }

    private fun scaleAndTopLeft(canvasSize: Size, density: Float): Triple<Float, Float, Float> {
        val fw = frame.width.toFloat()
        val fh = frame.height.toFloat()
        val basePx = baseSize * density
        val scale = min(basePx / fw, basePx / fh) * scaleFactor
        val viewW = fw * scale
        val viewH = fh * scale
        val topLeftX = (canvasSize.width - viewW) / 2f + offset.x
        val topLeftY = (canvasSize.height - viewH) / 2f + offset.y
        return Triple(scale, topLeftX, topLeftY)
    }

    /** 帧->视图变换矩阵 */
    fun frameToViewMatrix(canvasSize: Size, density: Float): Matrix3x3 {
        val fw = frame.width.toFloat()
        val fh = frame.height.toFloat()
        val (scale, topLeftX, topLeftY) = scaleAndTopLeft(canvasSize, density)
        val pivot = Offset(topLeftX + (fw * scale) / 2f, topLeftY + (fh * scale) / 2f)
        val mTranslate = Matrix3x3.translation(topLeftX, topLeftY)
        val mScale = Matrix3x3.scale(scale, scale)
        val mRotate = Matrix3x3.rotateAround(rotationRad, pivot)
        return mRotate.times(mTranslate.times(mScale))
    }

    /** 视图->帧逆变换矩阵 */
    fun viewToFrameMatrix(canvasSize: Size, density: Float): Matrix3x3 {
        val fw = frame.width.toFloat()
        val fh = frame.height.toFloat()
        val (scale, topLeftX, topLeftY) = scaleAndTopLeft(canvasSize, density)
        val pivot = Offset(topLeftX + (fw * scale) / 2f, topLeftY + (fh * scale) / 2f)
        val mTranslateInv = Matrix3x3.translation(-topLeftX, -topLeftY)
        val mRotateInv = Matrix3x3.rotateAround(-rotationRad, pivot)
        val mScaleInv = Matrix3x3.scale(1f / scale, 1f / scale)
        // F = R(pivot) * T(topLeft) * S(scale) => F^{-1} = S^{-1} * T^{-1} * R^{-1}
        return mScaleInv.times(mTranslateInv).times(mRotateInv)
    }

    private fun viewToFrame(viewPos: Offset, canvasSize: Size, density: Float): Offset {
        val inv = viewToFrameMatrix(canvasSize, density)
        return inv.apply(viewPos)
    }

    private fun insideFrame(framePos: Offset): Boolean {
        return framePos.x in 0f..frame.width.toFloat() && framePos.y in 0f..frame.height.toFloat()
    }

    /** 开始绘制：将视图点映射到帧，并创建待提交笔划 */
    fun beginDraw(viewPos: Offset, canvasSize: Size, density: Float) {
        if (drawMode != DrawMode.Draw) return
        val fp = viewToFrame(viewPos, canvasSize, density)
        val id = Random.nextLong().toString()
        val pts = mutableListOf(Point(fp.x.toDouble(), fp.y.toDouble()))
        val isEraserTool = currentTool == ToolType.Eraser
        val preset = brushPreset
        val width = if (isEraserTool) eraserWidth else brushWidth
        pendingStroke = Stroke(
            id = id,
            points = pts,
            color = brushColor,
            width = width,
            smooth = false,
            model = preset.mode,
            shape = preset.shape,
            spacing = preset.spacing,
            opacity = preset.opacity,
            velocitySpacingScale = preset.velocitySpacingScale,
            shaderKey = preset.materialShaderKey,
            softEdge = preset.softEdge,
            softEdgeFeather = preset.softEdgeFeather,
            angleFollow = preset.angleFollow,
            angleSmooth = preset.angleSmooth,
            isEraser = isEraserTool,
        )
    }

    /** 追加绘制点：实时更新待提交笔划 */
    fun appendDraw(viewPos: Offset, canvasSize: Size, density: Float) {
        val s = pendingStroke ?: return
        val fp = viewToFrame(viewPos, canvasSize, density)
        val pts = s.points.toMutableList()
        pts.add(Point(fp.x.toDouble(), fp.y.toDouble()))
        pendingStroke = s.copy(points = pts)
    }

    /** 结束绘制：根据阈值决定是否提交笔划并写入离屏 */
    fun endDraw() {
        val s = pendingStroke ?: return
        val isEraserTool = s.isEraser
        val countOk = if (isEraserTool) s.points.size >= 1 else s.points.size >= commitMinSamples
        val lenOk = if (isEraserTool) true else strokeLength(s) >= commitMinDistancePx
        if (countOk && lenOk) {
            val base = if (smoothLine && s.points.size >= 3) s.copy(smooth = true) else s.copy(smooth = false)
            val final = if (isEraserTool && base.points.size < 2) base.copy(model = StrokeModel.Stamp) else base
            strokes = strokes + final
            pushOp(Operation.AddStroke(final))
            commitStrokeToBacking(final)
            createCheckpointIfNeeded()
        }
        pendingStroke = null
    }

    /** 取消绘制：丢弃待提交笔划 */
    fun cancelDraw() {
        pendingStroke = null
    }

    private fun commitStrokeToBacking(stroke: Stroke) {
        ensureTiles()
        val b = strokeBounds(stroke)
        val c0 = (b.left.toInt() / tileSize).coerceAtLeast(0)
        val c1 = (b.right.toInt() / tileSize).coerceAtMost(max(0, tileCols - 1))
        val r0 = (b.top.toInt() / tileSize).coerceAtLeast(0)
        val r1 = (b.bottom.toInt() / tileSize).coerceAtMost(max(0, tileRows - 1))
        var r = r0
        while (r <= r1) {
            var c = c0
            while (c <= c1) {
                val tile = tileAt(c, r)
                val left = tile.originX.toFloat()
                val top = tile.originY.toFloat()
                val right = left + tile.width
                val bottom = top + tile.height
                val clip = Rect(
                    b.left.coerceIn(left, right),
                    b.top.coerceIn(top, bottom),
                    b.right.coerceIn(left, right),
                    b.bottom.coerceIn(top, bottom)
                )
                if (clip.width <= 0f || clip.height <= 0f) { c++; continue }
                if (stroke.model == StrokeModel.Stamp) {
                    rasterizeStampStrokeToCanvas(tile.canvas, stroke, tile.width, tile.height, originX = left, originY = top, clip = clip)
                } else {
                    rasterizePathStrokeToCanvas(tile.canvas, stroke, tile.width, tile.height, originX = left, originY = top, clip = clip)
                }
                c++
            }
            r++
        }
        updateCompositeForBounds(b)
    }

    private fun commitStrokeToTile(tile: Tile, stroke: Stroke) {
        val b = strokeBounds(stroke)
        val left = tile.originX.toFloat()
        val top = tile.originY.toFloat()
        val right = left + tile.width
        val bottom = top + tile.height
        val clip = Rect(
            b.left.coerceIn(left, right),
            b.top.coerceIn(top, bottom),
            b.right.coerceIn(left, right),
            b.bottom.coerceIn(top, bottom)
        )
        if (clip.width <= 0f || clip.height <= 0f) return
        if (stroke.model == StrokeModel.Stamp) {
            rasterizeStampStrokeToCanvas(tile.canvas, stroke, tile.width, tile.height, originX = left, originY = top, clip = clip)
        } else {
            rasterizePathStrokeToCanvas(tile.canvas, stroke, tile.width, tile.height, originX = left, originY = top, clip = clip)
        }
    }

    private fun rebuildBackingFromStrokes() {
        ensureTiles()
        clearTiles()
        val targetCount = strokes.size
        val cp = checkpoints.lastOrNull { it.strokeCount <= targetCount }
        if (cp != null) {
            tiles.forEach { t ->
                val ds = CanvasDrawScope()
                ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = t.canvas, size = Size(t.width.toFloat(), t.height.toFloat())) {
                    clipRect(0f, 0f, t.width.toFloat(), t.height.toFloat()) {
                        drawImage(cp.bitmap, topLeft = Offset(-t.originX.toFloat(), -t.originY.toFloat()))
                    }
                }
            }
        }
        val startIndex = cp?.strokeCount ?: 0
        var idx = startIndex
        while (idx < targetCount) {
            val s = strokes[idx]
            commitStrokeToBacking(s)
            idx++
        }
        copyAllTilesToComposite()
    }

    private fun rebuildBackingForBounds(bounds: Rect?) {
        ensureTiles()
        if (bounds == null) {
            rebuildBackingFromStrokes()
            return
        }
        val c0 = (bounds.left.toInt() / tileSize).coerceAtLeast(0)
        val c1 = (bounds.right.toInt() / tileSize).coerceAtMost(max(0, tileCols - 1))
        val r0 = (bounds.top.toInt() / tileSize).coerceAtLeast(0)
        val r1 = (bounds.bottom.toInt() / tileSize).coerceAtMost(max(0, tileRows - 1))
        var r = r0
        while (r <= r1) {
            var c = c0
            while (c <= c1) {
                val t = tileAt(c, r)
                val ds = CanvasDrawScope()
                ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = t.canvas, size = Size(t.width.toFloat(), t.height.toFloat())) {
                    clipRect(0f, 0f, t.width.toFloat(), t.height.toFloat()) {
                        drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
                    }
                }
                c++
            }
            r++
        }
        var idx = 0
        while (idx < strokes.size) {
            val s = strokes[idx]
            val sb = strokeBounds(s)
            val cc0 = (sb.left.toInt() / tileSize).coerceAtLeast(0)
            val cc1 = (sb.right.toInt() / tileSize).coerceAtMost(max(0, tileCols - 1))
            val rr0 = (sb.top.toInt() / tileSize).coerceAtLeast(0)
            val rr1 = (sb.bottom.toInt() / tileSize).coerceAtMost(max(0, tileRows - 1))
            var rr = rr0
            while (rr <= rr1) {
                var cc = cc0
                while (cc <= cc1) {
                    if (cc in c0..c1 && rr in r0..r1) {
                        val t = tileAt(cc, rr)
                        commitStrokeToTile(t, s)
                    }
                    cc++
                }
                rr++
            }
            idx++
        }
        updateCompositeForBounds(bounds)
    }

    private fun blitTileToComposite(tile: Tile) {
        ensureBacking()
        val canvas = backingCanvas ?: return
        val ds = CanvasDrawScope()
        ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = Size(frame.width.toFloat(), frame.height.toFloat())) {
            val l = tile.originX.toFloat()
            val t = tile.originY.toFloat()
            val r = (tile.originX + tile.width).toFloat()
            val b = (tile.originY + tile.height).toFloat()
            clipRect(l, t, r, b) {
                drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
                drawImage(tile.bitmap, topLeft = Offset(l, t))
            }
        }
    }

    private fun copyAllTilesToComposite() {
        ensureTiles()
        clearBacking()
        tiles.forEach { blitTileToComposite(it) }
    }

    private fun updateCompositeForBounds(bounds: Rect?) {
        ensureTiles()
        ensureBacking()
        if (bounds == null) { copyAllTilesToComposite(); return }
        val c0 = (bounds.left.toInt() / tileSize).coerceAtLeast(0)
        val c1 = (bounds.right.toInt() / tileSize).coerceAtMost(max(0, tileCols - 1))
        val r0 = (bounds.top.toInt() / tileSize).coerceAtLeast(0)
        val r1 = (bounds.bottom.toInt() / tileSize).coerceAtMost(max(0, tileRows - 1))
        var r = r0
        while (r <= r1) {
            var c = c0
            while (c <= c1) {
                blitTileToComposite(tileAt(c, r))
                c++
            }
            r++
        }
    }

    private fun strokeBounds(s: Stroke): Rect {
        val xs = s.points.map { it.x.toFloat() }
        val ys = s.points.map { it.y.toFloat() }
        if (xs.isEmpty()) return Rect(0f, 0f, 0f, 0f)
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        val half = s.width / 2f
        val left = (minX - half).coerceAtLeast(0f)
        val top = (minY - half).coerceAtLeast(0f)
        val right = (maxX + half).coerceAtMost(frame.width.toFloat())
        val bottom = (maxY + half).coerceAtMost(frame.height.toFloat())
        return Rect(left, top, right, bottom)
    }

    private fun strokeLength(s: Stroke): Float {
        var acc = 0f
        val pts = s.points
        if (pts.size < 2) return 0f
        var i = 1
        var px = pts[0].x.toFloat()
        var py = pts[0].y.toFloat()
        while (i < pts.size) {
            val qx = pts[i].x.toFloat()
            val qy = pts[i].y.toFloat()
            val dx = qx - px
            val dy = qy - py
            acc += sqrt(dx * dx + dy * dy)
            px = qx
            py = qy
            i++
        }
        return acc
    }
}
