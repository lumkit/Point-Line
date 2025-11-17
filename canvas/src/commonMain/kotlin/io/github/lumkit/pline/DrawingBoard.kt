package io.github.lumkit.pline

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.lumkit.pline.graphics.drawFrame
import io.github.lumkit.pline.util.canvasDrawing
import io.github.lumkit.pline.util.canvasTransforms
import kotlin.time.ExperimentalTime

/**
 * 绘制板
 * @param modifier 修饰符
 * @param color 颜色
 * @param state 绘制状态
 */
@OptIn(ExperimentalTime::class)
@Composable
fun DrawingBoard(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    state: DrawingState,
) {

    Canvas(
        modifier = modifier
            .canvasDrawing(state)
            .canvasTransforms(state),
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
