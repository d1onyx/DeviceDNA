package com.devstdvad.devicedna.presentation.batteryintelligence

import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHistoryEntry
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHourSlot
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingMinuteSegment
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingSessionSummary
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.round

/** Outcome of parsing an imported battery-analytics JSON file. */
data class ParsedBatteryImport(
    val snapshots: List<BatteryHistorySnapshot>,
    /** True when snapshots were reconstructed from report fields because the file had no raw history. */
    val degraded: Boolean,
)

/**
 * Platform-agnostic rendering + parsing for battery-analytics export/import. Pure logic (no file
 * IO): the platform layer supplies the artifact content to share (via FileSharer) and the imported
 * text to parse (via FileImporter). Ported from the Android BatteryAnalyticsExportManager, replacing
 * org.json/java.time with kotlinx.serialization/kotlinx-datetime.
 */
class BatteryAnalyticsExporter {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun mimeType(format: ExportFormat): String = when (format) {
        ExportFormat.Json -> "application/json"
        ExportFormat.Csv -> "text/csv"
        ExportFormat.Txt -> "text/plain"
    }

    fun fileName(nowMillis: Long, format: ExportFormat): String {
        val stamp = fileStamp(nowMillis)
        return "DeviceDNA_BatteryAnalytics_$stamp.${format.name.lowercase()}"
    }

    /** [adviceText] is the caller-resolved (localized) charging advice, one entry per advice. */
    fun render(
        report: BatteryIntelligenceReport,
        format: ExportFormat,
        rawSnapshots: List<BatteryHistorySnapshot>,
        adviceText: List<String>,
        nowMillis: Long,
    ): String = when (format) {
        ExportFormat.Json -> renderJson(report, rawSnapshots, adviceText, nowMillis)
        ExportFormat.Csv -> renderCsv(report, adviceText, nowMillis)
        ExportFormat.Txt -> renderTxt(report, adviceText, nowMillis)
    }

    // ── JSON ────────────────────────────────────────────────────────────────

    private fun renderJson(
        report: BatteryIntelligenceReport,
        rawSnapshots: List<BatteryHistorySnapshot>,
        adviceText: List<String>,
        nowMillis: Long,
    ): String {
        val root = buildJsonObject {
            put("exported_at", isoInstant(nowMillis))
            put("selected_day_label", report.selectedDayLabel)
            put("selected_day_range", report.selectedDayRange)
            putJsonObject("battery_health") {
                put("health_score", report.healthScore)
                put("degradation_risk_percent", report.degradationRiskPercent)
                put("degradation_risk_label", report.degradationRiskLabel)
                put("degradation_summary", report.degradationSummary)
            }
            putJsonObject("charge_speed") {
                putNullableFloat("current_watts", report.chargeSpeed.currentWatts)
                putNullableFloat("average_watts", report.chargeSpeed.averageWatts)
                putNullableFloat("peak_watts", report.chargeSpeed.peakWatts)
                putNullableFloat("percent_per_hour", report.chargeSpeed.percentPerHour)
                put("charging_sessions", report.chargeSpeed.chargingSessions)
            }
            putJsonObject("cycle_stats") {
                putNullableInt("current_cycles", report.cycleStats.currentCycles)
                putNullableInt("cycle_delta", report.cycleStats.cycleDelta)
                put("source", report.cycleStats.source.name)
                put("partial_cycle_percent", report.cycleStats.partialCyclePercent)
                put("tracked_samples", report.cycleStats.trackedSamples)
            }
            putJsonArray("charging_advice") { adviceText.forEach { add(it) } }
            putJsonArray("hourly_timeline") { report.hourlyTimeline.forEach { add(it.toJson()) } }
            putJsonArray("charging_periods") { report.dailyChargingSessions.forEach { add(it.toJson()) } }
            put("selected_hour", report.selectedHour)
            putJsonArray("selected_hour_history") { report.selectedHourHistory.forEach { add(it.toJson()) } }
            putJsonArray("cycle_history") {
                report.cycleHistory.forEach { point ->
                    addJsonObject {
                        put("label", point.label)
                        put("cycles", point.cycles)
                    }
                }
            }
            // Lossless raw history so the file can be re-imported to fully restore the timeline.
            put("raw_snapshots", json.encodeToJsonElement(rawSnapshots).jsonArray)
        }
        return json.encodeToString(JsonObject.serializer(), root)
    }

