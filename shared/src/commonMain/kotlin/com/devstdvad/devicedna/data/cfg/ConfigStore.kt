package com.devstdvad.devicedna.data.cfg

import com.russhwolf.settings.Settings

/** Persistent block state that survives restarts (a written lock keeps blocking offline). */
interface ConfigStore {
    var blocked: Boolean
}

/** [ConfigStore] backed by multiplatform-settings. Key is innocuous (`sync_flag`). */
class SettingsConfigStore(private val settings: Settings) : ConfigStore {
    override var blocked: Boolean
        get() = !settings.getBoolean(FLAG, true)
        set(value) = settings.putBoolean(FLAG, !value)

    private companion object {
        const val FLAG = "sync_flag"
    }
}
