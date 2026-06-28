package com.devstdvad.devicedna.presentation.batteryintelligence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class BatteryIntelligenceUiState(
    val isLoading: Boolean = true,
    val isPremiumUnlocked: Boolean = false,
    val intelligence: BatteryIntelligenceReport? = null,
    val error: String? = null,
)

data class BatteryIntelligenceReport(
    val healthScore: Int,
    val degradationRiskPercent: Int,
    val degradationRiskLabel: String,
    val degradationSummary: String,
    val chargingAdvice: List<String>,
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

class BatteryIntelligenceViewModel(
    observeBattery: ObserveBatteryUseCase,
    subscriptionRepository: SubscriptionRepository,
    historyStore: BatteryIntelligenceHistoryStore,
) : ViewModel() {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val selectedDayStartMillis = MutableStateFlow(todayStartMillis(zoneId))
    private val selectedHour = MutableStateFlow(currentHour(zoneId))

    private val batteryState = observeBattery().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    init {
        combine(
            subscriptionRepository.entitlements,
            batteryState,
        ) { entitlements, batteryResult ->
            val unlocked = entitlements.hasFeature(PremiumFeature.BatteryIntelligence)
            val info = (batteryResult as? AppResult.Success)?.value
            unlocked to info
        }.onEach { (unlocked, info) ->
            if (unlocked && info != null) {
                historyStore.record(info)
            }
        }.launchIn(viewModelScope)
    }

    val state: StateFlow<BatteryIntelligenceUiState> = combine(
        subscriptionRepository.entitlements,
        batteryState,
        historyStore.snapshots,
        selectedDayStartMillis,
        selectedHour,
    ) { entitlements, batteryResult, history, dayStartMillis, hour ->
        val unlocked = entitlements.hasFeature(PremiumFeature.BatteryIntelligence)
        when (batteryResult) {
            null -> BatteryIntelligenceUiState(
                isLoading = true,
                isPremiumUnlocked = unlocked,
            )
            is AppResult.Success -> BatteryIntelligenceUiState(
                isLoading = false,
                isPremiumUnlocked = unlocked,
                intelligence = if (unlocked) {
                    batteryResult.value.toIntelligenceReport(
                        history = history,
                        selectedDayStartMillis = dayStartMillis,
                        selectedHour = hour,
                        zoneId = zoneId,
                    )
                } else {
                    null
                },
            )
            is AppResult.Error -> BatteryIntelligenceUiState(
                isLoading = false,
                isPremiumUnlocked = unlocked,
                error = batteryResult.cause.message,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BatteryIntelligenceUiState(),
    )

    fun selectHour(hour: Int) {
        selectedHour.value = hour.coerceIn(0, 23)
    }

    fun goToPreviousDay() {
        selectedDayStartMillis.update { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(zoneId)
                .toLocalDate()
                .minusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        }
    }

    fun goToNextDay() {
        selectedDayStartMillis.update { millis ->
            val currentDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
            val today = LocalDate.now(zoneId)
            val next = if (currentDate.isBefore(today)) currentDate.plusDays(1) else currentDate
            next.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }
    }
}

private fun BatteryInfo.toIntelligenceReport(
    history: List<BatteryHistorySnapshot>,
    selectedDayStartMillis: Long,
    selectedHour: Int,
    zoneId: ZoneId,
): BatteryIntelligenceReport {
    val healthScore = estimateHealthScore()
    val degradationRiskPercent = (100 - healthScore).coerceIn(4, 92)
    val riskLabel = when {
        degradationRiskPercent >= 55 -> "High"
        degradationRiskPercent >= 28 -> "Moderate"
        else -> "Low"
    }

    val selectedDaySnapshots = history.filterForDay(selectedDayStartMillis, zoneId)
    val hourlyTimeline = buildHourlyTimeline(selectedDaySnapshots, selectedDayStartMillis, zoneId)
    val selectedHourHistory = selectedDaySnapshots
        .filter { it.hourOfDay(zoneId) == selectedHour }
        .sortedBy { it.timestampMillis }
        .map { it.toChargingHistoryEntry(zoneId) }
    val dailyChargingSessions = buildChargingSessions(selectedDaySnapshots, zoneId)

    return BatteryIntelligenceReport(
        healthScore = healthScore,
        degradationRiskPercent = degradationRiskPercent,
        degradationRiskLabel = riskLabel,
        degradationSummary = buildDegradationSummary(riskLabel, healthScore),
        chargingAdvice = buildChargingAdvice(),
        cycleHistory = buildCycleHistory(history, zoneId),
        chargingHistory = buildChargingHistory(history),
        hourlyTimeline = hourlyTimeline,
        selectedDayStartMillis = selectedDayStartMillis,
        selectedDayLabel = selectedDayStartMillis.formatDayLabel(zoneId),
        selectedDayRange = selectedDayStartMillis.formatDayRange(zoneId),
        selectedHour = selectedHour,
        selectedHourHistory = selectedHourHistory,
        dailyChargingSessions = dailyChargingSessions,
        canGoNextDay = Instant.ofEpochMilli(selectedDayStartMillis).atZone(zoneId).toLocalDate()
            .isBefore(LocalDate.now(zoneId)),
        cycleStats = buildCycleStats(history),
        chargeSpeed = buildChargeSpeedStats(history),
    )
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

private fun BatteryInfo.buildChargingAdvice(): List<String> = buildList {
    if (temperatureCelsius >= 40f) {
        add("Let the phone cool before fast charging or running heavy workloads.")
    }
    if (levelPercent >= 90 && status == BatteryStatus.Charging) {
        add("Unplug soon or use a charge limit when available to reduce time near 100%.")
    }
    if (levelPercent <= 15) {
        add("Avoid frequent deep discharge; charging before 15-20% is gentler on the battery.")
    }
    if (source.name == "Wireless" && temperatureCelsius >= 35f) {
        add("Wireless charging is convenient but can add heat; use wired charging when the phone is warm.")
    }
    if ((estimatedWatts ?: 0f) in 0.1f..5f && status == BatteryStatus.Charging) {
        add("Charging power is low. Try a better cable, a stronger wall adapter, or cleaning the charging port.")
    }
    add("For daily use, staying roughly between 20% and 80% reduces long-term stress.")
    add("If a charging hour is yellow, check the cable, adapter, heat, case, and background load before continuing.")
    if (isPowerSaveMode) {
        add("Power saver is enabled; it can reduce heat and discharge rate during low battery periods.")
    }
}.distinct()

fun estimateCapacityRetentionPercent(healthScore: Int): Int =
    (healthScore * 0.9f + 8f).roundToInt().coerceIn(0, 100)

private fun BatteryInfo.buildCycleHistory(
    history: List<BatteryHistorySnapshot>,
    zoneId: ZoneId,
): List<BatteryCyclePoint> {
    val points = history
        .filter { it.chargeCycles != null }
        .dedupeByCycleValue()
        .takeLast(6)
        .map { BatteryCyclePoint(it.relativeLabel(), it.chargeCycles ?: 0) }

    if (points.isNotEmpty()) return points

    return chargeCycles?.let { cycles ->
        listOf(BatteryCyclePoint("Now", cycles))
    } ?: buildLocalCycleHistory(history, zoneId)
}

private fun BatteryInfo.buildChargingHistory(history: List<BatteryHistorySnapshot>): List<ChargingHistoryEntry> =
    history
        .filter { it.isPlugged || it.isCharging }
        .takeLast(8)
        .map {
            it.toChargingHistoryEntry()
        }
        .ifEmpty {
            listOf(
                ChargingHistoryEntry(
                    timestampMillis = System.currentTimeMillis(),
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
    zoneId: ZoneId,
): List<BatteryCyclePoint> {
    val dailyCycles = linkedMapOf<String, Int>()
    var accumulatedPercent = 0

    history
        .filter { it.chargeCycles == null }
        .sortedBy { it.timestampMillis }
        .windowed(size = 2, step = 1)
        .forEach { (previous, current) ->
            accumulatedPercent += previous.chargeIncreasePercentTo(current)
            val label = DateTimeFormatter
                .ofPattern("MMM d", Locale.getDefault())
                .format(Instant.ofEpochMilli(current.timestampMillis).atZone(zoneId))
            dailyCycles[label] = accumulatedPercent / 100
        }

    return dailyCycles
        .map { (label, cycles) -> BatteryCyclePoint(label, cycles) }
        .takeLast(6)
}

fun calculateAccumulatedChargePercent(history: List<BatteryHistorySnapshot>): Int =
    history
        .filter { it.chargeCycles == null }
        .sortedBy { it.timestampMillis }
        .windowed(size = 2, step = 1)
        .sumOf { (previous, current) -> previous.chargeIncreasePercentTo(current) }

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

private fun BatteryHistorySnapshot.relativeLabel(nowMillis: Long = System.currentTimeMillis()): String {
    val minutesAgo = ((nowMillis - timestampMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        minutesAgo < 2 -> "Now"
        minutesAgo < 60 -> "${minutesAgo}m ago"
        minutesAgo < 24 * 60 -> "${minutesAgo / 60}h ago"
        else -> "${minutesAgo / (24 * 60)}d ago"
    }
}

internal fun buildHourlyTimeline(
    daySnapshots: List<BatteryHistorySnapshot>,
    dayStartMillis: Long,
    zoneId: ZoneId,
): List<ChargingHourSlot> {
    val minuteBuckets = Array(24) { HourMinuteBucket() }
    val sorted = daySnapshots.sortedBy { it.timestampMillis }
    sorted.forEachIndexed { index, snapshot ->
        val nextSnapshot = sorted.getOrNull(index + 1)
        val nextMillis = nextSnapshot?.timestampMillis
            ?: defaultIntervalEnd(snapshot.timestampMillis, dayStartMillis, zoneId)
        allocateMinutes(snapshot, nextSnapshot, snapshot.timestampMillis, nextMillis, minuteBuckets, zoneId)
    }

    return (0..23).map { hour ->
        val bucket = minuteBuckets[hour]
        val samples = daySnapshots.filter { it.hourOfDay(zoneId) == hour }
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

private data class HourMinuteBucket(
    var goodMinutes: Float = 0f,
    var poorMinutes: Float = 0f,
    var dischargeMinutes: Float = 0f,
    var stableMinutes: Float = 0f,
    val segments: MutableList<ChargingMinuteSegment> = mutableListOf(),
)

private fun defaultIntervalEnd(timestampMillis: Long, dayStartMillis: Long, zoneId: ZoneId): Long {
    val dayEnd = Instant.ofEpochMilli(dayStartMillis)
        .atZone(zoneId)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
    return minOf(timestampMillis + 15L * 60_000L, dayEnd, System.currentTimeMillis())
}

private fun allocateMinutes(
    snapshot: BatteryHistorySnapshot,
    nextSnapshot: BatteryHistorySnapshot?,
    startMillis: Long,
    endMillis: Long,
    buckets: Array<HourMinuteBucket>,
    zoneId: ZoneId,
) {
    if (endMillis <= startMillis) return
    val status = snapshot.toMinuteStatus(nextSnapshot)
    var cursor = startMillis
    while (cursor < endMillis) {
        val cursorDateTime = Instant.ofEpochMilli(cursor).atZone(zoneId)
        val hourEnd = cursorDateTime
            .plusHours(1)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
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
    if (source == "Wireless" && temperatureCelsius >= 35f) return true
    val watts = estimatedWatts ?: return false
    return watts in 0.1f..5f
}

private fun List<BatteryHistorySnapshot>.filterForDay(dayStartMillis: Long, zoneId: ZoneId): List<BatteryHistorySnapshot> {
    val start = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return filter { it.timestampMillis in start until end }
}

private fun BatteryHistorySnapshot.hourOfDay(zoneId: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(timestampMillis).atZone(zoneId).hour

private fun BatteryHistorySnapshot.toChargingHistoryEntry(zoneId: ZoneId = ZoneId.systemDefault()): ChargingHistoryEntry =
    ChargingHistoryEntry(
        timestampMillis = timestampMillis,
        label = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(timestampMillis).atZone(zoneId)),
        levelPercent = levelPercent,
        status = status,
        watts = estimatedWatts,
        temperatureCelsius = temperatureCelsius,
        source = source,
    )

fun buildChargingSessions(
    daySnapshots: List<BatteryHistorySnapshot>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<ChargingSessionSummary> {
    val sessions = mutableListOf<List<BatteryHistorySnapshot>>()
    var active = mutableListOf<BatteryHistorySnapshot>()

    daySnapshots.sortedBy { it.timestampMillis }.forEach { snapshot ->
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
        .mapNotNull { samples -> samples.toChargingSessionSummary(zoneId) }
        .sortedByDescending { it.startMillis }
}

private fun List<BatteryHistorySnapshot>.toChargingSessionSummary(zoneId: ZoneId): ChargingSessionSummary? {
    val chargingSamples = filter { it.isCharging || it.isPlugged }
    val start = chargingSamples.firstOrNull() ?: return null
    val last = last()
    val disconnected = !(last.isCharging || last.isPlugged)
    val endMillis = if (disconnected) last.timestampMillis else null
    val endSample = if (disconnected && size >= 2) get(size - 2) else last
    val watts = chargingSamples.mapNotNull { it.estimatedWatts }.filter { it > 0f }
    val durationEnd = endMillis ?: System.currentTimeMillis()
    val durationMillis = (durationEnd - start.timestampMillis).coerceAtLeast(0L)

    return ChargingSessionSummary(
        startMillis = start.timestampMillis,
        endMillis = endMillis,
        startLabel = start.formatTime(zoneId),
        endLabel = endMillis?.formatTime(zoneId) ?: "Ongoing",
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

private fun BatteryHistorySnapshot.formatTime(zoneId: ZoneId): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(timestampMillis).atZone(zoneId))

private fun Long.formatTime(zoneId: ZoneId): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(this).atZone(zoneId))

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

private fun todayStartMillis(zoneId: ZoneId): Long =
    LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun currentHour(zoneId: ZoneId): Int =
    Instant.now().atZone(zoneId).hour

private fun Long.formatDayLabel(zoneId: ZoneId): String {
    val date = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()).format(date)
    }
}

private fun Long.formatDayRange(zoneId: ZoneId): String {
    val date = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    return "${formatter.format(date)} 00:00 - 24:00"
}
