package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.widget.WidgetSnapshotBuilder
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.GpuInfo
import com.devstdvad.devicedna.domain.model.HealthInsight
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.InsightSeverity
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZone
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotTest {

    private fun battery(level: Int = 80, capacityMah: Int? = null) = BatteryInfo(
        levelPercent = level,
        status = BatteryStatus.Discharging,
        health = BatteryHealth.Good,
        source = ChargeSource.Unknown,
        technology = "Li-ion",
        temperatureCelsius = 33.5f,
        voltageMv = 4100,
        currentMa = null,
        capacityMah = capacityMah,
        chargeCycles = 120,
        isPresent = true,
    )

    private fun cpu(usage: Float?, temp: Float?) = CpuInfo(
        chipsetName = "Test SoC",
        architecture = "arm64",
        coreCount = 8,
        cores = emptyList(),
        clusters = emptyList(),
        governor = "schedutil",
        gpu = GpuInfo("Adreno", "Qualcomm", "OpenGL ES 3.2"),
        temperatureCelsius = temp,
        usagePercent = usage,
    )

    private fun device(rooted: Boolean = false, adb: Boolean = false) = DeviceInfo(
        name = "Pixel", model = "Pixel 8", manufacturer = "Google", brand = "google",
        board = "b", hardware = "h", codename = "c", buildFingerprint = "fp",
        androidId = "aid", supportedAbis = listOf("arm64-v8a"), isRooted = rooted, bootloader = "bl",
        isAdbEnabled = adb,
    )

    private fun build(
        battery: BatteryInfo? = null,
        ram: RamInfo? = null,
        storage: StorageInfo? = null,
        cpu: CpuInfo? = null,
        thermal: ThermalInfo? = null,
        device: DeviceInfo? = null,
        health: HealthScore? = null,
        thermalStatus: Int = -1,
        designCapacityMah: Int? = null,
    ) = WidgetSnapshotBuilder.buildSnapshot(
        isPremium = true, battery = battery, ram = ram, storage = storage, cpu = cpu,
        thermal = thermal, device = device, health = health,
        thermalStatus = thermalStatus, designCapacityMah = designCapacityMah, nowMillis = 1L,
    )

    @Test
    fun `battery wear estimated from charge counter and design capacity`() {
        // 80% level, 4000 mAh remaining -> full ~5000 mAh; design 5000 -> ~100% health
        val wear = WidgetSnapshotBuilder.batteryWear(battery(level = 80, capacityMah = 4000), 5000)
        assertEquals(100, wear)
        // worn battery: full ~3000 vs design 5000 -> 60%
        val worn = WidgetSnapshotBuilder.batteryWear(battery(level = 60, capacityMah = 1800), 5000)
        assertEquals(60, worn)
    }

    @Test
    fun `battery wear is minus one without capacity or design`() {
        assertEquals(-1, WidgetSnapshotBuilder.batteryWear(battery(capacityMah = null), 5000))
        assertEquals(-1, WidgetSnapshotBuilder.batteryWear(battery(capacityMah = 4000), null))
    }

    @Test
    fun `most severe insight is chosen`() {
        val health = HealthScore(
            overall = 72, battery = 70, performance = 80, storage = 90, security = 60, thermal = 75,
            insights = listOf(
                HealthInsight("a", "Minor info", "", InsightSeverity.Info, 1f, emptyList()),
                HealthInsight("b", "Battery wear high", "", InsightSeverity.Warning, 1f, emptyList()),
                HealthInsight("c", "Good", "", InsightSeverity.Good, 1f, emptyList()),
            ),
        )
        val snap = build(health = health)
        assertEquals(72, snap.healthOverall)
        assertEquals("Battery wear high", snap.healthInsight)
        assertEquals("Warning", snap.healthSeverity)
    }

    @Test
    fun `guardian aggregates integrity issues`() {
        val clean = WidgetSnapshotBuilder.integrityIssues(device(rooted = false, adb = false), null)
        assertEquals("", clean)
        val flagged = WidgetSnapshotBuilder.integrityIssues(device(rooted = true, adb = true), null)
        assertTrue(flagged.contains("root"))
        assertTrue(flagged.contains("adb"))
    }

    @Test
    fun `cpu freq averaged and thermal status passed through`() {
        val thermal = ThermalInfo(
            zones = listOf(ThermalZone("z", ThermalZoneType.Cpu, 48f)),
        )
        val snap = build(cpu = cpu(usage = 40f, temp = null), thermal = thermal, thermalStatus = 3)
        assertEquals(3, snap.thermalStatus)
        assertEquals(48f, snap.thermalMaxC, 0.001f)
        assertEquals(48f, snap.cpuTempC, 0.001f)
    }

    @Test
    fun `empty inputs produce no data`() {
        val snap = build()
        assertFalse(snap.hasData)
        assertFalse(snap.hasBatteryData)
        assertFalse(snap.hasMemoryData)
        assertFalse(snap.hasStorageData)
        assertFalse(snap.hasCpuData)
        assertFalse(snap.hasThermalData)
        assertFalse(snap.hasDeviceData)
        assertFalse(snap.hasHealthData)
        assertEquals(-1, snap.batteryLevel)
    }

    @Test
    fun `domain data flags only mark loaded sources`() {
        val snap = build(
            battery = battery(),
            ram = RamInfo(
                totalBytes = 4_000L,
                availableBytes = 1_000L,
                usedBytes = 3_000L,
                usedPercent = 0.75f,
                isLowMemory = false,
            ),
            storage = StorageInfo(
                totalBytes = 10_000L,
                usedBytes = 4_000L,
                freeBytes = 6_000L,
                usedPercent = 0.4f,
            ),
            thermal = ThermalInfo(listOf(ThermalZone("battery", ThermalZoneType.Battery, 31f))),
            device = device(),
        )

        assertTrue(snap.hasData)
        assertTrue(snap.hasBatteryData)
        assertTrue(snap.hasMemoryData)
        assertTrue(snap.hasStorageData)
        assertFalse(snap.hasCpuData)
        assertTrue(snap.hasThermalData)
        assertTrue(snap.hasDeviceData)
        assertFalse(snap.hasHealthData)
    }
}
