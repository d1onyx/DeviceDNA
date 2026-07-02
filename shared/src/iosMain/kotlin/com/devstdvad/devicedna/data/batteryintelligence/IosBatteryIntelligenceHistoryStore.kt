package com.devstdvad.devicedna.data.batteryintelligence

import com.devstdvad.devicedna.di.APP_GROUP_ID
import com.devstdvad.devicedna.domain.model.BatteryInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults
import kotlin.math.abs

/**
 * iOS [BatteryIntelligenceHistoryStore] persisted as JSON in the App Group NSUserDefaults so
 * the WidgetKit extension and BGTask worker share the same history. Ports the Android
 * DataStore implementation's retention/dedup rules 1:1.
 */
class IosBatteryIntelligenceHistoryStore(
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = APP_GROUP_ID) ?: NSUserDefaults.standardUserDefaults,
) : BatteryIntelligenceHistoryStore {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mutex = Mutex()

    private val snapshotsFlow = MutableStateFlow(loadSnapshots())
    private val trackingFlow = MutableStateFlow(loadTracking())

    override val snapshots: Flow<List<BatteryHistorySnapshot>> = snapshotsFlow
    override val chargingTrackingEnabled: Flow<Boolean> = trackingFlow

    private fun loadSnapshots(): List<BatteryHistorySnapshot> =
        defaults.stringForKey(SNAPSHOTS_KEY)
            ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
            .orEmpty()
            .sortedBy { it.timestampMillis }

    private fun loadTracking(): Boolean =
        if (defaults.objectForKey(TRACKING_KEY) == null) true else defaults.boolForKey(TRACKING_KEY)

    private fun save(snapshots: List<BatteryHistorySnapshot>) {
        defaults.setObject(json.encodeToString(BatteryHistoryPayload(snapshots = snapshots)), SNAPSHOTS_KEY)
        snapshotsFlow.value = snapshots
    }

    override suspend fun setChargingTrackingEnabled(value: Boolean) {
        defaults.setBool(value, TRACKING_KEY)
        trackingFlow.value = value
    }

    override suspend fun record(info: BatteryInfo, timestampMillis: Long) = mutex.withLock {
        if (!trackingFlow.value) return@withLock

        val existing = loadSnapshots()
        val retained = existing.filter { it.timestampMillis <= timestampMillis }
        val removedFutureSnapshots = retained.size != existing.size

        val next = info.toHistorySnapshot(timestampMillis)
        val last = retained.lastOrNull()
        if (last != null && !last.shouldAppend(next)) {
            if (removedFutureSnapshots) save(retained.trimToRetention(timestampMillis))
            return@withLock
        }
        save((retained + next).trimToRetention(next.timestampMillis))
    }

    override suspend fun markRecordingPaused(
        timestampMillis: Long,
        removeSnapshotsAfterMarker: Boolean,
    ) = mutex.withLock {
        val existing = loadSnapshots()
        val withoutClockFuture = existing.filter { it.timestampMillis <= timestampMillis }
        val retained = if (removeSnapshotsAfterMarker) {
            withoutClockFuture.filter { it.timestampMillis <= timestampMillis }
        } else {
            withoutClockFuture
        }
        val removedSnapshots = retained.size != existing.size
        val last = retained.lastOrNull()
        if (last == null) {
            if (removedSnapshots) save(emptyList())
            return@withLock
        }
        if (last.recordingPaused) {
            if (removedSnapshots) save(retained.trimToRetention(timestampMillis))
            return@withLock
        }
        val marker = last.copy(timestampMillis = timestampMillis, recordingPaused = true)
        save((retained + marker).trimToRetention(timestampMillis))
    }

    override suspend fun importSnapshots(imported: List<BatteryHistorySnapshot>): Int = mutex.withLock {
        if (imported.isEmpty()) return@withLock 0
        val existing = loadSnapshots()
        val existingTimestamps = existing.mapTo(HashSet()) { it.timestampMillis }
        val fresh = imported
            .filter { it.timestampMillis !in existingTimestamps }
            .distinctBy { it.timestampMillis }
        if (fresh.isEmpty()) return@withLock 0
        save(
            (existing + fresh)
                .sortedBy { it.timestampMillis }
                .let { it.trimToRetention(it.maxOf { s -> s.timestampMillis }) },
        )
        fresh.size
    }

    private fun List<BatteryHistorySnapshot>.trimToRetention(referenceTimestampMillis: Long): List<BatteryHistorySnapshot> {
        val cutoffMillis = referenceTimestampMillis - HISTORY_RETENTION_MS
        return filter { it.timestampMillis >= cutoffMillis }.takeLast(MAX_SNAPSHOTS)
    }

    private fun BatteryHistorySnapshot.shouldAppend(next: BatteryHistorySnapshot): Boolean {
        val elapsed = next.timestampMillis - timestampMillis
        if (elapsed >= MIN_SNAPSHOT_INTERVAL_MS) return true
        if (levelPercent != next.levelPercent) return true
        if (status != next.status) return true
        if (source != next.source) return true
        if (chargeCycles != next.chargeCycles) return true
        if (abs(temperatureCelsius - next.temperatureCelsius) >= 1.5f) return true
        return abs((estimatedWatts ?: 0f) - (next.estimatedWatts ?: 0f)) >= 1f
    }

    private fun BatteryInfo.toHistorySnapshot(timestampMillis: Long): BatteryHistorySnapshot =
        BatteryHistorySnapshot(
            timestampMillis = timestampMillis,
            levelPercent = levelPercent,
            status = status.name,
            source = source.name,
            temperatureCelsius = temperatureCelsius,
            currentMa = currentMa,
            estimatedWatts = estimatedWatts,
            chargeCycles = chargeCycles,
        )

    private companion object {
        const val SNAPSHOTS_KEY = "battery_history_payload"
        const val TRACKING_KEY = "charging_tracking_enabled"
        const val MAX_SNAPSHOTS = 20_000
        const val MIN_SNAPSHOT_INTERVAL_MS = 15L * 60L * 1000L
        const val HISTORY_RETENTION_MS = 120L * 24L * 60L * 60L * 1000L
    }
}
