package com.devstdvad.devicedna.navigation

import androidx.compose.runtime.Composable

/**
 * Platform back-navigation handler. Android maps to androidx.activity.compose.BackHandler;
 * iOS has no hardware back, so its actual is a no-op (navigation uses on-screen back buttons).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
