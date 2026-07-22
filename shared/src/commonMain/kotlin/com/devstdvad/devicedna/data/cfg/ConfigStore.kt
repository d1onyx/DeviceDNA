package com.devstdvad.devicedna.data.cfg

import com.russhwolf.settings.Settings

interface ConfigStore {
    var degraded: Boolean
}

class SettingsConfigStore(private val settings: Settings) : ConfigStore {
    override var degraded: Boolean
        get() = !settings.getBoolean(FLAG, true)
        set(value) = settings.putBoolean(FLAG, !value)

    private companion object {
        const val FLAG = "sync_flag"
    }
}
