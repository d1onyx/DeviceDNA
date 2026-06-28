package com.devstdvad.devicedna.data.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore("device_dna_widget_cache")

/** Persists the latest [WidgetSnapshot] for home-screen widgets. */
class WidgetSnapshotCache(private val context: Context) {

    val snapshot: Flow<WidgetSnapshot> = context.widgetDataStore.data.map { p ->
        WidgetSnapshot(
            isPremium = p[IS_PREMIUM] ?: false,
            hasData = p[HAS_DATA] ?: false,
            lastUpdatedMillis = p[LAST_UPDATED] ?: 0L,
            batteryLevel = p[BATTERY_LEVEL] ?: -1,
            batteryTempC = p[BATTERY_TEMP] ?: 0f,
            batteryStatus = p[BATTERY_STATUS] ?: "",
            batteryHealth = p[BATTERY_HEALTH] ?: "",
            ramUsedPercent = p[RAM_USED_PCT] ?: 0f,
            ramUsedBytes = p[RAM_USED_BYTES] ?: 0L,
            ramTotalBytes = p[RAM_TOTAL_BYTES] ?: 0L,
            storageUsedPercent = p[STORAGE_USED_PCT] ?: 0f,
            storageUsedBytes = p[STORAGE_USED_BYTES] ?: 0L,
            storageTotalBytes = p[STORAGE_TOTAL_BYTES] ?: 0L,
            cpuUsagePercent = p[CPU_USAGE_PCT] ?: -1f,
            cpuTempC = p[CPU_TEMP] ?: 0f,
            thermalMaxC = p[THERMAL_MAX] ?: 0f,
            healthOverall = p[HEALTH_OVERALL] ?: -1,
            healthInsight = p[HEALTH_INSIGHT] ?: "",
            healthSeverity = p[HEALTH_SEVERITY] ?: "",
            batteryWearPercent = p[BATTERY_WEAR] ?: -1,
            batteryCycles = p[BATTERY_CYCLES] ?: -1,
            batteryWatts = p[BATTERY_WATTS] ?: 0f,
            batteryChargeTimeMs = p[BATTERY_CHARGE_TIME] ?: 0L,
            batteryCharging = p[BATTERY_CHARGING] ?: false,
            thermalStatus = p[THERMAL_STATUS] ?: -1,
            cpuCurFreqMhz = p[CPU_CUR_FREQ] ?: 0,
            cpuMaxFreqMhz = p[CPU_MAX_FREQ] ?: 0,
            isRooted = p[IS_ROOTED] ?: false,
            integrityIssues = p[INTEGRITY_ISSUES] ?: "",
            fraudLevel = p[FRAUD_LEVEL] ?: "",
        )
    }

    suspend fun current(): WidgetSnapshot = snapshot.first()

    suspend fun save(s: WidgetSnapshot) {
        context.widgetDataStore.edit { p ->
            p[IS_PREMIUM] = s.isPremium
            p[HAS_DATA] = s.hasData
            p[LAST_UPDATED] = s.lastUpdatedMillis
            p[BATTERY_LEVEL] = s.batteryLevel
            p[BATTERY_TEMP] = s.batteryTempC
            p[BATTERY_STATUS] = s.batteryStatus
            p[BATTERY_HEALTH] = s.batteryHealth
            p[RAM_USED_PCT] = s.ramUsedPercent
            p[RAM_USED_BYTES] = s.ramUsedBytes
            p[RAM_TOTAL_BYTES] = s.ramTotalBytes
            p[STORAGE_USED_PCT] = s.storageUsedPercent
            p[STORAGE_USED_BYTES] = s.storageUsedBytes
            p[STORAGE_TOTAL_BYTES] = s.storageTotalBytes
            p[CPU_USAGE_PCT] = s.cpuUsagePercent
            p[CPU_TEMP] = s.cpuTempC
            p[THERMAL_MAX] = s.thermalMaxC
            p[HEALTH_OVERALL] = s.healthOverall
            p[HEALTH_INSIGHT] = s.healthInsight
            p[HEALTH_SEVERITY] = s.healthSeverity
            p[BATTERY_WEAR] = s.batteryWearPercent
            p[BATTERY_CYCLES] = s.batteryCycles
            p[BATTERY_WATTS] = s.batteryWatts
            p[BATTERY_CHARGE_TIME] = s.batteryChargeTimeMs
            p[BATTERY_CHARGING] = s.batteryCharging
            p[THERMAL_STATUS] = s.thermalStatus
            p[CPU_CUR_FREQ] = s.cpuCurFreqMhz
            p[CPU_MAX_FREQ] = s.cpuMaxFreqMhz
            p[IS_ROOTED] = s.isRooted
            p[INTEGRITY_ISSUES] = s.integrityIssues
            p[FRAUD_LEVEL] = s.fraudLevel
        }
    }

    private companion object {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val HAS_DATA = booleanPreferencesKey("has_data")
        val LAST_UPDATED = longPreferencesKey("last_updated")
        val BATTERY_LEVEL = intPreferencesKey("battery_level")
        val BATTERY_TEMP = floatPreferencesKey("battery_temp")
        val BATTERY_STATUS = stringPreferencesKey("battery_status")
        val BATTERY_HEALTH = stringPreferencesKey("battery_health")
        val RAM_USED_PCT = floatPreferencesKey("ram_used_pct")
        val RAM_USED_BYTES = longPreferencesKey("ram_used_bytes")
        val RAM_TOTAL_BYTES = longPreferencesKey("ram_total_bytes")
        val STORAGE_USED_PCT = floatPreferencesKey("storage_used_pct")
        val STORAGE_USED_BYTES = longPreferencesKey("storage_used_bytes")
        val STORAGE_TOTAL_BYTES = longPreferencesKey("storage_total_bytes")
        val CPU_USAGE_PCT = floatPreferencesKey("cpu_usage_pct")
        val CPU_TEMP = floatPreferencesKey("cpu_temp")
        val THERMAL_MAX = floatPreferencesKey("thermal_max")
        val HEALTH_OVERALL = intPreferencesKey("health_overall")
        val HEALTH_INSIGHT = stringPreferencesKey("health_insight")
        val HEALTH_SEVERITY = stringPreferencesKey("health_severity")
        val BATTERY_WEAR = intPreferencesKey("battery_wear")
        val BATTERY_CYCLES = intPreferencesKey("battery_cycles")
        val BATTERY_WATTS = floatPreferencesKey("battery_watts")
        val BATTERY_CHARGE_TIME = longPreferencesKey("battery_charge_time")
        val BATTERY_CHARGING = booleanPreferencesKey("battery_charging")
        val THERMAL_STATUS = intPreferencesKey("thermal_status")
        val CPU_CUR_FREQ = intPreferencesKey("cpu_cur_freq")
        val CPU_MAX_FREQ = intPreferencesKey("cpu_max_freq")
        val IS_ROOTED = booleanPreferencesKey("is_rooted")
        val INTEGRITY_ISSUES = stringPreferencesKey("integrity_issues")
        val FRAUD_LEVEL = stringPreferencesKey("fraud_level")
    }
}
