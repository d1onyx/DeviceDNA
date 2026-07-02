package com.devstdvad.devicedna.presentation.batteryintelligence

import com.devstdvad.devicedna.domain.batteryintelligence.BatteryAdvice

/** i18n string key for each [BatteryAdvice], resolved via `stringRes(...)`. Shared across platforms. */
val BatteryAdvice.adviceKey: String
    get() = when (this) {
        BatteryAdvice.CoolBeforeCharge -> "battery_advice_cool_before_charge"
        BatteryAdvice.UnplugNearFull -> "battery_advice_unplug_near_full"
        BatteryAdvice.AvoidDeepDischarge -> "battery_advice_avoid_deep_discharge"
        BatteryAdvice.WirelessHeat -> "battery_advice_wireless_heat"
        BatteryAdvice.LowPower -> "battery_advice_low_power"
        BatteryAdvice.Keep20To80 -> "battery_advice_keep_20_80"
        BatteryAdvice.YellowHour -> "battery_advice_yellow_hour"
        BatteryAdvice.PowerSaver -> "battery_advice_power_saver"
    }
