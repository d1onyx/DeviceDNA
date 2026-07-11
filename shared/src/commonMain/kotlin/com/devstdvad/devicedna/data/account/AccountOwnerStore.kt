package com.devstdvad.devicedna.data.account

import com.russhwolf.settings.Settings

/**
 * Persists the Firebase uid that owns the local data currently on this device. Device-level state,
 * deliberately NOT a [ClearableStore]: [LocalDataWiper.wipeAll] must not erase it, otherwise the
 * device would forget which account its data belongs to.
 */
interface AccountOwnerStore {
    fun getOwner(): String?
    fun setOwner(uid: String)
    fun clear()
}

/**
 * [AccountOwnerStore] backed by a [Settings] store kept separate from every wiped store's keys.
 * The `Settings` dependency stays inside the shared module (Android supplies it via a factory,
 * iOS via NSUserDefaultsSettings) so it never leaks onto the platform classpaths.
 */
class SettingsAccountOwnerStore(private val settings: Settings) : AccountOwnerStore {

    override fun getOwner(): String? = settings.getStringOrNull(KEY)

    override fun setOwner(uid: String) = settings.putString(KEY, uid)

    override fun clear() = settings.remove(KEY)

    private companion object {
        const val KEY = "account_owner_uid"
    }
}
