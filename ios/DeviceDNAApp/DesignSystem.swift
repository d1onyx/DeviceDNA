import SwiftUI

// MARK: - Design Tokens
// Values MUST match shared/src/commonMain/.../core/design/DesignTokens.kt exactly.
// DesignTokens.kt is the single source of truth; update both files together.

enum AppColors {
    // ── Dark theme (default) ───────────────────────────────────────────────
    static let accent   = Color(argb: 0xFFA7C7FF)
    static let success  = Color(argb: 0xFF22C55E)
    static let warning  = Color(argb: 0xFFF59E0B)
    static let critical = Color(argb: 0xFFEF4444)
    static let info     = Color(argb: 0xFF38BDF8)

    // Section colors — dark theme
    static let cpuColor      = Color(argb: 0xFF60A5FA)
    static let batteryColor  = Color(argb: 0xFF22C55E)
    static let thermalColor  = Color(argb: 0xFFF97316)
    static let ramColor      = Color(argb: 0xFFA78BFA)
    static let storageColor  = Color(argb: 0xFFF59E0B)
    static let displayColor  = Color(argb: 0xFF38BDF8)
    static let cameraColor   = Color(argb: 0xFFEC4899)
    static let networkColor  = Color(argb: 0xFF06B6D4)
    static let sensorsColor  = Color(argb: 0xFF10B981)
    static let integrityColor = Color(argb: 0xFF8B5CF6)

    // Semantic surfaces (system-adaptive)
    static let background      = Color(UIColor.systemBackground)
    static let surface         = Color(UIColor.secondarySystemBackground)
    static let surfaceElevated = Color(UIColor.tertiarySystemBackground)
    static let textPrimary     = Color(UIColor.label)
    static let textSecondary   = Color(UIColor.secondaryLabel)
    static let textMuted       = Color(UIColor.tertiaryLabel)
    static let border          = Color(UIColor.separator)
}

// Spacing tokens — match DesignTokens.kt spacingXs/S/M/L/Xl
enum AppSpacing {
    static let xs: CGFloat = 4
    static let s:  CGFloat = 8
    static let m:  CGFloat = 16
    static let l:  CGFloat = 24
    static let xl: CGFloat = 32
}

// Corner radius tokens — match DesignTokens.kt radiusS/M/L/Xl/Pill
enum AppRadius {
    static let s:    CGFloat = 8
    static let m:    CGFloat = 12
    static let l:    CGFloat = 16
    static let xl:   CGFloat = 24
    static let pill: CGFloat = 99
}

// MARK: - Color(argb:) helper

private extension Color {
    /// Create a Color from a packed ARGB Long (0xAARRGGBB), matching DesignTokens format.
    init(argb: UInt64) {
        let r = Double((argb >> 16) & 0xFF) / 255.0
        let g = Double((argb >> 8)  & 0xFF) / 255.0
        let b = Double(argb & 0xFF)         / 255.0
        let a = Double((argb >> 24) & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, opacity: a)
    }
}

// MARK: - Reusable components

struct MetricCard<Content: View>: View {
    let title: String
    let icon: String
    let color: Color
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.s + 2) {
            HStack(spacing: AppSpacing.s) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(color)
                    .frame(width: 28, height: 28)
                    .background(color.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: AppRadius.s))
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppColors.textPrimary)
            }
            content()
        }
        .padding(AppSpacing.m - 2)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppRadius.l))
    }
}

struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(AppColors.textSecondary)
            Spacer()
            Text(value)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(AppColors.textPrimary)
                .multilineTextAlignment(.trailing)
        }
    }
}

struct GaugeCard: View {
    let label: String
    let value: Double   // 0–100
    let color: Color
    let subtitle: String?

    var body: some View {
        VStack(spacing: AppSpacing.xs + 2) {
            ZStack {
                Circle()
                    .stroke(color.opacity(0.15), lineWidth: 8)
                Circle()
                    .trim(from: 0, to: min(value / 100, 1))
                    .stroke(color, style: StrokeStyle(lineWidth: 8, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 0.5), value: value)
                VStack(spacing: 1) {
                    Text("\(Int(value))%")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(AppColors.textPrimary)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 9))
                            .foregroundStyle(AppColors.textMuted)
                            .lineLimit(1)
                    }
                }
            }
            .frame(width: 80, height: 80)
            Text(label)
                .font(.caption.weight(.medium))
                .foregroundStyle(AppColors.textSecondary)
        }
    }
}

struct LiveBar: View {
    let fraction: Double
    let color: Color
    var height: CGFloat = 6

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(color.opacity(0.15)).frame(height: height)
                Capsule().fill(color)
                    .frame(width: geo.size.width * min(max(fraction, 0), 1), height: height)
                    .animation(.easeInOut(duration: 0.4), value: fraction)
            }
        }
        .frame(height: height)
    }
}

// MARK: - InsightCard

struct InsightCard: View {
    enum Severity { case critical, warning, info, good }

    let title: String
    let summary: String
    let severity: Severity

    private var color: Color {
        switch severity {
        case .critical: return AppColors.critical
        case .warning: return AppColors.warning
        case .info: return AppColors.info
        case .good: return AppColors.success
        }
    }

    private var icon: String {
        switch severity {
        case .critical, .warning: return "exclamationmark.triangle"
        case .info: return "info.circle"
        case .good: return "checkmark.circle"
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(color)
                .padding(.top, 1)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppColors.textPrimary)
                Text(summary)
                    .font(.caption)
                    .foregroundStyle(AppColors.textSecondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(color.opacity(0.07))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(color.opacity(0.2), lineWidth: 1))
    }
}

// MARK: - ErrorBanner

struct ErrorBanner: View {
    let message: String
    let onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(AppColors.warning)
            Text(message)
                .font(.caption)
                .foregroundStyle(AppColors.textSecondary)
            Spacer()
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(AppColors.textMuted)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(AppColors.warning.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(AppColors.warning.opacity(0.3), lineWidth: 1))
    }
}

