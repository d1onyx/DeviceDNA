package com.devstdvad.devicedna.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.data.alerts.SmartAlertsManager
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.widget.WidgetMetricsLoader
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import com.devstdvad.devicedna.widget.glance.BatteryDoctorWidget
import com.devstdvad.devicedna.widget.glance.BatteryWidget
import com.devstdvad.devicedna.widget.glance.CpuThermalWidget
import com.devstdvad.devicedna.widget.glance.DeviceHealthWidget
import com.devstdvad.devicedna.widget.glance.GuardianWidget
import com.devstdvad.devicedna.widget.glance.MemoryWidget
import com.devstdvad.devicedna.widget.glance.ThermalGuardWidget
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Periodic background job (~15 min): refreshes the widget cache, re-renders widgets, and
 * records a battery snapshot for Battery Intelligence so history is captured even while the
 * app is closed (the in-app screen only records while it is open).
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val loader: WidgetMetricsLoader by inject()
    private val subscriptionRepository: SubscriptionRepository by inject()
    private val batteryRepository: BatteryRepository by inject()
    private val batteryHistoryStore: BatteryIntelligenceHistoryStore by inject()
    private val smartAlertsManager: SmartAlertsManager by inject()

    override suspend fun doWork(): Result = try {
        val snapshot = loader.refresh()
        BatteryWidget().updateAll(applicationContext)
        MemoryWidget().updateAll(applicationContext)
        CpuThermalWidget().updateAll(applicationContext)
        DeviceHealthWidget().updateAll(applicationContext)
        BatteryDoctorWidget().updateAll(applicationContext)
        ThermalGuardWidget().updateAll(applicationContext)
        GuardianWidget().updateAll(applicationContext)
        recordBatteryHistory()
        runCatching { smartAlertsManager.evaluateAndNotify(snapshot) }
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    /**
     * Records a battery snapshot when Battery Intelligence is unlocked and tracking is on; otherwise
     * drops a "recording paused" marker so the timeline keeps that gap empty. Never fails the job.
     */
    private suspend fun recordBatteryHistory() {
        runCatching {
            val unlocked = subscriptionRepository.entitlements.first()
                .hasFeature(PremiumFeature.BatteryIntelligence)
            val trackingEnabled = batteryHistoryStore.chargingTrackingEnabled.first()
            if (unlocked && trackingEnabled) {
                batteryRepository.getBatterySnapshot().getOrNull()?.let { batteryHistoryStore.record(it) }
            } else {
                batteryHistoryStore.markRecordingPaused()
            }
        }
    }
}
