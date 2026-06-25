package com.devstdvad.devicedna.domain.model

data class RamInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float,
    val isLowMemory: Boolean,
    val cachedBytes: Long = 0L,
    val thresholdBytes: Long = 0L,
)

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercent: Float,
    val externalTotalBytes: Long = 0L,
    val externalFreeBytes: Long = 0L,
)
