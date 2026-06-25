package com.devstdvad.devicedna.core.common

object PrivacyMask {

    fun maskIpv4(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.xxx.xxx" else "xxx.xxx.xxx.xxx"
    }

    fun maskIpv6(ip: String): String {
        val zoneIndex = ip.indexOf('%')
        val zoneSuffix = if (zoneIndex >= 0) ip.substring(zoneIndex) else ""
        val address = if (zoneIndex >= 0) ip.substring(0, zoneIndex) else ip
        val groups = address.split(":").filter { it.isNotBlank() }
        return when {
            "::" in address && groups.isNotEmpty() -> "${groups.first()}::****$zoneSuffix"
            groups.size >= 2 -> "${groups[0]}:${groups[1]}::****$zoneSuffix"
            groups.size == 1 -> "${groups[0]}::****$zoneSuffix"
            else -> "****::****"
        }
    }

    fun maskPackage(pkg: String): String {
        val parts = pkg.split(".")
        return if (parts.size >= 3) {
            "${parts[0]}.${parts[1]}.*"
        } else {
            "${parts.firstOrNull() ?: ""}.*"
        }
    }

    fun maskSsid(ssid: String): String {
        if (ssid.length <= 2) return "**"
        val visible = ssid.take(2)
        return "$visible${"*".repeat((ssid.length - 2).coerceAtMost(6))}"
    }

    fun maskDeviceId(id: String): String {
        if (id.length <= 6) return "****"
        return "${id.take(3)}****${id.takeLast(3)}"
    }

    fun maskFingerprint(fp: String): String {
        if (fp.length <= 8) return "****"
        return "${fp.take(8)}…****"
    }

    fun maskMac(mac: String): String {
        val parts = mac.split(":")
        return if (parts.size == 6) "${parts[0]}:${parts[1]}:xx:xx:xx:xx" else "xx:xx:xx:xx:xx:xx"
    }
}
