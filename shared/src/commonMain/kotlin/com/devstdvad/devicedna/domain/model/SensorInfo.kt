package com.devstdvad.devicedna.domain.model

data class SensorInfo(
    val sensors: List<SensorDetails>,
)

data class SensorDetails(
    val name: String,
    val vendor: String,
    val type: Int,
    val typeName: String,
    val version: Int,
    val powerMa: Float,
    val resolution: Float,
    val maxRange: Float,
    val isWakeUp: Boolean,
    val isDynamic: Boolean,
)
