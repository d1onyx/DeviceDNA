package com.devstdvad.devicedna.data.batteryintelligence

import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.domain.model.BatteryInfo

/**
 * The single decision point for whether a battery reading is written to the history store, shared by
 * every recording site so all of them stay identical:
 *   • the BatteryIntelligence screen ViewModel (dense sampling while the screen is open),
 *   • the iOS app-wide recorder (any screen while foregrounded),
 *   • the iOS BGTask worker (background boundary samples).
 *
 * Mirrors Android's [BatteryHistoryRecorder.recordBoundary] gating 1:1: only a premium (unlocked)
 * user with charging tracking on gets samples recorded; otherwise a pause marker keeps the timeline
 * honest (empty gap for the un-tracked / un-entitled period).
 */
class BatteryHistoryTracker(
    private val store: BatteryIntelligenceHistoryStore,
) {
    suspend fun onBatterySample(
        entitlements: PremiumEntitlements,
        info: BatteryInfo?,
        trackingEnabled: Boolean,
        nowMillis: Long = currentTimeMillis(),
    ) {
        val unlocked = entitlements.hasFeature(PremiumFeature.BatteryIntelligence, nowMillis)
        val expiredAtMillis = entitlements.expiresAtMillis?.takeIf { it <= nowMillis }
        when {
            unlocked && trackingEnabled && info != null -> store.record(info, nowMillis)
            // Drop a marker so the timeline leaves the un-tracked gap empty. No-op if already marked.
            !trackingEnabled -> store.markRecordingPaused(nowMillis)
            !unlocked -> store.markRecordingPaused(
                timestampMillis = expiredAtMillis ?: nowMillis,
                removeSnapshotsAfterMarker = expiredAtMillis != null,
            )
        }
    }
}
