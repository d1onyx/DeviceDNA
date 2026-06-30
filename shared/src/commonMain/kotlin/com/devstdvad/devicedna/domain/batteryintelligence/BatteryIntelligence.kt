package com.devstdvad.devicedna.domain.batteryintelligence

import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

data class BatteryIntelligenceReport(
    val healthScore: Int,
    val degradationRiskPercent: Int,
    val degradationRiskLabel: String,
    val degradationSummary: String,
    val chargingAdvice: List<BatteryAdvice>,
    val cycleHistory: List<BatteryCyclePoint>,
    val chargingHistory: List<ChargingHistoryEntry>,
    val hourlyTimeline: List<ChargingHourSlot>,
    val selectedDayStartMillis: Long,
    val selectedDayLabel: String,
    val selectedDayRange: String,
    val selectedHour: Int,
    val selectedHourHistory: List<ChargingHistoryEntry>,
    val dailyChargingSessions: List<ChargingSessionSummary>,
    val canGoNextDay: Boolean,
    val cycleStats: BatteryCycleStats,
    val chargeSpeed: ChargeSpeedStats,
)

enum class BatteryAdvice {
    CoolBeforeCharge,
    UnplugNearFull,
    AvoidDeepDischarge,
    WirelessHeat,
    LowPower,
    Keep20To80,
    YellowHour,
    PowerSaver,
}

data class BatteryCyclePoint(
    val label: String,
    val cycles: Int,
)

data class ChargingHistoryEntry(
    val timestampMillis: Long,
    val label: String,
    val levelPercent: Int,
    val status: String,
    val watts: Float?,
    val temperatureCelsius: Float,
    val source: String,
)

data class ChargingSessionSummary(
    val startMillis: Long,
    val endMillis: Long?,
    val startLabel: String,
    val endLabel: String,
    val durationLabel: String,
    val startLevelPercent: Int,
    val endLevelPercent: Int,
    val levelDeltaPercent: Int,
    val averageWatts: Float?,
    val peakWatts: Float?,
    val maxTemperatureCelsius: Float,
    val needsAdvice: Boolean,
    val sampleCount: Int,
)

data class ChargingHourSlot(
    val hour: Int,
    val status: ChargingHourStatus,
    val sampleCount: Int,
    val goodMinutes: Float,
    val poorMinutes: Float,
    val dischargeMinutes: Float,
    val stableMinutes: Float,
    val segments: List<ChargingMinuteSegment>,
    val averageWatts: Float?,
    val minLevelPercent: Int?,
    val maxLevelPercent: Int?,
)

data class ChargingMinuteSegment(
    val startMinute: Float,
    val durationMinutes: Float,
    val status: ChargingHourStatus,
)

enum class ChargingHourStatus {
    NoData,
    GoodCharging,
    PoorCharging,
    Discharging,
    Stable,
}

data class BatteryCycleStats(
    val currentCycles: Int?,
    val cycleDelta: Int?,
    val source: BatteryCycleSource,
    val partialCyclePercent: Int,
    val trackedSamples: Int,
)

enum class BatteryCycleSource {
    SystemReported,
    LocalEstimate,
}

data class ChargeSpeedStats(
    val currentWatts: Float?,
    val averageWatts: Float?,
    val peakWatts: Float?,
    val percentPerHour: Float?,
    val chargingSessions: Int,
)

