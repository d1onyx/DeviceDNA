import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var authState: AuthState
    @EnvironmentObject var hw: HardwareService

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 14) {
                    // Gauges row
                    HStack(spacing: 0) {
                        GaugeCard(
                            label: "CPU",
                            value: hw.cpu?.usagePercent ?? 0,
                            color: AppColors.cpuColor,
                            subtitle: hw.cpu.map { "\($0.coreCount) cores" }
                        )
                        .frame(maxWidth: .infinity)

                        GaugeCard(
                            label: "RAM",
                            value: (hw.memory?.usedPercent ?? 0) * 100,
                            color: AppColors.ramColor,
                            subtitle: hw.memory.map { SharedFormatters.formatBytesU($0.usedBytes) }
                        )
                        .frame(maxWidth: .infinity)

                        let batLevel = Double(hw.battery?.levelPercent ?? 0)
                        GaugeCard(
                            label: "Battery",
                            value: max(batLevel, 0),
                            color: batLevel <= 20 ? AppColors.critical : AppColors.batteryColor,
                            subtitle: hw.battery?.status
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .padding(.horizontal, 16)

                    // Health score card
                    HealthScoreCard(hw: hw)
                        .padding(.horizontal, 16)

                    // Network quick card
                    if let net = hw.network {
                        NetworkQuickCard(net: net)
                            .padding(.horizontal, 16)
                    }

                    // Storage card
                    if let storage = hw.storage {
                        MetricCard(title: "Storage", icon: "internaldrive", color: AppColors.storageColor) {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(SharedFormatters.formatBytes(storage.usedBytes)) used of \(SharedFormatters.formatBytes(storage.totalBytes))")
                                        .font(.subheadline)
                                        .foregroundStyle(AppColors.textSecondary)
                                    Text("\(SharedFormatters.formatBytes(storage.freeBytes)) free")
                                        .font(.caption)
                                        .foregroundStyle(AppColors.textMuted)
                                }
                                Spacer()
                                Text("\(Int(storage.usedPercent * 100))%")
                                    .font(.title3.weight(.bold))
                                    .foregroundStyle(storage.usedPercent > 0.9 ? AppColors.critical : AppColors.storageColor)
                            }
                            LiveBar(
                                fraction: storage.usedPercent,
                                color: storage.usedPercent > 0.9 ? AppColors.critical : AppColors.storageColor
                            )
                        }
                        .padding(.horizontal, 16)
                    }

                    // Thermal state
                    if let cpu = hw.cpu {
                        MetricCard(title: "Thermal", icon: thermalIcon(cpu.thermalState), color: thermalColor(cpu.thermalState)) {
                            HStack {
                                Text(cpu.thermalState)
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(thermalColor(cpu.thermalState))
                                Spacer()
                                Image(systemName: thermalIcon(cpu.thermalState))
                                    .font(.title2)
                                    .foregroundStyle(thermalColor(cpu.thermalState))
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.vertical, 14)
            }
            .background(AppColors.background)
            .navigationTitle(hw.device.map { $0.model } ?? "Dashboard")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: hw.refresh) {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .overlay {
                if hw.isLoading && hw.storage == nil {
                    ProgressView("Loading…")
                        .padding(24)
                        .background(AppColors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }

    private func thermalColor(_ state: String) -> Color {
        switch state {
        case "Normal": return AppColors.success
        case "Fair": return AppColors.warning
        case "Serious": return AppColors.thermalColor
        case "Critical": return AppColors.critical
        default: return AppColors.textMuted
        }
    }

    private func thermalIcon(_ state: String) -> String {
        switch state {
        case "Normal": return "thermometer.low"
        case "Fair": return "thermometer.medium"
        case "Serious", "Critical": return "thermometer.high"
        default: return "thermometer"
        }
    }
}

// MARK: - HealthScoreCard

private struct HealthScoreCard: View {
    let hw: HardwareService

    private var computed: (overall: Int, label: String, color: Color, battery: Int, thermal: String, storage: Int) {
        var scores: [Int] = []

        if let bat = hw.battery, bat.levelPercent >= 0 {
            let s: Int
            if bat.levelPercent <= 10 { s = 20 }
            else if bat.levelPercent <= 25 { s = 50 }
            else if bat.levelPercent <= 50 { s = 75 }
            else { s = 100 }
            scores.append(bat.isLowPowerMode ? min(s, 50) : s)
        }
        if let mem = hw.memory {
            scores.append(max(0, Int((1 - mem.usedPercent) * 100)))
        }
        if let storage = hw.storage {
            let s: Int
            if storage.usedPercent >= 0.95 { s = 10 }
            else if storage.usedPercent >= 0.85 { s = 50 }
            else if storage.usedPercent >= 0.7 { s = 75 }
            else { s = 100 }
            scores.append(s)
        }
        if let cpu = hw.cpu {
            switch cpu.thermalState {
            case "Normal": scores.append(100)
            case "Fair": scores.append(70)
            case "Serious": scores.append(40)
            case "Critical": scores.append(10)
            default: scores.append(70)
            }
        }

        let overall = scores.isEmpty ? 0 : scores.reduce(0, +) / scores.count
        let (label, color): (String, Color)
        if overall >= 80 { (label, color) = ("Excellent", AppColors.success) }
        else if overall >= 60 { (label, color) = ("Good", AppColors.success) }
        else if overall >= 40 { (label, color) = ("Fair", AppColors.warning) }
        else { (label, color) = ("Needs Attention", AppColors.critical) }

        let batScore = hw.battery.map { $0.levelPercent >= 0 ? $0.levelPercent : 0 } ?? 0
        let storageScore = hw.storage.map { Int((1 - $0.usedPercent) * 100) } ?? 100
        let thermalLabel = hw.cpu?.thermalState ?? "N/A"

        return (overall, label, color, batScore, thermalLabel, storageScore)
    }

    var body: some View {
        let s = computed
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Health Score")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(AppColors.textMuted)
                    Text(s.label)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(AppColors.textPrimary)
                }
                Spacer()
                HealthScoreRingView(score: s.overall, color: s.color)
            }

            HStack(spacing: 0) {
                SubScore(label: "Battery", value: "\(s.battery)%",
                         color: s.battery <= 25 ? AppColors.warning : AppColors.success)
                Spacer()
                SubScore(label: "Thermal", value: s.thermal,
                         color: s.thermal == "Normal" ? AppColors.success : s.thermal == "Fair" ? AppColors.warning : AppColors.critical)
                Spacer()
                SubScore(label: "Storage", value: "\(s.storage)%",
                         color: s.storage <= 15 ? AppColors.warning : AppColors.success)
            }
        }
        .padding(14)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppRadius.l))
        .overlay(RoundedRectangle(cornerRadius: AppRadius.l).stroke(AppColors.border, lineWidth: 1))
    }
}

