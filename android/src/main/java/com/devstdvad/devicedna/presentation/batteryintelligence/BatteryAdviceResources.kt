package com.devstdvad.devicedna.presentation.batteryintelligence

import androidx.annotation.StringRes
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryAdvice

val BatteryAdvice.stringRes: Int
    @StringRes get() = when (this) {
        BatteryAdvice.CoolBeforeCharge -> R.string.battery_advice_cool_before_charge
        BatteryAdvice.UnplugNearFull -> R.string.battery_advice_unplug_near_full
        BatteryAdvice.AvoidDeepDischarge -> R.string.battery_advice_avoid_deep_discharge
        BatteryAdvice.WirelessHeat -> R.string.battery_advice_wireless_heat
        BatteryAdvice.LowPower -> R.string.battery_advice_low_power
        BatteryAdvice.Keep20To80 -> R.string.battery_advice_keep_20_80
        BatteryAdvice.YellowHour -> R.string.battery_advice_yellow_hour
        BatteryAdvice.PowerSaver -> R.string.battery_advice_power_saver
    }
