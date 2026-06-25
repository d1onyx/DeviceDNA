import SwiftUI
import Network

struct SystemView: View {
    @EnvironmentObject var hw: HardwareService

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 14) {
                    // OS Info
                    MetricCard(title: "Operating System", icon: "apple.logo", color: AppColors.accent) {
                        VStack(spacing: 8) {
                            InfoRow(label: "iOS Version", value: UIDevice.current.systemVersion)
                            InfoRow(label: "System", value: UIDevice.current.systemName)
                            InfoRow(label: "Uptime", value: formatUptime(ProcessInfo.processInfo.systemUptime))
                            InfoRow(label: "Processes", value: "\(ProcessInfo.processInfo.processorCount) cores active")
                            InfoRow(label: "Low Power Mode", value: ProcessInfo.processInfo.isLowPowerModeEnabled ? "On" : "Off")
                        }
                    }

                    // Memory
                    if let mem = hw.memory {
                        MetricCard(title: "Memory", icon: "memorychip", color: AppColors.ramColor) {
                            VStack(spacing: 8) {
                                HStack {
                                    Text("Used")
                                        .font(.subheadline)
                                        .foregroundStyle(AppColors.textSecondary)
                                    Spacer()
                                    Text("\(Int(mem.usedPercent * 100))%")
                                        .font(.title3.weight(.bold))
                                        .foregroundStyle(AppColors.ramColor)
                                }
                                LiveBar(fraction: mem.usedPercent, color: AppColors.ramColor)
                                InfoRow(label: "Total RAM", value: SharedFormatters.formatBytesU(mem.totalBytes))
                                InfoRow(label: "Used", value: SharedFormatters.formatBytesU(mem.usedBytes))
                                InfoRow(label: "Free", value: SharedFormatters.formatBytesU(mem.freeBytes))
                            }
                        }
                    }

                    // Network
                    if let net = hw.network {
                        MetricCard(title: "Network", icon: "wifi", color: AppColors.networkColor) {
                            VStack(spacing: 8) {
                                InfoRow(label: "Status", value: net.isConnected ? "Connected" : "Offline")
                                InfoRow(label: "Type", value: net.connectionType)
                                if let ip = net.ipv4 {
                                    InfoRow(label: "IPv4", value: ip)
                                }
                                if let ip = net.ipv6 {
                                    InfoRow(label: "IPv6", value: ip)
                                }
                                InfoRow(label: "SSID", value: net.ssid ?? "Requires entitlement")
                            }
                        }
                    }

                    // Sensors
                    if let sensors = hw.sensors {
                        MetricCard(title: "Sensors", icon: "sensor.tag.radiowaves.forward", color: AppColors.displayColor) {
                            VStack(spacing: 8) {
                                SensorRow("Accelerometer", sensors.hasAccelerometer)
                                SensorRow("Gyroscope", sensors.hasGyroscope)
                                SensorRow("Magnetometer", sensors.hasMagnetometer)
                                SensorRow("Barometer", sensors.hasBarometer)
                                SensorRow("Proximity", sensors.hasProximity)
                                SensorRow("Pedometer", sensors.hasPedometer)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(AppColors.background)
            .navigationTitle("System")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: hw.refresh) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
    }

    private func formatUptime(_ seconds: TimeInterval) -> String {
        SharedFormatters.formatUptimeSec(seconds)
    }
}

private struct SensorRow: View {
    let name: String
    let available: Bool

    init(_ name: String, _ available: Bool) {
        self.name = name
        self.available = available
    }

    var body: some View {
        HStack {
            Text(name)
                .font(.subheadline)
                .foregroundStyle(AppColors.textSecondary)
            Spacer()
            HStack(spacing: 4) {
                Circle()
                    .fill(available ? AppColors.success : AppColors.textMuted)
                    .frame(width: 8, height: 8)
                Text(available ? "Available" : "Not available")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(available ? AppColors.success : AppColors.textMuted)
            }
        }
    }
}
