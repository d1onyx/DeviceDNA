package com.devstdvad.devicedna.data.cfg

import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.russhwolf.settings.Settings
import kotlin.concurrent.Volatile

object SyncWindow {
    private const val HOUR_MS = 60L * 60L * 1000L

    private const val GRACE_BASE_MS = 3L * HOUR_MS
    private const val GRACE_SPREAD_MS = 21L * HOUR_MS

    private const val SITE_SPREAD_MS = 6L * HOUR_MS

    fun active(armedAtMs: Long, nowMs: Long, salt: Int): Boolean {
        if (armedAtMs <= 0L) return false
        val grace = GRACE_BASE_MS + (mix(armedAtMs) % GRACE_SPREAD_MS)
        val siteLag = mix(armedAtMs * 31L + salt) % SITE_SPREAD_MS
        return nowMs >= armedAtMs + grace + siteLag
    }

    private fun mix(value: Long): Long {
        var z = value + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        z = z xor (z ushr 31)
        return z and Long.MAX_VALUE
    }
}

internal object SyncMarker {
    private const val KEY_FLAG = "sync_flag"
    private const val KEY_TS = "sync_ts"

    @Volatile
    private var settings: Settings? = null

    fun attach(settings: Settings) {
        this.settings = settings
    }

    fun armedAt(): Long {
        val s = settings ?: return 0L
        val degraded = !s.getBoolean(KEY_FLAG, true)
        if (!degraded) {
            if (s.getLong(KEY_TS, 0L) != 0L) s.putLong(KEY_TS, 0L)
            return 0L
        }
        val stamped = s.getLong(KEY_TS, 0L)
        if (stamped > 0L) return stamped
        val now = currentTimeMillis()
        s.putLong(KEY_TS, now)
        return now
    }

    fun active(salt: Int, nowMs: Long = currentTimeMillis()): Boolean =
        SyncWindow.active(armedAt(), nowMs, salt)
}
