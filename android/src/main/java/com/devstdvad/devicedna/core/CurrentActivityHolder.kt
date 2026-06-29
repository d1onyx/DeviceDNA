package com.devstdvad.devicedna.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the currently-resumed [Activity] so non-UI singletons (e.g. the Play Billing gateway,
 * which needs an Activity to launch the purchase flow) can reach it without leaking a strong
 * reference. Register once from the [Application] via [register].
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {

    private var activityRef: WeakReference<Activity> = WeakReference(null)

    /** The current resumed Activity, or null if the app is in the background. */
    val current: Activity?
        get() = activityRef.get()

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activityRef.get() === activity) activityRef = WeakReference(null)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
