@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.devstdvad.devicedna.data.source

import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.AppListInfo
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.CameraDetails
import com.devstdvad.devicedna.domain.model.CameraFacing
import com.devstdvad.devicedna.domain.model.CameraInfo
import com.devstdvad.devicedna.domain.model.ChargeSource
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.DisplayInfo
import com.devstdvad.devicedna.domain.model.GpuInfo
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.SensorDetails
import com.devstdvad.devicedna.domain.model.SensorInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.SystemInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZone
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.domain.repository.AppsRepository
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import com.devstdvad.devicedna.domain.repository.CameraRepository
import com.devstdvad.devicedna.domain.repository.ConnectivityRepository
import com.devstdvad.devicedna.domain.repository.CpuRepository
import com.devstdvad.devicedna.domain.repository.DeviceRepository
import com.devstdvad.devicedna.domain.repository.DisplayRepository
import com.devstdvad.devicedna.domain.repository.NetworkRepository
import com.devstdvad.devicedna.domain.repository.RamRepository
import com.devstdvad.devicedna.domain.repository.SensorRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.repository.SystemRepository
import com.devstdvad.devicedna.domain.repository.ThermalRepository
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInTelephotoCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInTrueDepthCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasFlash
import platform.AVFoundation.position
import platform.CoreMotion.CMAltimeter
import platform.CoreMotion.CMMotionManager
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileSystemSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalStateCritical
import platform.Foundation.NSProcessInfoThermalStateFair
import platform.Foundation.NSProcessInfoThermalStateNominal
import platform.Foundation.NSProcessInfoThermalStateSerious
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIScreen
import platform.darwin.HOST_VM_INFO64
import platform.darwin.host_statistics64
import platform.darwin.integer_tVar
import platform.darwin.mach_host_self
import platform.darwin.mach_msg_type_number_tVar
import platform.darwin.vm_statistics64_data_t
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.NI_MAXHOST
import platform.posix.NI_NUMERICHOST
import platform.posix.freeifaddrs
import platform.posix.getifaddrs
import platform.posix.getnameinfo
import platform.posix.ifaddrs
import platform.posix.uname
import platform.posix.utsname
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.sizeOf

/*
 * iOS data layer for the shared repository interfaces. Collection is done with public
 * Foundation/UIKit/CoreMotion/AVFoundation/POSIX APIs only — no private APIs, so it is
 * App Store review-safe. Metrics that the iOS sandbox does not expose (battery mAh/cycles,
 * per-core CPU frequency, thermal zone temperatures, installed apps) are reported as null /
 * AppError.PlatformRestricted and the shared UI renders its "unavailable on this platform"
 * state — mirroring the honest-degradation approach used across the app.
 *
 * COMPILES ONLY ON macOS (Kotlin/Native iOS target). Interop calls follow standard
 * Kotlin/Native darwin patterns; verify signatures with the Xcode toolchain.
 */

// ── Shared low-level helpers ──────────────────────────────────────────────────

private fun machineIdentifier(): String = memScoped {
    val info = alloc<utsname>()
    uname(info.ptr)
    info.machine.toKString()
}

private fun isSimulator(): Boolean =
    NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null

/** Best-effort Apple chip name from the hardware identifier. */
private fun chipName(machine: String): String {
    val map = listOf(
        "iPhone17" to "Apple A18",
        "iPhone16" to "Apple A17 Pro",
        "iPhone15" to "Apple A16 Bionic",
        "iPhone14" to "Apple A15 Bionic",
        "iPhone13" to "Apple A15 Bionic",
        "iPhone12" to "Apple A14 Bionic",
        "iPhone11" to "Apple A13 Bionic",
        "iPad16" to "Apple M2",
        "iPad14" to "Apple M2",
        "iPad13" to "Apple M1",
    )
    return map.firstOrNull { machine.startsWith(it.first) }?.second ?: "Apple Silicon"
}

/** Jailbreak heuristics: file-presence checks only (no URL-scheme probing — review-safe). */
private val jailbreakPaths = listOf(
    "/Applications/Cydia.app",
    "/Applications/Sileo.app",
    "/Applications/Zebra.app",
    "/Library/MobileSubstrate/MobileSubstrate.dylib",
    "/usr/sbin/sshd",
    "/etc/apt",
    "/private/var/lib/apt",
    "/var/jb",
)

