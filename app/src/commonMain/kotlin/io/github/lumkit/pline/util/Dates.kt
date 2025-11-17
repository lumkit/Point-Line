package io.github.lumkit.pline.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 格式化时间戳（支持毫秒/秒输入），简单 pattern 支持:
 * yyyy MM dd HH mm ss
 *
 * @param timestamp 时间戳，默认认为是毫秒。如果 isSeconds=true 则视为秒数（会 *1000）。
 * @param pattern 支持的 token: "yyyy","MM","dd","HH","mm","ss". 例如 "yyyy-MM-dd HH:mm:ss"
 * @param tzId IANA 时区 ID (例如 "Asia/Tokyo")，若为 null 使用系统默认时区
 */
@OptIn(ExperimentalTime::class)
fun formatTimestamp(
    timestamp: Long,
    pattern: String = "yyyy-MM-dd HH:mm:ss",
    isSeconds: Boolean = false,
    tzId: String? = null
): String {
    val millis = if (isSeconds) timestamp * 1000L else timestamp
    val instant = Instant.fromEpochMilliseconds(millis)

    val zone = try {
        if (tzId.isNullOrBlank()) TimeZone.currentSystemDefault()
        else TimeZone.of(tzId)
    } catch (e: Exception) {
        // 时区字符串可能无效，回退到系统默认
        TimeZone.currentSystemDefault()
    }

    val ldt = instant.toLocalDateTime(zone)

    // helper: pad
    fun Int.pad2() = this.toString().padStart(2, '0')
    fun Int.pad4() = this.toString().padStart(4, '0')

    // map tokens to values
    val replacements = mapOf(
        "yyyy" to ldt.year.pad4(),
        "MM" to ldt.month.number.pad2(),
        "dd" to ldt.day.pad2(),
        "HH" to ldt.hour.pad2(),
        "mm" to ldt.minute.pad2(),
        "ss" to ldt.second.pad2()
    )

    var out = pattern
    replacements.keys.sortedByDescending { it.length }.forEach { key ->
        out = out.replace(key, replacements.getValue(key))
    }
    return out
}
