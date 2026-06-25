package com.devstdvad.devicedna

import com.devstdvad.devicedna.domain.health.HealthAnalyzer
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.FraudRiskLevel
import com.devstdvad.devicedna.domain.model.InsightSeverity
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthAnalyzerTest {

    private val analyzer = HealthAnalyzer()

    private fun battery(
        level: Int = 80,
        temp: Float = 30f,
        health: BatteryHealth = BatteryHealth.Good,
        status: BatteryStatus = BatteryStatus.Discharging,
    ) = BatteryInfo(level, status, health, ChargeSource.Unknown, "Li-ion", temp, 4000, null, null, null, true)

    private fun ram(usedPercent: Float, total: Long = 8L * 1024 * 1024 * 1024) = RamInfo(
        totalBytes = total,
        availableBytes = (total * (1 - usedPercent)).toLong(),
        usedBytes = (total * usedPercent).toLong(),
        usedPercent = usedPercent,
        isLowMemory = usedPercent >= 0.95f,
    )

    private fun storage(usedPercent: Float, total: Long = 128L * 1024 * 1024 * 1024) = StorageInfo(
        totalBytes = total,
        usedBytes = (total * usedPercent).toLong(),
        freeBytes = (total * (1 - usedPercent)).toLong(),
        usedPercent = usedPercent,
    )

    private fun deviceWithFraudSignals() = DeviceInfo(
        name = "Android SDK built for x86",
        model = "sdk_gphone64_x86_64",
        manufacturer = "Google",
        brand = "generic",
        board = "goldfish_x86",
        hardware = "ranchu",
        codename = "emu64x",
        buildFingerprint = "generic/sdk/generic:14/TEST:userdebug/test-keys",
        androidId = "abc123",
        supportedAbis = listOf("x86_64"),
        isRooted = true,
        bootloader = "unknown",
        isEmulator = true,
        isDeveloperOptionsEnabled = true,
        isAdbEnabled = true,
        buildTags = "test-keys",
        isTestKeysBuild = true,
        isDebuggableBuild = true,
        verifiedBootState = "orange",
        vbMetaDeviceState = "unlocked",
        flashLocked = "0",
        verityMode = "disabled",
        warrantyBit = "1",
        suspiciousRootPaths = listOf("/sbin/su", "/data/adb/magisk"),
    )

    private fun riskyNetwork() = NetworkInfo(
        connectionType = ConnectionType.WiFi,
        ssid = "Office",
        localIpv4 = "192.168.1.20",
        localIpv6 = null,
        gateway = "192.168.1.1",
        dns = listOf("1.1.1.1"),
        subnetMask = "/24",
        interfaceName = "wlan0",
        linkSpeedMbps = 300,
        frequencyMhz = 5180,
        channel = 36,
        wifiStandard = "Wi-Fi 6",
        securityType = null,
        signalStrength = -45,
        isVpnActive = true,
        isValidatedInternet = false,
        activeTransports = listOf("Wi-Fi", "VPN"),
        httpProxyHost = "127.0.0.1",
        httpProxyPort = 8080,
    )

    @Test
    fun `healthy device scores above 80`() = runTest {
        val score = analyzer.getHealthScore(
            battery = battery(level = 90, temp = 28f),
            ram = ram(0.5f),
            storage = storage(0.4f),
            thermal = null,
            device = null,
            system = null,
            network = null,
        )
        assertTrue("Expected score > 80, got ${score.overall}", score.overall > 80)
    }

    @Test
    fun `high battery temperature produces warning insight`() = runTest {
        val score = analyzer.getHealthScore(
            battery = battery(temp = 48f),
            ram = null, storage = null, thermal = null, device = null, system = null,
            network = null,
        )
        val hotInsight = score.insights.find { it.id == "bat_hot" }
        assertTrue("Expected battery hot insight", hotInsight != null)
        assertTrue("Expected Warning or Critical", hotInsight!!.severity in listOf(InsightSeverity.Warning, InsightSeverity.Critical))
    }

    @Test
    fun `critical battery temperature produces critical insight`() = runTest {
        val score = analyzer.getHealthScore(
            battery = battery(temp = 58f),
            ram = null, storage = null, thermal = null, device = null, system = null,
            network = null,
        )
        val hotInsight = score.insights.find { it.id == "bat_hot" }
        assertEquals(InsightSeverity.Critical, hotInsight?.severity)
    }

    @Test
    fun `full storage produces critical insight`() = runTest {
        val score = analyzer.getHealthScore(
            battery = null,
            ram = null,
            storage = storage(0.97f),
            thermal = null, device = null, system = null,
            network = null,
        )
        val storageInsight = score.insights.find { it.id == "storage_full" }
        assertTrue("Expected storage full insight", storageInsight != null)
    }

    @Test
    fun `high ram usage produces warning insight`() = runTest {
        val score = analyzer.getHealthScore(
            battery = null,
            ram = ram(0.92f),
            storage = null, thermal = null, device = null, system = null,
            network = null,
        )
        val ramInsight = score.insights.find { it.id == "ram_high" }
        assertTrue("Expected RAM high insight", ramInsight != null)
    }

    @Test
    fun `emulator root and unlocked boot chain produce critical fraud risk`() = runTest {
        val score = analyzer.getHealthScore(
            battery = null,
            ram = null,
            storage = null,
            thermal = null,
            device = deviceWithFraudSignals(),
            system = null,
            network = null,
        )

        assertEquals(FraudRiskLevel.Critical, score.fraudRisk.level)
        assertTrue(score.fraudRisk.signals.any { it.id == "emulator" })
        assertTrue(score.fraudRisk.signals.any { it.id == "bootloader_unlocked" })
        assertTrue(score.insights.any { it.id == "fraud_risk" })
    }

    @Test
    fun `vpn and proxy produce network fraud signals`() = runTest {
        val score = analyzer.getHealthScore(
            battery = null,
            ram = null,
            storage = null,
            thermal = null,
            device = null,
            system = null,
            network = riskyNetwork(),
        )

        assertTrue(score.fraudRisk.signals.any { it.id == "vpn" })
        assertTrue(score.fraudRisk.signals.any { it.id == "proxy" })
    }
}
