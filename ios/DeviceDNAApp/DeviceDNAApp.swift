import SwiftUI
import UIKit
import BackgroundTasks
import UserNotifications
import FirebaseCore
import GoogleSignIn
import WidgetKit
import shared   // Kotlin Multiplatform framework (baseName "shared")

// MARK: - App entry

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    static let backgroundTaskId = "com.devstdvad.devicedna.refresh"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 1. Firebase (Google Sign-In backend).
        if FirebaseApp.app() == nil,
           Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
        }

        // 2. Koin — must be up before the first Compose frame resolves ViewModels.
        KoinBridge.shared.start(deps: IosAppDependencies(
            authGateway: AuthBridge.shared.gateway,
            billingGateway: StoreKitBilling.shared.gateway,
            syncBaseUrl: AppConfig.syncBaseUrl,
            reloadWidgetTimelines: { WidgetCenter.shared.reloadAllTimelines() }
        ))
        AuthBridge.shared.startListening()
        StoreKitBilling.shared.start()

        // 3. Background refresh — registration MUST happen before launch finishes.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.backgroundTaskId,
            using: nil
        ) { task in
            guard let refresh = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            Self.scheduleBackgroundRefresh()   // chain the next window
            refresh.expirationHandler = { refresh.setTaskCompleted(success: false) }
            KoinBridge.shared.backgroundWorker().run { success in
                refresh.setTaskCompleted(success: success.boolValue)
            }
        }

        // 4. Notification taps → deep links into the shared Compose shell.
        UNUserNotificationCenter.current().delegate = self

        // 5. Ads (AdMob + UMP consent + ATT). Started lazily inside AdsHost so the
        //    consent flow presents after the first frame, not during launch.
        AdsHost.shared.startWhenReady()

        return true
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
        // Widget taps arrive as devicedna://open/<route> URLs.
        if url.scheme == "devicedna" {
            let route = url.absoluteString
                .replacingOccurrences(of: "devicedna://open/", with: "")
                .trimmingCharacters(in: CharacterSet(charactersIn: "/"))
            if !route.isEmpty { DeepLinkHolder.shared.push(route: route) }
            return true
        }
        return GIDSignIn.sharedInstance.handle(url)
    }

    static func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: backgroundTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    // MARK: UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let route = response.notification.request.content.userInfo["route"] as? String {
            DeepLinkHolder.shared.push(route: route)
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

// MARK: - Configuration

enum AppConfig {
    /// Cloudflare Worker sync backend (no trailing slash). Override per build configuration.
    static let syncBaseUrl = "https://devicedna-sync.workers.dev"
}

// MARK: - SwiftUI scene

@main
struct DeviceDNAApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea(.all)
                .ignoresSafeArea(.keyboard)
                .onOpenURL { url in
                    // SwiftUI lifecycle delivers widget URLs here as well.
                    _ = delegate.application(UIApplication.shared, open: url)
                }
        }
        .onChange(of: scenePhase) { phase in
            if phase == .background {
                AppDelegate.scheduleBackgroundRefresh()
                // Refresh widgets with the freshest foreground data before suspending.
                KoinBridge.shared.backgroundWorker().run { _ in }
            }
        }
    }
}

// MARK: - Compose host

/// Hosts the shared Compose Multiplatform UI — the ONLY iOS UI surface. The whole screen
/// graph lives in Kotlin (`shared` module → App()).
struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            onGoogleSignIn: { forceAccountPicker in
                AuthBridge.shared.signIn(forceAccountPicker: forceAccountPicker.boolValue)
            },
            interstitial: AdsHost.shared.interstitial,
            bannerViewFactory: { AdsHost.shared.makeBannerView() }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
