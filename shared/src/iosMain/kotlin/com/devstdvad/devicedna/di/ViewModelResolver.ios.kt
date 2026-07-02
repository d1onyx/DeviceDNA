package com.devstdvad.devicedna.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import kotlin.reflect.KClass
import org.koin.compose.currentKoinScope

/**
 * iOS actual: resolves via Koin's own `Scope.get(KClass)` (a plain, non-inline, non-reified
 * function — no androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner involved at all),
 * cached with `remember` so the same instance survives recomposition for as long as the
 * composable stays in the tree. iOS has no Android-style configuration-change teardown to
 * survive, so this is sufficient. See the expect declaration's KDoc for why
 * androidx.lifecycle.viewmodel.compose is avoided here.
 */
@Composable
actual fun <T : ViewModel> resolveViewModel(kClass: KClass<T>): T {
    val scope = currentKoinScope()
    return remember(kClass) { scope.get(kClass) }
}
