package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.auth.AuthGateway
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
 * Platform-agnostic: auth, snapshot building, state persistence and transport are all injected.
 */
class DeviceSyncManager(
    private val authRepository: AuthGateway,
    private val snapshotBuilder: DeviceSnapshotProvider,
    private val api: SyncApi,
    private val stateStore: SyncStateStore,
    private val now: () -> Long = { currentTimeMillis() },
) {
    suspend fun ensureAccountExists(): AccountCheckOutcome {
        if (authRepository.uid == null) return AccountCheckOutcome.NotSignedIn

        val idToken = runCatching {
            withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) { authRepository.getIdToken() }
        }.getOrElse { exception ->
            return AccountCheckOutcome.Failed(exception.message)
        }
        if (idToken == null) {
            return AccountCheckOutcome.Failed("No id token before timeout.")
        }

        return try {
            val status = withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) {
                api.getAccountStatus(idToken)
            } ?: return AccountCheckOutcome.Failed("Account check timed out.")

            when (status) {
                AccountStatus.Exists -> AccountCheckOutcome.Verified
                AccountStatus.NotFound -> {
                    authRepository.clearLocalSession(removeGoogleAccount = true)
                    AccountCheckOutcome.Removed
                }
                AccountStatus.Disabled -> {
                    authRepository.clearLocalSession(removeGoogleAccount = true)
                    AccountCheckOutcome.Disabled
                }
            }
        } catch (e: Exception) {
            AccountCheckOutcome.Failed(e.message)
        }
    }

    /**
     * Best-effort server-side purge of the account's data, called (while still authenticated) just
     * before the Firebase Auth user is deleted. Returns true when the backend confirmed the delete.
     */
    suspend fun deleteAccountData(): Boolean {
        val idToken = runCatching {
            withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) { authRepository.getIdToken() }
        }.getOrNull() ?: return false
        return runCatching {
            withTimeoutOrNull(ACCOUNT_CHECK_TIMEOUT_MS) { api.deleteAccountData(idToken) } ?: false
        }.getOrDefault(false)
    }

    suspend fun syncIfNeeded(force: Boolean = false): SyncOutcome {
        if (authRepository.uid == null) return SyncOutcome.NotSignedIn
        val idToken = authRepository.getIdToken() ?: return SyncOutcome.NotSignedIn

        val snapshot = snapshotBuilder.build()
        val deviceId = snapshot.device?.androidId?.takeIf { it.isNotBlank() }
            ?: return SyncOutcome.NoAndroidId
        val localHash = SnapshotHasher.stableHash(snapshot)

        return try {
            val status = api.getStatus(idToken, deviceId)
            val local = stateStore.current()
            val needPush = SyncDecision.shouldPush(
                force = force,
                serverExists = status.exists,
                serverHash = status.snapshotHash,
                localHash = localHash,
                lastSyncTimeMs = local.lastSyncTime,
                nowMs = now(),
            )
            if (!needPush) {
                SyncOutcome.UpToDate
            } else {
                val payload = DeviceSyncPayload(
                    deviceId = deviceId,
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
                stateStore.update(now(), localHash, deviceId)
                SyncOutcome.Synced(result.lastSyncedAt)
            }
        } catch (e: Exception) {
            SyncOutcome.Failed(e.message)
        }
    }

    private companion object {
        const val ACCOUNT_CHECK_TIMEOUT_MS = 8_000L
    }
}
