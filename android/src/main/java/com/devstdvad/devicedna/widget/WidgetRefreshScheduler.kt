package com.devstdvad.devicedna.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules background refresh of the home-screen widgets. */
object WidgetRefreshScheduler {

    private const val PERIODIC_WORK = "widget_refresh_periodic"
    private const val ONESHOT_WORK = "widget_refresh_now"

    /** ~15 min periodic refresh (the platform minimum). Idempotent via unique work. */
    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Immediate one-off refresh (manual button / first widget add). */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}
