package com.devstdvad.devicedna.data.batteryintelligence

import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.account.ClearableStore
import com.devstdvad.devicedna.domain.model.BatteryInfo
import kotlinx.coroutines.flow.Flow

/**
 * Persistent battery-history store, platform-agnostic. Implemented per platform:
 *   • Android → AndroidBatteryIntelligenceHistoryStore (DataStore)
 *   • iOS     → NSUserDefaults/file-backed, later
 * Shared ViewModels observe [snapshots] and feed them into the shared BatteryIntelligence analytics.
 */
interface BatteryIntelligenceHistoryStore : ClearableStore {
    val snapshots: Flow<List<BatteryHistorySnapshot>>
    val chargingTrackingEnabled: Flow<Boolean>

    suspend fun setChargingTrackingEnabled(value: Boolean)

    suspend fun record(
        info: BatteryInfo,
        timestampMillis: Long = currentTimeMillis(),
    )

    suspend fun markRecordingPaused(
        timestampMillis: Long = currentTimeMillis(),
        removeSnapshotsAfterMarker: Boolean = false,
    )

    /** Merge imported snapshots (dedup by timestamp), returning how many were added. */
    suspend fun importSnapshots(imported: List<BatteryHistorySnapshot>): Int
}
