package com.devstdvad.devicedna.data.sync

import android.util.Log
import com.devstdvad.devicedna.BuildConfig
import com.devstdvad.devicedna.data.auth.AuthRepository
import com.devstdvad.devicedna.data.sync.model.DeviceSyncPayload
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SyncOutcome {
    data object NotSignedIn : SyncOutcome
    data object NoAndroidId : SyncOutcome
    data object UpToDate : SyncOutcome
    data class Synced(val lastSyncedAt: String?) : SyncOutcome
    data class Failed(val reason: String?) : SyncOutcome
}

sealed interface AccountCheckOutcome {
    data object Verified : AccountCheckOutcome
    data object NotSignedIn : AccountCheckOutcome
    data object Removed : AccountCheckOutcome
    data object Disabled : AccountCheckOutcome
    data class Failed(val reason: String?) : AccountCheckOutcome
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
    suspend fun ensureAccountExists(): AccountCheckOutcome {
        if (authRepository.uid == null) {
            Log.d(TAG, "account check skipped: not signed in")
            return AccountCheckOutcome.NotSignedIn
        }

        val idToken = runCatching {
            withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) { authRepository.getIdToken() }
        }
            .getOrElse { exception ->
                Log.w(TAG, "account check failed to get id token: ${exception.message}", exception)
                return AccountCheckOutcome.Failed(exception.message)
            }
        if (idToken == null) {
            Log.w(TAG, "account check failed: no id token before timeout")
            return AccountCheckOutcome.Failed("No id token before timeout.")
        }

        return try {
            val status = withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) {
                api.getAccountStatus(idToken)
            } ?: return AccountCheckOutcome.Failed("Account check timed out.")

            when (status) {
                AccountStatus.Exists -> AccountCheckOutcome.Verified
                AccountStatus.NotFound -> {
                    Log.w(TAG, "account no longer exists on backend; signing out")
                    authRepository.clearLocalSession(removeGoogleAccount = true)
                    AccountCheckOutcome.Removed
                }
                AccountStatus.Disabled -> {
                    Log.w(TAG, "account is disabled on backend; signing out")
                    authRepository.clearLocalSession(removeGoogleAccount = true)
                    AccountCheckOutcome.Disabled
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Account check failed: ${e.message}", e)
            AccountCheckOutcome.Failed(e.message)
        }
    }

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
        const val ACCOUNT_CHECK_TIMEOUT_MS = 8_000L
    }
}
