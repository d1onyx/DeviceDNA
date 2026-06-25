import SwiftUI

struct HardwareView: View {
    @EnvironmentObject var hw: HardwareService
    @State private var selectedTab = 0

    private let tabs = ["Device", "CPU", "Battery", "Display", "Camera"]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Tab strip
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(tabs.enumerated()), id: \.offset) { i, tab in
                            Button(action: { withAnimation { selectedTab = i } }) {
                                Text(tab)
                                    .font(.subheadline.weight(.medium))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(selectedTab == i ? AppColors.accent.opacity(0.15) : AppColors.surface)
                                    .foregroundStyle(selectedTab == i ? AppColors.accent : AppColors.textSecondary)
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(selectedTab == i ? AppColors.accent.opacity(0.4) : AppColors.border, lineWidth: 1)
                                    )
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                }
                .background(AppColors.surface)

                TabView(selection: $selectedTab) {
                    DeviceInfoPage(device: hw.device).tag(0)
                    CpuPage(cpu: hw.cpu).tag(1)
                    BatteryPage(battery: hw.battery).tag(2)
                    DisplayPage(display: hw.display).tag(3)
                    CameraPage(cameras: hw.cameras).tag(4)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .background(AppColors.background)
            .navigationTitle("Hardware")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: hw.refresh) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .overlay {
                if hw.isLoading && hw.device == nil {
                    ProgressView("Loading hardware…")
                        .padding(24)
                        .background(AppColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }
}

// MARK: - Pages

private struct DeviceInfoPage: View {
    let device: DeviceData?
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let d = device {
                    MetricCard(title: "Device Identity", icon: "iphone", color: AppColors.accent) {
                        VStack(spacing: 8) {
                            InfoRow(label: "Model", value: d.model)
                            InfoRow(label: "Name", value: d.name)
                            InfoRow(label: "System", value: "\(d.systemName) \(d.systemVersion)")
                            InfoRow(label: "Platform", value: d.isSimulator ? "Simulator" : "Physical Device")
                        }
                    }
                    MetricCard(title: "Screen", icon: "rectangle", color: AppColors.displayColor) {
                        VStack(spacing: 8) {
                            InfoRow(label: "Resolution", value: "\(d.screenWidth) × \(d.screenHeight) px")
                            InfoRow(label: "Scale", value: "\(d.scale, specifier: "%.1f")×")
                        }
                    }
                    MetricCard(title: "Privacy", icon: "lock.shield", color: AppColors.success) {
                        VStack(spacing: 8) {
                            InfoRow(label: "Vendor ID", value: d.identifier)
                        }
                    }
                } else {
                    placeholderRows()
                }
            }
            .padding(16)
        }
    }
}

private struct CpuPage: View {
    let cpu: CpuData?
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let c = cpu {
                    MetricCard(title: "Processor", icon: "cpu", color: AppColors.cpuColor) {
                        VStack(spacing: 8) {
                            InfoRow(label: "Chip", value: c.model)
                            InfoRow(label: "Architecture", value: c.architecture)
                            InfoRow(label: "Logical Cores", value: "\(c.coreCount)")
                            InfoRow(label: "Physical Cores", value: "\(c.physicalCores)")
                        }
                    }
                    MetricCard(title: "CPU Usage", icon: "chart.bar", color: AppColors.cpuColor) {
                        VStack(spacing: 8) {
                            HStack {
                                Text("Usage")
                                    .font(.subheadline)
                                    .foregroundStyle(AppColors.textSecondary)
                                Spacer()
                                Text("\(c.usagePercent, specifier: "%.1f")%")
                                    .font(.title3.weight(.bold))
                                    .foregroundStyle(AppColors.cpuColor)
                            }
                            LiveBar(fraction: c.usagePercent / 100, color: AppColors.cpuColor)
                        }
                    }
                    MetricCard(title: "Thermal", icon: "thermometer", color: AppColors.thermalColor) {
                        InfoRow(label: "State", value: c.thermalState)
                    }
                } else {
                    placeholderRows()
                }
            }
            .padding(16)
        }
    }
}

