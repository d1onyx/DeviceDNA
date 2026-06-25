import SwiftUI

struct DiagnosticsView: View {
    @StateObject private var hw = HardwareService()

    private var checks: [DiagnosticCheck] {
        buildChecks()
    }

    private var passedCount: Int {
        checks.filter { $0.status == .passed }.count
    }

    private var warningCount: Int {
        checks.filter { $0.status == .warning }.count
    }

    private var failedCount: Int {
        checks.filter { $0.status == .failed }.count
    }

    private var unavailableCount: Int {
        checks.filter { $0.status == .unavailable }.count
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 14) {
                    MetricCard(title: "Hardware Diagnostics", icon: "checklist.checked", color: failedCount > 0 ? AppColors.critical : AppColors.success) {
                        VStack(spacing: 12) {
                            HStack(spacing: 8) {
                                CounterChip(label: "Passed", value: passedCount, color: AppColors.success)
                                CounterChip(label: "Warnings", value: warningCount, color: AppColors.warning)
                                CounterChip(label: "Failed", value: failedCount, color: AppColors.critical)
                                CounterChip(label: "N/A", value: unavailableCount, color: AppColors.textMuted)
                            }
                            LiveBar(
                                fraction: checks.isEmpty ? 0 : Double(passedCount) / Double(checks.count),
                                color: failedCount > 0 ? AppColors.critical : AppColors.success,
                                height: 8
                            )
                            Button(action: hw.refresh) {
                                Label("Run hardware checks", systemImage: "arrow.clockwise")
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(AppColors.accent)
                        }
                    }

                    ForEach(groupedChecks, id: \.group) { group in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(group.group)
                                .font(.headline)
                                .foregroundStyle(AppColors.textPrimary)
                                .padding(.horizontal, 4)

                            ForEach(group.checks) { check in
                                DiagnosticRow(check: check)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(AppColors.background)
            .navigationTitle("Tests")
            .navigationBarTitleDisplayMode(.large)
            .overlay {
                if hw.isLoading {
                    ProgressView("Running checks...")
                        .padding(24)
                        .background(AppColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
        .onAppear { hw.loadAll() }
    }

    private var groupedChecks: [(group: String, checks: [DiagnosticCheck])] {
        let order = ["Device", "Performance", "Power", "Memory", "Storage", "Display", "Camera", "Network", "Sensors", "Apps"]
        return order.compactMap { group in
            let items = checks.filter { $0.group == group }
            return items.isEmpty ? nil : (group, items)
        }
    }

    private func buildChecks() -> [DiagnosticCheck] {
        var result: [DiagnosticCheck] = []

        if let device = hw.device {
            result.append(check("Device", "Device identity", "\(device.model), \(device.systemName) \(device.systemVersion)", !device.model.isEmpty))
            result.append(check("Device", "Physical environment", device.isSimulator ? "Simulator" : "Physical device", !device.isSimulator, warningWhenFalse: true))
            result.append(check("Device", "Screen metrics", "\(device.screenWidth) x \(device.screenHeight) px @ \(String(format: "%.1f", device.screenScale))x", device.screenWidth > 0 && device.screenHeight > 0))
        } else {
            result.append(error("Device", "Device identity", "Not loaded"))
        }

        if let cpu = hw.cpu {
            result.append(check("Performance", "CPU topology", "\(cpu.coreCount) logical / \(cpu.physicalCores) physical cores", cpu.coreCount > 0))
            result.append(quality("Performance", "CPU usage", "\(String(format: "%.1f", cpu.usagePercent))%", cpu.usagePercent < 75, acceptable: cpu.usagePercent < 92))
            result.append(check("Performance", "Thermal state", cpu.thermalState, cpu.thermalState != "Critical", warningWhenFalse: cpu.thermalState == "Serious"))
        } else {
            result.append(error("Performance", "CPU snapshot", "Not loaded"))
        }

        if let battery = hw.battery {
            result.append(check("Power", "Battery state", battery.levelPercent >= 0 ? "\(battery.levelPercent)% \(battery.status)" : battery.status, battery.levelPercent >= 0))
            result.append(check("Power", "Low Power Mode", battery.isLowPowerMode ? "On" : "Off", !battery.isLowPowerMode, warningWhenFalse: true))
        } else {
            result.append(error("Power", "Battery snapshot", "Not loaded"))
        }

        if let memory = hw.memory {
            result.append(check("Memory", "RAM capacity", HardwareService.formatBytesU(memory.totalBytes), memory.totalBytes > 0))
            result.append(quality("Memory", "RAM pressure", "\(Int(memory.usedPercent * 100))% used", memory.usedPercent < 0.75, acceptable: memory.usedPercent < 0.92))
        } else {
            result.append(error("Memory", "RAM snapshot", "Not loaded"))
        }

        if let storage = hw.storage {
            result.append(check("Storage", "Storage capacity", "\(HardwareService.formatBytes(storage.freeBytes)) free of \(HardwareService.formatBytes(storage.totalBytes))", storage.totalBytes > 0))
            result.append(quality("Storage", "Storage pressure", "\(Int(storage.usedPercent * 100))% used", storage.usedPercent < 0.80, acceptable: storage.usedPercent < 0.95))
        } else {
            result.append(error("Storage", "Storage snapshot", "Not loaded"))
        }

        if let display = hw.display {
            result.append(check("Display", "Display metrics", "\(display.widthPx) x \(display.heightPx), \(Int(display.refreshRateHz)) Hz", display.widthPx > 0 && display.heightPx > 0))
            result.append(check("Display", "Brightness", "\(Int(display.brightness * 100))%", display.brightness >= 0))
            result.append(check("Display", "Wide color / HDR", display.isHDR ? "Detected" : "Not detected", display.isHDR, unavailableWhenFalse: true))
        } else {
            result.append(error("Display", "Display metrics", "Not loaded"))
        }

        if let cameras = hw.cameras {
            result.append(check("Camera", "Rear cameras", "\(cameras.backCameras.count) camera(s)", !cameras.backCameras.isEmpty, unavailableWhenFalse: true))
            result.append(check("Camera", "Front cameras", "\(cameras.frontCameras.count) camera(s)", !cameras.frontCameras.isEmpty, unavailableWhenFalse: true))
        } else {
            result.append(error("Camera", "Camera inventory", "Not loaded"))
        }

        if let network = hw.network {
            result.append(check("Network", "Network state", network.isConnected ? network.connectionType : "Offline", network.isConnected, unavailableWhenFalse: true))
            result.append(check("Network", "IPv4 address", network.ipv4 ?? "Not reported", network.ipv4 != nil, unavailableWhenFalse: !network.isConnected, warningWhenFalse: network.isConnected))
            result.append(check("Network", "SSID access", network.ssid ?? "Requires entitlement", network.ssid != nil, unavailableWhenFalse: true))
        } else {
            result.append(error("Network", "Network state", "Not loaded"))
        }

        if let sensors = hw.sensors {
            result.append(check("Sensors", "Accelerometer", sensors.hasAccelerometer ? "Available" : "Not available", sensors.hasAccelerometer, unavailableWhenFalse: true))
            result.append(check("Sensors", "Gyroscope", sensors.hasGyroscope ? "Available" : "Not available", sensors.hasGyroscope, unavailableWhenFalse: true))
            result.append(check("Sensors", "Barometer", sensors.hasBarometer ? "Available" : "Not available", sensors.hasBarometer, unavailableWhenFalse: true))
            result.append(check("Sensors", "Pedometer", sensors.hasPedometer ? "Available" : "Not available", sensors.hasPedometer, unavailableWhenFalse: true))
        } else {
            result.append(error("Sensors", "Sensor inventory", "Not loaded"))
        }

        result.append(DiagnosticCheck(group: "Apps", title: "App inventory", detail: "Restricted by iOS privacy policy", status: .unavailable))
        return result
    }

    private func check(
        _ group: String,
        _ title: String,
        _ detail: String,
        _ passed: Bool,
        warningWhenFalse: Bool = false,
        unavailableWhenFalse: Bool = false
    ) -> DiagnosticCheck {
        let status: DiagnosticStatus
        if passed {
            status = .passed
        } else if unavailableWhenFalse {
            status = .unavailable
        } else if warningWhenFalse {
            status = .warning
        } else {
            status = .failed
        }
        return DiagnosticCheck(group: group, title: title, detail: detail, status: status)
    }

    private func quality(_ group: String, _ title: String, _ detail: String, _ passed: Bool, acceptable: Bool) -> DiagnosticCheck {
        let status: DiagnosticStatus = passed ? .passed : (acceptable ? .warning : .failed)
        return DiagnosticCheck(group: group, title: title, detail: detail, status: status)
    }

    private func error(_ group: String, _ title: String, _ detail: String) -> DiagnosticCheck {
        DiagnosticCheck(group: group, title: title, detail: detail, status: .failed)
    }
}

private struct DiagnosticCheck: Identifiable {
    let id = UUID()
    let group: String
    let title: String
    let detail: String
    let status: DiagnosticStatus
}

private enum DiagnosticStatus {
    case passed
    case warning
    case failed
    case unavailable
}

private struct DiagnosticRow: View {
    let check: DiagnosticCheck

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: iconName)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(statusColor)
                .frame(width: 34, height: 34)
                .background(statusColor.opacity(0.14))
                .clipShape(RoundedRectangle(cornerRadius: 10))

            VStack(alignment: .leading, spacing: 3) {
                Text(check.title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppColors.textPrimary)
                Text(check.detail)
                    .font(.caption)
                    .foregroundStyle(AppColors.textMuted)
                    .lineLimit(2)
            }
            Spacer()
            Text(statusText)
                .font(.caption.weight(.semibold))
                .foregroundStyle(statusColor)
        }
        .padding(12)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(AppColors.border, lineWidth: 1))
    }

    private var iconName: String {
        switch check.status {
        case .passed: return "checkmark"
        case .warning: return "exclamationmark"
        case .failed: return "xmark"
        case .unavailable: return "minus"
        }
    }

    private var statusText: String {
        switch check.status {
        case .passed: return "OK"
        case .warning: return "Check"
        case .failed: return "Fail"
        case .unavailable: return "N/A"
        }
    }

    private var statusColor: Color {
        switch check.status {
        case .passed: return AppColors.success
        case .warning: return AppColors.warning
        case .failed: return AppColors.critical
        case .unavailable: return AppColors.textMuted
        }
    }
}

private struct CounterChip: View {
    let label: String
    let value: Int
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text("\(value)")
                .font(.headline)
                .foregroundStyle(color)
            Text(label)
                .font(.caption2)
                .foregroundStyle(AppColors.textMuted)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(AppColors.surfaceElevated)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
