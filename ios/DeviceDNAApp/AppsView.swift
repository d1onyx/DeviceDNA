import SwiftUI

struct AppsView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "lock.shield")
                    .font(.system(size: 52))
                    .foregroundStyle(AppColors.accent)
                    .padding(.top, 60)

                Text("App List Restricted")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(AppColors.textPrimary)

                Text("iOS does not allow apps to enumerate all installed applications for privacy and security reasons. Only apps that have declared specific URL schemes can be detected.")
                    .font(.body)
                    .foregroundStyle(AppColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Divider().padding(.horizontal, 32)

                VStack(spacing: 12) {
                    InfoRow(label: "Total User Apps", value: "Restricted by iOS")
                    InfoRow(label: "System Apps", value: "Restricted by iOS")
                    InfoRow(label: "Running Processes", value: "Restricted by iOS")
                }
                .padding(16)
                .background(AppColors.surface)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 20)

                Spacer()
            }
            .background(AppColors.background)
            .navigationTitle("Apps")
            .navigationBarTitleDisplayMode(.large)
        }
    }
}
