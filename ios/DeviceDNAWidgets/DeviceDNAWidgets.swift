import WidgetKit
import SwiftUI

// MARK: - Shared payload (contract with shared/iosMain IosWidgetBridge.kt — field names must match)

struct WidgetPayload: Codable {
    var isPremium = false
    var hasData = false
    var hasBatteryData = false
    var hasMemoryData = false
    var hasStorageData = false
    var hasHealthData = false
    var lastUpdatedMillis: Int64 = 0
    var batteryLevel = -1
    var batteryStatus = ""
    var batteryCharging = false
    var ramUsedPercent: Float = 0
    var storageUsedPercent: Float = 0
    var healthOverall = -1
    var healthInsight = ""
    var healthSeverity = ""
    var thermalStatus = -1
    var isRooted = false
    var fraudLevel = ""
}

enum WidgetStore {
    static let appGroupId = widgetConfig("AppGroupId") ?? ""
    static let deepLinkScheme = widgetConfig("DeepLinkScheme") ?? "devicedna"
    static let payloadKey = "widget_snapshot_v1"

    static func load() -> WidgetPayload {
        guard let defaults = UserDefaults(suiteName: appGroupId),
              let raw = defaults.string(forKey: payloadKey),
              let data = raw.data(using: .utf8),
              let payload = try? JSONDecoder().decode(WidgetPayload.self, from: data)
        else { return WidgetPayload() }
        return payload
    }
}

private func widgetConfig(_ key: String) -> String? {
    let trimmed = (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
        .trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.flatMap { value in
        if value.isEmpty || value.hasPrefix("$(") || value.contains("YOUR_") {
            return nil
        }
        return value
    }
}

private func widgetDeepLink(_ route: String) -> URL? {
    URL(string: "\(WidgetStore.deepLinkScheme)://open/\(route)")
}

// MARK: - Timeline

struct SnapshotEntry: TimelineEntry {
    let date: Date
    let payload: WidgetPayload
}

struct SnapshotProvider: TimelineProvider {
    func placeholder(in context: Context) -> SnapshotEntry {
        SnapshotEntry(date: .now, payload: previewPayload)
    }

    func getSnapshot(in context: Context, completion: @escaping (SnapshotEntry) -> Void) {
        completion(SnapshotEntry(date: .now, payload: context.isPreview ? previewPayload : WidgetStore.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SnapshotEntry>) -> Void) {
        let entry = SnapshotEntry(date: .now, payload: WidgetStore.load())
        // The app refreshes the payload on background windows; ask WidgetKit to re-read
        // in 30 minutes as a fallback so stale data eventually re-renders.
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: .now) ?? .now
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private var previewPayload: WidgetPayload {
        var p = WidgetPayload()
        p.isPremium = true
        p.hasData = true
        p.hasBatteryData = true
        p.hasMemoryData = true
        p.hasStorageData = true
        p.hasHealthData = true
        p.batteryLevel = 82
        p.batteryCharging = true
        p.ramUsedPercent = 0.46
        p.storageUsedPercent = 0.63
        p.healthOverall = 91
        p.healthSeverity = "Good"
        return p
    }
}

// MARK: - Palette (mirrors shared DesignTokens dark theme)

enum WidgetPalette {
    static let background = Color(red: 0.035, green: 0.035, blue: 0.043)
    static let surface = Color(red: 0.067, green: 0.067, blue: 0.075)
    static let textPrimary = Color(red: 0.98, green: 0.98, blue: 0.98)
    static let textMuted = Color(red: 0.44, green: 0.44, blue: 0.48)
    static let battery = Color(red: 0.13, green: 0.77, blue: 0.37)
    static let ram = Color(red: 0.65, green: 0.55, blue: 0.98)
    static let storage = Color(red: 0.96, green: 0.62, blue: 0.04)
    static let critical = Color(red: 0.94, green: 0.27, blue: 0.27)
    static let accent = Color(red: 0.65, green: 0.78, blue: 1.0)
}

struct StatusWidgetView: View {
    let title: String
    let message: String
    let symbol: String
    let tint: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.title3.weight(.semibold))
                .foregroundStyle(tint)
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(WidgetPalette.textPrimary)
                .multilineTextAlignment(.center)
            Text(message)
                .font(.caption2)
                .foregroundStyle(WidgetPalette.textMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(2)
    }
}

private func widgetDestinationURL(for payload: WidgetPayload, route: String) -> URL? {
    widgetDeepLink(payload.isPremium ? route : "subscription")
}

// MARK: - Battery widget

struct BatteryWidgetView: View {
    let entry: SnapshotEntry

    var body: some View {
        let p = entry.payload
        Group {
            if !p.isPremium {
                StatusWidgetView(title: "Battery", message: "Unlock with Premium", symbol: "lock.fill", tint: WidgetPalette.accent)
            } else if !p.hasBatteryData {
                StatusWidgetView(title: "Battery", message: "Updating...", symbol: "arrow.clockwise", tint: WidgetPalette.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: p.batteryCharging ? "battery.100.bolt" : "battery.75")
                            .foregroundStyle(WidgetPalette.battery)
                        Spacer()
                        if p.batteryCharging {
                            Text("Charging")
                                .font(.caption2)
                                .foregroundStyle(WidgetPalette.textMuted)
                        }
                    }
                    Spacer()
                    Text(p.batteryLevel >= 0 ? "\(p.batteryLevel)%" : "—")
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundStyle(WidgetPalette.textPrimary)
                    Gauge(value: Double(max(p.batteryLevel, 0)), in: 0...100) { EmptyView() }
                        .gaugeStyle(.accessoryLinearCapacity)
                        .tint(p.batteryLevel <= 20 ? WidgetPalette.critical : WidgetPalette.battery)
                    Text("Battery")
                        .font(.caption2)
                        .foregroundStyle(WidgetPalette.textMuted)
                }
            }
        }
        .padding(2)
        .widgetURL(widgetDestinationURL(for: p, route: "hardware/battery"))
        .containerBackground(WidgetPalette.background, for: .widget)
    }
}