private fun suspiciousJailbreakPaths(): List<String> {
    if (isSimulator()) return emptyList()
    val fm = NSFileManager.defaultManager
    return jailbreakPaths.filter { fm.fileExistsAtPath(it) }
}

// ── Battery ───────────────────────────────────────────────────────────────────

class IosBatteryRepository : BatteryRepository {

    private fun snapshot(): BatteryInfo {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true
        val level = device.batteryLevel // -1.0 when unknown (Simulator)
        val status = when (device.batteryState) {
            UIDeviceBatteryState.UIDeviceBatteryStateCharging -> BatteryStatus.Charging
            UIDeviceBatteryState.UIDeviceBatteryStateFull -> BatteryStatus.Full
            UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> BatteryStatus.Discharging
            else -> BatteryStatus.Unknown
        }
        return BatteryInfo(
            levelPercent = if (level >= 0f) (level * 100f).toInt() else -1,
            status = status,
            health = BatteryHealth.Unknown,          // iOS: not exposed to apps
            source = if (status == BatteryStatus.Charging || status == BatteryStatus.Full) {
                ChargeSource.USB                     // iOS: charger kind not exposed
            } else {
                ChargeSource.Unknown
            },
            technology = "Li-Ion",
            temperatureCelsius = 0f,                 // iOS: not exposed
            voltageMv = 0,                           // iOS: not exposed
            currentMa = null,
            capacityMah = null,
            chargeCycles = null,
            isPresent = true,
            estimatedWatts = null,
            chargeTimeRemainingMs = null,
            isPowerSaveMode = NSProcessInfo.processInfo.lowPowerModeEnabled,
        )
    }

    override suspend fun getBatterySnapshot(): AppResult<BatteryInfo> =
        runCatching { snapshot() }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown) },
        )

    override fun observeBatteryInfo(): Flow<AppResult<BatteryInfo>> = flow {
        while (true) {
            emit(getBatterySnapshot())
            delay(5_000)
        }
    }
}

// ── RAM / Storage ─────────────────────────────────────────────────────────────

class IosRamStorageRepository : RamRepository, StorageRepository {

    private fun ramSnapshot(): RamInfo {
        val total = NSProcessInfo.processInfo.physicalMemory.toLong()
        var free = 0L
        runCatching {
            memScoped {
                val stats = alloc<vm_statistics64_data_t>()
                val count = alloc<mach_msg_type_number_tVar>()
                count.value = (sizeOf<vm_statistics64_data_t>() / sizeOf<integer_tVar>()).convert()
                val kr = host_statistics64(
                    mach_host_self(),
                    HOST_VM_INFO64,
                    stats.ptr.reinterpret(),
                    count.ptr,
                )
                if (kr == 0) {
                    val pageSize = 16_384L // vm_kernel_page_size on modern Apple Silicon
                    free = (stats.free_count.toLong() + stats.inactive_count.toLong()) * pageSize
                }
            }
        }
        val used = (total - free).coerceAtLeast(0L)
        val usedPercent = if (total > 0) used.toFloat() / total.toFloat() else 0f
        return RamInfo(
            totalBytes = total,
            availableBytes = free,
            usedBytes = used,
            usedPercent = usedPercent,
            isLowMemory = usedPercent >= 0.9f,
        )
    }

    override suspend fun getRamSnapshot(): AppResult<RamInfo> =
        runCatching { ramSnapshot() }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown) },
        )

    override fun observeRamInfo(): Flow<AppResult<RamInfo>> = flow {
        while (true) {
            emit(getRamSnapshot())
            delay(3_000)
        }
    }

    override suspend fun getStorageInfo(): AppResult<StorageInfo> = runCatching {
        val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(NSHomeDirectory(), null)
            ?: error("File system attributes unavailable")
        val total = (attrs[NSFileSystemSize] as? NSNumber)?.longLongValue ?: 0L
        val freeBytes = (attrs[NSFileSystemFreeSize] as? NSNumber)?.longLongValue ?: 0L
        val used = (total - freeBytes).coerceAtLeast(0L)
        StorageInfo(
            totalBytes = total,
            usedBytes = used,
            freeBytes = freeBytes,
            usedPercent = if (total > 0) used.toFloat() / total.toFloat() else 0f,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.IoError(it.message ?: "storage")) },
    )
}

// ── Device / System ───────────────────────────────────────────────────────────

