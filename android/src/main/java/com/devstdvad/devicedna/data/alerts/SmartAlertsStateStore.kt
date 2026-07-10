package com.devstdvad.devicedna.data.alerts

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devstdvad.devicedna.data.account.ClearableStore
import kotlinx.coroutines.flow.first

private val Context.smartAlertsDataStore by preferencesDataStore("device_dna_smart_alerts")

data class AlertState(val lastNotifiedMillis: Long = 0L, val wasActive: Boolean = false)

/** Persists per-alert notification state so the same condition does not spam every 15 min. */
class SmartAlertsStateStore(private val context: Context) : ClearableStore {

    suspend fun state(type: SmartAlertType): AlertState {
        val prefs = context.smartAlertsDataStore.data.first()
        return AlertState(
            lastNotifiedMillis = prefs[lastKey(type)] ?: 0L,
            wasActive = prefs[activeKey(type)] ?: false,
        )
    }

    suspend fun update(type: SmartAlertType, lastNotifiedMillis: Long, wasActive: Boolean) {
        context.smartAlertsDataStore.edit {
            it[lastKey(type)] = lastNotifiedMillis
            it[activeKey(type)] = wasActive
        }
    }

    override suspend fun clear() {
        context.smartAlertsDataStore.edit { it.clear() }
    }

    private fun lastKey(type: SmartAlertType) = longPreferencesKey("smart_${type.key}_last")
    private fun activeKey(type: SmartAlertType) = booleanPreferencesKey("smart_${type.key}_active")

    companion object {
        const val RENOTIFY_COOLDOWN_MS = 2L * 60L * 60L * 1000L // 2 hours

        /**
         * Pure decision: notify on inactive→active edge, and re-notify if still active after the
         * cooldown. Returns false when the condition is not active (caller clears the state).
         */
        fun shouldNotify(
            isActive: Boolean,
            wasActive: Boolean,
            lastNotifiedMillis: Long,
            nowMillis: Long,
            cooldownMs: Long = RENOTIFY_COOLDOWN_MS,
        ): Boolean {
            if (!isActive) return false
            if (!wasActive) return true
            return nowMillis - lastNotifiedMillis >= cooldownMs
        }
    }
}