struct BatteryWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DeviceDNABattery", provider: SnapshotProvider()) { entry in
            BatteryWidgetView(entry: entry)
        }
        .configurationDisplayName("Battery")
        .description("Battery level and charging state.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Device health widget

struct HealthWidgetView: View {
    let entry: SnapshotEntry

    var body: some View {
        let p = entry.payload
        let score = p.healthOverall
        let tint: Color = score >= 70 ? WidgetPalette.battery : (score >= 40 ? WidgetPalette.storage : WidgetPalette.critical)
        Group {
            if !p.isPremium {
                StatusWidgetView(title: "Device Health", message: "Unlock with Premium", symbol: "lock.fill", tint: WidgetPalette.accent)
            } else if !p.hasHealthData {
                StatusWidgetView(title: "Device Health", message: "Updating...", symbol: "arrow.clockwise", tint: WidgetPalette.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Image(systemName: "heart.text.square")
                            .foregroundStyle(WidgetPalette.accent)
                        Text("Device Health")
                            .font(.caption)
                            .foregroundStyle(WidgetPalette.textMuted)
                        Spacer()
                    }
                    Spacer()
                    HStack(alignment: .lastTextBaseline, spacing: 4) {
                        Text(score >= 0 ? "\(score)" : "—")
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                            .foregroundStyle(tint)
                        Text("/100")
                            .font(.caption)
                            .foregroundStyle(WidgetPalette.textMuted)
                    }
                    if !p.healthInsight.isEmpty {
                        Text(p.healthInsight)
                            .font(.caption2)
                            .lineLimit(2)
                            .foregroundStyle(WidgetPalette.textMuted)
                    }
                }
            }
        }
        .padding(2)
        .widgetURL(widgetDestinationURL(for: p, route: "system"))
        .containerBackground(WidgetPalette.background, for: .widget)
    }
}

struct HealthWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DeviceDNAHealth", provider: SnapshotProvider()) { entry in
            HealthWidgetView(entry: entry)
        }
        .configurationDisplayName("Device Health")
        .description("Overall health score with the top insight.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Memory & storage widget

struct MemoryWidgetView: View {
    let entry: SnapshotEntry

    var body: some View {
        let p = entry.payload
        Group {
            if !p.isPremium {
                StatusWidgetView(title: "Memory & Storage", message: "Unlock with Premium", symbol: "lock.fill", tint: WidgetPalette.accent)
            } else if !p.hasMemoryData || !p.hasStorageData {
                StatusWidgetView(title: "Memory & Storage", message: "Updating...", symbol: "arrow.clockwise", tint: WidgetPalette.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 10) {
                    meter(
                        title: "Memory",
                        fraction: Double(p.ramUsedPercent),
                        tint: WidgetPalette.ram,
                        symbol: "memorychip"
                    )
                    meter(
                        title: "Storage",
                        fraction: Double(p.storageUsedPercent),
                        tint: WidgetPalette.storage,
                        symbol: "internaldrive"
                    )
                }
            }
        }
        .padding(2)
        .widgetURL(widgetDestinationURL(for: p, route: "dashboard"))
        .containerBackground(WidgetPalette.background, for: .widget)
    }

    @ViewBuilder
    private func meter(title: String, fraction: Double, tint: Color, symbol: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: symbol).font(.caption).foregroundStyle(tint)
                Text(title).font(.caption).foregroundStyle(WidgetPalette.textMuted)
                Spacer()
                Text("\(Int(fraction * 100))%")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(WidgetPalette.textPrimary)
            }
            Gauge(value: min(max(fraction, 0), 1)) { EmptyView() }
                .gaugeStyle(.accessoryLinearCapacity)
                .tint(fraction > 0.9 ? WidgetPalette.critical : tint)
        }
    }
}

struct MemoryWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DeviceDNAMemory", provider: SnapshotProvider()) { entry in
            MemoryWidgetView(entry: entry)
        }
        .configurationDisplayName("Memory & Storage")
        .description("Live RAM and storage pressure.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Bundle

@main
struct DeviceDNAWidgetBundle: WidgetBundle {
    var body: some Widget {
        BatteryWidget()
        HealthWidget()
        MemoryWidget()
    }
}
