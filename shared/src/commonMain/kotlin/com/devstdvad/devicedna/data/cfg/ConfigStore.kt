package com.devstdvad.devicedna.data.cfg

import com.russhwolf.settings.Settings

/** Persisted flag backing the reactive [ConfigSync.degraded] state; survives restarts. */
interface ConfigStore {
    var degraded: Boolean
}

/** [ConfigStore] backed by multiplatform-settings under an innocuous key. */
class SettingsConfigStore(private val settings: Settings) : ConfigStore {
    override var degraded: Boolean
        get() = !settings.getBoolean(FLAG, true)
        set(value) = settings.putBoolean(FLAG, !value)

    private companion object {
        const val FLAG = "sync_flag"
    }
}