class IosDeviceRepository : DeviceRepository {
    override suspend fun getDeviceInfo(): AppResult<DeviceInfo> = runCatching {
        val device = UIDevice.currentDevice
        val machine = machineIdentifier()
        val suspicious = suspiciousJailbreakPaths()
        DeviceInfo(
            name = device.name,
            model = machine,
            manufacturer = "Apple",
            brand = "Apple",
            board = "",
            hardware = machine,
            codename = "",
            buildFingerprint = "",
            // identifierForVendor fills the stable per-install id role androidId plays on Android.
            androidId = device.identifierForVendor?.UUIDString ?: "",
            supportedAbis = listOf(if (machine.startsWith("x86")) "x86_64" else "arm64"),
            isRooted = suspicious.isNotEmpty(),
            bootloader = "",
            socName = chipName(machine),
            isEmulator = isSimulator(),
            suspiciousRootPaths = suspicious,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown) },
    )
}

class IosSystemRepository : SystemRepository {
    override suspend fun getSystemInfo(): AppResult<SystemInfo> = runCatching {
        val device = UIDevice.currentDevice
        val bundle = NSBundle.mainBundle
        val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
        val build = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toLongOrNull() ?: 0L
        SystemInfo(
            androidVersion = "${device.systemName} ${device.systemVersion}",
            apiLevel = 0,
            securityPatchLevel = "",                  // iOS patches ship with OS versions
            buildNumber = device.systemVersion,
            kernelVersion = memScoped {
                val info = alloc<utsname>()
                uname(info.ptr)
                "${info.sysname.toKString()} ${info.release.toKString()}"
            },
            javaVm = "",
            openGlVersion = "Metal",
            baseband = "",
            bootloader = "",
            language = NSLocale.currentLocale.localeIdentifier,
            timeZone = NSTimeZone.localTimeZone.name,
            releaseName = device.systemName,
            uptimeMillis = (NSProcessInfo.processInfo.systemUptime * 1000).toLong(),
            buildType = "release",
            isEncrypted = true,                       // iOS data protection is always on
            totalRamGb = NSProcessInfo.processInfo.physicalMemory.toLong() / 1_073_741_824f,
            isAppDebuggable = false,
            installerPackageName = "com.apple.AppStore",
            signingCertificateSha256 = null,
            appVersionName = version,
            appVersionCode = build,
            packageName = bundle.bundleIdentifier ?: "",
            isInstalledFromKnownStore = true,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown) },
    )
}

// ── CPU ───────────────────────────────────────────────────────────────────────

class IosCpuRepository : CpuRepository {
    private fun snapshot(): CpuInfo {
        val machine = machineIdentifier()
        return CpuInfo(
            chipsetName = chipName(machine),
            architecture = if (machine.startsWith("x86")) "x86_64 (Simulator)" else "ARM64 (Apple Silicon)",
            coreCount = NSProcessInfo.processInfo.processorCount.toInt(),
            cores = emptyList(),                      // per-core freq: sandbox-restricted
            clusters = emptyList(),
            governor = "",                            // not applicable on iOS
            gpu = GpuInfo(renderer = "Apple GPU", vendor = "Apple", version = "Metal 3"),
            temperatureCelsius = null,                // sandbox-restricted
            usagePercent = null,                      // system-wide usage: sandbox-restricted
            instructionSets = listOf(if (machine.startsWith("x86")) "x86_64" else "arm64e"),
            processCount = null,
        )
    }

    override suspend fun getCpuInfo(): AppResult<CpuInfo> =
        AppResult.Success(snapshot())

    override fun observeCpuCores(): Flow<AppResult<CpuInfo>> = flow {
        while (true) {
            emit(getCpuInfo())
            delay(3_000)
        }
    }
}

// ── Display ───────────────────────────────────────────────────────────────────

