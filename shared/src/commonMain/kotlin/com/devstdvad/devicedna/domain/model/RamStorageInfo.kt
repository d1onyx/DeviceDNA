package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RamInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float,
    val isLowMemory: Boolean,
    val cachedBytes: Long = 0L,
    val thresholdBytes: Long = 0L,
)

@Serializable
data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercent: Float,
    val externalTotalBytes: Long = 0L,
    val externalFreeBytes: Long = 0L,
)
