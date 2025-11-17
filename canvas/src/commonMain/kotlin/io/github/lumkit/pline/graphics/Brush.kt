package io.github.lumkit.pline.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.lumkit.pline.util.Math
import kotlin.math.atan2
import androidx.compose.ui.geometry.Size as GSize
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 画笔形状类型
 */
enum class BrushShapeType { Circle, Square }

/**
 * 笔划渲染模型
 * - `Path`：以连续路径方式绘制；
 * - `Stamp`：按间距投放形状（贴章）绘制。
 */
enum class StrokeModel { Path, Stamp }

/**
 * 画笔预设，定义绘制时的形状、模式与混合等属性。
 * @property shape 贴章形状
 * @property mode 渲染模型
 * @property spacing 贴章间距（相对宽度比例）
 * @property opacity 不透明度 0..1
 * @property velocitySpacingScale 速度对间距的影响系数
 * @property blendMode 混合模式
 * @property materialShaderKey 材质/着色器键
 * @property softEdge 是否软边
 * @property softEdgeFeather 软边羽化强度 0..1
 * @property angleFollow 贴章是否随方向旋转
 * @property angleSmooth 角度变化平滑系数 0..1
 */
data class BrushPreset(
    val shape: BrushShapeType = BrushShapeType.Circle,
    val mode: StrokeModel = StrokeModel.Stamp,
    val spacing: Float = 0f,
    val opacity: Float = 1f,
    val velocitySpacingScale: Float = 0f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val materialShaderKey: String? = null,
    val softEdge: Boolean = false,
    val softEdgeFeather: Float = 0.5f,
    val angleFollow: Boolean = false,
    val angleSmooth: Float = 0.3f,
)

/**
 * 在 `DrawScope` 上以贴章模型绘制笔划。
 *
 * 性能：内部会根据裁剪区域和间距进行跳采；
 * 软边时使用径向渐变模拟羽化效果。
 *
 * @param s 笔划
 * @param ox 视图空间中帧原点 X 偏移
 * @param oy 视图空间中帧原点 Y 偏移
 * @param scale 帧->视图缩放比例
 * @param left 裁剪区域左
 * @param top 裁剪区域上
 * @param right 裁剪区域右
 * @param bottom 裁剪区域下
 */