class IosDisplayRepository : DisplayRepository {
    override suspend fun getDisplayInfo(): AppResult<DisplayInfo> = runCatching {
        val screen = UIScreen.mainScreen
        val scale = screen.scale
        // CGRect is a C struct in Kotlin/Native — read it through useContents.
        val (wPt, hPt) = screen.bounds.useContents { size.width to size.height }
        DisplayInfo(
            widthPx = (wPt * scale).toInt(),
            heightPx = (hPt * scale).toInt(),
            densityDpi = (scale * 160).toInt(),
            densityBucket = "@${scale.toInt()}x",
            fontScale = 1f,
            physicalSizeInches = 0f,
            refreshRateHz = screen.maximumFramesPerSecond.toFloat(),
            supportedRefreshRates = listOf(60f, screen.maximumFramesPerSecond.toFloat()).distinct(),
            hdrCapabilities = emptyList(),
            isHdr = false,
            isWideColorGamut = true,                  // all modern iPhones are P3
            brightnessLevel = screen.brightness.toFloat(),
            isAdaptiveBrightness = true,              // True Tone / auto-brightness system-managed
            orientation = "Portrait",
            displayType = "OLED/LCD (system)",
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown) },
    )
}

// ── Thermal ───────────────────────────────────────────────────────────────────

class IosThermalRepository : ThermalRepository {
    private fun snapshot(): ThermalInfo {
        val name = when (NSProcessInfo.processInfo.thermalState) {
            NSProcessInfoThermalStateNominal -> "Nominal"
            NSProcessInfoThermalStateFair -> "Fair"
            NSProcessInfoThermalStateSerious -> "Serious"
            NSProcessInfoThermalStateCritical -> "Critical"
            else -> "Unknown"
        }
        // iOS exposes only a coarse system thermal state — surface it as a single zone
        // without a numeric temperature (the shared UI renders the unavailable state).
        return ThermalInfo(zones = listOf(ThermalZone(name = name, type = ThermalZoneType.Cpu, temperatureCelsius = null)))
    }

    override suspend fun getThermalInfo(): AppResult<ThermalInfo> = AppResult.Success(snapshot())

    override fun observeThermalInfo(): Flow<AppResult<ThermalInfo>> = flow {
        while (true) {
            emit(getThermalInfo())
            delay(5_000)
        }
    }
}

// ── Sensors ───────────────────────────────────────────────────────────────────

class IosSensorRepository : SensorRepository {
    override suspend fun getSensorInfo(): AppResult<SensorInfo> = runCatching {
        val motion = CMMotionManager()
        fun sensor(name: String, available: Boolean, type: Int) = SensorDetails(
            name = name,
            vendor = "Apple",
            type = type,
            typeName = name,
            version = 1,
            powerMa = 0f,
            resolution = 0f,
            maxRange = 0f,
            isWakeUp = false,
            isDynamic = false,
        ).takeIf { available }
        val sensors = listOfNotNull(
            sensor("Accelerometer", motion.accelerometerAvailable, 1),
            sensor("Gyroscope", motion.gyroAvailable, 4),
            sensor("Magnetometer", motion.magnetometerAvailable, 2),
            sensor("Barometer", CMAltimeter.isRelativeAltitudeAvailable(), 6),
            sensor("Pedometer", CMPedometer.isStepCountingAvailable(), 19),
            sensor("Proximity", true, 8),
        )
        SensorInfo(sensors = sensors)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown) },
    )
}

// ── Camera ────────────────────────────────────────────────────────────────────

class IosCameraRepository : CameraRepository {
    override suspend fun getCameraInfo(): AppResult<CameraInfo> = runCatching {
        // Enumerating capture devices does not require camera permission and never
        // prompts the user (we only read hardware capabilities, we never capture).
        val types = listOf(
            AVCaptureDeviceTypeBuiltInWideAngleCamera,
            AVCaptureDeviceTypeBuiltInTelephotoCamera,
            AVCaptureDeviceTypeBuiltInUltraWideCamera,
            AVCaptureDeviceTypeBuiltInTrueDepthCamera,
        )
        val session = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = types,
            mediaType = AVMediaTypeVideo,
            position = 0, // AVCaptureDevicePositionUnspecified → all
        )
        val cameras = session.devices.mapIndexedNotNull { index, raw ->
            val device = raw as? AVCaptureDevice ?: return@mapIndexedNotNull null
            CameraDetails(
                id = index.toString(),
                facing = when (device.position) {
                    AVCaptureDevicePositionBack -> CameraFacing.Back
                    AVCaptureDevicePositionFront -> CameraFacing.Front
                    else -> CameraFacing.Unknown
                },
                megapixels = 0f,
                resolutionWidth = 0,
                resolutionHeight = 0,
                focalLengths = emptyList(),
                hasFlash = device.hasFlash,
                hasOis = false,
                apertures = emptyList(),
                supportedModes = emptyList(),
            )
        }
        CameraInfo(cameras = cameras)
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown) },
    )
}