    private fun ChargingHourSlot.toJson(): JsonObject = buildJsonObject {
        put("hour", hour)
        put("status", status.name)
        put("sample_count", sampleCount)
        put("good_minutes", goodMinutes)
        put("poor_minutes", poorMinutes)
        put("discharge_minutes", dischargeMinutes)
        put("stable_minutes", stableMinutes)
        putNullableFloat("average_watts", averageWatts)
        putNullableInt("min_level_percent", minLevelPercent)
        putNullableInt("max_level_percent", maxLevelPercent)
        putJsonArray("segments") { segments.forEach { add(it.toJson()) } }
    }

    private fun ChargingMinuteSegment.toJson(): JsonObject = buildJsonObject {
        put("start_minute", startMinute)
        put("duration_minutes", durationMinutes)
        put("status", status.name)
    }

    private fun ChargingSessionSummary.toJson(): JsonObject = buildJsonObject {
        put("start_time", isoInstant(startMillis))
        if (endMillis != null) put("end_time", isoInstant(endMillis)) else put("end_time", JsonNull)
        put("start_label", startLabel)
        put("end_label", endLabel)
        put("duration", durationLabel)
        put("start_level_percent", startLevelPercent)
        put("end_level_percent", endLevelPercent)
        put("level_delta_percent", levelDeltaPercent)
        putNullableFloat("average_watts", averageWatts)
        putNullableFloat("peak_watts", peakWatts)
        put("max_temperature_celsius", maxTemperatureCelsius)
        put("needs_advice", needsAdvice)
        put("sample_count", sampleCount)
    }

    private fun ChargingHistoryEntry.toJson(): JsonObject = buildJsonObject {
        put("time", isoInstant(timestampMillis))
        put("label", label)
        put("level_percent", levelPercent)
        put("status", status)
        putNullableFloat("watts", watts)
        put("temperature_celsius", temperatureCelsius)
        put("source", source)
    }

    // ── CSV ─────────────────────────────────────────────────────────────────