private struct BatteryPage: View {
    let battery: BatteryData?
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let b = battery {
                    MetricCard(title: "Battery", icon: "battery.100", color: AppColors.batteryColor) {
                        VStack(spacing: 8) {
                            HStack {
                                Text("Level")
                                    .font(.subheadline)
                                    .foregroundStyle(AppColors.textSecondary)
                                Spacer()
                                Text(b.levelPercent >= 0 ? "\(b.levelPercent)%" : "Unknown")
                                    .font(.title3.weight(.bold))
                                    .foregroundStyle(b.levelPercent <= 20 ? AppColors.critical : AppColors.batteryColor)
                            }
                            if b.levelPercent >= 0 {
                                LiveBar(fraction: Double(b.levelPercent) / 100, color: b.levelPercent <= 20 ? AppColors.critical : AppColors.batteryColor)
                            }
                            InfoRow(label: "Status", value: b.status)
                            InfoRow(label: "Low Power Mode", value: b.isLowPowerMode ? "On" : "Off")
                        }
                    }
                    MetricCard(title: "Note", icon: "info.circle", color: AppColors.textMuted) {
                        Text("iOS restricts access to battery capacity, cycle count, and chemistry data. Detailed metrics are available only on jailbroken devices or via system diagnostics.")
                            .font(.caption)
                            .foregroundStyle(AppColors.textMuted)
                    }
                } else {
                    placeholderRows()
                }
            }
            .padding(16)
        }
    }
}

private struct DisplayPage: View {
    let display: DisplayData?
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let d = display {
                    MetricCard(title: "Display", icon: "display", color: AppColors.displayColor) {
                        VStack(spacing: 8) {
                            InfoRow(label: "Resolution", value: "\(d.widthPx) × \(d.heightPx) px")
                            InfoRow(label: "Logical Size", value: "\(Int(d.physicalWidthPt)) × \(Int(d.physicalHeightPt)) pt")
                            InfoRow(label: "Scale", value: "\(d.scale, specifier: "%.1f")×")
                            InfoRow(label: "Refresh Rate", value: "\(Int(d.refreshRateHz)) Hz")
                            InfoRow(label: "HDR / P3", value: d.isHDR ? "Supported" : "Not detected")
                        }
                    }
                    MetricCard(title: "Brightness", icon: "sun.max", color: AppColors.displayColor) {
                        VStack(spacing: 8) {
                            HStack {
                                Text("Level")
                                    .font(.subheadline)
                                    .foregroundStyle(AppColors.textSecondary)
                                Spacer()
                                Text("\(Int(d.brightness * 100))%")
                                    .font(.title3.weight(.bold))
                                    .foregroundStyle(AppColors.displayColor)
                            }
                            LiveBar(fraction: d.brightness, color: AppColors.displayColor)
                        }
                    }
                } else {
                    placeholderRows()
                }
            }
            .padding(16)
        }
    }
}

private struct CameraPage: View {
    let cameras: CameraData?
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let c = cameras {
                    if !c.backCameras.isEmpty {
                        MetricCard(title: "Rear Cameras (\(c.backCameras.count))", icon: "camera", color: AppColors.cameraColor) {
                            VStack(spacing: 10) {
                                ForEach(c.backCameras, id: \.name) { cam in
                                    VStack(spacing: 6) {
                                        InfoRow(label: cam.name, value: cam.position)
                                        InfoRow(label: "Flash", value: cam.hasFlash ? "Yes" : "No")
                                        InfoRow(label: "OIS", value: cam.hasOIS ? "Yes" : "No")
                                        InfoRow(label: "Max Zoom", value: "\(cam.maxZoom, specifier: "%.1f")×")
                                    }
                                    if cam.name != c.backCameras.last?.name {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                    if !c.frontCameras.isEmpty {
                        MetricCard(title: "Front Cameras (\(c.frontCameras.count))", icon: "camera.on.rectangle", color: AppColors.cameraColor) {
                            VStack(spacing: 10) {
                                ForEach(c.frontCameras, id: \.name) { cam in
                                    VStack(spacing: 6) {
                                        InfoRow(label: cam.name, value: cam.position)
                                        InfoRow(label: "OIS", value: cam.hasOIS ? "Yes" : "No")
                                    }
                                    if cam.name != c.frontCameras.last?.name {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    placeholderRows()
                }
            }
            .padding(16)
        }
    }
}

@ViewBuilder
private func placeholderRows() -> some View {
    ForEach(0..<3) { _ in
        RoundedRectangle(cornerRadius: 16)
            .fill(AppColors.surface)
            .frame(height: 80)
            .redacted(reason: .placeholder)
    }
}
