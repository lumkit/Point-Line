package io.github.lumkit.pline.graphics

import io.github.lumkit.pline.style.FillRule
import kotlinx.serialization.Serializable

/**
 * 笔划实体，描述一次连续绘制产生的几何与表现参数（帧坐标系）。
 *
 * 约定：`points` 中的坐标均为帧坐标（像素），与视图坐标无关；
 * 具体的显示/栅格化由上层在 `DrawScope` 或 `Canvas` 中结合缩放、平移进行。
 *
 * @property id 笔划唯一标识，用于撤销/重做、选择等场景
 * @property points 采样点序列（帧坐标，单位像素），时间顺序排列
 * @property color 颜色（ARGB，0xAARRGGBB）
 * @property width 线宽，单位为帧像素
 * @property smooth 是否采用平滑采样重建路径
 * @property isEraser 是否为橡皮擦笔划（影响混合模式与遮罩）
 * @property fillRule 填充规则，主要用于路径模型
 * @property model 笔划渲染模型，路径/贴章
 * @property shape 贴章形状（圆/方）
 * @property spacing 贴章间距（相对宽度的比例系数）
 * @property opacity 不透明度 0..1
 * @property velocitySpacingScale 速度影响的额外间距比例
 * @property shaderKey 着色器键（如线性/径向渐变等）
 * @property softEdge 是否软边（贴章以径向渐变模拟羽化）
 * @property softEdgeFeather 软边羽化强度 0..1
 * @property angleFollow 贴章是否随运动方向旋转
 * @property angleSmooth 角度变化的平滑系数 0..1
 */
@Serializable
data class Stroke(
    val id: String,
    val points: List<Point>,
    val color: Long,
    val width: Float,
    val smooth: Boolean = false,
    val isEraser: Boolean = false,
    val fillRule: FillRule = FillRule.NonZero,
    val model: StrokeModel = StrokeModel.Path,
    val shape: BrushShapeType = BrushShapeType.Circle,
    val spacing: Float = 0f,
    val opacity: Float = 1f,
    val velocitySpacingScale: Float = 0f,
    val shaderKey: String? = null,
    val softEdge: Boolean = false,
    val softEdgeFeather: Float = 0.5f,
    val angleFollow: Boolean = false,
    val angleSmooth: Float = 0.3f,
)