private struct SubScore: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.caption.weight(.semibold))
                .foregroundStyle(color)
            Text(label)
                .font(.caption2)
                .foregroundStyle(AppColors.textMuted)
        }
    }
}

private struct HealthScoreRingView: View {
    let score: Int
    let color: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.15), lineWidth: 7)
                .frame(width: 72, height: 72)
            Circle()
                .trim(from: 0, to: CGFloat(score) / 100)
                .stroke(color, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .frame(width: 72, height: 72)
                .animation(.easeInOut(duration: 0.8), value: score)
            VStack(spacing: 0) {
                Text("\(score)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(color)
                Text("/100")
                    .font(.system(size: 9))
                    .foregroundStyle(AppColors.textMuted)
            }
        }
    }
}

// MARK: - NetworkQuickCard

private struct NetworkQuickCard: View {
    let net: NetworkData

    private var icon: String {
        if !net.isConnected { return "wifi.slash" }
        if net.connectionType == "Wi-Fi" { return "wifi" }
        if net.connectionType == "Cellular" { return "antenna.radiowaves.left.and.right" }
        return "network"
    }

    var body: some View {
        HStack(spacing: 12) {
            let tint = net.isConnected ? AppColors.networkColor : AppColors.textMuted
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 32, height: 32)
                .background(tint.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text(net.connectionType.isEmpty ? "Network" : net.connectionType)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppColors.textPrimary)
                Text(net.isConnected ? (net.ipv4 ?? "Connected") : "No internet")
                    .font(.caption)
                    .foregroundStyle(AppColors.textMuted)
                    .lineLimit(1)
            }
            Spacer()
            Circle()
                .fill(net.isConnected ? AppColors.success : AppColors.critical)
                .frame(width: 8, height: 8)
        }
        .padding(12)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppRadius.m))
        .overlay(RoundedRectangle(cornerRadius: AppRadius.m).stroke(AppColors.border, lineWidth: 1))
    }
}
