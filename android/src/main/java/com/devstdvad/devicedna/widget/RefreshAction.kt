package com.devstdvad.devicedna.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/** Tap handler for the widget "refresh" control. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetRefreshScheduler.refreshNow(context)
    }
}
