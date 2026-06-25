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
        if (gb >= 1.0) return "${formatDecimal1(gb)} GB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        if (mb >= 1.0) return "${mb.toLong()} MB"
        val kb = bytes.toDouble() / 1024.0
        return "${kb.toLong()} KB"
    }

    fun formatBytesShort(bytes: Long): String {
        if (bytes < 0) return "?"
        val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) return "${formatDecimal1(gb)}G"
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
        mhz >= 1000 -> "${formatDecimal2(mhz / 1000.0)} GHz"
        else -> "$mhz MHz"
    }

    fun formatFrequencyKhz(khz: Long): String = formatFrequencyMhz((khz / 1000).toInt())

    // ── Temperature ───────────────────────────────────────────────────────

    fun formatCelsius(celsius: Float): String = "${formatDecimal1(celsius.toDouble())}°C"

    fun formatFahrenheit(celsius: Float): String {
        val f = celsius * 9.0 / 5.0 + 32.0
        return "${formatDecimal1(f)}°F"
    }

    // ── Duration ──────────────────────────────────────────────────────────

    fun formatUptimeMs(millis: Long): String {
        val totalSec = millis / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatUptimeSec(seconds: Long): String = formatUptimeMs(seconds * 1000L)

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun formatDecimal1(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val abs = abs(value)
        val intPart = abs.toLong()
        val fracPart = ((abs - intPart) * 10).toLong()
        return "$sign$intPart.$fracPart"
    }

    private fun formatDecimal2(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val abs = abs(value)
        val intPart = abs.toLong()
        val fracPart = ((abs - intPart) * 100).toLong()
        val fracStr = if (fracPart < 10) "0$fracPart" else "$fracPart"
        return "$sign$intPart.$fracStr"
    }
}
