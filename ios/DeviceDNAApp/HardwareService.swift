import Foundation
import UIKit
import AVFoundation
import CoreMotion
import Network
import SystemConfiguration

// MARK: - Models

struct DeviceData {
    let model: String
    let name: String
    let systemName: String
    let systemVersion: String
    let identifier: String
    let isSimulator: Bool
    let screenWidth: Int
    let screenHeight: Int
    let scale: Double
}

struct BatteryData {
    let levelPercent: Int
    let status: String
    let isLowPowerMode: Bool
}

struct CpuData {
    let coreCount: Int
    let physicalCores: Int
    let architecture: String
    let model: String
    let usagePercent: Double
    let thermalState: String
}

struct MemoryData {
    let totalBytes: UInt64
    let usedBytes: UInt64
    let freeBytes: UInt64
    let usedPercent: Double
}

struct StorageData {
    let totalBytes: Int64
    let usedBytes: Int64
    let freeBytes: Int64
    let usedPercent: Double
}

struct DisplayData {
    let widthPx: Int
    let heightPx: Int
    let scale: Double
    let physicalWidthPt: Double
    let physicalHeightPt: Double
    let brightness: Double
    let refreshRateHz: Double
    let isHDR: Bool
}

struct CameraData {
    let backCameras: [CameraDevice]
    let frontCameras: [CameraDevice]
}

struct CameraDevice {
    let name: String
    let position: String
    let hasFlash: Bool
    let hasOIS: Bool
    let maxZoom: Double
    let formats: Int
}

struct NetworkData {
    let isConnected: Bool
    let connectionType: String
    let ipv4: String?
    let ipv6: String?
    let ssid: String?
}

struct SensorAvailability {
    let hasAccelerometer: Bool
    let hasGyroscope: Bool
    let hasMagnetometer: Bool
    let hasBarometer: Bool
    let hasProximity: Bool
    let hasPedometer: Bool
}

// MARK: - Service

@MainActor
class HardwareService: ObservableObject {
    @Published var device: DeviceData?
    @Published var battery: BatteryData?
    @Published var cpu: CpuData?
    @Published var memory: MemoryData?
    @Published var storage: StorageData?
    @Published var display: DisplayData?
    @Published var cameras: CameraData?
    @Published var network: NetworkData?
    @Published var sensors: SensorAvailability?
    @Published var isLoading = true

    private let motionManager = CMMotionManager()
    private let altimeter = CMAltimeter()
    private var networkMonitor: NWPathMonitor?
    private var networkQueue = DispatchQueue(label: "network.monitor")
    private var cpuTimer: Timer?
    private var batteryObservers: [NSObjectProtocol] = []
    private var liveUpdatesStarted = false

    deinit {
        cpuTimer?.invalidate()
        networkMonitor?.cancel()
        batteryObservers.forEach { NotificationCenter.default.removeObserver($0) }
    }

    func loadAll() {
        isLoading = true
        Task {
            await collectAll()
            isLoading = false
            startLiveUpdates()
        }
    }

    func refresh() {
        Task {
            await collectAll()
        }
    }

    private func collectAll() async {
        device = collectDevice()
        battery = collectBattery()
        cpu = collectCpu()
        memory = collectMemory()
        storage = collectStorage()
        display = collectDisplay()
        cameras = collectCameras()
        sensors = collectSensors()
    }

