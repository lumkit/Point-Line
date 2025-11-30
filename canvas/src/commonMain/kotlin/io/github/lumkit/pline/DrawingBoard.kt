package io.github.lumkit.pline

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import io.github.lumkit.pline.graphics.drawFrame
import io.github.lumkit.pline.util.canvasDrawing
import io.github.lumkit.pline.util.canvasTransforms
import kotlin.time.ExperimentalTime

/**
 * 绘制板
 * @param modifier 修饰符
 * @param color 颜色
 * @param state 绘制状态
 * @param onDragging 拖动回调
 * @param onScaling 缩放回调
 * @param onRotating 旋转回调
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DrawingBoard(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    state: DrawingState,
    onDragging: (Density.(Offset) -> Unit)? = null,
    onScaling: (Density.(Float) -> Unit)? = null,
    onRotating: (Density.(Float) -> Unit)? = null,
) {

    Canvas(
        modifier = modifier
            .canvasDrawing(state, onDragging, onScaling, onRotating)
            .canvasTransforms(state, onDragging, onScaling, onRotating),
    ) {
        // 绘制背景
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = size,
        )

        // 绘制画布
        drawFrame(state)
    }
}
