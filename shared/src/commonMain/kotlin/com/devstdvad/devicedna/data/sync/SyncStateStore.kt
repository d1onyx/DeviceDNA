package com.devstdvad.devicedna.data.sync

import kotlinx.coroutines.flow.Flow

data class SyncState(
    val lastSyncTime: Long = 0L,
    val lastSyncHash: String = "",
    val lastSyncAndroidId: String = "",
)

/**
 * Local persistent sync state, platform-agnostic (Android DataStore / iOS NSUserDefaults).
 */
interface SyncStateStore {
    val state: Flow<SyncState>
    suspend fun current(): SyncState
    suspend fun update(timeMs: Long, hash: String, androidId: String)
}
