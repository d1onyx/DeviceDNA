package com.devstdvad.devicedna.domain.model

data class CpuInfo(
    val chipsetName: String,
    val architecture: String,
    val coreCount: Int,
    val cores: List<CpuCore>,
    val clusters: List<CpuCluster>,
    val governor: String,
    val gpu: GpuInfo,
    val temperatureCelsius: Float? = null,
    val usagePercent: Float? = null,
    val instructionSets: List<String> = emptyList(),
    val processCount: Int? = null,
    val minFreqMhz: Int = 0,
    val maxFreqMhz: Int = 0,
)

data class CpuCore(
    val index: Int,
    val currentFrequencyKhz: Long?,
    val minFrequencyKhz: Long,
    val maxFrequencyKhz: Long,
    val isOnline: Boolean,
)

data class CpuCluster(
    val name: String,
    val coreIndices: List<Int>,
    val minFrequencyMhz: Int,
    val maxFrequencyMhz: Int,
)

data class GpuInfo(
    val renderer: String,
    val vendor: String,
    val version: String,
)
