package io.github.lumkit.pline.graphics

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 采样点（帧坐标）。
 *
 * @property x 帧坐标 X（像素）
 * @property y 帧坐标 Y（像素）
 * @property pressure 压力信息（0..1），根据设备支持可选
 * @property time 采样时间戳（毫秒）
 */
@OptIn(ExperimentalTime::class)
@Serializable
data class Point(
    val x: Double,
    val y: Double,
    val pressure: Float = 1f,
    val time: Long = Clock.System.now().toEpochMilliseconds(),
)
