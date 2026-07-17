package com.devstdvad.devicedna.presentation.tests

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.domain.usecase.GetAppsUseCase
import com.devstdvad.devicedna.domain.usecase.GetCameraInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetConnectivityInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDisplayInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetSensorsUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetSystemInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetThermalInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveRamUseCase
import com.devstdvad.devicedna.platform.PlatformInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TestsState(
    val isRunning: Boolean = true,
    val tests: List<HardwareTestResult> = emptyList(),
    val summary: String = "Checking hardware collectors and runtime capabilities.",
)

data class HardwareTestResult(
    val group: String,
    val title: String,
    val detail: String,
    val status: HardwareTestStatus,
    val icon: ImageVector,
)

enum class HardwareTestStatus {
    Passed,
    Warning,
    Failed,
    Unavailable,
}

class TestsViewModel(
    private val getDeviceInfo: GetDeviceInfoUseCase,
    private val getSystemInfo: GetSystemInfoUseCase,
    private val getCpuInfo: GetCpuInfoUseCase,
    private val observeBattery: ObserveBatteryUseCase,
    private val observeRam: ObserveRamUseCase,
    private val getStorageInfo: GetStorageInfoUseCase,
    private val getNetworkInfo: GetNetworkInfoUseCase,
    private val getConnectivityInfo: GetConnectivityInfoUseCase,
    private val getDisplayInfo: GetDisplayInfoUseCase,
    private val getCameraInfo: GetCameraInfoUseCase,
    private val getThermalInfo: GetThermalInfoUseCase,
    private val getSensors: GetSensorsUseCase,
    private val getApps: GetAppsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TestsState())
    val state: StateFlow<TestsState> = _state.asStateFlow()

    // Many checks probe Android-only capabilities; on iOS they must read as "Unavailable"
    // (grey) rather than spuriously Failed/Warning.
    private val isIos = PlatformInfo.isIos

    init {
        runTests()
    }

    fun runTests() {
        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, summary = "Running hardware checks.") }

            val deviceDef = async { getDeviceInfo() }
            val systemDef = async { getSystemInfo() }
            val cpuDef = async { getCpuInfo() }
            val batteryDef = async { observeBattery().first() }
            val ramDef = async { observeRam().first() }
            val storageDef = async { getStorageInfo() }
            val networkDef = async { getNetworkInfo() }
            val connectivityDef = async { getConnectivityInfo() }
            val displayDef = async { getDisplayInfo() }
            val cameraDef = async { getCameraInfo() }
            val thermalDef = async { getThermalInfo() }
            val sensorsDef = async { getSensors() }
            val appsDef = async { getApps() }

            val results = buildList {
                when (val result = deviceDef.await()) {
                    is AppResult.Success -> {
                        val device = result.value
                        add(test("Device", "Device identity", "${device.manufacturer} ${device.model}", device.model.isNotBlank()))
                        add(test("Device", "CPU ABI support", device.supportedAbis.joinToString(", "), device.supportedAbis.isNotEmpty()))
                        if (isIos) {
                            add(test("Device", "Environment flags", "Jailbreak: ${device.isRooted}, simulator: ${device.isEmulator}", !device.isRooted && !device.isEmulator, warningWhenFalse = true))
                        } else {
                            add(test("Device", "Boot integrity data", "Verified boot: ${device.verifiedBootState.ifBlank { "unknown" }}", device.verifiedBootState.isNotBlank(), warningWhenFalse = true))
                            add(test("Device", "Environment flags", "Root: ${device.isRooted}, emulator: ${device.isEmulator}, ADB: ${device.isAdbEnabled}", !device.isRooted && !device.isEmulator, warningWhenFalse = true))
                        }
                    }
                    is AppResult.Error -> add(errorTest("Device", "Device identity", result.cause.message))
                }

                when (val result = systemDef.await()) {
                    is AppResult.Success -> {
                        val system = result.value
                        if (isIos) {
                            add(test("Device", "OS build", system.androidVersion, system.androidVersion.isNotBlank()))
                        } else {
                            add(test("Device", "OS build", "Android ${system.androidVersion}, API ${system.apiLevel}", system.apiLevel > 0))
                            add(test("Device", "Security patch", system.securityPatchLevel, system.securityPatchLevel != "Unknown", warningWhenFalse = true))
                            add(test("Device", "App signature", system.signingCertificateSha256 ?: "Unavailable", !system.signingCertificateSha256.isNullOrBlank(), warningWhenFalse = true))
                        }
                    }
                    is AppResult.Error -> add(errorTest("Device", "OS build", result.cause.message))
                }

                when (val result = cpuDef.await()) {
                    is AppResult.Success -> {
                        val cpu = result.value
                        add(test("Performance", "CPU topology", "${cpu.coreCount} cores, ${cpu.clusters.size} cluster(s)", cpu.coreCount > 0))
                        if (!isIos) {
                            add(test("Performance", "CPU frequency", "${cpu.minFreqMhz}-${cpu.maxFreqMhz} MHz", cpu.maxFreqMhz > 0, warningWhenFalse = true))
                        }
                        add(test("Performance", "CPU live load", cpu.usagePercent?.let { "${Formatters.oneDecimal(it)}%" } ?: "Not reported", cpu.usagePercent != null, warningWhenFalse = !isIos, unavailableWhenFalse = isIos))
                        add(test("Performance", "GPU metadata", "${cpu.gpu.vendor} ${cpu.gpu.renderer}", cpu.gpu.renderer.isNotBlank(), warningWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Performance", "CPU topology", result.cause.message))
                }

                when (val result = batteryDef.await()) {
                    is AppResult.Success -> {
                        val battery = result.value
                        add(test("Power", "Battery presence", "${battery.levelPercent}% ${battery.status}", battery.isPresent))
                        if (isIos) {
                            add(unavailableTest("Power", "Battery temperature", "Not exposed on iOS"))
                            add(unavailableTest("Power", "Battery voltage", "Not exposed on iOS"))
                            add(unavailableTest("Power", "Battery health", "Not exposed on iOS"))
                        } else {
                            add(qualityTest("Power", "Battery temperature", "${Formatters.oneDecimal(battery.temperatureCelsius)}°C", battery.temperatureCelsius < 38f, battery.temperatureCelsius < 45f))
                            add(test("Power", "Battery voltage", "${battery.voltageMv} mV", battery.voltageMv > 0))
                            add(test("Power", "Battery health", battery.health.name, battery.health == BatteryHealth.Good, warningWhenFalse = true))
                        }
                    }
                    is AppResult.Error -> add(errorTest("Power", "Battery snapshot", result.cause.message))
                }

                when (val result = ramDef.await()) {
                    is AppResult.Success -> {
                        val ram = result.value
                        add(test("Memory", "RAM capacity", formatBytes(ram.totalBytes), ram.totalBytes > 0))
                        add(qualityTest("Memory", "RAM pressure", "${(ram.usedPercent * 100).toInt()}% used", ram.usedPercent < 0.75f, ram.usedPercent < 0.92f))
                    }
                    is AppResult.Error -> add(errorTest("Memory", "RAM snapshot", result.cause.message))
                }

                when (val result = storageDef.await()) {
                    is AppResult.Success -> {
                        val storage = result.value
                        add(test("Storage", "Internal storage", "${formatBytes(storage.freeBytes)} free of ${formatBytes(storage.totalBytes)}", storage.totalBytes > 0))
                        add(qualityTest("Storage", "Storage pressure", "${(storage.usedPercent * 100).toInt()}% used", storage.usedPercent < 0.80f, storage.usedPercent < 0.95f))
                        add(test("Storage", "External storage", formatBytes(storage.externalTotalBytes), storage.externalTotalBytes > 0, unavailableWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Storage", "Internal storage", result.cause.message))
                }

                when (val result = displayDef.await()) {
                    is AppResult.Success -> {
                        val display = result.value
                        add(test("Display", "Display metrics", "${display.widthPx} x ${display.heightPx}, ${display.densityDpi} dpi", display.widthPx > 0 && display.heightPx > 0))
                        add(test("Display", "Refresh modes", display.supportedRefreshRates.joinToString(", ") { "${Formatters.noDecimals(it)}Hz" }, display.supportedRefreshRates.isNotEmpty()))
                        val enhancedColor = display.isHdr == true || display.isWideColorGamut == true
                        add(test("Display", "HDR / wide color", (display.hdrCapabilities + listOfNotNull(if (display.isWideColorGamut == true) "Wide color" else null)).joinToString(", "), enhancedColor, unavailableWhenFalse = display.isHdr == null && display.isWideColorGamut == null))
                    }
                    is AppResult.Error -> add(errorTest("Display", "Display metrics", result.cause.message))
                }

                when (val result = cameraDef.await()) {
                    is AppResult.Success -> {
                        val cameras = result.value.cameras
                        add(test("Camera", "Camera enumeration", "${cameras.size} camera(s)", cameras.isNotEmpty(), unavailableWhenFalse = true))
                        add(test("Camera", "Rear camera", "${cameras.count { it.facing.name == "Back" }} rear camera(s)", cameras.any { it.facing.name == "Back" }, unavailableWhenFalse = true))
                        add(test("Camera", "Front camera", "${cameras.count { it.facing.name == "Front" }} front camera(s)", cameras.any { it.facing.name == "Front" }, unavailableWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Camera", "Camera enumeration", result.cause.message))
                }

                when (val result = thermalDef.await()) {
                    is AppResult.Success -> {
                        val thermal = result.value
                        val hottest = thermal.zones.mapNotNull { it.temperatureCelsius }.maxOrNull()
                        add(test("Thermal", "Thermal zones", "${thermal.zones.size} zone(s)", thermal.zones.isNotEmpty()))
                        if (isIos) {
                            add(unavailableTest("Thermal", "Thermal readings", "Coarse thermal state only (no numeric temperature)"))
                        } else {
                            add(qualityTest("Thermal", "Thermal readings", hottest?.let { "${Formatters.oneDecimal(it)}°C max" } ?: "No live temperature", hottest == null || hottest < 55f, hottest == null || hottest < 75f))
                        }
                        add(test("Thermal", "CPU thermal zone", "${thermal.zones.count { it.type == ThermalZoneType.Cpu }} CPU zone(s)", thermal.zones.any { it.type == ThermalZoneType.Cpu }, warningWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Thermal", "Thermal zones", result.cause.message))
                }

                when (val result = sensorsDef.await()) {
                    is AppResult.Success -> {
                        val sensors = result.value.sensors
                        add(test("Sensors", "Sensor inventory", "${sensors.size} sensor(s)", sensors.isNotEmpty(), unavailableWhenFalse = true))
                        add(test("Sensors", "Accelerometer", sensorDetail(sensors, "Accelerometer"), sensors.any { it.typeName == "Accelerometer" }, unavailableWhenFalse = true))
                        add(test("Sensors", "Gyroscope", sensorDetail(sensors, "Gyroscope"), sensors.any { it.typeName == "Gyroscope" }, unavailableWhenFalse = true))
                        add(test("Sensors", "Light / proximity", "${sensorDetail(sensors, "Light")} / ${sensorDetail(sensors, "Proximity")}", sensors.any { it.typeName == "Light" || it.typeName == "Proximity" }, unavailableWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Sensors", "Sensor inventory", result.cause.message))
                }

                when (val result = networkDef.await()) {
                    is AppResult.Success -> {
                        val network = result.value
                        add(test("Network", "Network stack", network.connectionType.name, network.connectionType != ConnectionType.Unknown))
                        add(test("Network", "Internet validation", when (network.isValidatedInternet) { true -> "Validated"; false -> "Not validated"; null -> "Restricted" }, network.isValidatedInternet == true, warningWhenFalse = network.isValidatedInternet == false && network.connectionType != ConnectionType.None, unavailableWhenFalse = network.isValidatedInternet == null || network.connectionType == ConnectionType.None))
                        add(test("Network", "IP addressing", listOfNotNull(network.localIpv4, network.localIpv6).joinToString(", "), network.localIpv4 != null || network.localIpv6 != null, unavailableWhenFalse = network.connectionType == ConnectionType.None, warningWhenFalse = network.connectionType != ConnectionType.None))
                    }
                    is AppResult.Error -> add(errorTest("Network", "Network stack", result.cause.message))
                }

                when (val result = connectivityDef.await()) {
                    is AppResult.Success -> {
                        val conn = result.value
                        add(test("Connectivity", "Wi-Fi radio", if (conn.hasWifi) "Available" else "Not present", conn.hasWifi, unavailableWhenFalse = true))
                        add(test("Connectivity", "Bluetooth radio", if (conn.hasBluetooth) "Available" else "Not present", conn.hasBluetooth, unavailableWhenFalse = true))
                        val hasPersonalRadio = listOf(conn.hasNfc, conn.hasUwb, conn.hasEsim).any { it == true }
                        add(test("Connectivity", "NFC / UWB / eSIM", "NFC: ${conn.hasNfc ?: "Restricted"}, UWB: ${conn.hasUwb ?: "Restricted"}, eSIM: ${conn.hasEsim ?: "Restricted"}", hasPersonalRadio, unavailableWhenFalse = true))
                    }
                    is AppResult.Error -> add(errorTest("Connectivity", "Radio inventory", result.cause.message))
                }

                when (val result = appsDef.await()) {
                    is AppResult.Success -> {
                        val apps = result.value
                        add(test("Apps", "Package visibility", "${apps.totalCount} package(s)", apps.totalCount > 0, warningWhenFalse = true))
                        add(test("Apps", "User apps", "${apps.userCount} user app(s)", apps.userCount >= 0))
                    }
                    is AppResult.Error ->
                        if (isIos) {
                            add(unavailableTest("Apps", "Package visibility", "Listing installed apps is not available on iOS (sandbox)"))
                        } else {
                            add(errorTest("Apps", "Package visibility", result.cause.message))
                        }
                }
            }

            val failed = results.count { it.status == HardwareTestStatus.Failed }
            val warnings = results.count { it.status == HardwareTestStatus.Warning }
            _state.update {
                it.copy(
                    isRunning = false,
                    tests = results,
                    summary = when {
                        failed > 0 -> "$failed check(s) failed, $warnings need attention."
                        warnings > 0 -> "$warnings check(s) need attention."
                        else -> "All available hardware checks passed."
                    },
                )
            }
        }
    }

    private fun test(
        group: String,
        title: String,
        detail: String,
        passed: Boolean,
        warningWhenFalse: Boolean = false,
        unavailableWhenFalse: Boolean = false,
    ): HardwareTestResult {
        val status = when {
            passed -> HardwareTestStatus.Passed
            unavailableWhenFalse -> HardwareTestStatus.Unavailable
            warningWhenFalse -> HardwareTestStatus.Warning
            else -> HardwareTestStatus.Failed
        }
        return HardwareTestResult(group, title, detail.ifBlank { "Not reported" }, status, defaultIconForGroup(group))
    }

    private fun qualityTest(
        group: String,
        title: String,
        detail: String,
        passed: Boolean,
        acceptable: Boolean,
    ): HardwareTestResult {
        val status = when {
            passed -> HardwareTestStatus.Passed
            acceptable -> HardwareTestStatus.Warning
            else -> HardwareTestStatus.Failed
        }
        return HardwareTestResult(group, title, detail, status, defaultIconForGroup(group))
    }

    private fun errorTest(group: String, title: String, detail: String): HardwareTestResult =
        HardwareTestResult(group, title, detail, HardwareTestStatus.Failed, defaultIconForGroup(group))

    private fun unavailableTest(group: String, title: String, detail: String): HardwareTestResult =
        HardwareTestResult(group, title, detail, HardwareTestStatus.Unavailable, defaultIconForGroup(group))

    private fun sensorDetail(
        sensors: List<com.devstdvad.devicedna.domain.model.SensorDetails>,
        typeName: String,
    ): String = sensors.firstOrNull { it.typeName == typeName }?.let { "${it.vendor} ${it.name}" } ?: "Not present"

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Not present"
        val gb = bytes / (1024f * 1024f * 1024f)
        return if (gb >= 1f) "${Formatters.oneDecimal(gb)} GB" else "${Formatters.noDecimals(bytes / (1024f * 1024f))} MB"
    }
}