fun BatteryInfo.toBatteryIntelligenceReport(
    history: List<BatteryHistorySnapshot>,
    selectedDayStartMillis: Long,
    selectedHour: Int,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): BatteryIntelligenceReport {
    val healthScore = estimateHealthScore()
    val degradationRiskPercent = (100 - healthScore).coerceIn(4, 92)
    val riskLabel = when {
        degradationRiskPercent >= 55 -> "High"
        degradationRiskPercent >= 28 -> "Moderate"
        else -> "Low"
    }

    val selectedDaySnapshots = history.filterForDay(selectedDayStartMillis, timeZone)
    val hourlyTimeline = buildHourlyTimeline(history, selectedDayStartMillis, timeZone, nowMillis)
    val selectedHourHistory = selectedDaySnapshots
        .filter { it.hourOfDay(timeZone) == selectedHour }
        .sortedBy { it.timestampMillis }
        .map { it.toChargingHistoryEntry(timeZone) }
    val dailyChargingSessions = buildChargingSessions(history, selectedDayStartMillis, timeZone, nowMillis)

    return BatteryIntelligenceReport(
        healthScore = healthScore,
        degradationRiskPercent = degradationRiskPercent,
        degradationRiskLabel = riskLabel,
        degradationSummary = buildDegradationSummary(riskLabel, healthScore),
        chargingAdvice = buildChargingAdvice(),
        cycleHistory = buildCycleHistory(history, timeZone, nowMillis),
        chargingHistory = buildChargingHistory(history, nowMillis),
        hourlyTimeline = hourlyTimeline,
        selectedDayStartMillis = selectedDayStartMillis,
        selectedDayLabel = selectedDayStartMillis.formatDayLabel(timeZone, nowMillis),
        selectedDayRange = selectedDayStartMillis.formatDayRange(timeZone),
        selectedHour = selectedHour,
        selectedHourHistory = selectedHourHistory,
        dailyChargingSessions = dailyChargingSessions,
        canGoNextDay = selectedDayStartMillis.toLocalDate(timeZone) < today(timeZone, nowMillis),
        cycleStats = buildCycleStats(history),
        chargeSpeed = buildChargeSpeedStats(history),
    )
}

fun estimateCapacityRetentionPercent(healthScore: Int): Int =
    (healthScore * 0.9f + 8f).roundToInt().coerceIn(0, 100)

fun calculateAccumulatedChargePercent(history: List<BatteryHistorySnapshot>): Int =
    history
        .filter { it.chargeCycles == null }
        .sortedBy { it.timestampMillis }
        .windowed(size = 2, step = 1)
        .sumOf { (previous, current) -> previous.chargeIncreasePercentTo(current) }

