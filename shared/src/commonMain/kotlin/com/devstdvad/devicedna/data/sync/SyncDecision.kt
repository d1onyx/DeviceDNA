package com.devstdvad.devicedna.data.sync

/** Pure decision logic: whether the snapshot must be pushed to the server. */
object SyncDecision {

    const val STALE_AFTER_MS: Long = 24L * 60L * 60L * 1000L

    fun shouldPush(
        force: Boolean,
        serverExists: Boolean,
        serverHash: String?,
        localHash: String,
        lastSyncTimeMs: Long,
        nowMs: Long,
        staleAfterMs: Long = STALE_AFTER_MS,
    ): Boolean {
        if (force) return true
        if (!serverExists) return true
        if (serverHash != localHash) return true
        if (nowMs - lastSyncTimeMs > staleAfterMs) return true
        return false
    }
}