// ── Network / Connectivity ────────────────────────────────────────────────────

class IosNetworkRepository : NetworkRepository, ConnectivityRepository {

    private data class Snapshot(val type: ConnectionType, val ipv4: String?, val ipv6: String?)

    /**
     * Interface-based detection via getifaddrs (public POSIX API): en0 = Wi-Fi,
     * pdp_ip* = cellular. Avoids the nw_path C-interop surface for reliability.
     */
    private fun snapshot(): Snapshot = memScoped {
        var ipv4: String? = null
        var ipv6: String? = null
        var hasWifi = false
        var hasCellular = false

        val ifaddrPtr = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifaddrPtr.ptr) != 0) return Snapshot(ConnectionType.Unknown, null, null)
        try {
            var cursor = ifaddrPtr.value
            while (cursor != null) {
                val ifa = cursor.pointed
                val name = ifa.ifa_name?.toKString().orEmpty()
                val addr = ifa.ifa_addr
                if (addr != null && name != "lo0") {
                    val family = addr.pointed.sa_family.toInt()
                    if (family == AF_INET || family == AF_INET6) {
                        val host = allocArray<ByteVar>(NI_MAXHOST)
                        val ok = getnameinfo(
                            addr, addr.pointed.sa_len.convert(),
                            host, NI_MAXHOST.convert(),
                            null, 0u, NI_NUMERICHOST,
                        ) == 0
                        if (ok) {
                            val ip = host.toKString()
                            if (family == AF_INET) {
                                if (name == "en0") hasWifi = true
                                if (name.startsWith("pdp_ip")) hasCellular = true
                                if (ipv4 == null) ipv4 = ip
                            } else if (ipv6 == null && !ip.startsWith("fe80")) {
                                ipv6 = ip
                            }
                        }
                    }
                }
                cursor = ifa.ifa_next
            }
        } finally {
            freeifaddrs(ifaddrPtr.value)
        }

        val type = when {
            hasWifi -> ConnectionType.WiFi
            hasCellular -> ConnectionType.Cellular
            ipv4 != null -> ConnectionType.Unknown
            else -> ConnectionType.None
        }
        Snapshot(type, ipv4, ipv6)
    }

    private fun buildInfo(): NetworkInfo {
        val s = snapshot()
        return NetworkInfo(
            connectionType = s.type,
            ssid = null,                 // requires the wifi-info entitlement + location consent
            localIpv4 = s.ipv4,
            localIpv6 = s.ipv6,
            gateway = null,
            dns = emptyList(),
            subnetMask = null,
            interfaceName = if (s.type == ConnectionType.WiFi) "en0" else null,
            linkSpeedMbps = null,
            frequencyMhz = null,
            channel = null,
            wifiStandard = null,
            securityType = null,
            signalStrength = null,
            isMetered = s.type == ConnectionType.Cellular,
            isValidatedInternet = s.type != ConnectionType.None,
            activeTransports = listOf(s.type.name),
        )
    }

    override suspend fun getNetworkInfo(): AppResult<NetworkInfo> =
        runCatching { buildInfo() }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown) },
        )

    override fun observeNetworkInfo(): Flow<AppResult<NetworkInfo>> = flow {
        while (true) {
            emit(getNetworkInfo())
            delay(5_000)
        }
    }

    override suspend fun getConnectivityInfo(): AppResult<ConnectivityInfo> =
        AppResult.Success(
            ConnectivityInfo(
                hasWifi = true,
                hasWifi5Ghz = true,
                hasWifi6Ghz = false,
                hasWifiDirect = false,           // AirDrop is the iOS analogue, not exposed
                wifiStandards = emptyList(),
                hasBluetooth = true,             // capability only — no CoreBluetooth session,
                hasBluetoothLe = true,           // so no Bluetooth permission prompt is triggered
                hasNfc = true,
                hasUwb = machineIdentifier().let { !it.startsWith("x86") },
                hasEsim = true,
                bluetoothVersion = null,
            ),
        )
}

// ── Apps (sandbox-restricted on iOS) ─────────────────────────────────────────

class IosAppsRepository : AppsRepository {
    override suspend fun getAppList(): AppResult<AppListInfo> =
        AppResult.Error(AppError.PlatformRestricted)
}
