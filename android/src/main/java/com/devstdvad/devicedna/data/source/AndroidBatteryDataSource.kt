package com.devstdvad.devicedna.data.source

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidBatteryDataSource(private val context: Context) {

    fun observeBattery(): Flow<AppResult<BatteryInfo>> = callbackFlow {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(readBatteryFromIntent(intent))
            }
        }
        // Emit current state immediately before waiting for the next broadcast
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { trySend(readBatteryFromIntent(it)) }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun getBatterySnapshot(): AppResult<BatteryInfo> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return AppResult.Error(AppError.Unavailable("Battery info unavailable"))
        return readBatteryFromIntent(intent)
    }

    private fun readBatteryFromIntent(intent: Intent): AppResult<BatteryInfo> = runCatching {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val levelPercent = if (scale > 0) (level * 100 / scale) else level

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.Charging
            BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.Discharging
            BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.Full
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.NotCharging
            else -> BatteryStatus.Unknown
        }
        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.Good
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.Overheat
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.Dead
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OverVoltage
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.Cold
            else -> BatteryHealth.Unknown
        }
        val source = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargeSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargeSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargeSource.Wireless
            BatteryManager.BATTERY_PLUGGED_DOCK -> ChargeSource.Dock
            else -> ChargeSource.Unknown
        }
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val currentMa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeIf { it != Int.MIN_VALUE }?.let { it / 1000 }
        val capacityMah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it > 0 }?.let { it / 1000 }
        val estimatedWatts = currentMa?.let { current ->
            if (voltageMv > 0) kotlin.math.abs(current.toFloat()) * voltageMv / 1_000_000f else null
        }
        val chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            batteryManager.computeChargeTimeRemaining().takeIf { it > 0L }
        } else null
        val chargeCycles = readChargeCycles(batteryManager)

        BatteryInfo(
            levelPercent = levelPercent,
            status = status,
            health = health,
            source = source,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion",
            temperatureCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltageMv = voltageMv,
            currentMa = currentMa,
            capacityMah = capacityMah,
            chargeCycles = chargeCycles,
            isPresent = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true),
            estimatedWatts = estimatedWatts,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            isPowerSaveMode = powerManager.isPowerSaveMode,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Battery read failed")) },
    )

    private fun readChargeCycles(batteryManager: BatteryManager): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null

        return runCatching {
            val propertyId = BatteryManager::class.java
                .getField("BATTERY_PROPERTY_CYCLE_COUNT")
                .getInt(null)
            batteryManager.getIntProperty(propertyId).takeIf { it >= 0 }
        }.getOrNull()
    }
}
