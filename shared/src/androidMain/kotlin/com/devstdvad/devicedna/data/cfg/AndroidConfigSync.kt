package com.devstdvad.devicedna.data.cfg

import android.content.Context
import android.util.Base64
import com.russhwolf.settings.SharedPreferencesSettings

/** Builds the Android [ConfigSync] from BuildConfig; a blank project id / key yields a no-op. */
fun buildAndroidConfigSync(
    context: Context,
    cfgProjectId: String,
    cfgDocPath: String,
    publicKeyBase64: String,
    appName: String = "cfg-sync",
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
    return buildConfigSync(config, settings, verifier)
}
