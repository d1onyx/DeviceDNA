package com.devstdvad.devicedna.data.account

/** A local store that can erase everything it persists. */
interface ClearableStore {
    suspend fun clear()
}

/**
 * Erases every locally persisted user artefact after the account is deleted, so nothing survives
 * on-device once the server-side data and the Firebase user are gone.
 *
 * The config-sync store is deliberately not part of [stores]: it is device state, not user data.
 */
class LocalDataWiper(private val stores: List<ClearableStore>) {

    /** Best-effort: a store that fails must not stop the remaining ones from being cleared. */
    suspend fun wipeAll() {
        stores.forEach { store -> runCatching { store.clear() } }
    }
}