fun DrawScope.drawStampStroke(
    s: Stroke,
    ox: Float,
    oy: Float,
    scale: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    val pts = s.points
    if (pts.isEmpty()) return
    val color = Color(s.color).copy(alpha = s.opacity)
    val w = s.width * scale
    var interval = (s.spacing.coerceAtLeast(0f)) * w
    val minInterval = if (s.softEdge) w * 0.2f else w * 0.25f
    if (interval <= 0f) interval = minInterval else interval = max(interval, minInterval)
    val bm = if (s.isEraser) BlendMode.DstOut else BlendMode.SrcOver

    fun insideClip(x: Float, y: Float, r: Float): Boolean {
        return x >= left - r && x <= right + r && y >= top - r && y <= bottom + r
    }

    fun addStamp(path: Path, x: Float, y: Float, w: Float) {
        if (!insideClip(x, y, w / 2f)) return
        when (s.shape) {
            BrushShapeType.Circle -> path.addOval(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
            BrushShapeType.Square -> path.addRect(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
        }
    }

    fun addStampRot(path: Path, x: Float, y: Float, w: Float, angle: Float) {
        if (!insideClip(x, y, w / 2f)) return
        if (s.shape == BrushShapeType.Circle) {
            path.addOval(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
        } else {
            val hw = w / 2f
            val ca = cos(angle)
            val sa = sin(angle)
            val p0x = x + (-hw * ca - -hw * sa)
            val p0y = y + (-hw * sa + -hw * ca)
            val p1x = x + ( hw * ca - -hw * sa)
            val p1y = y + ( hw * sa + -hw * ca)
            val p2x = x + ( hw * ca - hw * sa)
            val p2y = y + ( hw * sa + hw * ca)
            val p3x = x + (-hw * ca - hw * sa)
            val p3y = y + (-hw * sa + hw * ca)
            path.moveTo(p0x, p0y)
            path.lineTo(p1x, p1y)
            path.lineTo(p2x, p2y)
            path.lineTo(p3x, p3y)
            path.close()
        }
    }

    fun drawSoftStamp(cx: Float, cy: Float, w: Float, angle: Float) {
        if (!insideClip(cx, cy, w / 2f)) return
        val r = w / 2f
        val innerA = color
        val outerA = color.copy(alpha = color.alpha * (1f - s.softEdgeFeather.coerceIn(0f, 1f)))
        val brush = Brush.radialGradient(colors = listOf(innerA, outerA), center = Offset(cx, cy), radius = r)
        when (s.shape) {
            BrushShapeType.Circle -> drawPath(path = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }, brush = brush, blendMode = bm)
            BrushShapeType.Square -> {
                val hw = r
                val ca = cos(angle)
                val sa = sin(angle)
                val p = Path().apply {
                    val p0x = cx + (-hw * ca - -hw * sa)
                    val p0y = cy + (-hw * sa + -hw * ca)
                    val p1x = cx + ( hw * ca - -hw * sa)
                    val p1y = cy + ( hw * sa + -hw * ca)
                    val p2x = cx + ( hw * ca - hw * sa)
                    val p2y = cy + ( hw * sa + hw * ca)
                    val p3x = cx + (-hw * ca - hw * sa)
                    val p3y = cy + (-hw * sa + hw * ca)
                    moveTo(p0x, p0y)
                    lineTo(p1x, p1y)
                    lineTo(p2x, p2y)
                    lineTo(p3x, p3y)
                    close()
                }
                drawPath(path = p, brush = brush, blendMode = bm)
            }
        }
    }

    fun stamp(path: Path, x: Float, y: Float, w: Float, angle: Float) {
        if (s.softEdge) {
            drawSoftStamp(x, y, w, angle)
        } else if (s.angleFollow && s.shape == BrushShapeType.Square) {
            addStampRot(path, x, y, w, angle)
        } else {
            addStamp(path, x, y, w)
        }
    }

    val path = Path()
    var anglePrev = 0f
    if (s.smooth && pts.size >= 3) {
        val stepFrame = interval / scale
        val samples = sampleSmoothFramePoints(pts, stepFrame)
        if (samples.isNotEmpty()) {
            var lastX = ox + samples.first().x * scale
            var lastY = oy + samples.first().y * scale
            stamp(path, lastX, lastY, w, anglePrev)
            var distanceSinceLastStamp = 0f
            var i = 1
            while (i < samples.size) {
                val tx = ox + samples[i].x * scale
                val ty = oy + samples[i].y * scale
                val segDx = tx - lastX
                val segDy = ty - lastY
                var segLen = sqrt(segDx * segDx + segDy * segDy)
                if (segLen > 0f) {
                    val dirX = segDx / segLen
                    val dirY = segDy / segLen
                    val target = if (s.angleFollow) atan2(dirY.toDouble(), dirX.toDouble()).toFloat() else anglePrev
                    val delta = Math.shortestAngleDelta(anglePrev, target)
                    anglePrev += delta * s.angleSmooth
                    var nextPosLen = if (distanceSinceLastStamp == 0f) interval else interval - distanceSinceLastStamp
                    while (segLen >= nextPosLen) {
                        val nx = lastX + dirX * nextPosLen
                        val ny = lastY + dirY * nextPosLen
                        stamp(path, nx, ny, w, anglePrev)
                        lastX = nx
                        lastY = ny
                        segLen -= nextPosLen
                        distanceSinceLastStamp = 0f
                        nextPosLen = interval
                    }
                    distanceSinceLastStamp += segLen
                }
                lastX = tx
                lastY = ty
                i++
            }
            if (distanceSinceLastStamp > (interval * 0.5f)) {
                stamp(path, lastX, lastY, w, anglePrev)
            }
        }
    } else {
        var lastX = ox + (pts.first().x.toFloat() * scale)
        var lastY = oy + (pts.first().y.toFloat() * scale)
        stamp(path, lastX, lastY, w, anglePrev)
        var distanceSinceLastStamp = 0f
        var i = 1
        while (i < pts.size) {
            val tx = ox + (pts[i].x.toFloat() * scale)
            val ty = oy + (pts[i].y.toFloat() * scale)
            var segDx = tx - lastX
            var segDy = ty - lastY
            var segLen = sqrt(segDx * segDx + segDy * segDy)
            val dt = max(1f, (pts[i].time - pts[i - 1].time).toFloat())
            val vel = segLen / dt
            val effInterval = interval * (1f + s.velocitySpacingScale * vel)
            if (segLen > 0f) {
                val dirX = segDx / segLen
                val dirY = segDy / segLen
                val target = if (s.angleFollow) atan2(dirY.toDouble(), dirX.toDouble()).toFloat() else anglePrev
                val delta = Math.shortestAngleDelta(anglePrev, target)
                anglePrev += delta * s.angleSmooth
                var nextPosLen = if (distanceSinceLastStamp == 0f) effInterval else effInterval - distanceSinceLastStamp
                while (segLen >= nextPosLen) {
                    val nx = lastX + dirX * nextPosLen
                    val ny = lastY + dirY * nextPosLen
                    stamp(path, nx, ny, w, anglePrev)
                    lastX = nx
                    lastY = ny
                    segLen -= nextPosLen
                    distanceSinceLastStamp = 0f
                    nextPosLen = effInterval
                }
                distanceSinceLastStamp += segLen
            }
            lastX = tx
            lastY = ty
            i++
        }
        if (distanceSinceLastStamp > (interval * 0.5f)) {
            stamp(path, lastX, lastY, w, anglePrev)
        }
    }

    if (!path.isEmpty && !s.softEdge) {
        val brush = gradientBrushFor(s)
        if (brush != null) {
            drawPath(path = path, brush = brush, blendMode = bm)
        } else {
            drawPath(path = path, color = color, blendMode = bm)
        }
    }
}

/**
 * 将路径模型笔划栅格化到离屏 `Canvas`（帧坐标系）。
 * @param canvas 目标画布（像素大小应与 `frameWidth/Height` 对齐）
 * @param s 笔划
 * @param frameWidth 帧宽度
 * @param frameHeight 帧高度
 * @param originX 相对于画布的帧原点 X 偏移
 * @param originY 相对于画布的帧原点 Y 偏移
 * @param clip 可选裁剪区域（帧坐标系）
 */
fun rasterizePathStrokeToCanvas(
    canvas: Canvas,
    s: Stroke,
    frameWidth: Int,
    frameHeight: Int,
    originX: Float = 0f,
    originY: Float = 0f,
    clip: Rect? = null,
) {
    val pts = s.points
    if (pts.isEmpty()) return
    val bm = if (s.isEraser) BlendMode.Clear else BlendMode.SrcOver
    val path = Path()
    val useSmooth = s.smooth && pts.size >= 3
    if (useSmooth) {
        val step = max(1f, s.width * 0.5f)
        val samples = sampleSmoothFramePoints(pts, step)
        if (samples.isNotEmpty()) {
            val p0 = samples.first()
            path.moveTo(p0.x - originX, p0.y - originY)
            var i = 1
            while (i < samples.size) {
                val p = samples[i]
                path.lineTo(p.x - originX, p.y - originY)
                i++
            }
        }
    } else {
        val p0 = pts.first()
        path.moveTo(p0.x.toFloat() - originX, p0.y.toFloat() - originY)
        var i = 1
        while (i < pts.size) {
            val p = pts[i]
            path.lineTo(p.x.toFloat() - originX, p.y.toFloat() - originY)
            i++
        }
    }
    val ds = CanvasDrawScope()
    ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = GSize(frameWidth.toFloat(), frameHeight.toFloat())) {
        if (clip != null) {
            clipRect(clip.left - originX, clip.top - originY, clip.right - originX, clip.bottom - originY) {
                val brush = gradientBrushFor(s)
                val style = androidx.compose.ui.graphics.drawscope.Stroke(width = s.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                if (brush != null) {
                    drawPath(path = path, brush = brush, style = style, blendMode = bm)
                } else {
                    drawPath(path = path, color = Color(s.color).copy(alpha = s.opacity), style = style, blendMode = bm)
                }
            }
        } else {
            val brush = gradientBrushFor(s)
            val style = androidx.compose.ui.graphics.drawscope.Stroke(width = s.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (brush != null) {
                drawPath(path = path, brush = brush, style = style, blendMode = bm)
            } else {
                drawPath(path = path, color = Color(s.color).copy(alpha = s.opacity), style = style, blendMode = bm)
            }
        }
    }
}

/**
 * 将贴章模型笔划栅格化到离屏 `Canvas`（帧坐标系）。
 * @param canvas 目标画布（像素大小应与 `frameWidth/Height` 对齐）
 * @param s 笔划
 * @param frameWidth 帧宽度
 * @param frameHeight 帧高度
 * @param originX 相对于画布的帧原点 X 偏移
 * @param originY 相对于画布的帧原点 Y 偏移
 * @param clip 可选裁剪区域（帧坐标系）
 */
fun rasterizeStampStrokeToCanvas(
    canvas: Canvas,
    s: Stroke,
    frameWidth: Int,
    frameHeight: Int,
    originX: Float = 0f,
    originY: Float = 0f,
    clip: Rect? = null,
) {
    val pts = s.points
    if (pts.isEmpty()) return
    val color = Color(s.color).copy(alpha = s.opacity)
    val w = s.width
    var interval = (s.spacing.coerceAtLeast(0f)) * w
    val minInterval = if (s.softEdge) w * 0.2f else w * 0.25f
    if (interval <= 0f) interval = minInterval else interval = max(interval, minInterval)
    val bm = if (s.isEraser) BlendMode.Clear else BlendMode.SrcOver

    fun insideClip(x: Float, y: Float, r: Float): Boolean {
        if (clip != null) {
            val l = clip.left - originX
            val t = clip.top - originY
            val rr = clip.right - originX
            val bb = clip.bottom - originY
            return x >= l - r && x <= rr + r && y >= t - r && y <= bb + r
        }
        return x >= -r && x <= frameWidth + r && y >= -r && y <= frameHeight + r
    }

    fun addStamp(path: Path, x: Float, y: Float, w: Float) {
        if (!insideClip(x, y, w / 2f)) return
        when (s.shape) {
            BrushShapeType.Circle -> path.addOval(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
            BrushShapeType.Square -> path.addRect(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
        }
    }

    fun addStampRot(path: Path, x: Float, y: Float, w: Float, angle: Float) {
        if (!insideClip(x, y, w / 2f)) return
        if (s.shape == BrushShapeType.Circle) {
            path.addOval(Rect(x - w / 2f, y - w / 2f, x + w / 2f, y + w / 2f))
        } else {
            val hw = w / 2f
            val ca = cos(angle)
            val sa = sin(angle)
            val p0x = x + (-hw * ca - -hw * sa)
            val p0y = y + (-hw * sa + -hw * ca)
            val p1x = x + ( hw * ca - -hw * sa)
            val p1y = y + ( hw * sa + -hw * ca)
            val p2x = x + ( hw * ca - hw * sa)
            val p2y = y + ( hw * sa + hw * ca)
            val p3x = x + (-hw * ca - hw * sa)
            val p3y = y + (-hw * sa + hw * ca)
            path.moveTo(p0x, p0y)
            path.lineTo(p1x, p1y)
            path.lineTo(p2x, p2y)
            path.lineTo(p3x, p3y)
            path.close()
        }
    }

    fun stamp(path: Path, x: Float, y: Float, w: Float, angle: Float) {
        if (s.softEdge) {
            val r = w / 2f
            val innerA = color
            val outerA = color.copy(alpha = color.alpha * (1f - s.softEdgeFeather.coerceIn(0f, 1f)))
            val brush = Brush.radialGradient(colors = listOf(innerA, outerA), center = Offset(x, y), radius = r)
            val ds = CanvasDrawScope()
            ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = GSize(frameWidth.toFloat(), frameHeight.toFloat())) {
                val p = if (s.shape == BrushShapeType.Circle) Path().apply { addOval(Rect(x - r, y - r, x + r, y + r)) } else Path().apply {
                    val hw = r
                    val ca = cos(angle)
                    val sa = sin(angle)
                    val p0x = x + (-hw * ca - -hw * sa)
                    val p0y = y + (-hw * sa + -hw * ca)
                    val p1x = x + ( hw * ca - -hw * sa)
                    val p1y = y + ( hw * sa + -hw * ca)
                    val p2x = x + ( hw * ca - hw * sa)
                    val p2y = y + ( hw * sa + hw * ca)
                    val p3x = x + (-hw * ca - hw * sa)
                    val p3y = y + (-hw * sa + hw * ca)
                    moveTo(p0x, p0y)
                    lineTo(p1x, p1y)
                    lineTo(p2x, p2y)
                    lineTo(p3x, p3y)
                    close()
                }
                if (clip != null) {
                    clipRect(clip.left - originX, clip.top - originY, clip.right - originX, clip.bottom - originY) {
                        drawPath(path = p, brush = brush, blendMode = bm)
                    }
                } else {
                    drawPath(path = p, brush = brush, blendMode = bm)
                }
            }
        } else if (s.angleFollow && s.shape == BrushShapeType.Square) {
            addStampRot(path, x, y, w, angle)
        } else {
            addStamp(path, x, y, w)
        }
    }

    val path = Path()
    var anglePrev = 0f
    if (s.smooth && pts.size >= 3) {
        val samples = sampleSmoothFramePoints(pts, interval)
        if (samples.isNotEmpty()) {
            var lastX = samples.first().x - originX
            var lastY = samples.first().y - originY
            stamp(path, lastX, lastY, w, anglePrev)
            var distanceSinceLastStamp = 0f
            var i = 1
            while (i < samples.size) {
                val tx = samples[i].x - originX
                val ty = samples[i].y - originY
                val segDx = tx - lastX
                val segDy = ty - lastY
                var segLen = sqrt(segDx * segDx + segDy * segDy)
                if (segLen > 0f) {
                    val dirX = segDx / segLen
                    val dirY = segDy / segLen
                    val target = if (s.angleFollow) atan2(dirY.toDouble(), dirX.toDouble()).toFloat() else anglePrev
                    val delta = Math.shortestAngleDelta(anglePrev, target)
                    anglePrev += delta * s.angleSmooth
                    var nextPosLen = if (distanceSinceLastStamp == 0f) interval else interval - distanceSinceLastStamp
                    while (segLen >= nextPosLen) {
                        val nx = lastX + dirX * nextPosLen
                        val ny = lastY + dirY * nextPosLen
                        stamp(path, nx, ny, w, anglePrev)
                        lastX = nx
                        lastY = ny
                        segLen -= nextPosLen
                        distanceSinceLastStamp = 0f
                        nextPosLen = interval
                    }
                    distanceSinceLastStamp += segLen
                }
                lastX = tx
                lastY = ty
                i++
            }
            if (distanceSinceLastStamp > (interval * 0.5f)) {
                stamp(path, lastX, lastY, w, anglePrev)
            }
        }
    } else {
        var lastX = pts.first().x.toFloat() - originX
        var lastY = pts.first().y.toFloat() - originY
        stamp(path, lastX, lastY, w, anglePrev)
        var distanceSinceLastStamp = 0f
        var i = 1
        while (i < pts.size) {
            val tx = pts[i].x.toFloat() - originX
            val ty = pts[i].y.toFloat() - originY
            val segDx = tx - lastX
            val segDy = ty - lastY
            var segLen = sqrt(segDx * segDx + segDy * segDy)
            val dt = max(1f, (pts[i].time - pts[i - 1].time).toFloat())
            val vel = segLen / dt
            val effInterval = interval * (1f + s.velocitySpacingScale * vel)
            if (segLen > 0f) {
                val dirX = segDx / segLen
                val dirY = segDy / segLen
                val target = if (s.angleFollow) atan2(dirY.toDouble(), dirX.toDouble()).toFloat() else anglePrev
                val delta = Math.shortestAngleDelta(anglePrev, target)
                anglePrev += delta * s.angleSmooth
                var nextPosLen = if (distanceSinceLastStamp == 0f) effInterval else effInterval - distanceSinceLastStamp
                while (segLen >= nextPosLen) {
                    val nx = lastX + dirX * nextPosLen
                    val ny = lastY + dirY * nextPosLen
                    stamp(path, nx, ny, w, anglePrev)
                    lastX = nx
                    lastY = ny
                    segLen -= nextPosLen
                    distanceSinceLastStamp = 0f
                    nextPosLen = effInterval
                }
                distanceSinceLastStamp += segLen
            }
            lastX = tx
            lastY = ty
            i++
        }
        if (distanceSinceLastStamp > (interval * 0.5f)) {
            stamp(path, lastX, lastY, w, anglePrev)
        }
    }

    if (!path.isEmpty && !s.softEdge) {
        val ds = CanvasDrawScope()
        ds.draw(density = Density(1f), layoutDirection = LayoutDirection.Ltr, canvas = canvas, size = GSize(frameWidth.toFloat(), frameHeight.toFloat())) {
            if (clip != null) {
                clipRect(clip.left - originX, clip.top - originY, clip.right - originX, clip.bottom - originY) {
                    val brush = gradientBrushFor(s)
                    if (brush != null) {
                        drawPath(path = path, brush = brush, blendMode = bm)
                    } else {
                        drawPath(path = path, color = color, blendMode = bm)
                    }
                }
            } else {
                val brush = gradientBrushFor(s)
                if (brush != null) {
                    drawPath(path = path, brush = brush, blendMode = bm)
                } else {
                    drawPath(path = path, color = color, blendMode = bm)
                }
            }
        }
    }
}

/**
 * 依据笔划 `shaderKey` 生成渐变画刷。
 * @param s 笔划
 * @return 渐变画刷；不匹配时返回 `null`
 */
fun gradientBrushFor(s: Stroke): Brush? {
    val base = Color(s.color).copy(alpha = s.opacity)
    return when (s.shaderKey) {
        "linear" -> Brush.linearGradient(colors = listOf(base.copy(alpha = base.alpha * 0.8f), base.copy(alpha = base.alpha)))
        "radial" -> Brush.radialGradient(colors = listOf(base.copy(alpha = base.alpha), base.copy(alpha = base.alpha * 0.6f)))
        else -> null
    }
}

/**
 * 对帧坐标点列进行平滑采样，生成等步长的采样点（用于路径/贴章插值）。
 * @param pts 原始点列（帧坐标）
 * @param stepLen 采样步长（帧像素）
 * @return 平滑采样后的点列（帧坐标）
 */
fun sampleSmoothFramePoints(pts: List<Point>, stepLen: Float): List<Offset> {
    val out = mutableListOf<Offset>()
    if (pts.isEmpty()) return out
    out.add(Offset(pts.first().x.toFloat(), pts.first().y.toFloat()))
    if (pts.size < 3) return out
    var p0 = pts[0]
    var p1 = pts[1]
    var i = 2
    while (i < pts.size) {
        val p2 = pts[i]
        val m1x = ((p0.x + p1.x) * 0.5).toFloat()
        val m1y = ((p0.y + p1.y) * 0.5).toFloat()
        val m2x = ((p1.x + p2.x) * 0.5).toFloat()
        val m2y = ((p1.y + p2.y) * 0.5).toFloat()
        val dx = m2x - m1x
        val dy = m2y - m1y
        val segLen = sqrt(dx * dx + dy * dy)
        val steps = max(1, ceil(segLen / stepLen).toInt())
        var j = 1
        while (j <= steps) {
            val t = j.toFloat() / steps.toFloat()
            val one = 1f - t
            val x = one * one * m1x + 2f * one * t * p1.x.toFloat() + t * t * m2x
            val y = one * one * m1y + 2f * one * t * p1.y.toFloat() + t * t * m2y
            out.add(Offset(x, y))
            j++
        }
        p0 = p1
        p1 = p2
        i++
    }
    return out
}
