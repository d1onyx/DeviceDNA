package com.devstdvad.devicedna.data.widget

import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Reads a couple of signals that are not exposed by the existing data sources but are
 * needed by the Thermal Guard and Battery Doctor widgets.
 */
class WidgetSystemProbe(private val context: Context) {

    /** PowerManager thermal status (NONE..SHUTDOWN), or -1 if unavailable (< API 29). */
    fun thermalStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        return runCatching {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
        }.getOrDefault(-1)
    }

    /**
     * Battery design capacity in mAh, read from the hidden PowerProfile.
     * Returns null if reflection fails (varies by OEM / Android version).
     */
    fun designCapacityMah(): Int? = runCatching {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val instance = powerProfileClass
            .getConstructor(Context::class.java)
            .newInstance(context)
        val capacity = powerProfileClass
            .getMethod("getAveragePower", String::class.java)
            .invoke(instance, "battery.capacity") as Double
        capacity.toInt().takeIf { it > 0 }
    }.getOrNull()
}
