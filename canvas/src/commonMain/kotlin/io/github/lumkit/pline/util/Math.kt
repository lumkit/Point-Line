package io.github.lumkit.pline.util

import kotlin.math.PI

/**
 * 数学工具集（角度、投影与距离计算）。
 */
object Math {
    /**
     * 计算从 current 到 target 的最短角度差（弧度，范围 [-PI, PI]）。
     */
    fun shortestAngleDelta(current: Float, target: Float): Float {
        val pi = PI.toFloat()
        val twoPi = 2f * pi
        return ((target - current + pi) % twoPi) - pi
    }

    /**
     * 点到线段投影参数（0..1）。
     */
    fun projectParamOnSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        return if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
    }

    /**
     * 点到线段距离平方。
     */
    fun pointSegmentDistanceSquared(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val t = projectParamOnSegment(px, py, ax, ay, bx, by)
        val projX = ax + t * dx
        val projY = ay + t * dy
        val ddx = px - projX
        val ddy = py - projY
        return ddx * ddx + ddy * ddy
    }

    /**
     * 点到线段投影参数（0..1）。
     */
    fun projectParamOnSegment(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        return if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
    }

    /**
     * 点到线段距离平方。
     */
    fun pointSegmentDistanceSquared(px: Double, py: Double, ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val t = projectParamOnSegment(px, py, ax, ay, bx, by)
        val projX = ax + t * dx
        val projY = ay + t * dy
        val ddx = px - projX
        val ddy = py - projY
        return ddx * ddx + ddy * ddy
    }
}
