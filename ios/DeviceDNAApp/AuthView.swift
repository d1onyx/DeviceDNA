import SwiftUI

struct AuthView: View {
    @EnvironmentObject var authState: AuthState

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("DeviceDNA")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(AppColors.textPrimary)
                    Spacer()
                    HStack(spacing: 6) {
                        Image(systemName: authState.isConfigured ? "checkmark.shield" : "exclamationmark.shield")
                            .font(.caption)
                            .foregroundStyle(AppColors.accent)
                        Text(authState.isConfigured ? "Firebase ready" : "Setup needed")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(AppColors.textSecondary)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(AppColors.surfaceElevated)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(AppColors.border, lineWidth: 1))
                }
                .padding(.horizontal, 22)
                .padding(.top, 28)

                Spacer().frame(height: 36)

                // Hero graphic
                HeroConsole()
                    .padding(.horizontal, 22)

                Spacer().frame(height: 36)

                // Title
                VStack(spacing: 12) {
                    Text("Understand Your\nDevice Inside Out")
                        .font(.largeTitle.weight(.bold))
                        .multilineTextAlignment(.center)
                        .foregroundStyle(AppColors.textPrimary)

                    Text("Deep hardware diagnostics, battery health, thermal monitoring, and device integrity checks — all in one place.")
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(AppColors.textSecondary)
                        .padding(.horizontal, 8)
                }
                .padding(.horizontal, 22)

                Spacer().frame(height: 28)

                // Value props
                HStack(spacing: 10) {
                    ValueProp(icon: "lock.fill", label: "Privacy\nFirst")
                    ValueProp(icon: "icloud.and.arrow.up", label: "Cloud\nSync")
                    ValueProp(icon: "cpu", label: "Deep\nHardware")
                }
                .padding(.horizontal, 22)

                Spacer().frame(height: 40)

                // Sign-in section
                VStack(spacing: 16) {
                    if let error = authState.errorMessage {
                        Text(error)
                            .font(.subheadline)
                            .foregroundStyle(AppColors.warning)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 22)
                    }

                    Button(action: authState.signInWithGoogle) {
                        HStack(spacing: 10) {
                            if authState.isLoading {
                                ProgressView()
                                    .tint(AppColors.textMuted)
                            } else {
                                ZStack {
                                    Circle()
                                        .fill(.white)
                                        .frame(width: 24, height: 24)
                                    Text("G")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundStyle(Color(red: 0.1, green: 0.45, blue: 0.91))
                                }
                                Text("Continue with Google")
                                    .font(.headline)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(AppColors.textPrimary)
                        .foregroundStyle(AppColors.background)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(authState.isLoading)
                    .padding(.horizontal, 22)

                    Text("By continuing, you agree to our Terms of Service and Privacy Policy. Your data stays on your device.")
                        .font(.caption)
                        .foregroundStyle(AppColors.textMuted)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 22)
                }
                .padding(.bottom, 40)
            }
        }
        .background(AppColors.background.ignoresSafeArea())
    }
}

private struct HeroConsole: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 30)
            .fill(Color(UIColor.tertiarySystemBackground))
            .overlay(
                RoundedRectangle(cornerRadius: 30)
                    .stroke(AppColors.border, lineWidth: 1)
            )
            .overlay(
                VStack(spacing: 20) {
                    HStack {
                        PulseDot(color: AppColors.success)
                        Spacer()
                        PulseDot(color: AppColors.accent)
                        Spacer()
                        PulseDot(color: AppColors.warning)
                    }

                    ZStack {
                        Circle()
                            .fill(Color(UIColor.secondarySystemBackground))
                            .frame(width: 100, height: 100)
                            .overlay(Circle().stroke(AppColors.border, lineWidth: 1))
                        Circle()
                            .fill(AppColors.accent)
                            .frame(width: 64, height: 64)
                        Image(systemName: "cpu")
                            .font(.system(size: 30))
                            .foregroundStyle(.white)
                    }

                    VStack(spacing: 10) {
                        SignalBar(fraction: 0.9)
                        SignalBar(fraction: 0.65)
                        SignalBar(fraction: 0.78)
                    }
                }
                .padding(24)
            )
            .frame(height: 220)
    }
}

private struct PulseDot: View {
    let color: Color
    var body: some View {
        ZStack {
            Circle().fill(color.opacity(0.14)).frame(width: 40, height: 40)
            Circle().fill(color).frame(width: 12, height: 12)
        }
    }
}

private struct SignalBar: View {
    let fraction: Double
    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(AppColors.accent.opacity(0.1)).frame(height: 8)
                Capsule().fill(AppColors.accent).frame(width: geo.size.width * fraction, height: 8)
            }
        }
        .frame(height: 8)
    }
}

private struct ValueProp: View {
    let icon: String
    let label: String
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 18))
                .foregroundStyle(AppColors.accent)
            Text(label)
                .font(.caption.weight(.medium))
                .foregroundStyle(AppColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(12)
        .background(Color(UIColor.tertiarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(AppColors.border, lineWidth: 1))
    }
}
