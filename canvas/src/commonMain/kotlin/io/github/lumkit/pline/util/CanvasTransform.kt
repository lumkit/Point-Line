package io.github.lumkit.pline.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.*
import io.github.lumkit.pline.DrawingState
import io.github.lumkit.pline.graphics.DrawMode
import io.github.lumkit.pline.tool.ToolType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 画布视图交互（滚轮缩放/平移、键盘修饰键控制），带动画过渡。
 *
 * 约定：
 * - Ctrl/Cmd + 滚轮：以指针为中心缩放；
 * - Shift + 滚轮：水平平移；
 * - 无修饰 + 滚轮：垂直平移；
 * - 拖拽（DrawMode.Drag）：按灵敏度进行平移。
 *
 * @param state 绘制状态
 */
fun Modifier.canvasTransforms(state: DrawingState): Modifier = this
    .pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                if (state.drawMode == DrawMode.Drag) {
                    state.offset += Offset(dragAmount.x * state.panSensitivity, dragAmount.y * state.panSensitivity)
                    change.consume()
                }
            }
        )
    }
    .composed {
        val spec = tween<Float>(durationMillis = 200, easing = LinearOutSlowInEasing)
        val scaleAnim = remember { Animatable(state.scaleFactor) }
        val offsetXAnim = remember { Animatable(state.offset.x) }
        val offsetYAnim = remember { Animatable(state.offset.y) }
        var scaleTarget by remember { mutableStateOf(state.scaleFactor) }
        var offsetXTarget by remember { mutableStateOf(state.offset.x) }
        var offsetYTarget by remember { mutableStateOf(state.offset.y) }

        LaunchedEffect(scaleTarget) {
            scaleAnim.snapTo(state.scaleFactor)
            scaleAnim.animateTo(scaleTarget, spec) { state.scaleFactor = value }
        }
        LaunchedEffect(offsetXTarget) {
            offsetXAnim.snapTo(state.offset.x)
            offsetXAnim.animateTo(offsetXTarget, spec) { state.offset = Offset(value, state.offset.y) }
        }
        LaunchedEffect(offsetYTarget) {
            offsetYAnim.snapTo(state.offset.y)
            offsetYAnim.animateTo(offsetYTarget, spec) { state.offset = Offset(state.offset.x, value) }
        }
        pointerInput(state.scaleFactor, state.panSensitivity) {
            awaitPointerEventScope {
                while (true) {
                    val event: PointerEvent = awaitPointerEvent()
                    var scroll = Offset.Zero
                    event.changes.forEach { scroll += it.scrollDelta }
                    if (scroll != Offset.Zero) {
                        val mods = event.keyboardModifiers
                        val ctrlOrCmd = mods.isCtrlPressed || mods.isMetaPressed
                        val shift = mods.isShiftPressed
                        val dx = scroll.x
                        val dy = scroll.y
                        val kBase = 0.05f
                        val k = kBase
                        if (ctrlOrCmd) {
                            val delta = if (abs(dy) > 0f) dy else dx
                            val zoomRatio = (1f + (-delta) * k).coerceAtLeast(0.1f)
                            val newFactor = (state.scaleFactor * zoomRatio).coerceIn(state.minScale, state.maxScale)

                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val fw = state.frame.width.toFloat()
                            val fh = state.frame.height.toFloat()
                            val basePx = state.baseSize * this.density
                            val r = min(basePx / fw, basePx / fh)

                            val sold = r * state.scaleFactor
                            val snew = r * newFactor

                            val viewWOld = fw * sold
                            val viewHOld = fh * sold
                            val lox = (canvasW - viewWOld) / 2f + state.offset.x
                            val loy = (canvasH - viewHOld) / 2f + state.offset.y

                            val pos = event.changes.firstOrNull()?.position ?: Offset(canvasW / 2f, canvasH / 2f)
                            val wx = if (sold != 0f) (pos.x - lox) / sold else 0f
                            val wy = if (sold != 0f) (pos.y - loy) / sold else 0f

                            val viewWNew = fw * snew
                            val viewHNew = fh * snew
                            val offX = pos.x - snew * wx - (canvasW - viewWNew) / 2f
                            val offY = pos.y - snew * wy - (canvasH - viewHNew) / 2f

                            scaleTarget = newFactor
                            offsetXTarget = offX
                            offsetYTarget = offY
                        } else if (shift) {
                            val delta = if (abs(dx) > 0f) dx else dy
                            val s = state.scaleFactor
                            val amp = state.panSensitivity * (6f + 3f * s)
                            val targetX = state.offset.x + delta * amp
                            offsetXTarget = targetX
                        } else {
                            val delta = if (abs(dy) > abs(dx)) dy else dx
                            val s = state.scaleFactor
                            val amp = state.panSensitivity * (6f + 3f * s)
                            val targetY = state.offset.y + delta * amp
                            offsetYTarget = targetY
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }

/**
 * 绘制交互（单指/鼠标绘制；双指平移缩放旋转）。
 *
 * 规则：
 * - DrawMode.Draw 且工具为 Brush/Eraser 时，单指/左键进行绘制；
 * - 两指/多指手势时进行平移、缩放与旋转；
 * - 小位移抖动通过 `drawTapSlopViewPx` 抑制。
 *
 * @param state 绘制状态
 */
fun Modifier.canvasDrawing(state: DrawingState): Modifier = this
    .pointerInput(state.drawMode, state.scaleFactor) {
        awaitPointerEventScope {
            var drawing = false
            var pendingStart = false
            var startPos = Offset.Zero
            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (state.drawMode != DrawMode.Draw || (state.currentTool != ToolType.Brush && state.currentTool != ToolType.Eraser)) {
                    if (drawing) {
                        state.endDraw()
                        drawing = false
                    }
                    continue
                }
                if (pressed.size == 1) {
                    val pos = pressed[0].position
                    val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    if (!drawing) {
                        if (!pendingStart) {
                            pendingStart = true
                            startPos = pos
                        } else {
                            val dx = pos.x - startPos.x
                            val dy = pos.y - startPos.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist >= state.drawTapSlopViewPx) {
                                state.beginDraw(pos, canvasSize, this.density)
                                drawing = true
                                pendingStart = false
                            }
                        }
                    } else {
                        state.appendDraw(pos, canvasSize, this.density)
                    }
                    pressed[0].consume()
                } else {
                    pendingStart = false
                    if (drawing) {
                        state.endDraw()
                        drawing = false
                    }
                }
            }
        }
    }
    .pointerInput(state.drawMode) {
        awaitPointerEventScope {
            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val p0Prev = pressed[0].previousPosition
                    val p1Prev = pressed[1].previousPosition
                    val p0 = pressed[0].position
                    val p1 = pressed[1].position

                    val prevCentroid = (p0Prev + p1Prev) / 2f
                    val currCentroid = (p0 + p1) / 2f
                    val pan = currCentroid - prevCentroid

                    val vxPrev = p1Prev - p0Prev
                    val vxCurr = p1 - p0
                    val prevDist = sqrt(vxPrev.x * vxPrev.x + vxPrev.y * vxPrev.y)
                    val currDist = sqrt(vxCurr.x * vxCurr.x + vxCurr.y * vxCurr.y)
                    val zoomRatio = if (prevDist > 0f) (currDist / prevDist) else 1f
                    val prevAngle = atan2(vxPrev.y.toDouble(), vxPrev.x.toDouble())
                    val currAngle = atan2(vxCurr.y.toDouble(), vxCurr.x.toDouble())
                    val rotDelta =
                        Math.shortestAngleDelta(prevAngle.toFloat(), currAngle.toFloat()) * state.rotationSensitivity

                    val canvasW = size.width.toFloat()
                    val canvasH = size.height.toFloat()
                    val fw = state.frame.width.toFloat()
                    val fh = state.frame.height.toFloat()
                    val basePx = state.baseSize * this.density
                    val r = min(basePx / fw, basePx / fh)

                    val sold = r * state.scaleFactor
                    val snewFactor = (state.scaleFactor * zoomRatio).coerceIn(state.minScale, state.maxScale)
                    val snew = r * snewFactor

                    val viewWOld = fw * sold
                    val viewHOld = fh * sold
                    val lox = (canvasW - viewWOld) / 2f + state.offset.x
                    val loy = (canvasH - viewHOld) / 2f + state.offset.y
                    val pivotOldX = lox + viewWOld / 2f
                    val pivotOldY = loy + viewHOld / 2f

                    val pivotPanX = pivotOldX + pan.x
                    val pivotPanY = pivotOldY + pan.y

                    val ratio = if (sold != 0f) snew / sold else 1f
                    val dx = pivotPanX - currCentroid.x
                    val dy = pivotPanY - currCentroid.y
                    val dxs = dx * ratio
                    val dys = dy * ratio
                    val dxr = dxs * cos(rotDelta) - dys * sin(rotDelta)
                    val dyr = dxs * sin(rotDelta) + dys * cos(rotDelta)
                    val lnxPivot = currCentroid.x + dxr
                    val lnyPivot = currCentroid.y + dyr

                    val viewWNew = fw * snew
                    val viewHNew = fh * snew
                    val offX = lnxPivot - (canvasW - viewWNew) / 2f - viewWNew / 2f
                    val offY = lnyPivot - (canvasH - viewHNew) / 2f - viewHNew / 2f

                    state.scaleFactor = snewFactor
                    state.rotationRad += rotDelta
                    state.offset = Offset(offX, offY)

                    pressed.forEach { it.consume() }
                }
            }
        }
    }
