// SharedBridge.swift
//
// Swift mirror of shared KMM business logic.
// Logic MUST stay identical to the Kotlin implementations in shared/commonMain:
//   • Formatters.kt  → SharedFormatters
//   • HealthAnalyzer.kt (scoring rules only — model types handled separately)
//
// FRAMEWORK UPGRADE PATH
// When the shared XCFramework is compiled (./gradlew :shared:embedAndSignAppleFrameworkForXcode
// on macOS), replace each method body with:
//   return shared.Formatters.shared.formatBytes(bytes: bytes)
// and delete the Swift fallback implementation below.

import Foundation

// MARK: - Formatters
// Swift implementation of shared/commonMain/.../core/common/Formatters.kt

enum SharedFormatters {

    static func formatBytes(_ bytes: Int64) -> String {
        if bytes < 0 { return "Unknown" }
        let gb = Double(bytes) / (1024.0 * 1024.0 * 1024.0)
        if gb >= 1.0 { return String(format: "%.1f GB", gb) }
        let mb = Double(bytes) / (1024.0 * 1024.0)
        if mb >= 1.0 { return "\(Int(mb)) MB" }
        let kb = Double(bytes) / 1024.0
        return "\(Int(kb)) KB"
    }

    static func formatBytesShort(_ bytes: Int64) -> String {
        if bytes < 0 { return "?" }
        let gb = Double(bytes) / (1024.0 * 1024.0 * 1024.0)
        if gb >= 1.0 { return String(format: "%.1fG", gb) }
        let mb = Double(bytes) / (1024.0 * 1024.0)
        return "\(Int(mb))M"
    }

    static func formatBytesU(_ bytes: UInt64) -> String {
        formatBytes(Int64(bitPattern: bytes))
    }

    static func formatPercent(fraction: Double) -> String {
        "\(Int(fraction * 100))%"
    }

    static func formatPercentInt(_ value: Int) -> String {
        "\(value)%"
    }

    static func formatFrequencyMhz(_ mhz: Int) -> String {
        if mhz <= 0 { return "Unknown" }
        if mhz >= 1000 { return String(format: "%.2f GHz", Double(mhz) / 1000.0) }
        return "\(mhz) MHz"
    }

    static func formatCelsius(_ celsius: Float) -> String {
        String(format: "%.1f°C", celsius)
    }

    static func formatFahrenheit(_ celsius: Float) -> String {
        let f = Double(celsius) * 9.0 / 5.0 + 32.0
        return String(format: "%.1f°F", f)
    }

    static func formatUptimeSec(_ seconds: TimeInterval) -> String {
        let totalSec = Int(seconds)
        let h = totalSec / 3600
        let m = (totalSec % 3600) / 60
        let d = h / 24
        if d > 0 { return "\(d)d \(h % 24)h" }
        if h > 0 { return "\(h)h \(m)m" }
        return "\(m)m"
    }
}

// MARK: - Platform Info
// Swift mirror of shared/commonMain/.../platform/PlatformInfo.kt

enum SharedPlatformInfo {
    static var deviceModel: String {
        var sysinfo = utsname()
        uname(&sysinfo)
        return String(bytes: Data(bytes: &sysinfo.machine, count: Int(_SYS_NAMELEN)), encoding: .ascii)?
            .trimmingCharacters(in: .controlCharacters) ?? UIDevice.current.model
    }
    static var manufacturer: String { "Apple" }
    static var osName: String       { UIDevice.current.systemName }
    static var osVersion: String    { UIDevice.current.systemVersion }
    static var processorCount: Int  { ProcessInfo.processInfo.processorCount }
    static var totalMemoryBytes: UInt64 { ProcessInfo.processInfo.physicalMemory }
}

import UIKit
