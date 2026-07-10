package com.devstdvad.devicedna.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore by preferencesDataStore("device_dna_sync")

/** Android [SyncStateStore] backed by DataStore. */
class AndroidSyncStateStore(private val context: Context) : SyncStateStore {

    override val state: Flow<SyncState> = context.syncDataStore.data.map { prefs ->
        SyncState(
            lastSyncTime = prefs[LAST_SYNC_TIME] ?: 0L,
            lastSyncHash = prefs[LAST_SYNC_HASH] ?: "",
            lastSyncAndroidId = prefs[LAST_SYNC_ANDROID_ID] ?: "",
        )
    }

    override suspend fun current(): SyncState = state.first()

    override suspend fun update(timeMs: Long, hash: String, androidId: String) {
        context.syncDataStore.edit {
            it[LAST_SYNC_TIME] = timeMs
            it[LAST_SYNC_HASH] = hash
            it[LAST_SYNC_ANDROID_ID] = androidId
        }
    }

    override suspend fun clear() {
        context.syncDataStore.edit { it.clear() }
    }

    private companion object {
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val LAST_SYNC_HASH = stringPreferencesKey("last_sync_hash")
        val LAST_SYNC_ANDROID_ID = stringPreferencesKey("last_sync_android_id")
    }
}
