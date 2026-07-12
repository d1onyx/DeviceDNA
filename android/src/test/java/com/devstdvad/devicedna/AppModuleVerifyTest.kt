package com.devstdvad.devicedna

import android.content.Context
import com.devstdvad.devicedna.di.appModule
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import org.junit.Test

/**
 * Verifies every Koin definition in [appModule] can resolve its constructor dependencies, without
 * instantiating them. Catches missing bindings (e.g. a type resolved by interface but only
 * registered as its concrete class) at test time instead of as an app-startup crash.
 */
class AppModuleVerifyTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `appModule has no missing dependencies`() {
        // Context comes from androidContext(); CoroutineScope is constructed inline in a few
        // definitions (e.g. AppReadiness) rather than injected, so verify() must be told they exist.
        appModule.verify(
            extraTypes = listOf(
                Context::class,
                CoroutineScope::class,
                HttpClientEngine::class,
                HttpClientConfig::class,
                // Defaulted params (DeviceSyncManager `now` lambda, SubscriptionRepository flags)
                // that Koin fills from the constructor default rather than the graph.
                Function0::class,
                Boolean::class,
                // LocalDataWiper's `List<ClearableStore>` is assembled inline with listOf(get()...).
                List::class,
            ),
        )
    }
}
