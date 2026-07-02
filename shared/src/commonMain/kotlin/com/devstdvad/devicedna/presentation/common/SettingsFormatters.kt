package com.devstdvad.devicedna.presentation.common

import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.TemperatureUnit

object SettingsFormatters {

    fun formatTemperature(celsius: Float, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.Celsius -> celsius
            TemperatureUnit.Fahrenheit -> celsius * 9f / 5f + 32f
        }
        val suffix = when (unit) {
            TemperatureUnit.Celsius -> "C"
            TemperatureUnit.Fahrenheit -> "F"
        }
        return "${Formatters.oneDecimal(value)}°$suffix"
    }

    fun formatTemperatureWhole(celsius: Float, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.Celsius -> celsius
            TemperatureUnit.Fahrenheit -> celsius * 9f / 5f + 32f
        }
        val suffix = when (unit) {
            TemperatureUnit.Celsius -> "C"
            TemperatureUnit.Fahrenheit -> "F"
        }
        return "${Formatters.noDecimals(value)}°$suffix"
    }

    fun formatBytes(bytes: Long, unit: DataUnit): String {
        if (bytes < 0L) return "Unknown"
        val base = base(unit)
        val labels = labels(unit)
        val value = bytes.toDouble()
        return when {
            value >= base * base * base -> "${Formatters.oneDecimal(value / (base * base * base))} ${labels[2]}"
            value >= base * base -> "${Formatters.oneDecimal(value / (base * base))} ${labels[1]}"
            value >= base -> "${Formatters.oneDecimal(value / base)} ${labels[0]}"
            else -> "$bytes B"
        }
    }

    fun formatBytesShort(bytes: Long, unit: DataUnit): String {
        if (bytes < 0L) return "?"
        val base = base(unit)
        val labels = shortLabels(unit)
        val value = bytes.toDouble()
        return when {
            value >= base * base * base -> "${Formatters.oneDecimal(value / (base * base * base))}${labels[2]}"
            value >= base * base -> "${Formatters.oneDecimal(value / (base * base))}${labels[1]}"
            value >= base -> "${Formatters.noDecimals(value / base)}${labels[0]}"
            else -> "${bytes}B"
        }
    }

    fun formatBytesPerSecond(bytesPerSecond: Long, unit: DataUnit): String =
        "${formatBytes(bytesPerSecond, unit)}/s"

    private fun base(unit: DataUnit): Double = when (unit) {
        DataUnit.GB -> 1000.0
        DataUnit.GiB -> 1024.0
    }

    private fun labels(unit: DataUnit): List<String> = when (unit) {
        DataUnit.GB -> listOf("KB", "MB", "GB")
        DataUnit.GiB -> listOf("KiB", "MiB", "GiB")
    }

    private fun shortLabels(unit: DataUnit): List<String> = when (unit) {
        DataUnit.GB -> listOf("K", "M", "G")
        DataUnit.GiB -> listOf("Ki", "Mi", "Gi")
    }
}
