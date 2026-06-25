import SwiftUI

struct RootView: View {
    @EnvironmentObject var authState: AuthState

    var body: some View {
        if authState.isSignedIn {
            MainTabView()
        } else {
            AuthView()
        }
    }
}

struct MainTabView: View {
    @StateObject private var hw = HardwareService()

    var body: some View {
        TabView {
            DashboardView()
                .tabItem { Label("Dashboard", systemImage: "square.grid.2x2") }
                .environmentObject(hw)

            HardwareView()
                .tabItem { Label("Hardware", systemImage: "cpu") }
                .environmentObject(hw)

            SystemView()
                .tabItem { Label("System", systemImage: "gearshape.2") }
                .environmentObject(hw)

            DiagnosticsView()
                .tabItem { Label("Tests", systemImage: "checklist.checked") }

            SettingsView()
                .tabItem { Label("Settings", systemImage: "slider.horizontal.3") }
        }
        .accentColor(AppColors.accent)
        .onAppear { hw.loadAll() }
    }
}
