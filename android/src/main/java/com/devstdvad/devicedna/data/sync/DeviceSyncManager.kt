package com.devstdvad.devicedna.data.sync

import android.util.Log
import com.devstdvad.devicedna.BuildConfig
import com.devstdvad.devicedna.data.auth.AuthRepository
import com.devstdvad.devicedna.data.sync.model.DeviceSyncPayload

sealed interface SyncOutcome {
    data object NotSignedIn : SyncOutcome
    data object NoAndroidId : SyncOutcome
    data object UpToDate : SyncOutcome
    data class Synced(val lastSyncedAt: String?) : SyncOutcome
    data class Failed(val reason: String?) : SyncOutcome
}

/**
 * Checks the device sync status on the server and pushes a full snapshot when needed.
 * Network errors are swallowed (returns [SyncOutcome.Failed]) so the UI is never blocked.
 */
class DeviceSyncManager(
    private val authRepository: AuthRepository,
    private val snapshotBuilder: DeviceSnapshotBuilder,
    private val api: SyncApi,
    private val stateStore: SyncStateStore,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun syncIfNeeded(force: Boolean = false): SyncOutcome {
        Log.d(TAG, "syncIfNeeded(force=$force) baseUrl=${BuildConfig.SYNC_BASE_URL}")

        if (authRepository.uid == null) {
            Log.d(TAG, "skip: not signed in")
            return SyncOutcome.NotSignedIn
        }
        val idToken = authRepository.getIdToken()
        if (idToken == null) {
            Log.d(TAG, "skip: no id token")
            return SyncOutcome.NotSignedIn
        }
        // Debug only, for manual backend testing via curl.
        if (BuildConfig.DEBUG) Log.d(TAG, "idToken=$idToken")

        val snapshot = snapshotBuilder.build()
        val androidId = snapshot.device?.androidId?.takeIf { it.isNotBlank() }
        if (androidId == null) {
            Log.w(TAG, "skip: empty androidId")
            return SyncOutcome.NoAndroidId
        }
        val localHash = SnapshotHasher.stableHash(snapshot)

        return try {
            val status = api.getStatus(idToken, androidId)
            val local = stateStore.current()
            val needPush = SyncDecision.shouldPush(
                force = force,
                serverExists = status.exists,
                serverHash = status.snapshotHash,
                localHash = localHash,
                lastSyncTimeMs = local.lastSyncTime,
                nowMs = now(),
            )
            Log.d(TAG, "status(exists=${status.exists}, serverHash=${status.snapshotHash}) localHash=$localHash needPush=$needPush")
            if (!needPush) {
                SyncOutcome.UpToDate
            } else {
                val payload = DeviceSyncPayload(
                    androidId = androidId,
                    deviceName = snapshot.device?.name,
                    manufacturer = snapshot.device?.manufacturer,
                    model = snapshot.device?.model,
                    osVersion = snapshot.system?.androidVersion,
                    appVersion = snapshot.system
                        ?.let { "${it.appVersionName} (${it.appVersionCode})" },
                    snapshotHash = localHash,
                    snapshot = snapshot,
                )
                val result = api.postSync(idToken, payload)
                stateStore.update(now(), localHash, androidId)
                Log.d(TAG, "Synced device $androidId at ${result.lastSyncedAt}")
                SyncOutcome.Synced(result.lastSyncedAt)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed: ${e.message}", e)
            SyncOutcome.Failed(e.message)
        }
    }

    private companion object {
        const val TAG = "DeviceSync"
    }
}
