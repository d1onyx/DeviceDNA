import SwiftUI
import UIKit
import BackgroundTasks
import UserNotifications
import CryptoKit
import FirebaseCore
import GoogleSignIn
import WidgetKit
import shared   // Kotlin Multiplatform framework (baseName "shared")

// MARK: - App entry

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    static let backgroundTaskId = AppConfig.backgroundTaskId

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 1. Firebase (Google Sign-In backend).
        if FirebaseApp.app() == nil,
           AppConfig.hasUsableFirebaseConfig {
            FirebaseApp.configure()
        }

        // 1b. GIDSignIn needs its own `configuration` set explicitly before the first
        //     signIn(withPresenting:) call — it does NOT infer the client ID from Firebase
        //     automatically. Missing this raises an NSException from GIDSignIn's own
        //     precondition check (same crash signature as a missing URL scheme).
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        }

        // 1c. Secondary "cfg-sync" Firebase app (no-op unless its plist is bundled).
        if FirebaseApp.app(name: AppConfig.cfgAppName) == nil,
           let cfgPath = AppConfig.cfgPlistPath,
           let cfgOptions = FirebaseOptions(contentsOfFile: cfgPath) {
            FirebaseApp.configure(name: AppConfig.cfgAppName, options: cfgOptions)
        }

        // 2. Koin — must be up before the first Compose frame resolves ViewModels.
        KoinBridge.shared.start(deps: IosAppDependencies(
            authGateway: AuthBridge.shared.gateway,
            billingGateway: StoreKitBilling.shared.gateway,
            useRealBilling: AppConfig.useRealBilling,
            syncBaseUrl: AppConfig.syncBaseUrl,
            appGroupId: AppConfig.appGroupId,
            reloadWidgetTimelines: { WidgetCenter.shared.reloadAllTimelines() },
            cfgEnabled: AppConfig.cfgEnabled,
            cfgDocPath: AppConfig.cfgDocPath,
            // Signature check via CryptoKit.
            verifySignature: { message, signature in
                KotlinBoolean(bool: AppConfig.verifyConfigSignature(
                    message: message.toData(),
                    signature: signature.toData()
                ))
            }
        ))
        AuthBridge.shared.startListening()
        // Only run the StoreKit entitlement bridge when real billing is the source of truth
        // (release/App Store). In dev-billing builds premium is granted locally by IosDevBillingGateway and
        // persisted in the entitlements store; StoreKit's launch sync would otherwise clear that dev
        // entitlement on every relaunch (premium wouldn't persist, unlike Android's local dev unlock).
        if KoinBridge.shared.usesRealBilling() {
            StoreKitBilling.shared.start()
        }

        // 2b. Populate the widget snapshot immediately on launch so a freshly-added
        //     widget doesn't sit on its placeholder until the next background
        //     transition or the next (opportunistic, iOS-scheduled) BGAppRefreshTask.
        KoinBridge.shared.backgroundWorker().run { _ in }

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
        // Widget taps arrive as <scheme>://open/<route> URLs.
        if url.scheme == AppConfig.deepLinkScheme {
            let route = url.absoluteString
                .replacingOccurrences(of: "\(AppConfig.deepLinkScheme)://open/", with: "")
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
    /// Cloudflare Worker sync backend (no trailing slash), injected via the
    /// SYNC_BASE_URL build setting (ios/project.yml) into Info.plist. Must match
    /// Android's `syncBaseUrl` (local.properties / -PsyncBaseUrl, see
    /// android/build.gradle.kts).
    static let syncBaseUrl: String = {
        guard let url = string("SyncBaseUrl") else {
            fatalError("Missing SyncBaseUrl in Info.plist — check SYNC_BASE_URL in ios/project.yml")
        }
        return url
    }()

    static let appGroupId: String = {
        guard let value = string("AppGroupId") else {
            fatalError("Missing AppGroupId in Info.plist — check IOS_APP_GROUP_ID in ios/project.yml")
        }
        return value
    }()

    static let backgroundTaskId: String = {
        guard let value = string("BackgroundTaskId") else {
            fatalError("Missing BackgroundTaskId in Info.plist — check IOS_BACKGROUND_TASK_ID in ios/project.yml")
        }
        return value
    }()

    static let deepLinkScheme: String = {
        guard let value = string("DeepLinkScheme") else {
            fatalError("Missing DeepLinkScheme in Info.plist — check IOS_DEEP_LINK_SCHEME in ios/project.yml")
        }
        return value
    }()

    static let storeKitPremiumProductId: String = {
        guard let value = string("StoreKitPremiumProductId") else {
            fatalError("Missing StoreKitPremiumProductId in Info.plist — check IOS_STOREKIT_PREMIUM_PRODUCT_ID in ios/project.yml")
        }
        return value
    }()

    /// An explicit build-setting override supports optimized sideload artifacts with dev billing.
    /// Without an override, the safe default is Debug = local unlock and Release = StoreKit.
    static let useRealBilling: Bool = {
        if let value = string("UseRealBilling")?.lowercased() {
            return value == "yes" || value == "true" || value == "1"
        }
        return string("AppBuildConfiguration")?.lowercased() == "release"
    }()

    static let adMobBannerAdUnitId: String = {
        guard let value = string("AdMobBannerAdUnitId") else {
            fatalError("Missing AdMobBannerAdUnitId in Info.plist — check IOS_ADMOB_BANNER_AD_UNIT_ID in ios/project.yml")
        }
        return value
    }()

    static let adMobInterstitialAdUnitId: String = {
        guard let value = string("AdMobInterstitialAdUnitId") else {
            fatalError("Missing AdMobInterstitialAdUnitId in Info.plist — check IOS_ADMOB_INTERSTITIAL_AD_UNIT_ID in ios/project.yml")
        }
        return value
    }()

    static let hasUsableFirebaseConfig: Bool = {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let plist = NSDictionary(contentsOfFile: path),
            let appId = plist["GOOGLE_APP_ID"] as? String,
            !appId.hasPrefix("YOUR_")
        else {
            return false
        }
        return true
    }()

    // MARK: Remote config

    /// Secondary Firebase app name (must match the Kotlin side).
    static let cfgAppName = "cfg-sync"

    /// Path to GoogleService-Info-CfgSync.plist, or nil when absent/placeholder.
    static let cfgPlistPath: String? = {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info-CfgSync", ofType: "plist"),
            let plist = NSDictionary(contentsOfFile: path),
            let appId = plist["GOOGLE_APP_ID"] as? String,
            !appId.hasPrefix("YOUR_")
        else {
            return nil
        }
        return path
    }()

    static let cfgDocPath: String = string("CfgDocPath") ?? "cfg/state"

    /// Raw 32-byte key from a base64 CfgPubKey in Info.plist, or nil.
    static let cfgPublicKey: Data? = {
        guard let b64 = string("CfgPubKey"),
              let data = Data(base64Encoded: b64) else { return nil }
        return data
    }()

    /// Active only when both the plist and the key are present.
    static let cfgEnabled: Bool = cfgPlistPath != nil && cfgPublicKey != nil

    /// Checks [signature] over [message] with the embedded key (CryptoKit).
    static func verifyConfigSignature(message: Data, signature: Data) -> Bool {
        guard let keyData = cfgPublicKey,
              let key = try? Curve25519.Signing.PublicKey(rawRepresentation: keyData) else {
            return false
        }
        return key.isValidSignature(signature, for: message)
    }

    private static func string(_ key: String) -> String? {
        let trimmed = (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.flatMap { value in
            if value.isEmpty ||
                value.hasPrefix("$(") ||
                value.contains("YOUR_") ||
                value.contains("xxxxxxxx") {
                return nil
            }
            return value
        }
    }
}

// MARK: - Remote config helpers

extension KotlinByteArray {
    /// Copies a Kotlin `ByteArray` into a Swift `Data` (Kotlin bytes are signed; reinterpret as UInt8).
    func toData() -> Data {
        var data = Data(count: Int(size))
        for i in 0..<size {
            data[Int(i)] = UInt8(bitPattern: get(index: i))
        }
        return data
    }
}

// Availability is handled by the shared Compose UI; no Swift-side handling is needed here.

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
            if phase == .active {
                // Capture a battery-history sample the moment the app returns to the foreground.
                // iOS runs BGAppRefreshTask rarely, so without this the graph would have a long gap
                // for the whole time the app was backgrounded; this bounds that gap regardless of
                // which tab is open.
                KoinBridge.shared.backgroundWorker().run { _ in }
                // Re-subscribe on foreground.
                KoinBridge.shared.attachConfigSync()
            }
            if phase == .background {
                AppDelegate.scheduleBackgroundRefresh()
                KoinBridge.shared.detachConfigSync()
                // Refresh widgets with the freshest foreground data before suspending.
                // Without an active background-task assertion, iOS can (and typically does)
                // suspend the process within seconds of entering .background — killing this
                // work mid-flight before the widget payload is written or timelines reload,
                // which is why widgets silently never picked up fresh data.
                var bgTaskId: UIBackgroundTaskIdentifier = .invalid
                bgTaskId = UIApplication.shared.beginBackgroundTask(withName: "WidgetRefresh") {
                    UIApplication.shared.endBackgroundTask(bgTaskId)
                    bgTaskId = .invalid
                }
                KoinBridge.shared.backgroundWorker().run { _ in
                    if bgTaskId != .invalid {
                        UIApplication.shared.endBackgroundTask(bgTaskId)
                        bgTaskId = .invalid
                    }
                }
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
            onAppleSignIn: {
                AuthBridge.shared.signInWithApple()
            },
            interstitial: AdsHost.shared.interstitial,
            bannerViewFactory: { AdsHost.shared.makeBannerView() },
            onAdPrivacyOptions: { AdsHost.shared.presentPrivacyOptions() }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
