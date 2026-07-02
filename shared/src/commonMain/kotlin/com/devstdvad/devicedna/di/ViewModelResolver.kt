package com.devstdvad.devicedna.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass

/**
 * Cross-platform Koin ViewModel resolution for Compose.
 *
 * Deliberately NOT `inline`/`reified`: `@Composable inline fun <reified T>` triggers a known,
 * unresolved Kotlin/Native static-framework linking bug — "Symbol ... is unbound" for iOS builds
 * (see JetBrains/compose-multiplatform#2900). This is exactly why Koin's own
 * `org.koin.compose.viewmodel.koinViewModel<T>()` fails to link on iOS with an
 * "undefined symbol ...LocalViewModelStoreOwner$stableprop_getter$artificial" error
 * (InsertKoinIO/koin#2175, still open at the Kotlin-compiler level).
 *
 * Call sites pass the KClass explicitly instead of relying on reified inference:
 *   viewModel: DisplayViewModel = resolveViewModel(DisplayViewModel::class)
 */
@Composable
expect fun <T : ViewModel> resolveViewModel(kClass: KClass<T>): T
