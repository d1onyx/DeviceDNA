package com.devstdvad.devicedna.presentation.batteryintelligence

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryCyclePoint
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHistoryEntry
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHourSlot
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingMinuteSegment
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingSessionSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class BatteryAnalyticsExportManager(
    private val context: Context,
) {
    suspend fun export(report: BatteryIntelligenceReport, format: ExportFormat): Result<Uri> = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = format.name.lowercase(Locale.US)
        val file = File(context.cacheDir, "DeviceDNA_BatteryAnalytics_$timestamp.$extension")

        withContext(Dispatchers.IO) {
            file.writeText(
                when (format) {
                    ExportFormat.Json -> renderJson(report)
                    ExportFormat.Csv -> renderCsv(report)
                    ExportFormat.Txt -> renderTxt(report)
                },
            )
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(uri: Uri, format: ExportFormat): Intent {
        val mimeType = when (format) {
            ExportFormat.Json -> "application/json"
            ExportFormat.Csv -> "text/csv"
            ExportFormat.Txt -> "text/plain"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DeviceDNA Battery Analytics")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun renderJson(report: BatteryIntelligenceReport): String {
        val json = JSONObject()
            .put("exported_at", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            .put("selected_day_label", report.selectedDayLabel)
            .put("selected_day_range", report.selectedDayRange)
            .put(
                "battery_health",
                JSONObject()
                    .put("health_score", report.healthScore)
                    .put("degradation_risk_percent", report.degradationRiskPercent)
                    .put("degradation_risk_label", report.degradationRiskLabel)
                    .put("degradation_summary", report.degradationSummary),
            )
            .put(
                "charge_speed",
                JSONObject()
                    .putNullable("current_watts", report.chargeSpeed.currentWatts)
                    .putNullable("average_watts", report.chargeSpeed.averageWatts)
                    .putNullable("peak_watts", report.chargeSpeed.peakWatts)
                    .putNullable("percent_per_hour", report.chargeSpeed.percentPerHour)
                    .put("charging_sessions", report.chargeSpeed.chargingSessions),
            )
            .put(
                "cycle_stats",
                JSONObject()
                    .putNullable("current_cycles", report.cycleStats.currentCycles)
                    .putNullable("cycle_delta", report.cycleStats.cycleDelta)
                    .put("source", report.cycleStats.source.name)
                    .put("partial_cycle_percent", report.cycleStats.partialCyclePercent)
                    .put("tracked_samples", report.cycleStats.trackedSamples),
            )
            .put("charging_advice", JSONArray(report.chargingAdvice.map { context.getString(it.stringRes) }))
            .put("hourly_timeline", JSONArray(report.hourlyTimeline.map { it.toJson() }))
            .put("charging_periods", JSONArray(report.dailyChargingSessions.map { it.toJson() }))
            .put("selected_hour", report.selectedHour)
            .put("selected_hour_history", JSONArray(report.selectedHourHistory.map { it.toJson() }))
            .put("cycle_history", JSONArray(report.cycleHistory.map { it.toJson() }))

        return json.toString(2)
    }

    private fun renderCsv(report: BatteryIntelligenceReport): String {
        val rows = mutableListOf<CsvRow>()
        rows += CsvRow("meta", "exported_at", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        rows += CsvRow("meta", "selected_day_label", report.selectedDayLabel)
        rows += CsvRow("meta", "selected_day_range", report.selectedDayRange)
        rows += CsvRow("health", "health_score", report.healthScore.toString())
        rows += CsvRow("health", "degradation_risk_percent", report.degradationRiskPercent.toString())
        rows += CsvRow("health", "degradation_risk_label", report.degradationRiskLabel)
        rows += CsvRow("health", "degradation_summary", report.degradationSummary)
        rows += CsvRow("charge_speed", "current_watts", report.chargeSpeed.currentWatts.formatNullable())
        rows += CsvRow("charge_speed", "average_watts", report.chargeSpeed.averageWatts.formatNullable())
        rows += CsvRow("charge_speed", "peak_watts", report.chargeSpeed.peakWatts.formatNullable())
        rows += CsvRow("charge_speed", "percent_per_hour", report.chargeSpeed.percentPerHour.formatNullable())
        rows += CsvRow("charge_speed", "charging_sessions", report.chargeSpeed.chargingSessions.toString())
        rows += CsvRow("cycle_stats", "current_cycles", report.cycleStats.currentCycles?.toString() ?: "n/a")
        rows += CsvRow("cycle_stats", "cycle_delta", report.cycleStats.cycleDelta?.toString() ?: "n/a")
        rows += CsvRow("cycle_stats", "source", report.cycleStats.source.name)
        rows += CsvRow("cycle_stats", "partial_cycle_percent", report.cycleStats.partialCyclePercent.toString())
        rows += CsvRow("cycle_stats", "tracked_samples", report.cycleStats.trackedSamples.toString())
        report.chargingAdvice.forEachIndexed { index, advice ->
            rows += CsvRow("charging_advice", "advice_${index + 1}", context.getString(advice.stringRes))
        }
        report.hourlyTimeline.forEach { slot ->
            val prefix = "hour_${slot.hour.toString().padStart(2, '0')}"
            rows += CsvRow("hourly_timeline", "$prefix.status", slot.status.name)
            rows += CsvRow("hourly_timeline", "$prefix.sample_count", slot.sampleCount.toString())
            rows += CsvRow("hourly_timeline", "$prefix.good_minutes", slot.goodMinutes.format())
            rows += CsvRow("hourly_timeline", "$prefix.poor_minutes", slot.poorMinutes.format())
            rows += CsvRow("hourly_timeline", "$prefix.discharge_minutes", slot.dischargeMinutes.format())
            rows += CsvRow("hourly_timeline", "$prefix.stable_minutes", slot.stableMinutes.format())
            rows += CsvRow("hourly_timeline", "$prefix.average_watts", slot.averageWatts.formatNullable())
            rows += CsvRow("hourly_timeline", "$prefix.min_level_percent", slot.minLevelPercent?.toString() ?: "n/a")
            rows += CsvRow("hourly_timeline", "$prefix.max_level_percent", slot.maxLevelPercent?.toString() ?: "n/a")
        }
        report.dailyChargingSessions.forEachIndexed { index, session ->
            val prefix = "period_${index + 1}"
            rows += CsvRow("charging_periods", "$prefix.started", session.startLabel)
            rows += CsvRow("charging_periods", "$prefix.ended", session.endLabel)
            rows += CsvRow("charging_periods", "$prefix.duration", session.durationLabel)
            rows += CsvRow("charging_periods", "$prefix.level_delta_percent", session.levelDeltaPercent.toString())
            rows += CsvRow("charging_periods", "$prefix.average_watts", session.averageWatts.formatNullable())
            rows += CsvRow("charging_periods", "$prefix.peak_watts", session.peakWatts.formatNullable())
            rows += CsvRow("charging_periods", "$prefix.max_temperature_celsius", session.maxTemperatureCelsius.format())
            rows += CsvRow("charging_periods", "$prefix.needs_advice", session.needsAdvice.toString())
        }
        report.selectedHourHistory.forEachIndexed { index, entry ->
            val prefix = "entry_${index + 1}"
            rows += CsvRow("selected_hour_history", "$prefix.time", entry.label)
            rows += CsvRow("selected_hour_history", "$prefix.level_percent", entry.levelPercent.toString())
            rows += CsvRow("selected_hour_history", "$prefix.status", entry.status)
            rows += CsvRow("selected_hour_history", "$prefix.watts", entry.watts.formatNullable())
            rows += CsvRow("selected_hour_history", "$prefix.temperature_celsius", entry.temperatureCelsius.format())
            rows += CsvRow("selected_hour_history", "$prefix.source", entry.source)
        }
        report.cycleHistory.forEachIndexed { index, point ->
            rows += CsvRow("cycle_history", "point_${index + 1}.${point.label}", point.cycles.toString())
        }

        return buildString {
            appendLine("section,key,value")
            rows.forEach { row ->
                append(row.section.csv())
                    .append(',')
                    .append(row.key.csv())
                    .append(',')
                    .append(row.value.csv())
                    .append('\n')
            }
        }
    }

    private fun renderTxt(report: BatteryIntelligenceReport): String = buildString {
        appendLine("DeviceDNA Battery Analytics")
        appendLine("Generated: ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}")
        appendLine("Day: ${report.selectedDayLabel} (${report.selectedDayRange})")
        appendLine("=".repeat(42))
        appendLine()
        appendLine("[HEALTH]")
        appendLine("Health score: ${report.healthScore}")
        appendLine("Degradation risk: ${report.degradationRiskPercent}% (${report.degradationRiskLabel})")
        appendLine(report.degradationSummary)
        appendLine()
        appendLine("[CHARGE SPEED]")
        appendLine("Current: ${report.chargeSpeed.currentWatts.formatNullable()} W")
        appendLine("Average: ${report.chargeSpeed.averageWatts.formatNullable()} W")
        appendLine("Peak: ${report.chargeSpeed.peakWatts.formatNullable()} W")
        appendLine("Rate: ${report.chargeSpeed.percentPerHour.formatNullable()}%/h")
        appendLine("Sessions: ${report.chargeSpeed.chargingSessions}")
        appendLine()
        appendLine("[CYCLES]")
        appendLine("Source: ${report.cycleStats.source.name}")
        appendLine("Current cycles: ${report.cycleStats.currentCycles?.toString() ?: "n/a"}")
        appendLine("Cycle delta: ${report.cycleStats.cycleDelta?.toString() ?: "n/a"}")
        appendLine("Next cycle progress: ${report.cycleStats.partialCyclePercent}%")
        appendLine("Tracked samples: ${report.cycleStats.trackedSamples}")
        appendLine()
        appendLine("[CHARGING ADVICE]")
        report.chargingAdvice.forEachIndexed { index, advice ->
            appendLine("${index + 1}. ${context.getString(advice.stringRes)}")
        }
        appendLine()
        appendLine("[HOURLY TIMELINE]")
        report.hourlyTimeline.forEach { slot ->
            appendLine(
                "${slot.hour.toString().padStart(2, '0')}:00 " +
                    "${slot.status.name}, good ${slot.goodMinutes.format()}m, " +
                    "poor ${slot.poorMinutes.format()}m, discharge ${slot.dischargeMinutes.format()}m, " +
                    "stable ${slot.stableMinutes.format()}m, avg ${slot.averageWatts.formatNullable()}W",
            )
        }
        appendLine()
        appendLine("[CHARGING PERIODS]")
        if (report.dailyChargingSessions.isEmpty()) {
            appendLine("No charging periods recorded.")
        } else {
            report.dailyChargingSessions.forEach { session ->
                appendLine(
                    "${session.startLabel} - ${session.endLabel}: ${session.durationLabel}, " +
                        "${session.startLevelPercent}% to ${session.endLevelPercent}% " +
                        "(${session.levelDeltaPercent}%), avg ${session.averageWatts.formatNullable()}W, " +
                        "peak ${session.peakWatts.formatNullable()}W",
                )
            }
        }
        appendLine()
        appendLine("[SELECTED HOUR HISTORY ${report.selectedHour.toString().padStart(2, '0')}:00]")
        if (report.selectedHourHistory.isEmpty()) {
            appendLine("No entries recorded.")
        } else {
            report.selectedHourHistory.forEach { entry ->
                appendLine(
                    "${entry.label}: ${entry.levelPercent}%, ${entry.status}, " +
                        "${entry.watts.formatNullable()}W, ${entry.temperatureCelsius.format()}C, ${entry.source}",
                )
            }
        }
        appendLine()
        appendLine("[CYCLE HISTORY]")
        if (report.cycleHistory.isEmpty()) {
            appendLine("No cycle history recorded.")
        } else {
            report.cycleHistory.forEach { point ->
                appendLine("${point.label}: ${point.cycles}")
            }
        }
    }
}

private data class CsvRow(
    val section: String,
    val key: String,
    val value: String,
)

private fun ChargingHourSlot.toJson(): JSONObject = JSONObject()
    .put("hour", hour)
    .put("status", status.name)
    .put("sample_count", sampleCount)
    .put("good_minutes", goodMinutes)
    .put("poor_minutes", poorMinutes)
    .put("discharge_minutes", dischargeMinutes)
    .put("stable_minutes", stableMinutes)
    .putNullable("average_watts", averageWatts)
    .putNullable("min_level_percent", minLevelPercent)
    .putNullable("max_level_percent", maxLevelPercent)
    .put("segments", JSONArray(segments.map { it.toJson() }))

private fun ChargingMinuteSegment.toJson(): JSONObject = JSONObject()
    .put("start_minute", startMinute)
    .put("duration_minutes", durationMinutes)
    .put("status", status.name)

private fun ChargingSessionSummary.toJson(): JSONObject = JSONObject()
    .put("start_time", startMillis.isoTime())
    .putNullable("end_time", endMillis?.isoTime())
    .put("start_label", startLabel)
    .put("end_label", endLabel)
    .put("duration", durationLabel)
    .put("start_level_percent", startLevelPercent)
    .put("end_level_percent", endLevelPercent)
    .put("level_delta_percent", levelDeltaPercent)
    .putNullable("average_watts", averageWatts)
    .putNullable("peak_watts", peakWatts)
    .put("max_temperature_celsius", maxTemperatureCelsius)
    .put("needs_advice", needsAdvice)
    .put("sample_count", sampleCount)

private fun ChargingHistoryEntry.toJson(): JSONObject = JSONObject()
    .put("time", timestampMillis.isoTime())
    .put("label", label)
    .put("level_percent", levelPercent)
    .put("status", status)
    .putNullable("watts", watts)
    .put("temperature_celsius", temperatureCelsius)
    .put("source", source)

private fun BatteryCyclePoint.toJson(): JSONObject = JSONObject()
    .put("label", label)
    .put("cycles", cycles)

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
    put(name, value ?: JSONObject.NULL)

private fun Long.isoTime(): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private fun Float?.formatNullable(): String =
    this?.format() ?: "n/a"

private fun Float.format(): String =
    "%.2f".format(Locale.US, this)

private fun String.csv(): String =
    "\"${replace("\"", "\"\"")}\""
