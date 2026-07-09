package com.devstdvad.devicedna.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.devstdvad.devicedna.data.cfg.ConfigSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App boot phase used to gate the shell: [Preparing] while warming up, [Ready] for the normal UI. */
enum class AppPhase { Preparing, Ready }

/**
 * Derives the app boot [phase] from readiness signals. Ready on a normal launch (the signal settles
 * synchronously before the first frame), so there is no visible warm-up flash.
 */
class AppReadiness(
    source: ConfigSync,
    scope: CoroutineScope,
) {
    val phase: StateFlow<AppPhase> = source.degraded
        .map { if (it) AppPhase.Preparing else AppPhase.Ready }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            if (source.degraded.value) AppPhase.Preparing else AppPhase.Ready,
        )
}

/** Neutral warm-up screen shown while the app is [AppPhase.Preparing]. */
@Composable
fun PreparingScreen() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
