package com.devstdvad.devicedna.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

/** iOS [SyncStateStore] backed by NSUserDefaults. */
class IosSyncStateStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SyncStateStore {

    private companion object {
        const val LAST_SYNC_TIME = "last_sync_time"
        const val LAST_SYNC_HASH = "last_sync_hash"
        const val LAST_SYNC_ANDROID_ID = "last_sync_android_id"
    }

    private val stateFlow = MutableStateFlow(load())

    override val state: Flow<SyncState> = stateFlow

    private fun load() = SyncState(
        lastSyncTime = defaults.integerForKey(LAST_SYNC_TIME),
        lastSyncHash = defaults.stringForKey(LAST_SYNC_HASH) ?: "",
        lastSyncAndroidId = defaults.stringForKey(LAST_SYNC_ANDROID_ID) ?: "",
    )

    override suspend fun current(): SyncState = stateFlow.value

    override suspend fun update(timeMs: Long, hash: String, androidId: String) {
        defaults.setInteger(timeMs, LAST_SYNC_TIME)
        defaults.setObject(hash, LAST_SYNC_HASH)
        defaults.setObject(androidId, LAST_SYNC_ANDROID_ID)
        stateFlow.value = load()
    }

    override suspend fun clear() {
        listOf(LAST_SYNC_TIME, LAST_SYNC_HASH, LAST_SYNC_ANDROID_ID)
            .forEach { defaults.removeObjectForKey(it) }
        stateFlow.value = load()
    }
}