    private func startLiveUpdates() {
        guard !liveUpdatesStarted else { return }
        liveUpdatesStarted = true

        UIDevice.current.isBatteryMonitoringEnabled = true
        let levelObserver = NotificationCenter.default.addObserver(forName: UIDevice.batteryLevelDidChangeNotification, object: nil, queue: .main) { [weak self] _ in
            self?.battery = self?.collectBattery()
        }
        let stateObserver = NotificationCenter.default.addObserver(forName: UIDevice.batteryStateDidChangeNotification, object: nil, queue: .main) { [weak self] _ in
            self?.battery = self?.collectBattery()
        }
        batteryObservers = [levelObserver, stateObserver]

        networkMonitor = NWPathMonitor()
        networkMonitor?.pathUpdateHandler = { [weak self] path in
            Task { @MainActor [weak self] in
                self?.network = self?.collectNetworkFromPath(path)
            }
        }
        networkMonitor?.start(queue: networkQueue)

        cpuTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.cpu = self?.collectCpu()
                self?.memory = self?.collectMemory()
            }
        }
    }

    // MARK: - Collectors

    private func collectDevice() -> DeviceData {
        let screen = UIScreen.main
        return DeviceData(
            model: SharedPlatformInfo.deviceModel,
            name: UIDevice.current.name,
            systemName: SharedPlatformInfo.osName,
            systemVersion: SharedPlatformInfo.osVersion,
            identifier: identifierForVendor(),
            isSimulator: ProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != nil,
            screenWidth: Int(screen.bounds.width * screen.scale),
            screenHeight: Int(screen.bounds.height * screen.scale),
            scale: Double(screen.scale)
        )
    }

    private func collectBattery() -> BatteryData {
        let device = UIDevice.current
        device.isBatteryMonitoringEnabled = true
        let level = device.batteryLevel >= 0 ? Int(device.batteryLevel * 100) : -1
        let status: String
        switch device.batteryState {
        case .charging: status = "Charging"
        case .full: status = "Full"
        case .unplugged: status = "Discharging"
        default: status = "Unknown"
        }
        return BatteryData(
            levelPercent: level,
            status: status,
            isLowPowerMode: ProcessInfo.processInfo.isLowPowerModeEnabled
        )
    }

    private func collectCpu() -> CpuData {
        let physicalCores = Int(sysctlValue("hw.physicalcpu") ?? 0)
        let logicalCores = ProcessInfo.processInfo.processorCount
        let arch = cpuArchitecture()
        let model = cpuModel()
        let usage = measureCpuUsage()

        let thermalState: String
        switch ProcessInfo.processInfo.thermalState {
        case .nominal: thermalState = "Normal"
        case .fair: thermalState = "Fair"
        case .serious: thermalState = "Serious"
        case .critical: thermalState = "Critical"
        @unknown default: thermalState = "Unknown"
        }

        return CpuData(
            coreCount: logicalCores,
            physicalCores: physicalCores > 0 ? physicalCores : logicalCores,
            architecture: arch,
            model: model,
            usagePercent: usage,
            thermalState: thermalState
        )
    }

    private func collectMemory() -> MemoryData {
        let total = ProcessInfo.processInfo.physicalMemory
        let (used, free) = memoryUsage()
        let usedPercent = total > 0 ? Double(used) / Double(total) : 0
        return MemoryData(
            totalBytes: total,
            usedBytes: used,
            freeBytes: free,
            usedPercent: usedPercent
        )
    }

    private func collectStorage() -> StorageData {
        let fm = FileManager.default
        guard let attrs = try? fm.attributesOfFileSystem(forPath: NSHomeDirectory()) else {
            return StorageData(totalBytes: 0, usedBytes: 0, freeBytes: 0, usedPercent: 0)
        }
        let total = (attrs[.systemSize] as? Int64) ?? 0
        let free = (attrs[.systemFreeSize] as? Int64) ?? 0
        let used = total - free
        let pct = total > 0 ? Double(used) / Double(total) : 0
        return StorageData(totalBytes: total, usedBytes: used, freeBytes: free, usedPercent: pct)
    }

    private func collectDisplay() -> DisplayData {
        let screen = UIScreen.main
        let bounds = screen.bounds
        let scale = screen.scale
        let widthPx = Int(bounds.width * scale)
        let heightPx = Int(bounds.height * scale)
        let brightness = Double(UIScreen.main.brightness)

        var refreshRate: Double = 60
        if #available(iOS 15.0, *) {
            refreshRate = Double(UIScreen.main.maximumFramesPerSecond)
        }

        return DisplayData(
            widthPx: widthPx,
            heightPx: heightPx,
            scale: Double(scale),
            physicalWidthPt: Double(bounds.width),
            physicalHeightPt: Double(bounds.height),
            brightness: brightness,
            refreshRateHz: refreshRate,
            isHDR: screen.traitCollection.displayGamut == .P3
        )
    }

    private func collectCameras() -> CameraData {
        let backSession = AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInWideAngleCamera, .builtInTelephotoCamera, .builtInUltraWideCamera],
            mediaType: .video,
            position: .back
        )
        let frontSession = AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInWideAngleCamera, .builtInTrueDepthCamera],
            mediaType: .video,
            position: .front
        )
        return CameraData(
            backCameras: backSession.devices.map { mapCamera($0, position: "Back") },
            frontCameras: frontSession.devices.map { mapCamera($0, position: "Front") }
        )
    }

    private func mapCamera(_ device: AVCaptureDevice, position: String) -> CameraDevice {
        CameraDevice(
            name: device.localizedName,
            position: position,
            hasFlash: device.hasFlash,
            hasOIS: device.isOpticalImageStabilizationSupported,
            maxZoom: Double(device.maxAvailableVideoZoomFactor),
            formats: device.formats.count
        )
    }

    private func collectSensors() -> SensorAvailability {
        SensorAvailability(
            hasAccelerometer: motionManager.isAccelerometerAvailable,
            hasGyroscope: motionManager.isGyroAvailable,
            hasMagnetometer: motionManager.isMagnetometerAvailable,
            hasBarometer: CMAltimeter.isRelativeAltitudeAvailable(),
            hasProximity: UIDevice.current.isProximityMonitoringEnabled || isProximitySensorAvailable(),
            hasPedometer: CMPedometer.isStepCountingAvailable()
        )
    }

    private func collectNetworkFromPath(_ path: NWPath) -> NetworkData {
        let isConnected = path.status == .satisfied
        let connectionType: String
        if path.usesInterfaceType(.wifi) {
            connectionType = "Wi-Fi"
        } else if path.usesInterfaceType(.cellular) {
            connectionType = "Cellular"
        } else if path.usesInterfaceType(.wiredEthernet) {
            connectionType = "Ethernet"
        } else {
            connectionType = isConnected ? "Other" : "None"
        }
        let (ipv4, ipv6) = localIPAddresses()
        return NetworkData(
            isConnected: isConnected,
            connectionType: connectionType,
            ipv4: ipv4,
            ipv6: ipv6,
            ssid: currentSSID()
        )
    }

    // MARK: - Helpers

    private func identifierForVendor() -> String {
        UIDevice.current.identifierForVendor?.uuidString ?? "Unknown"
    }

    private func cpuArchitecture() -> String {
        var sysinfo = utsname()
        uname(&sysinfo)
        let machine = String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)?
            .trimmingCharacters(in: .controlCharacters) ?? ""
        if machine.contains("arm64") || machine.hasPrefix("iPhone") || machine.hasPrefix("iPad") {
            return "ARM64 (Apple Silicon)"
        }
        if machine.hasPrefix("x86") { return "x86_64 (Simulator)" }
        return "ARM64"
    }

    private func cpuModel() -> String {
        var sysinfo = utsname()
        uname(&sysinfo)
        let machine = String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)?
            .trimmingCharacters(in: .controlCharacters) ?? ""
        // Map known identifiers to Apple chip names
        let chipMap: [String: String] = [
            "iPhone15": "Apple A16 Bionic", "iPhone14": "Apple A15 Bionic",
            "iPhone13": "Apple A15 Bionic", "iPhone12": "Apple A14 Bionic",
            "iPhone11": "Apple A13 Bionic", "iPhone10": "Apple A11 Bionic",
            "iPad16": "Apple M2", "iPad15": "Apple M1",
            "iPad14": "Apple M2", "iPad13": "Apple M1",
        ]
        for (prefix, name) in chipMap {
            if machine.hasPrefix(prefix) { return name }
        }
        return "Apple Silicon"
    }

    private func sysctlValue(_ name: String) -> Int64? {
        var size = 0
        sysctlbyname(name, nil, &size, nil, 0)
        var value: Int64 = 0
        sysctlbyname(name, &value, &size, nil, 0)
        return value > 0 ? value : nil
    }

    private func measureCpuUsage() -> Double {
        var threadList: thread_act_array_t?
        var threadCount: mach_msg_type_number_t = 0
        guard task_threads(mach_task_self_, &threadList, &threadCount) == KERN_SUCCESS,
              let threads = threadList else { return 0 }
        defer { vm_deallocate(mach_task_self_, vm_address_t(bitPattern: threads), vm_size_t(threadCount) * vm_size_t(MemoryLayout<thread_t>.stride)) }

        var totalUsage: Double = 0
        for i in 0..<Int(threadCount) {
            var info = thread_basic_info()
            var count = mach_msg_type_number_t(THREAD_BASIC_INFO_COUNT)
            let result = withUnsafeMutablePointer(to: &info) {
                $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                    thread_info(threads[i], thread_flavor_t(THREAD_BASIC_INFO), $0, &count)
                }
            }
            if result == KERN_SUCCESS && info.flags & TH_FLAGS_IDLE == 0 {
                totalUsage += Double(info.cpu_usage) / Double(TH_USAGE_SCALE)
            }
        }
        let cores = max(ProcessInfo.processInfo.processorCount, 1)
        return min(totalUsage / Double(cores) * 100, 100)
    }

    private func memoryUsage() -> (used: UInt64, free: UInt64) {
        var info = vm_statistics64_data_t()
        var count = mach_msg_type_number_t(MemoryLayout<vm_statistics64_data_t>.size / MemoryLayout<integer_t>.size)
        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                host_statistics64(mach_host_self(), HOST_VM_INFO64, $0, &count)
            }
        }
        guard result == KERN_SUCCESS else { return (0, 0) }
        let pageSize = UInt64(vm_kernel_page_size)
        let free = UInt64(info.free_count) * pageSize
        let active = UInt64(info.active_count) * pageSize
        let inactive = UInt64(info.inactive_count) * pageSize
        let wired = UInt64(info.wire_count) * pageSize
        let used = active + inactive + wired
        return (used, free)
    }

    private func localIPAddresses() -> (ipv4: String?, ipv6: String?) {
        var ipv4: String?
        var ipv6: String?
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return (nil, nil) }
        defer { freeifaddrs(ifaddr) }
        var ptr = ifaddr
        while let iface = ptr {
            let addr = iface.pointee.ifa_addr
            if addr?.pointee.sa_family == UInt8(AF_INET) && String(cString: iface.pointee.ifa_name) != "lo0" {
                var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                if getnameinfo(addr, socklen_t(addr!.pointee.sa_len), &hostname, socklen_t(hostname.count), nil, 0, NI_NUMERICHOST) == 0 {
                    if ipv4 == nil { ipv4 = String(cString: hostname) }
                }
            } else if addr?.pointee.sa_family == UInt8(AF_INET6) && String(cString: iface.pointee.ifa_name) != "lo0" {
                var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                if getnameinfo(addr, socklen_t(addr!.pointee.sa_len), &hostname, socklen_t(hostname.count), nil, 0, NI_NUMERICHOST) == 0 {
                    let ip = String(cString: hostname)
                    if ipv6 == nil && !ip.hasPrefix("fe80") { ipv6 = ip }
                }
            }
            ptr = iface.pointee.ifa_next
        }
        return (ipv4, ipv6)
    }

    private func currentSSID() -> String? {
        // SSID access requires special entitlements on iOS 13+
        // Returns nil unless com.apple.developer.networking.wifi-info entitlement is granted
        return nil
    }

    private func isProximitySensorAvailable() -> Bool {
        let device = UIDevice.current
        let wasEnabled = device.isProximityMonitoringEnabled
        device.isProximityMonitoringEnabled = true
        let available = device.isProximityMonitoringEnabled
        device.isProximityMonitoringEnabled = wasEnabled
        return available
    }
}

// MARK: - Formatters (delegates to SharedFormatters / SharedBridge.swift)

extension HardwareService {
    static func formatBytes(_ bytes: Int64) -> String  { SharedFormatters.formatBytes(bytes) }
    static func formatBytesU(_ bytes: UInt64) -> String { SharedFormatters.formatBytesU(bytes) }
}