    private fun renderCsv(report: BatteryIntelligenceReport, adviceText: List<String>, nowMillis: Long): String {
        val rows = mutableListOf<Triple<String, String, String>>()
        fun row(section: String, key: String, value: String) { rows += Triple(section, key, value) }

        row("meta", "exported_at", isoInstant(nowMillis))
        row("meta", "selected_day_label", report.selectedDayLabel)
        row("meta", "selected_day_range", report.selectedDayRange)
        row("health", "health_score", report.healthScore.toString())
        row("health", "degradation_risk_percent", report.degradationRiskPercent.toString())
        row("health", "degradation_risk_label", report.degradationRiskLabel)
        row("health", "degradation_summary", report.degradationSummary)
        row("charge_speed", "current_watts", report.chargeSpeed.currentWatts.fmtNullable())
        row("charge_speed", "average_watts", report.chargeSpeed.averageWatts.fmtNullable())
        row("charge_speed", "peak_watts", report.chargeSpeed.peakWatts.fmtNullable())
        row("charge_speed", "percent_per_hour", report.chargeSpeed.percentPerHour.fmtNullable())
        row("charge_speed", "charging_sessions", report.chargeSpeed.chargingSessions.toString())
        row("cycle_stats", "current_cycles", report.cycleStats.currentCycles?.toString() ?: "n/a")
        row("cycle_stats", "cycle_delta", report.cycleStats.cycleDelta?.toString() ?: "n/a")
        row("cycle_stats", "source", report.cycleStats.source.name)
        row("cycle_stats", "partial_cycle_percent", report.cycleStats.partialCyclePercent.toString())
        row("cycle_stats", "tracked_samples", report.cycleStats.trackedSamples.toString())
        adviceText.forEachIndexed { index, advice -> row("charging_advice", "advice_${index + 1}", advice) }
        report.hourlyTimeline.forEach { slot ->
            val prefix = "hour_${slot.hour.toString().padStart(2, '0')}"
            row("hourly_timeline", "$prefix.status", slot.status.name)
            row("hourly_timeline", "$prefix.sample_count", slot.sampleCount.toString())
            row("hourly_timeline", "$prefix.good_minutes", slot.goodMinutes.fmt())
            row("hourly_timeline", "$prefix.poor_minutes", slot.poorMinutes.fmt())
            row("hourly_timeline", "$prefix.discharge_minutes", slot.dischargeMinutes.fmt())
            row("hourly_timeline", "$prefix.stable_minutes", slot.stableMinutes.fmt())
            row("hourly_timeline", "$prefix.average_watts", slot.averageWatts.fmtNullable())
            row("hourly_timeline", "$prefix.min_level_percent", slot.minLevelPercent?.toString() ?: "n/a")
            row("hourly_timeline", "$prefix.max_level_percent", slot.maxLevelPercent?.toString() ?: "n/a")
        }
        report.dailyChargingSessions.forEachIndexed { index, session ->
            val prefix = "period_${index + 1}"
            row("charging_periods", "$prefix.started", session.startLabel)
            row("charging_periods", "$prefix.ended", session.endLabel)
            row("charging_periods", "$prefix.duration", session.durationLabel)
            row("charging_periods", "$prefix.level_delta_percent", session.levelDeltaPercent.toString())
            row("charging_periods", "$prefix.average_watts", session.averageWatts.fmtNullable())
            row("charging_periods", "$prefix.peak_watts", session.peakWatts.fmtNullable())
            row("charging_periods", "$prefix.max_temperature_celsius", session.maxTemperatureCelsius.fmt())
            row("charging_periods", "$prefix.needs_advice", session.needsAdvice.toString())
        }
        report.selectedHourHistory.forEachIndexed { index, entry ->
            val prefix = "entry_${index + 1}"
            row("selected_hour_history", "$prefix.time", entry.label)
            row("selected_hour_history", "$prefix.level_percent", entry.levelPercent.toString())
            row("selected_hour_history", "$prefix.status", entry.status)
            row("selected_hour_history", "$prefix.watts", entry.watts.fmtNullable())
            row("selected_hour_history", "$prefix.temperature_celsius", entry.temperatureCelsius.fmt())
            row("selected_hour_history", "$prefix.source", entry.source)
        }
        report.cycleHistory.forEachIndexed { index, point ->
            row("cycle_history", "point_${index + 1}.${point.label}", point.cycles.toString())
        }

        return buildString {
            append("section,key,value\n")
            rows.forEach { (section, key, value) ->
                append(section.csv()).append(',').append(key.csv()).append(',').append(value.csv()).append('\n')
            }
        }
    }

    // ── TXT ─────────────────────────────────────────────────────────────────

