package com.devstdvad.devicedna

import android.app.Application
import com.devstdvad.devicedna.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DeviceDnaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DeviceDnaApp)
            modules(appModule)
        }
    }
}
