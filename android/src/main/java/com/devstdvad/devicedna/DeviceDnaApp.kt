package com.devstdvad.devicedna

import android.app.Application
import com.devstdvad.devicedna.core.CurrentActivityHolder
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.di.appModule
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DeviceDnaApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            MobileAds.initialize(this@DeviceDnaApp) {}
        }
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DeviceDnaApp)
            modules(appModule)
        }
        // Track the foreground Activity so the Play Billing gateway can launch the purchase flow.
        GlobalContext.get().get<CurrentActivityHolder>().register(this)

        // Refresh home-screen widgets the moment premium status changes (purchase, restore, dev
        // activate, or a backend-authoritative refresh), regardless of which screen is open —
        // otherwise widgets only unlock/lock on the next 15-min periodic worker run.
        val subscriptionRepository = GlobalContext.get().get<SubscriptionRepository>()
        applicationScope.launch {
            subscriptionRepository.entitlements
                .map { it.hasFeature(PremiumFeature.Widgets) }
                .distinctUntilChanged()
                .drop(1)
                .collect { WidgetRefreshScheduler.refreshNow(this@DeviceDnaApp) }
        }
    }
}
