package com.devstdvad.devicedna.platform

/**
 * Basic platform identity — no Context or platform-specific imports on the call site.
 * Android actual reads from Build.* and /proc/meminfo.
 * iOS actual reads from UIDevice / NSProcessInfo via Kotlin/Native interop.
 */
expect object PlatformInfo {
    val deviceModel: String
    val manufacturer: String
    val osName: String
    val osVersion: String
    val processorCount: Int
    val totalMemoryBytes: Long
}
