import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authState: AuthState
    @AppStorage("reducedMotion") private var reducedMotion = false
    @AppStorage("hapticFeedback") private var hapticFeedback = true

    var body: some View {
        NavigationStack {
            List {
                // Profile section
                if let user = authState.user {
                    Section {
                        HStack(spacing: 14) {
                            AsyncImage(url: user.photoURL) { image in
                                image.resizable().scaledToFill()
                            } placeholder: {
                                Circle().fill(AppColors.accent.opacity(0.2))
                                    .overlay(Text(user.displayName.prefix(1)).font(.title2.weight(.bold)).foregroundStyle(AppColors.accent))
                            }
                            .frame(width: 52, height: 52)
                            .clipShape(Circle())

                            VStack(alignment: .leading, spacing: 3) {
                                Text(user.displayName)
                                    .font(.headline)
                                Text(user.email)
                                    .font(.subheadline)
                                    .foregroundStyle(AppColors.textSecondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                Section("Preferences") {
                    Toggle("Reduced Motion", isOn: $reducedMotion)
                    Toggle("Haptic Feedback", isOn: $hapticFeedback)
                }

                Section("Privacy") {
                    HStack {
                        Text("Data Storage")
                        Spacer()
                        Text("On-Device Only")
                            .foregroundStyle(AppColors.textSecondary)
                    }
                    HStack {
                        Text("Analytics")
                        Spacer()
                        Text("None")
                            .foregroundStyle(AppColors.textSecondary)
                    }
                }

                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0")
                            .foregroundStyle(AppColors.textSecondary)
                    }
                    HStack {
                        Text("Platform")
                        Spacer()
                        Text("iOS")
                            .foregroundStyle(AppColors.textSecondary)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        authState.signOut()
                    } label: {
                        HStack {
                            Spacer()
                            Text("Sign Out")
                                .foregroundStyle(.red)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}
