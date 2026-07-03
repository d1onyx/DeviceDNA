package com.devstdvad.devicedna.platform

import android.os.Build

actual object PlatformInfo {
    actual val deviceModel: String
        get() = Build.MODEL
    actual val manufacturer: String
        get() = Build.MANUFACTURER
    actual val osName: String
        get() = "Android"
    actual val osVersion: String
        get() = Build.VERSION.RELEASE
    actual val processorCount: Int
        get() = Runtime.getRuntime().availableProcessors()
    actual val totalMemoryBytes: Long
        get() = readTotalRamFromProcMeminfo()
    actual val isIos: Boolean
        get() = false
}

private fun readTotalRamFromProcMeminfo(): Long = runCatching {
    java.io.BufferedReader(java.io.FileReader("/proc/meminfo")).use { reader ->
        val line = reader.readLine() ?: return@runCatching 0L
        // "MemTotal:    3932160 kB"
        val kb = line.trim().split("\\s+".toRegex()).getOrNull(1)?.toLongOrNull() ?: 0L
        kb * 1024L
    }
}.getOrDefault(0L)
