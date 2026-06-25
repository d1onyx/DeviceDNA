package com.devstdvad.devicedna.domain.model

data class ThermalInfo(
    val zones: List<ThermalZone>,
)

data class ThermalZone(
    val name: String,
    val type: ThermalZoneType,
    val temperatureCelsius: Float?,
)

enum class ThermalZoneType {
    Battery, Cpu, Camera, Charger, Audio, Modem, Board, Connector, Unknown
}
