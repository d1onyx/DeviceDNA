package com.devstdvad.devicedna.data.cfg

import android.content.Context
import android.util.Base64
import com.russhwolf.settings.SharedPreferencesSettings

fun buildAndroidConfigSync(
    context: Context,
    cfgProjectId: String,
    cfgDocPath: String,
    publicKeyBase64: String,
    appName: String = "cfg-sync",
    startupRefreshWindowMs: Long = 5_000L,
): ConfigSync {
    val verifier = publicKeyBase64
        .takeIf { it.isNotBlank() }
        ?.let { PayloadCheck(Base64.decode(it, Base64.NO_WRAP)) }
    val config = SyncConfig(
        enabled = cfgProjectId.isNotBlank(),
        documentPath = cfgDocPath,
        appName = appName,
        publicKeyBase64 = publicKeyBase64,
    )
    val settings = SharedPreferencesSettings(
        context.getSharedPreferences("sync", Context.MODE_PRIVATE),
    )
    SyncMarker.attach(settings)
    if (!config.enabled || verifier == null) {
        return ConfigSync(store = null, updates = null)
    }
    val source = RemoteConfigSource(
        documentPath = config.documentPath,
        check = verifier,
        appName = config.appName,
    )
    return ConfigSync(
        store = SettingsConfigStore(settings),
        updates = { source.updates() },
        startupRefreshWindowMs = startupRefreshWindowMs,
    )
}
