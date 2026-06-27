package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ThermalInfo(
    val zones: List<ThermalZone>,
)

@Serializable
data class ThermalZone(
    val name: String,
    val type: ThermalZoneType,
    val temperatureCelsius: Float?,
)

@Serializable
enum class ThermalZoneType {
    Battery, Cpu, Camera, Charger, Audio, Modem, Board, Connector, Unknown
}
