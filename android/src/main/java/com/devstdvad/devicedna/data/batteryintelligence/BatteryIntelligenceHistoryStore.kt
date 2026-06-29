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

class BatteryIntelligenceHistoryStore(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val snapshots: Flow<List<BatteryHistorySnapshot>> = context.batteryIntelligenceDataStore.data.map { prefs ->
        prefs[SNAPSHOTS]
            ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
            .orEmpty()
            .sortedBy { it.timestampMillis }
    }

    val chargingTrackingEnabled: Flow<Boolean> = context.batteryIntelligenceDataStore.data.map { prefs ->
        prefs[CHARGING_TRACKING_ENABLED] ?: true
    }

    suspend fun setChargingTrackingEnabled(value: Boolean) {
        context.batteryIntelligenceDataStore.edit { prefs ->
            prefs[CHARGING_TRACKING_ENABLED] = value
        }
    }

    suspend fun record(info: BatteryInfo, timestampMillis: Long = System.currentTimeMillis()) {
        context.batteryIntelligenceDataStore.edit { prefs ->
            if (prefs[CHARGING_TRACKING_ENABLED] == false) return@edit

            val existing = prefs[SNAPSHOTS]
                ?.let { encoded -> runCatching { json.decodeFromString<BatteryHistoryPayload>(encoded).snapshots }.getOrNull() }
                .orEmpty()
                .sortedBy { it.timestampMillis }

            val next = info.toHistorySnapshot(timestampMillis)
            val last = existing.lastOrNull()
            if (last != null && !last.shouldAppend(next)) return@edit

            prefs[SNAPSHOTS] = json.encodeToString(
                BatteryHistoryPayload(
                    snapshots = (existing + next).takeLast(MAX_SNAPSHOTS),
                ),
            )
        }
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
        const val MAX_SNAPSHOTS = 240
        const val MIN_SNAPSHOT_INTERVAL_MS = 15L * 60L * 1000L
    }
}
