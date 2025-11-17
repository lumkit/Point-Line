package io.github.lumkit.pline.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.lumkit.pline.DrawingState
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.min
import androidx.compose.ui.geometry.Rect as GRect

@Serializable
data class Frame(
    val color: Long,
    val width: Int,
    val height: Int,
) {
    /**
     * 常用帧模板。
     * - `Square`：4096x4096，浅灰背景；
     * - `Transparent`：4096x4096，透明背景。
     */
    companion object {
        val Square = Frame(
            color = 0xFFF5F5F5L,
            width = 4096,
            height = 4096,
        )

        val Transparent = Frame(
            color = 0x00F5F5F5L,
            width = 4096,
            height = 4096,
        )
    }
}

/**
 * 绘制画布（背景与已栅格化内容），并按状态进行缩放/平移/旋转。
 * @param state 绘制状态，包含帧尺寸、缩放、偏移等
 */
fun DrawScope.drawFrame(state: DrawingState) {
    val base = state.baseSize.dp.toPx()
    val fw = state.frame.width.toFloat()
    val fh = state.frame.height.toFloat()
    val scale = min(base / fw, base / fh) * state.scaleFactor
    val viewW = fw * scale
    val viewH = fh * scale
    val topLeftX = (size.width - viewW) / 2f + state.offset.x
    val topLeftY = (size.height - viewH) / 2f + state.offset.y
    val pivotX = topLeftX + viewW / 2f
    val pivotY = topLeftY + viewH / 2f
    val rotationDeg = state.rotationRad * 180f / PI.toFloat()

    withTransform({ rotate(rotationDeg, pivot = Offset(pivotX, pivotY)) }) {
        drawRect(
            color = Color(state.frame.color),
            topLeft = Offset(topLeftX, topLeftY),
            size = Size(viewW, viewH)
        )

        val clipLeft = topLeftX
        val clipTop = topLeftY
        val clipRight = topLeftX + viewW
        val clipBottom = topLeftY + viewH

        drawStrokes(state, scale, topLeftX, topLeftY, clipLeft, clipTop, clipRight, clipBottom)
    }
}

/**
 * 绘制笔划层（包含离屏复合图与当前待提交的笔划）。
 */
private fun DrawScope.drawStrokes(
    state: DrawingState,
    scale: Float,
    ox: Float,
    oy: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    clipRect(left, top, right, bottom) {
        drawIntoCanvas { it.saveLayer(GRect(left, top, right, bottom), Paint()) }
        val bmp = state.backingBitmap
        if (bmp != null) {
            withTransform({ scale(scaleX = scale, scaleY = scale, pivot = Offset(ox, oy)) }) {
                drawImage(
                    image = bmp,
                    topLeft = Offset(ox, oy)
                )
            }
        } else if (state.tiles.isNotEmpty()) {
            withTransform({ scale(scaleX = scale, scaleY = scale, pivot = Offset(ox, oy)) }) {
                state.tiles.forEach { t ->
                    drawImage(
                        image = t.bitmap,
                        topLeft = Offset(ox + t.originX.toFloat(), oy + t.originY.toFloat()),
//                        dstOffset = androidx.compose.ui.unit.IntOffset(
//                            (ox + t.originX.toFloat()).toInt(),
//                            (oy + t.originY.toFloat()).toInt()
//                        ),
//                        dstSize = androidx.compose.ui.unit.IntSize(t.width, t.height),
//                        filterQuality = FilterQuality.None
                    )
                }
            }
        }
        val cur = state.pendingStroke
        if (cur != null) {
            if (cur.model == StrokeModel.Stamp) {
                drawStampStroke(cur, ox, oy, scale, left, top, right, bottom)
            } else {
                val path = Path()
                val pts = cur.points
                if (pts.isNotEmpty()) {
                    val useSmooth = state.smoothLine && pts.size >= 3
                    if (useSmooth) {
                        val step = kotlin.math.max(1f, cur.width * 0.5f)
                        val samples = sampleSmoothFramePoints(pts, step)
                        if (samples.isNotEmpty()) {
                            val p0 = samples.first()
                            path.moveTo(ox + (p0.x * scale), oy + (p0.y * scale))
                            var i = 1
                            while (i < samples.size) {
                                val p = samples[i]
                                path.lineTo(ox + (p.x * scale), oy + (p.y * scale))
                                i++
                            }
                        }
                    } else {
                        val p0 = pts.first()
                        path.moveTo(ox + (p0.x.toFloat() * scale), oy + (p0.y.toFloat() * scale))
                        var i = 1
                        while (i < pts.size) {
                            val p = pts[i]
                            path.lineTo(ox + (p.x.toFloat() * scale), oy + (p.y.toFloat() * scale))
                            i++
                        }
                    }
                    val brush = gradientBrushFor(cur)
                    val bm = if (cur.isEraser) BlendMode.DstOut else BlendMode.SrcOver
                    val style = Stroke(width = cur.width * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    if (brush != null) {
                        drawPath(path = path, brush = brush, style = style, blendMode = bm)
                    } else {
                        drawPath(path = path, color = Color(cur.color).copy(alpha = cur.opacity), style = style, blendMode = bm)
                    }
                }
            }
        }
        drawIntoCanvas { it.restore() }
    }
}
