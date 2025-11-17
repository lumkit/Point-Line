package io.github.lumkit.pline.graphics

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable

/**
 * 3x3 齐次矩阵，支持平移/缩放/旋转以及坐标变换。
 */
@Serializable
data class Matrix3x3(
    val m00: Float, val m01: Float, val m02: Float,
    val m10: Float, val m11: Float, val m12: Float,
    val m20: Float, val m21: Float, val m22: Float,
) {
    /**
     * 矩阵相乘（右乘）。
     */
    fun times(other: Matrix3x3): Matrix3x3 = Matrix3x3(
        m00 * other.m00 + m01 * other.m10 + m02 * other.m20,
        m00 * other.m01 + m01 * other.m11 + m02 * other.m21,
        m00 * other.m02 + m01 * other.m12 + m02 * other.m22,
        m10 * other.m00 + m11 * other.m10 + m12 * other.m20,
        m10 * other.m01 + m11 * other.m11 + m12 * other.m21,
        m10 * other.m02 + m11 * other.m12 + m12 * other.m22,
        m20 * other.m00 + m21 * other.m10 + m22 * other.m20,
        m20 * other.m01 + m21 * other.m11 + m22 * other.m21,
        m20 * other.m02 + m21 * other.m12 + m22 * other.m22,
    )

    /**
     * 将矩阵作用于点（齐次坐标）。
     */
    fun apply(p: Offset): Offset {
        val x = p.x
        val y = p.y
        val nx = m00 * x + m01 * y + m02
        val ny = m10 * x + m11 * y + m12
        val w = m20 * x + m21 * y + m22
        return if (w != 0f) Offset(nx / w, ny / w) else Offset(nx, ny)
    }

    companion object {
        /** 单位矩阵 */
        fun identity() = Matrix3x3(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f,
        )

        /** 平移矩阵 */
        fun translation(tx: Float, ty: Float) = Matrix3x3(
            1f, 0f, tx,
            0f, 1f, ty,
            0f, 0f, 1f,
        )

        /** 缩放矩阵 */
        fun scale(sx: Float, sy: Float) = Matrix3x3(
            sx, 0f, 0f,
            0f, sy, 0f,
            0f, 0f, 1f,
        )

        /** 旋转矩阵（弧度） */
        fun rotation(rad: Float) = Matrix3x3(
            kotlin.math.cos(rad), -kotlin.math.sin(rad), 0f,
            kotlin.math.sin(rad),  kotlin.math.cos(rad), 0f,
            0f, 0f, 1f,
        )

        /** 围绕给定枢轴的旋转矩阵（弧度） */
        fun rotateAround(rad: Float, pivot: Offset): Matrix3x3 {
            val t1 = translation(pivot.x, pivot.y)
            val r = rotation(rad)
            val t2 = translation(-pivot.x, -pivot.y)
            return t1.times(r).times(t2)
        }
    }
}
