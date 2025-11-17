package io.github.lumkit.pline.graphics

import kotlinx.serialization.Serializable

/**
 * 图层数据（可选）。通常用于组织多个笔划集合及其相对位移。
 * @property strokes 图层包含的笔划集合
 * @property color 图层基色（可用于特殊渲染）
 * @property width 图层宽度（像素）
 * @property height 图层高度（像素）
 * @property offsetX 图层相对帧的 X 偏移
 * @property offsetY 图层相对帧的 Y 偏移
 */
@Serializable
data class Layer(
    val strokes: List<Stroke>,
    val color: Long,
    val width: Int,
    val height: Int,
    val offsetX: Float,
    val offsetY: Float,
)
