package com.devstdvad.devicedna.widget.glance

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryStatus
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Returns a [Context] localized to the language the user picked inside the app
 * (`settings.appLanguage`), falling back to the system locale when it is unset.
 *
 * Glance widgets render in a background context (the receiver/worker) that does NOT inherit the
 * Activity's per-app locale override, so every widget must build this explicitly — otherwise the
 * widget would ignore the in-app language choice and always use the system locale. Read once at
 * the top of `provideGlance` and use the result for all `getString` / formatting calls.
 */
suspend fun localizedWidgetContext(context: Context): Context {
    val language = runCatching { SettingsStore(context).settings.first().appLanguage }.getOrDefault("")
    return context.withLanguage(language)
}

/** Wraps the context so its resources resolve against [languageTag] (e.g. "de"); blank = unchanged. */
fun Context.withLanguage(languageTag: String): Context {
    if (languageTag.isBlank()) return this
    val locale = Locale.forLanguageTag(languageTag)
    val configuration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    val localized = createConfigurationContext(configuration)
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localized.resources
        override fun getAssets() = localized.assets
    }
}

/** The effective locale of this (possibly localized) context, used for number/date formatting. */
fun Context.currentLocale(): Locale = resources.configuration.locales[0]

/** PowerManager.THERMAL_STATUS_* → localized label. */
fun Context.thermalStatusText(status: Int): String = getString(
    when (status) {
        0 -> R.string.thermal_status_normal
        1 -> R.string.thermal_status_light
        2 -> R.string.thermal_status_moderate
        3 -> R.string.thermal_status_throttling
        4 -> R.string.thermal_status_severe
        5, 6 -> R.string.thermal_status_critical
        else -> R.string.widget_value_unknown
    },
)

/** [BatteryStatus] name (as stored in the snapshot) → localized label. */
fun Context.batteryStatusText(name: String): String = getString(
    when (name) {
        BatteryStatus.Charging.name -> R.string.battery_status_charging
        BatteryStatus.Discharging.name -> R.string.battery_status_discharging
        BatteryStatus.Full.name -> R.string.battery_status_full
        BatteryStatus.NotCharging.name -> R.string.battery_status_not_charging
        else -> R.string.widget_value_unknown
    },
)

/** [BatteryHealth] name (as stored in the snapshot) → localized label. */
fun Context.batteryHealthText(name: String): String = getString(
    when (name) {
        BatteryHealth.Good.name -> R.string.battery_health_good
        BatteryHealth.Overheat.name -> R.string.battery_health_overheat
        BatteryHealth.Dead.name -> R.string.battery_health_dead
        BatteryHealth.OverVoltage.name -> R.string.battery_health_overvoltage
        BatteryHealth.Failure.name -> R.string.battery_health_failure
        BatteryHealth.Cold.name -> R.string.battery_health_cold
        else -> R.string.widget_value_unknown
    },
)

/**
 * Maps a single integrity-issue key (as emitted by `WidgetMetricsLoader.integrityIssues`) to a
 * localized phrase. The fraud key carries the level after a colon, e.g. "fraud:High". Unknown
 * keys are returned verbatim so stale (pre-update) cached snapshots still show something readable
 * until the next background refresh rewrites them as keys.
 */
fun Context.integrityIssueText(key: String): String = when {
    key == "root" -> getString(R.string.widget_integrity_root)
    key == "suspicious_files" -> getString(R.string.widget_integrity_suspicious_files)
    key == "adb" -> getString(R.string.widget_integrity_adb)
    key == "dev_options" -> getString(R.string.widget_integrity_dev_options)
    key == "emulator" -> getString(R.string.widget_integrity_emulator)
    key.startsWith("fraud:") -> getString(R.string.widget_integrity_fraud, fraudLevelText(key.substringAfter("fraud:")))
    else -> key
}

private fun Context.fraudLevelText(level: String): String = getString(
    when (level) {
        "Medium" -> R.string.fraud_level_medium
        "High" -> R.string.fraud_level_high
        "Critical" -> R.string.fraud_level_critical
        else -> R.string.fraud_level_low
    },
)
