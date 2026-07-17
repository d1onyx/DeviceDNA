package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    val connectionType: ConnectionType,
    val ssid: String?,
    val localIpv4: String?,
    val localIpv6: String?,
    val gateway: String?,
    val dns: List<String>,
    val subnetMask: String?,
    val interfaceName: String?,
    val linkSpeedMbps: Int?,
    val frequencyMhz: Int?,
    val channel: Int?,
    val wifiStandard: String?,
    val securityType: String?,
    val signalStrength: Int?,
    val macAddress: String? = null,
    val rxBytesPerSec: Long? = null,
    val txBytesPerSec: Long? = null,
    val isMetered: Boolean = false,
    val cellularOperator: String? = null,
    val cellularGeneration: String? = null,
    val isVpnActive: Boolean = false,
    val isValidatedInternet: Boolean? = null,
    val isCaptivePortal: Boolean? = null,
    val activeTransports: List<String> = emptyList(),
    val privateDnsServerName: String? = null,
    val httpProxyHost: String? = null,
    val httpProxyPort: Int? = null,
)

@Serializable
enum class ConnectionType { WiFi, Cellular, Ethernet, None, Unknown }

@Serializable
data class ConnectivityInfo(
    val hasWifi: Boolean,
    val hasWifi5Ghz: Boolean?,
    val hasWifi6Ghz: Boolean?,
    val hasWifiDirect: Boolean,
    val wifiStandards: List<String>,
    val hasBluetooth: Boolean,
    val hasBluetoothLe: Boolean,
    val hasNfc: Boolean?,
    val hasUwb: Boolean?,
    val hasEsim: Boolean?,
    val bluetoothVersion: String?,
)
