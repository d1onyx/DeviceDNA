package com.devstdvad.devicedna.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.NetworkInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

class AndroidNetworkDataSource(private val context: Context) {

    private val cm get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wm get() = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val tm get() = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var prevRxBytes: Long = 0L
    private var prevTxBytes: Long = 0L
    private var prevTimestamp: Long = 0L

    fun observeNetwork(): Flow<AppResult<NetworkInfo>> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(getNetworkInfo()) }
            override fun onLost(network: Network) { trySend(getNetworkInfo()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(getNetworkInfo()) }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        trySend(getNetworkInfo())
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    @SuppressLint("MissingPermission")
    fun getNetworkInfo(): AppResult<NetworkInfo> = runCatching {
        val activeNet = cm.activeNetwork
        val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
        val activeTransports = buildActiveTransports(caps)
        val connectionType = when {
            caps == null -> ConnectionType.None
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WiFi
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.Cellular
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.Ethernet
            else -> ConnectionType.Unknown
        }

        val (ipv4, ipv6) = readLocalIps()
        val linkProperties = activeNet?.let { cm.getLinkProperties(it) }
        val wifiInfo = if (connectionType == ConnectionType.WiFi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) caps?.transportInfo as? WifiInfo
            else @Suppress("DEPRECATION") wm.connectionInfo
        } else null
        val dns = linkProperties?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty()
        val gateway = linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway?.hostAddress
        val subnetMask = linkProperties?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address }
            ?.prefixLength?.let { "/$it" }

        val isMetered = cm.isActiveNetworkMetered
        val (rxSpeed, txSpeed) = measureNetworkSpeed(linkProperties?.interfaceName)

        val cellularOperator = if (connectionType == ConnectionType.Cellular) {
            runCatching { tm.networkOperatorName.takeIf { it.isNotBlank() } }.getOrNull()
        } else null

        val cellularGeneration = if (connectionType == ConnectionType.Cellular) {
            runCatching { getCellularGeneration() }.getOrNull()
        } else null

        NetworkInfo(
            connectionType = connectionType,
            ssid = wifiInfo?.ssid?.cleanSsid(),
            localIpv4 = ipv4,
            localIpv6 = ipv6,
            gateway = gateway,
            dns = dns,
            subnetMask = subnetMask,
            interfaceName = linkProperties?.interfaceName,
            linkSpeedMbps = wifiInfo?.linkSpeed?.takeIf { it > 0 },
            frequencyMhz = wifiInfo?.frequency?.takeIf { it > 0 },
            channel = wifiInfo?.frequency?.takeIf { it > 0 }?.toWifiChannel(),
            wifiStandard = wifiInfo?.wifiStandardName(),
            securityType = null,
            signalStrength = wifiInfo?.rssi,
            macAddress = wifiInfo?.macAddress?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || it == "02:00:00:00:00:00") {
                    "Restricted (Android 10+)"
                } else it
            },
            rxBytesPerSec = rxSpeed,
            txBytesPerSec = txSpeed,
            isMetered = isMetered,
            cellularOperator = cellularOperator,
            cellularGeneration = cellularGeneration,
            isVpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            isValidatedInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            isCaptivePortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
            activeTransports = activeTransports,
            privateDnsServerName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                linkProperties?.privateDnsServerName
            } else null,
            httpProxyHost = System.getProperty("http.proxyHost")?.takeIf { it.isNotBlank() },
            httpProxyPort = System.getProperty("http.proxyPort")?.toIntOrNull(),
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Network read failed")) },
    )

    fun getConnectivityInfo(): AppResult<ConnectivityInfo> = runCatching {
        ConnectivityInfo(
            hasWifi = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI),
            hasWifi5Ghz = wm.is5GHzBandSupported,
            hasWifi6Ghz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wm.is6GHzBandSupported else false,
            hasWifiDirect = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_DIRECT),
            wifiStandards = buildList {
                add("Wi-Fi 4 (802.11n)")
                if (wm.is5GHzBandSupported) add("Wi-Fi 5 (802.11ac)")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm.is6GHzBandSupported) add("Wi-Fi 6E (802.11ax)")
            },
            hasBluetooth = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothLe = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE),
            hasNfc = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_NFC),
            hasUwb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_UWB)
            } else false,
            hasEsim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY_EUICC)
            } else false,
            bluetoothVersion = null,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Connectivity read failed")) },
    )

    private fun measureNetworkSpeed(interfaceName: String?): Pair<Long?, Long?> = runCatching {
        val now = System.currentTimeMillis()
        val iface = interfaceName ?: return Pair(null, null)
        val rxFile = File("/sys/class/net/$iface/statistics/rx_bytes")
        val txFile = File("/sys/class/net/$iface/statistics/tx_bytes")
        if (!rxFile.exists() || !txFile.exists()) return Pair(null, null)
        val rxNow = rxFile.readText().trim().toLong()
        val txNow = txFile.readText().trim().toLong()
        val dt = (now - prevTimestamp).coerceAtLeast(1L)
        val rx = if (prevTimestamp > 0 && prevRxBytes > 0) ((rxNow - prevRxBytes) * 1000L / dt).coerceAtLeast(0L) else null
        val tx = if (prevTimestamp > 0 && prevTxBytes > 0) ((txNow - prevTxBytes) * 1000L / dt).coerceAtLeast(0L) else null
        prevRxBytes = rxNow; prevTxBytes = txNow; prevTimestamp = now
        Pair(rx, tx)
    }.getOrDefault(Pair(null, null))

    @SuppressLint("MissingPermission")
    private fun getCellularGeneration(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null
        return when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA -> "3G HSPA+"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G UMTS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G EDGE"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G GPRS"
            else -> null
        }
    }

    private fun readLocalIps(): Pair<String?, String?> = runCatching {
        var v4: String? = null; var v6: String? = null
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.filter { !it.isLoopback && it.isUp }
            ?.forEach { iface ->
                iface.inetAddresses.asSequence().forEach { addr ->
                    when (addr) {
                        is Inet4Address -> if (v4 == null) v4 = addr.hostAddress
                        is Inet6Address -> if (v6 == null && !addr.isLinkLocalAddress) v6 = addr.hostAddress
                    }
                }
            }
        v4 to v6
    }.getOrDefault(null to null)

    private fun String.cleanSsid(): String? = trim('"').takeUnless { it.isBlank() || it == WifiManager.UNKNOWN_SSID }

    private fun Int.toWifiChannel(): Int? = when (this) {
        in 2412..2484 -> ((this - 2412) / 5) + 1
        in 5170..5895 -> ((this - 5000) / 5)
        in 5955..7115 -> ((this - 5950) / 5)
        else -> null
    }

    private fun WifiInfo.wifiStandardName(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return when (wifiStandard) {
            4 -> "Wi-Fi 4 (802.11n)"
            5 -> "Wi-Fi 5 (802.11ac)"
            6 -> "Wi-Fi 6 (802.11ax)"
            7 -> "Wi-Fi 6E (802.11ax)"
            8 -> "Wi-Fi 7 (802.11be)"
            else -> null
        }
    }

    private fun buildActiveTransports(caps: NetworkCapabilities?): List<String> {
        if (caps == null) return emptyList()
        return buildList {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Cellular")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            ) add("Wi-Fi Aware")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
            ) add("LoWPAN")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_USB)
            ) add("USB")
        }
    }
}
