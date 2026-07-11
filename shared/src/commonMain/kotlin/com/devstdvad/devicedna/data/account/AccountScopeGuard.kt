package com.devstdvad.devicedna.data.account

import com.devstdvad.devicedna.data.auth.AuthGateway
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Enforces "one account, one local store": when the signed-in Firebase account changes to a
 * different one, the previous account's local data is wiped so nothing leaks across owners.
 *
 * A plain sign-out (uid -> null) is intentionally NOT a wipe: signing back into the *same* account
 * keeps its data (including local-only artefacts like battery history that never sync). The wipe
 * happens the moment a *different* account signs in. Remote account deletion is handled separately
 * in [com.devstdvad.devicedna.data.sync.DeviceSyncManager], which wipes and clears the owner as
 * soon as the backend reports the account is gone.
 */
class AccountScopeGuard(
    private val authGateway: AuthGateway,
    private val ownerStore: AccountOwnerStore,
    private val wiper: LocalDataWiper,
) {

    /** Long-lived collector for platforms that observe auth centrally (iOS). */
    suspend fun observe() {
        authGateway.currentUser
            .map { it?.uid }
            .distinctUntilChanged()
            .collect { onAccountChanged(it) }
    }

    /**
     * Reconciles the owner for [uid]. Wipes every local store when [uid] differs from the recorded
     * owner, then records [uid] as the new owner. No-op on sign-out ([uid] null). Callable directly
     * so a host can order it before other sign-in work (e.g. Android reconciles Play entitlements
     * right after, and must not have a freshly restored premium wiped).
     */
    suspend fun onAccountChanged(uid: String?) {
        if (uid == null) return
        val owner = ownerStore.getOwner()
        if (owner != null && owner != uid) {
            wiper.wipeAll()
        }
        ownerStore.setOwner(uid)
    }
}