fun todayStartMillis(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Long = today(timeZone, nowMillis).startMillis(timeZone)

fun currentHour(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Int = nowMillis.toLocalDateTime(timeZone).hour

fun previousDayStartMillis(dayStartMillis: Long, timeZone: TimeZone): Long =
    dayStartMillis.toLocalDate(timeZone)
        .minus(1, DateTimeUnit.DAY)
        .startMillis(timeZone)

fun nextDayStartMillis(
    dayStartMillis: Long,
    timeZone: TimeZone,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): Long {
    val currentDate = dayStartMillis.toLocalDate(timeZone)
    val today = today(timeZone, nowMillis)
    val next = if (currentDate < today) currentDate.plus(1, DateTimeUnit.DAY) else currentDate
    return next.startMillis(timeZone)
}

fun buildChargingSessions(
    history: List<BatteryHistorySnapshot>,
    dayStartMillis: Long? = null,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): List<ChargingSessionSummary> {
    val scoped = if (dayStartMillis == null) {
        history.sortedBy { it.timestampMillis }
    } else {
        history.scopedToDayWithCarryIn(dayStartMillis, timeZone)
    }
    val sessions = mutableListOf<List<BatteryHistorySnapshot>>()
    var active = mutableListOf<BatteryHistorySnapshot>()

    scoped.forEach { snapshot ->
        val charging = snapshot.isCharging || snapshot.isPlugged
        if (charging) {
            active.add(snapshot)
        } else if (active.isNotEmpty()) {
            active.add(snapshot)
            sessions.add(active.toList())
            active = mutableListOf()
        }
    }
    if (active.isNotEmpty()) {
        sessions.add(active.toList())
    }

    return sessions
        .mapNotNull { samples -> samples.toChargingSessionSummary(timeZone, nowMillis) }
        .sortedByDescending { it.startMillis }
}

fun buildHourlyTimeline(
    history: List<BatteryHistorySnapshot>,
    dayStartMillis: Long,
    timeZone: TimeZone,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): List<ChargingHourSlot> {
    val dayStart = dayStartMillis.toLocalDate(timeZone).startMillis(timeZone)
    val dayEnd = dayStartMillis.toLocalDate(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .startMillis(timeZone)
    val minuteBuckets = Array(24) { HourMinuteBucket() }
    val sorted = history.sortedBy { it.timestampMillis }
    sorted.forEachIndexed { index, snapshot ->
        val nextSnapshot = sorted.getOrNull(index + 1)
        val nextMillis = nextSnapshot?.timestampMillis
            ?: defaultIntervalEnd(snapshot.timestampMillis, dayStartMillis, timeZone, nowMillis)
        // A sample only vouches for a bounded window after it. A longer gap means recording was
        // paused (premium inactive, app closed, Doze), so it must stay NoData rather than be filled
        // by extrapolating the previous state.
        val filledEnd = minOf(nextMillis, snapshot.timestampMillis + MAX_INTERVAL_FILL_MS)
        // Keep only the portion that lands inside the selected day (handles near-midnight overlap).
        val clampedStart = maxOf(snapshot.timestampMillis, dayStart)
        val clampedEnd = minOf(filledEnd, dayEnd)
        allocateMinutes(snapshot, nextSnapshot, clampedStart, clampedEnd, minuteBuckets, timeZone)
    }

    val daySnapshots = sorted.filter { it.timestampMillis in dayStart until dayEnd }
    return (0..23).map { hour ->
        val bucket = minuteBuckets[hour]
        val samples = daySnapshots.filter { it.hourOfDay(timeZone) == hour }
        val watts = samples.mapNotNull { it.estimatedWatts }.filter { it > 0f }
        val status = when {
            bucket.poorMinutes > 0f -> ChargingHourStatus.PoorCharging
            bucket.goodMinutes > 0f -> ChargingHourStatus.GoodCharging
            bucket.dischargeMinutes > 0f -> ChargingHourStatus.Discharging
            bucket.stableMinutes > 0f -> ChargingHourStatus.Stable
            else -> ChargingHourStatus.NoData
        }
        ChargingHourSlot(
            hour = hour,
            status = status,
            sampleCount = samples.size,
            goodMinutes = bucket.goodMinutes.coerceAtMost(60f),
            poorMinutes = bucket.poorMinutes.coerceAtMost(60f),
            dischargeMinutes = bucket.dischargeMinutes.coerceAtMost(60f),
            stableMinutes = bucket.stableMinutes.coerceAtMost(60f),
            segments = bucket.segments,
            averageWatts = watts.averageOrNull(),
            minLevelPercent = samples.minOfOrNull { it.levelPercent },
            maxLevelPercent = samples.maxOfOrNull { it.levelPercent },
        )
    }
}

private fun BatteryInfo.estimateHealthScore(): Int {
    val base = when (health) {
        BatteryHealth.Good -> 94
        BatteryHealth.Unknown -> 72
        BatteryHealth.Cold -> 68
        BatteryHealth.OverVoltage -> 54
        BatteryHealth.Overheat -> 48
        BatteryHealth.Failure -> 32
        BatteryHealth.Dead -> 12
    }
    val cyclePenalty = ((chargeCycles ?: 0) / 25).coerceAtMost(28)
    val temperaturePenalty = when {
        temperatureCelsius >= 45f -> 22
        temperatureCelsius >= 40f -> 12
        temperatureCelsius <= 0f -> 10
        else -> 0
    }
    val levelPenalty = when {
        levelPercent >= 95 && status == BatteryStatus.Charging -> 5
        levelPercent <= 5 -> 4
        else -> 0
    }
    return (base - cyclePenalty - temperaturePenalty - levelPenalty).coerceIn(0, 100)
}

private fun BatteryInfo.buildDegradationSummary(riskLabel: String, healthScore: Int): String {
    val cycles = chargeCycles
    return when {
        cycles == null -> "$riskLabel risk based on current health, temperature and charging state. Cycle history is not reported on this device."
        cycles >= 800 -> "$riskLabel risk. Reported cycles are high, so capacity loss is likely even if the current status looks stable."
        healthScore >= 85 -> "$riskLabel risk. Current readings look healthy; keep heat low and avoid long 100% charging sessions."
        else -> "$riskLabel risk based on current health, cycle count and thermal behavior."
    }
}

private fun BatteryInfo.buildChargingAdvice(): List<BatteryAdvice> = buildList {
    if (temperatureCelsius >= 40f) {
        add(BatteryAdvice.CoolBeforeCharge)
    }
    if (levelPercent >= 90 && status == BatteryStatus.Charging) {
        add(BatteryAdvice.UnplugNearFull)
    }
    if (levelPercent <= 15) {
        add(BatteryAdvice.AvoidDeepDischarge)
    }
    if (source == ChargeSource.Wireless && temperatureCelsius >= 35f) {
        add(BatteryAdvice.WirelessHeat)
    }
    if ((estimatedWatts ?: 0f) in 0.1f..5f && status == BatteryStatus.Charging) {
        add(BatteryAdvice.LowPower)
    }
    add(BatteryAdvice.Keep20To80)
    add(BatteryAdvice.YellowHour)
    if (isPowerSaveMode) {
        add(BatteryAdvice.PowerSaver)
    }
}.distinct()

private fun BatteryInfo.buildCycleHistory(
    history: List<BatteryHistorySnapshot>,
    timeZone: TimeZone,
    nowMillis: Long,
): List<BatteryCyclePoint> {
    val points = history
        .filter { it.chargeCycles != null }
        .dedupeByCycleValue()
        .takeLast(6)
        .map { BatteryCyclePoint(it.relativeLabel(nowMillis), it.chargeCycles ?: 0) }

    if (points.isNotEmpty()) return points

    return chargeCycles?.let { cycles ->
        listOf(BatteryCyclePoint("Now", cycles))
    } ?: buildLocalCycleHistory(history, timeZone)
}

private fun BatteryInfo.buildChargingHistory(
    history: List<BatteryHistorySnapshot>,
    nowMillis: Long,
): List<ChargingHistoryEntry> =
    history
        .filter { it.isPlugged || it.isCharging }
        .takeLast(8)
        .map { it.toChargingHistoryEntry() }
        .ifEmpty {
            listOf(
                ChargingHistoryEntry(
                    timestampMillis = nowMillis,
                    label = "Now",
                    levelPercent = levelPercent,
                    status = status.name,
                    watts = estimatedWatts,
                    temperatureCelsius = temperatureCelsius,
                    source = source.name,
                ),
            ).filter { status == BatteryStatus.Charging || status == BatteryStatus.Full }
        }

private fun BatteryInfo.buildCycleStats(history: List<BatteryHistorySnapshot>): BatteryCycleStats {
    val cycleSamples = (history.mapNotNull { it.chargeCycles } + listOfNotNull(chargeCycles))
    if (cycleSamples.isNotEmpty()) {
        return BatteryCycleStats(
            currentCycles = cycleSamples.maxOrNull(),
            cycleDelta = if (cycleSamples.size >= 2) {
                cycleSamples.maxOrNull()?.minus(cycleSamples.minOrNull() ?: 0)
            } else {
                null
            },
            source = BatteryCycleSource.SystemReported,
            partialCyclePercent = 0,
            trackedSamples = history.size,
        )
    }

    val accumulatedPercent = calculateAccumulatedChargePercent(history)
    return BatteryCycleStats(
        currentCycles = accumulatedPercent / 100,
        cycleDelta = null,
        source = BatteryCycleSource.LocalEstimate,
        partialCyclePercent = accumulatedPercent % 100,
        trackedSamples = history.size,
    )
}

private fun buildLocalCycleHistory(
    history: List<BatteryHistorySnapshot>,
    timeZone: TimeZone,
): List<BatteryCyclePoint> {
    val dailyCycles = linkedMapOf<String, Int>()
    var accumulatedPercent = 0

    history
        .filter { it.chargeCycles == null }
        .sortedBy { it.timestampMillis }
        .windowed(size = 2, step = 1)
        .forEach { (previous, current) ->
            accumulatedPercent += previous.chargeIncreasePercentTo(current)
            dailyCycles[current.timestampMillis.formatShortDate(timeZone)] = accumulatedPercent / 100
        }

    return dailyCycles
        .map { (label, cycles) -> BatteryCyclePoint(label, cycles) }
        .takeLast(6)
}

private fun BatteryHistorySnapshot.chargeIncreasePercentTo(next: BatteryHistorySnapshot): Int {
    val levelDelta = next.levelPercent - levelPercent
    if (levelDelta <= 0) return 0
    val chargingBetweenSamples = isCharging || isPlugged || next.isCharging || next.isPlugged
    return if (chargingBetweenSamples) levelDelta else 0
}

private fun BatteryInfo.buildChargeSpeedStats(history: List<BatteryHistorySnapshot>): ChargeSpeedStats {
    val charging = history.filter { it.isCharging || it.isPlugged }
    val watts = charging.mapNotNull { it.estimatedWatts }.filter { it > 0f }
    val rate = charging
        .sortedBy { it.timestampMillis }
        .windowed(size = 2, step = 1)
        .mapNotNull { (first, second) ->
            val hours = (second.timestampMillis - first.timestampMillis) / 3_600_000f
            val levelDelta = second.levelPercent - first.levelPercent
            if (hours > 0f && levelDelta > 0) levelDelta / hours else null
        }
        .maxOrNull()

    return ChargeSpeedStats(
        currentWatts = estimatedWatts?.takeIf { it > 0f },
        averageWatts = watts.averageOrNull(),
        peakWatts = watts.maxOrNull(),
        percentPerHour = rate,
        chargingSessions = charging.countChargingSessions(),
    )
}

private fun List<BatteryHistorySnapshot>.dedupeByCycleValue(): List<BatteryHistorySnapshot> =
    fold(emptyList()) { acc, snapshot ->
        if (acc.lastOrNull()?.chargeCycles == snapshot.chargeCycles) acc else acc + snapshot
    }

private fun List<BatteryHistorySnapshot>.countChargingSessions(): Int {
    if (isEmpty()) return 0
    return sortedBy { it.timestampMillis }
        .fold(0 to false) { (sessions, wasCharging), snapshot ->
            val charging = snapshot.isCharging || snapshot.isPlugged
            val nextSessions = if (charging && !wasCharging) sessions + 1 else sessions
            nextSessions to charging
        }
        .first
}

private fun List<Float>.averageOrNull(): Float? =
    takeIf { it.isNotEmpty() }?.let { values -> (values.sum() / values.size) }

private fun BatteryHistorySnapshot.relativeLabel(nowMillis: Long): String {
    val minutesAgo = ((nowMillis - timestampMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        minutesAgo < 2 -> "Now"
        minutesAgo < 60 -> "${minutesAgo}m ago"
        minutesAgo < 24 * 60 -> "${minutesAgo / 60}h ago"
        else -> "${minutesAgo / (24 * 60)}d ago"
    }
}

private data class HourMinuteBucket(
    var goodMinutes: Float = 0f,
    var poorMinutes: Float = 0f,
    var dischargeMinutes: Float = 0f,
    var stableMinutes: Float = 0f,
    val segments: MutableList<ChargingMinuteSegment> = mutableListOf(),
)

/**
 * How long a single recorded sample stands in for. Recording runs roughly every 15 min while
 * active (and on plug/unplug events), so a gap beyond this window means recording was paused and
 * the period must stay NoData instead of being filled by the previous sample's state.
 */
private const val MAX_INTERVAL_FILL_MS = 20L * 60L * 1000L

private fun defaultIntervalEnd(
    timestampMillis: Long,
    dayStartMillis: Long,
    timeZone: TimeZone,
    nowMillis: Long,
): Long {
    val dayEnd = dayStartMillis.toLocalDate(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .startMillis(timeZone)
    return minOf(timestampMillis + 15L * 60_000L, dayEnd, nowMillis)
}

private fun allocateMinutes(
    snapshot: BatteryHistorySnapshot,
    nextSnapshot: BatteryHistorySnapshot?,
    startMillis: Long,
    endMillis: Long,
    buckets: Array<HourMinuteBucket>,
    timeZone: TimeZone,
) {
    if (endMillis <= startMillis) return
    val status = snapshot.toMinuteStatus(nextSnapshot)
    var cursor = startMillis
    while (cursor < endMillis) {
        val cursorDateTime = cursor.toLocalDateTime(timeZone)
        val hourEnd = cursorDateTime.nextHourStartMillis(timeZone)
        val segmentEnd = minOf(endMillis, hourEnd)
        val minutes = (segmentEnd - cursor) / 60_000f
        val bucket = buckets[cursorDateTime.hour]
        when (status) {
            ChargingHourStatus.GoodCharging -> bucket.goodMinutes += minutes
            ChargingHourStatus.PoorCharging -> bucket.poorMinutes += minutes
            ChargingHourStatus.Discharging -> bucket.dischargeMinutes += minutes
            ChargingHourStatus.Stable -> bucket.stableMinutes += minutes
            ChargingHourStatus.NoData -> Unit
        }
        if (status != ChargingHourStatus.NoData) {
            bucket.segments += ChargingMinuteSegment(
                startMinute = cursorDateTime.minute + cursorDateTime.second / 60f,
                durationMinutes = minutes,
                status = status,
            )
        }
        cursor = segmentEnd
    }
}

private fun BatteryHistorySnapshot.toMinuteStatus(nextSnapshot: BatteryHistorySnapshot?): ChargingHourStatus {
    val next = nextSnapshot ?: return ChargingHourStatus.Stable
    val levelDelta = next.levelPercent - levelPercent
    return when {
        levelDelta > 0 && (isPoorCharging() || next.isPoorCharging()) -> ChargingHourStatus.PoorCharging
        levelDelta > 0 -> ChargingHourStatus.GoodCharging
        levelDelta < 0 -> ChargingHourStatus.Discharging
        else -> ChargingHourStatus.Stable
    }
}

private fun BatteryHistorySnapshot.isPoorCharging(): Boolean {
    if (temperatureCelsius >= 40f) return true
    if (levelPercent >= 90) return true
    if (source == ChargeSource.Wireless.name && temperatureCelsius >= 35f) return true
    val watts = estimatedWatts ?: return false
    return watts in 0.1f..5f
}

private fun List<BatteryHistorySnapshot>.filterForDay(
    dayStartMillis: Long,
    timeZone: TimeZone,
): List<BatteryHistorySnapshot> {
    val start = dayStartMillis.toLocalDate(timeZone).startMillis(timeZone)
    val end = dayStartMillis.toLocalDate(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .startMillis(timeZone)
    return filter { it.timestampMillis in start until end }
}

/**
 * The day's snapshots, prefixed with a synthetic "carry-in" sample at 00:00 when the device was
 * still charging at midnight. This lets a charging session that began the previous evening be
 * detected on the selected day instead of being lost at the day boundary.
 */
private fun List<BatteryHistorySnapshot>.scopedToDayWithCarryIn(
    dayStartMillis: Long,
    timeZone: TimeZone,
): List<BatteryHistorySnapshot> {
    val sorted = sortedBy { it.timestampMillis }
    val dayStart = dayStartMillis.toLocalDate(timeZone).startMillis(timeZone)
    val dayEnd = dayStartMillis.toLocalDate(timeZone)
        .plus(1, DateTimeUnit.DAY)
        .startMillis(timeZone)
    val day = sorted.filter { it.timestampMillis in dayStart until dayEnd }
    // Only bridge an ongoing charge across midnight when it was still being recorded just before
    // midnight. A stale sample (long recording gap before the day) must not fabricate a session.
    val carryIn = sorted.lastOrNull { it.timestampMillis < dayStart }
        ?.takeIf { (it.isCharging || it.isPlugged) && dayStart - it.timestampMillis <= MAX_INTERVAL_FILL_MS }
        ?.copy(timestampMillis = dayStart)
    return listOfNotNull(carryIn) + day
}

private fun BatteryHistorySnapshot.hourOfDay(timeZone: TimeZone): Int =
    timestampMillis.toLocalDateTime(timeZone).hour

private fun BatteryHistorySnapshot.toChargingHistoryEntry(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): ChargingHistoryEntry =
    ChargingHistoryEntry(
        timestampMillis = timestampMillis,
        label = timestampMillis.formatTime(timeZone),
        levelPercent = levelPercent,
        status = status,
        watts = estimatedWatts,
        temperatureCelsius = temperatureCelsius,
        source = source,
    )

private fun List<BatteryHistorySnapshot>.toChargingSessionSummary(
    timeZone: TimeZone,
    nowMillis: Long,
): ChargingSessionSummary? {
    val chargingSamples = filter { it.isCharging || it.isPlugged }
    val start = chargingSamples.firstOrNull() ?: return null
    val last = last()
    val disconnected = !(last.isCharging || last.isPlugged)
    val endMillis = if (disconnected) last.timestampMillis else null
    val endSample = if (disconnected && size >= 2) get(size - 2) else last
    val watts = chargingSamples.mapNotNull { it.estimatedWatts }.filter { it > 0f }
    val durationEnd = endMillis ?: nowMillis
    val durationMillis = (durationEnd - start.timestampMillis).coerceAtLeast(0L)

    return ChargingSessionSummary(
        startMillis = start.timestampMillis,
        endMillis = endMillis,
        startLabel = start.timestampMillis.formatTime(timeZone),
        endLabel = endMillis?.formatTime(timeZone) ?: "Ongoing",
        durationLabel = durationMillis.formatDuration(),
        startLevelPercent = start.levelPercent,
        endLevelPercent = endSample.levelPercent,
        levelDeltaPercent = endSample.levelPercent - start.levelPercent,
        averageWatts = watts.averageOrNull(),
        peakWatts = watts.maxOrNull(),
        maxTemperatureCelsius = maxOf { it.temperatureCelsius },
        needsAdvice = chargingSamples.any { it.isPoorCharging() },
        sampleCount = size,
    )
}

private fun Long.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

private fun Long.toLocalDate(timeZone: TimeZone): LocalDate =
    toLocalDateTime(timeZone).date

private fun today(timeZone: TimeZone, nowMillis: Long): LocalDate =
    nowMillis.toLocalDate(timeZone)

private fun LocalDate.startMillis(timeZone: TimeZone): Long =
    atStartOfDayIn(timeZone).toEpochMilliseconds()

private fun LocalDateTime.nextHourStartMillis(timeZone: TimeZone): Long {
    val nextDate = if (hour == 23) date.plus(1, DateTimeUnit.DAY) else date
    val nextHour = (hour + 1) % 24
    return LocalDateTime(nextDate, LocalTime(nextHour, 0)).toInstant(timeZone).toEpochMilliseconds()
}

private fun Long.formatTime(timeZone: TimeZone): String {
    val local = toLocalDateTime(timeZone)
    return "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

private fun Long.formatDuration(): String {
    val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m"
        else -> "<1m"
    }
}

private fun Long.formatDayLabel(timeZone: TimeZone, nowMillis: Long): String {
    val date = toLocalDate(timeZone)
    val today = today(timeZone, nowMillis)
    return when (date) {
        today -> "Today"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> "${date.monthShortName()} ${date.dayOfMonth}, ${date.year}"
    }
}

private fun Long.formatDayRange(timeZone: TimeZone): String {
    val date = toLocalDate(timeZone)
    return "${date.monthShortName()} ${date.dayOfMonth} 00:00 - 24:00"
}

private fun Long.formatShortDate(timeZone: TimeZone): String {
    val date = toLocalDate(timeZone)
    return "${date.monthShortName()} ${date.dayOfMonth}"
}

private fun LocalDate.monthShortName(): String = MONTHS[monthNumber - 1]

private fun Int.twoDigits(): String = if (this < 10) "0$this" else toString()

private val MONTHS = listOf(
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "May",
    "Jun",
    "Jul",
    "Aug",
    "Sep",
    "Oct",
    "Nov",
    "Dec",
)
