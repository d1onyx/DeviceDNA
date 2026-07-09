package com.devstdvad.devicedna

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.devstdvad.devicedna.core.CurrentActivityHolder
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.cfg.ConfigSync
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.di.appModule
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
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
        // Secondary "cfg-sync" Firebase app (no-op unless configured).
        initConfigSyncApp()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DeviceDnaApp)
            modules(appModule)
        }
        // Seed remote config state before the first frame, then keep a foreground subscription.
        val configSync = GlobalContext.get().get<ConfigSync>()
        configSync.onStartup()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = configSync.attach(applicationScope)
            override fun onStop(owner: LifecycleOwner) = configSync.detach()
        })

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

        // Reconcile entitlements with Google Play on every sign-in. Play is the source of truth, so
        // this re-grants a still-active subscription even after an account deletion wiped the backend
        // record (deleting the app account never cancels the store subscription). Real billing only —
        // dev billing would grant a fake unlock on each sign-in.
        if (BuildConfig.USE_REAL_BILLING) {
            val authGateway = GlobalContext.get().get<AuthGateway>()
            applicationScope.launch {
                authGateway.currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .filterNotNull()
                    .collect { runCatching { subscriptionRepository.restorePurchases() } }
            }
        }
    }

    /** Initializes the secondary "cfg-sync" [FirebaseApp] from BuildConfig. Skipped when unset. */
    private fun initConfigSyncApp() {
        if (BuildConfig.CFG_PROJECT_ID.isBlank()) return
        if (FirebaseApp.getApps(this).any { it.name == CFG_APP_NAME }) return
        val options = FirebaseOptions.Builder()
            .setProjectId(BuildConfig.CFG_PROJECT_ID)
            .setApplicationId(BuildConfig.CFG_APP_ID)
            .setApiKey(BuildConfig.CFG_API_KEY)
            .build()
        FirebaseApp.initializeApp(this, options, CFG_APP_NAME)
    }

    private companion object {
        const val CFG_APP_NAME = "cfg-sync"
    }
}
