package com.devstdvad.devicedna.core.common

import kotlin.math.abs

/**
 * Platform-agnostic formatting utilities shared by Android and iOS.
 * Uses only Kotlin stdlib — no java.util.Formatter or printf.
 */
object Formatters {

    // ── Bytes ──────────────────────────────────────────────────────────────

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "Unknown"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return "${oneDecimal(gb)} GB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        if (mb >= 1.0) return "${mb.toLong()} MB"
        val kb = bytes.toDouble() / 1024.0
        return "${kb.toLong()} KB"
    }

    fun formatBytesShort(bytes: Long): String {
        if (bytes < 0) return "?"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return "${oneDecimal(gb)}G"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return "${mb.toLong()}M"
    }

    // ── Percentage ─────────────────────────────────────────────────────────

    /** fraction 0.0–1.0 → "75%" */
    fun formatPercent(fraction: Float): String = "${(fraction * 100).toInt()}%"

    /** raw percent value 0–100 → "75%" */
    fun formatPercentInt(value: Int): String = "$value%"

    // ── Frequency ─────────────────────────────────────────────────────────

    fun formatFrequencyMhz(mhz: Int): String = when {
        mhz <= 0 -> "Unknown"
        mhz >= 1000 -> "${twoDecimals(mhz / 1000.0)} GHz"
        else -> "$mhz MHz"
    }

    fun formatFrequencyKhz(khz: Long): String = formatFrequencyMhz((khz / 1000).toInt())

    // ── Temperature ───────────────────────────────────────────────────────

    fun formatCelsius(celsius: Float): String = "${oneDecimal(celsius)}°C"

    fun formatFahrenheit(celsius: Float): String {
        val f = celsius * 9.0 / 5.0 + 32.0
        return "${oneDecimal(f)}°F"
    }

    fun noDecimals(value: Float): String = noDecimals(value.toDouble())

    fun noDecimals(value: Double): String = kotlin.math.round(value).toLong().toString()

    fun oneDecimal(value: Float): String = oneDecimal(value.toDouble())

    fun oneDecimal(value: Double): String = formatFixed(value, decimals = 1)

    fun twoDecimals(value: Float): String = twoDecimals(value.toDouble())

    fun twoDecimals(value: Double): String = formatFixed(value, decimals = 2)

    // ── Duration ──────────────────────────────────────────────────────────

    fun formatUptimeMs(millis: Long): String {
        val totalSec = millis / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatUptimeSec(seconds: Long): String = formatUptimeMs(seconds * 1000L)

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun formatFixed(value: Double, decimals: Int): String {
        val scale = when (decimals) {
            1 -> 10.0
            2 -> 100.0
            else -> 1.0
        }
        val rounded = kotlin.math.round(value * scale) / scale
        if (decimals == 0) return rounded.toLong().toString()
        val sign = if (value < 0) "-" else ""
        val abs = abs(rounded)
        val intPart = abs.toLong()
        val fractionalScale = scale.toLong()
        val fracPart = kotlin.math.round((abs - intPart) * scale).toLong()
        val fracStr = fracPart.toString().padStart(fractionalScale.toString().length - 1, '0')
        return "$sign$intPart.$fracStr"
    }
}
