package com.devstdvad.devicedna.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.reflect.KClass
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel as koinResolveViewModel

/**
 * Android actual: full ViewModel-store-backed resolution (survives configuration changes,
 * proper onCleared()), identical in behavior to Koin's own koinViewModel(). Safe to reference
 * LocalViewModelStoreOwner directly here — the Kotlin/Native static-linking bug this whole file
 * exists to avoid (see the expect declaration's KDoc) is specific to Kotlin/Native, not JVM/Android.
 */
@OptIn(KoinInternalApi::class)
@Composable
actual fun <T : ViewModel> resolveViewModel(kClass: KClass<T>): T {
    val owner = LocalViewModelStoreOwner.current
        ?: error("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner")
    val scope = currentKoinScope()
    return koinResolveViewModel(
        vmClass = kClass,
        viewModelStore = owner.viewModelStore,
        extras = defaultExtras(owner),
        scope = scope,
    )
}
