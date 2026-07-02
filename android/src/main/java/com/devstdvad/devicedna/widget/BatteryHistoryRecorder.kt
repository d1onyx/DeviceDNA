package com.devstdvad.devicedna.widget

import android.content.Context
import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.data.batteryintelligence.AndroidBatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.source.AndroidBatteryDataSource
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionStore
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

object BatteryHistoryRecorder {
    suspend fun recordBoundary(context: Context) {
        val appContext = context.applicationContext
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val historyStore = AndroidBatteryIntelligenceHistoryStore(appContext)
        val entitlements = SubscriptionStore(appContext).entitlements.first()
        val trackingEnabled = historyStore.chargingTrackingEnabled.first()
        val unlocked = entitlements.hasFeature(PremiumFeature.BatteryIntelligence, nowMillis)
        val expiredAtMillis = entitlements.expiresAtMillis?.takeIf { it <= nowMillis }

        if (unlocked && trackingEnabled) {
            AndroidBatteryDataSource(appContext).getBatterySnapshot().getOrNull()?.let { battery ->
                historyStore.record(battery, timestampMillis = nowMillis)
            }
        } else {
            historyStore.markRecordingPaused(
                timestampMillis = if (trackingEnabled) expiredAtMillis ?: nowMillis else nowMillis,
                removeSnapshotsAfterMarker = trackingEnabled && expiredAtMillis != null,
            )
        }
    }
}
