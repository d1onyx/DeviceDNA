package com.devstdvad.devicedna

import android.app.Application
import com.devstdvad.devicedna.di.appModule
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
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
    }
}
