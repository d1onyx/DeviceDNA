package com.devstdvad.devicedna.data.account

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Builds the Android [AccountOwnerStore] over a dedicated SharedPreferences file. Kept in the
 * shared module (which owns the multiplatform-settings dependency) so the app module never names
 * the `Settings` type, mirroring `buildAndroidConfigSync`.
 */
fun buildAndroidAccountOwnerStore(context: Context): AccountOwnerStore =
    SettingsAccountOwnerStore(
        SharedPreferencesSettings(context.getSharedPreferences("account_owner", Context.MODE_PRIVATE)),
    )
