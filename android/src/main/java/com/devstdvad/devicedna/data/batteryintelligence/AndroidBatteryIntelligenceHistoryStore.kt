package com.devstdvad.devicedna.data.batteryintelligence

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devstdvad.devicedna.domain.model.BatteryInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

private val Context.batteryIntelligenceDataStore by preferencesDataStore("device_dna_battery_intelligence")

class AndroidBatteryIntelligenceHistoryStore(
    private val context: Context,
) : BatteryIntelligenceHistoryStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val snapshots: Flow<List<BatteryHistorySnapshot>> = context.batteryIntelligenceDataStore.data.map { prefs ->
        prefs[SNAPSHOTS]
            ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
            .orEmpty()
            .sortedBy { it.timestampMillis }
    }

    override val chargingTrackingEnabled: Flow<Boolean> = context.batteryIntelligenceDataStore.data.map { prefs ->
        prefs[CHARGING_TRACKING_ENABLED] ?: true
    }

    override suspend fun setChargingTrackingEnabled(value: Boolean) {
        context.batteryIntelligenceDataStore.edit { prefs ->
            prefs[CHARGING_TRACKING_ENABLED] = value
        }
    }

    override suspend fun clear() {
        context.batteryIntelligenceDataStore.edit { it.clear() }
    }

    override suspend fun record(info: BatteryInfo, timestampMillis: Long) {
        context.batteryIntelligenceDataStore.edit { prefs ->
            if (prefs[CHARGING_TRACKING_ENABLED] == false) return@edit

            val existing = prefs[SNAPSHOTS]
                ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
                .orEmpty()
                .sortedBy { it.timestampMillis }
            val retained = existing.discardFutureSnapshots(timestampMillis)
            val removedFutureSnapshots = retained.size != existing.size

            val next = info.toHistorySnapshot(timestampMillis)
            val last = retained.lastOrNull()
            if (last != null && !last.shouldAppend(next)) {
                if (removedFutureSnapshots) {
                    prefs[SNAPSHOTS] = json.encodeToString(
                        BatteryHistoryPayload(snapshots = retained.trimToRetention(timestampMillis)),
                    )
                }
                return@edit
            }

            prefs[SNAPSHOTS] = json.encodeToString(
                BatteryHistoryPayload(
                    snapshots = (retained + next).trimToRetention(next.timestampMillis),
                ),
            )
        }
    }

    /**
     * Records a "recording paused" marker (premium/tracking turned off) so the timeline leaves the
     * following gap empty instead of bridging across it. No-op when there is no history yet or the
     * last entry is already a marker, so it can be called freely on every locked emission.
     */
    override suspend fun markRecordingPaused(
        timestampMillis: Long,
        removeSnapshotsAfterMarker: Boolean,
    ) {
        context.batteryIntelligenceDataStore.edit { prefs ->
            val existing = prefs[SNAPSHOTS]
                ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
                .orEmpty()
                .sortedBy { it.timestampMillis }
            val withoutClockFuture = existing.discardFutureSnapshots(timestampMillis)

            val retained = if (removeSnapshotsAfterMarker) {
                withoutClockFuture.filter { it.timestampMillis <= timestampMillis }
            } else {
                withoutClockFuture
            }
            val removedSnapshots = retained.size != existing.size
            val last = retained.lastOrNull()
            if (last == null) {
                if (removedSnapshots) {
                    prefs[SNAPSHOTS] = json.encodeToString(BatteryHistoryPayload())
                }
                return@edit
            }
            if (last.recordingPaused) {
                if (removedSnapshots) {
                    prefs[SNAPSHOTS] = json.encodeToString(
                        BatteryHistoryPayload(snapshots = retained.trimToRetention(timestampMillis)),
                    )
                }
                return@edit
            }

            val marker = last.copy(timestampMillis = timestampMillis, recordingPaused = true)
            prefs[SNAPSHOTS] = json.encodeToString(
                BatteryHistoryPayload(
                    snapshots = (retained + marker).trimToRetention(timestampMillis),
                ),
            )
        }
    }

    /**
     * Merges externally imported snapshots into the stored history, de-duplicating by timestamp,
     * keeping chronological order and the retention cap. Bypasses the tracking toggle because importing
     * is an explicit user action. Returns how many new snapshots were added.
     */
    override suspend fun importSnapshots(imported: List<BatteryHistorySnapshot>): Int {
        if (imported.isEmpty()) return 0
        var added = 0
        context.batteryIntelligenceDataStore.edit { prefs ->
            val existing = prefs[SNAPSHOTS]
                ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
                .orEmpty()
            val existingTimestamps = existing.mapTo(HashSet()) { it.timestampMillis }
            val fresh = imported
                .filter { it.timestampMillis !in existingTimestamps }
                .distinctBy { it.timestampMillis }
            added = fresh.size
            if (fresh.isEmpty()) return@edit

            prefs[SNAPSHOTS] = json.encodeToString(
                BatteryHistoryPayload(
                    snapshots = (existing + fresh)
                        .sortedBy { it.timestampMillis }
                        .trimToRetention(),
                ),
            )
        }
        return added
    }

    private fun List<BatteryHistorySnapshot>.discardFutureSnapshots(
        timestampMillis: Long,
    ): List<BatteryHistorySnapshot> = filter { it.timestampMillis <= timestampMillis }

    private fun List<BatteryHistorySnapshot>.trimToRetention(
        referenceTimestampMillis: Long = maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis(),
    ): List<BatteryHistorySnapshot> {
        val cutoffMillis = referenceTimestampMillis - HISTORY_RETENTION_MS
        return filter { it.timestampMillis >= cutoffMillis }
            .takeLast(MAX_SNAPSHOTS)
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
        val SNAPSHOTS = stringPreferencesKey("snapshots")
        val CHARGING_TRACKING_ENABLED = booleanPreferencesKey("charging_tracking_enabled")
        const val MAX_SNAPSHOTS = 20_000
        const val MIN_SNAPSHOT_INTERVAL_MS = 15L * 60L * 1000L
        const val HISTORY_RETENTION_MS = 120L * 24L * 60L * 60L * 1000L
    }
}
