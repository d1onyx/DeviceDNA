package com.devstdvad.devicedna.navigation

import androidx.compose.runtime.Composable

/** iOS has no hardware back button — navigation relies on on-screen back controls. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