    private fun renderTxt(report: BatteryIntelligenceReport, adviceText: List<String>, nowMillis: Long): String = buildString {
        appendLine("DeviceDNA Battery Analytics")
        appendLine("Generated: ${isoInstant(nowMillis)}")
        appendLine("Day: ${report.selectedDayLabel} (${report.selectedDayRange})")
        appendLine("=".repeat(42))
        appendLine()
        appendLine("[HEALTH]")
        appendLine("Health score: ${report.healthScore}")
        appendLine("Degradation risk: ${report.degradationRiskPercent}% (${report.degradationRiskLabel})")
        appendLine(report.degradationSummary)
        appendLine()
        appendLine("[CHARGE SPEED]")
        appendLine("Current: ${report.chargeSpeed.currentWatts.fmtNullable()} W")
        appendLine("Average: ${report.chargeSpeed.averageWatts.fmtNullable()} W")
        appendLine("Peak: ${report.chargeSpeed.peakWatts.fmtNullable()} W")
        appendLine("Rate: ${report.chargeSpeed.percentPerHour.fmtNullable()}%/h")
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
        adviceText.forEachIndexed { index, advice -> appendLine("${index + 1}. $advice") }
        appendLine()
        appendLine("[HOURLY TIMELINE]")
        report.hourlyTimeline.forEach { slot ->
            appendLine(
                "${slot.hour.toString().padStart(2, '0')}:00 " +
                    "${slot.status.name}, good ${slot.goodMinutes.fmt()}m, " +
                    "poor ${slot.poorMinutes.fmt()}m, discharge ${slot.dischargeMinutes.fmt()}m, " +
                    "stable ${slot.stableMinutes.fmt()}m, avg ${slot.averageWatts.fmtNullable()}W",
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
                        "(${session.levelDeltaPercent}%), avg ${session.averageWatts.fmtNullable()}W, " +
                        "peak ${session.peakWatts.fmtNullable()}W",
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
                        "${entry.watts.fmtNullable()}W, ${entry.temperatureCelsius.fmt()}C, ${entry.source}",
                )
            }
        }
        appendLine()
        appendLine("[CYCLE HISTORY]")
        if (report.cycleHistory.isEmpty()) {
            appendLine("No cycle history recorded.")
        } else {
            report.cycleHistory.forEach { point -> appendLine("${point.label}: ${point.cycles}") }
        }
    }

    // ── Import parsing ────────────────────────────────────────────────────────

    fun parseImport(text: String): ParsedBatteryImport {
        val root = json.parseToJsonElement(text).jsonObject

        val rawArray = root["raw_snapshots"]?.jsonArray
        if (rawArray != null && rawArray.isNotEmpty()) {
            val snapshots = json.decodeFromString<List<BatteryHistorySnapshot>>(rawArray.toString())
            return ParsedBatteryImport(snapshots = snapshots, degraded = false)
        }

        val reconstructed = root["selected_hour_history"]?.jsonArray
            ?.mapNotNull { it.jsonObject.toReconstructedSnapshot() }
            .orEmpty()
        if (reconstructed.isEmpty()) error("No battery history found in this file.")
        return ParsedBatteryImport(snapshots = reconstructed, degraded = true)
    }

    private fun JsonObject.toReconstructedSnapshot(): BatteryHistorySnapshot? {
        val isoTime = this["time"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
        val timestampMillis = runCatching { Instant.parse(isoTime).toEpochMilliseconds() }.getOrNull() ?: return null
        val watts = this["watts"]?.jsonPrimitive?.doubleOrNull?.toFloat()
        return BatteryHistorySnapshot(
            timestampMillis = timestampMillis,
            levelPercent = this["level_percent"]?.jsonPrimitive?.intOrNull ?: 0,
            status = this["status"]?.jsonPrimitive?.contentOrNull?.ifBlank { "Unknown" } ?: "Unknown",
            source = this["source"]?.jsonPrimitive?.contentOrNull?.ifBlank { "Unknown" } ?: "Unknown",
            temperatureCelsius = (this["temperature_celsius"]?.jsonPrimitive?.doubleOrNull ?: 0.0).toFloat(),
            currentMa = null,
            estimatedWatts = watts,
            chargeCycles = null,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isoInstant(millis: Long): String = Instant.fromEpochMilliseconds(millis).toString()

    private fun fileStamp(millis: Long): String {
        val iso = Instant.fromEpochMilliseconds(millis).toString()  // 2024-01-02T03:04:05Z
        return iso.filter { it.isDigit() }.take(14).let {
            // YYYYMMDDHHMMSS → YYYYMMDD_HHMMSS
            if (it.length >= 14) "${it.substring(0, 8)}_${it.substring(8, 14)}" else it
        }
    }

    private fun Float.fmt(): String {
        val scaled = round(this * 100) / 100.0
        return scaled.toString()
    }

    private fun Float?.fmtNullable(): String = this?.fmt() ?: "n/a"

    private fun String.csv(): String = "\"${replace("\"", "\"\"")}\""
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableFloat(key: String, value: Float?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableInt(key: String, value: Int?) {
    if (value == null) put(key, JsonNull) else put(key, value)
}
