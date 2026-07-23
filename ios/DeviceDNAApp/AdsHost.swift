import Foundation
import UIKit
import AppTrackingTransparency
import GoogleMobileAds
import UserMessagingPlatform
import shared

/// AdMob integration wired into the shared Compose shell's ad slots.
///
/// App Store review checklist covered here:
///  • UMP consent flow runs BEFORE any ad request (Google EU/UK policy + Apple privacy).
///  • ATT prompt is requested via UMP only when consent requires it; the
///    NSUserTrackingUsageDescription string explains why (Info.plist).
///  • SKAdNetworkItems are declared in Info.plist for attribution without IDFA.
///  • Premium users never see ads — the shared shell disables both slots.
final class AdsHost: NSObject {

    static let shared = AdsHost()

    static let appId = AppConfig.adMobAppId
    static let bannerAdUnitId = AppConfig.adMobBannerAdUnitId
    static let interstitialAdUnitId = AppConfig.adMobInterstitialAdUnitId

    private static let googleDemoAppId = "ca-app-pub-3940256099942544~1458002511"
    private static let googleDemoBannerAdUnitId = "ca-app-pub-3940256099942544/2934735716"
    private static let googleDemoInterstitialAdUnitId = "ca-app-pub-3940256099942544/4411468910"

    private static var isUsingGoogleDemoAds: Bool {
        appId == googleDemoAppId &&
            bannerAdUnitId == googleDemoBannerAdUnitId &&
            interstitialAdUnitId == googleDemoInterstitialAdUnitId
    }

    /// Gates the on-screen ad-status caption in MainViewController — only ever true for the
    /// unsigned CI test build (Google demo ad ids), never for a real App Store build.
    static var isTestBuild: Bool { isUsingGoogleDemoAds }

    private var started = false
    private var sdkStarting = false
    private var consentFlowStarted = false
    private var interstitialAd: InterstitialAd?
    private var bannerRequested = false

    // Compose's UIKitView factory can be invoked more than once across recompositions
    // (e.g. while `enabled`/`canShowAds` settle during SDK startup); a single cached
    // BannerView avoids spawning extra, never-loaded banner instances each time.
    private lazy var bannerView: BannerView = {
        let banner = BannerView(adSize: AdSizeBanner)
        banner.adUnitID = Self.bannerAdUnitId
        banner.delegate = self
        // Compose punches a transparent hole in its Skia canvas for this UIKitView; without an
        // opaque background here, that hole shows the raw (black) layer behind it until — or
        // unless — an ad creative paints over it.
        banner.backgroundColor = .secondarySystemBackground
        return banner
    }()

    var isPrivacyOptionsRequired: Bool {
        ConsentInformation.shared.privacyOptionsRequirementStatus == .required
    }

    var canShowAds: Bool {
        started && (Self.isUsingGoogleDemoAds || ConsentInformation.shared.canRequestAds)
    }

    // MARK: - Startup: consent → ATT → SDK

    /// Runs the UMP consent flow, then starts the Google Mobile Ads SDK. Safe to call
    /// once from didFinishLaunching; presentation happens after the first frame.
    func startWhenReady() {
        guard !consentFlowStarted else { return }
        consentFlowStarted = true
        requestConsentInfo()
    }

    private func requestConsentInfo(attempt: Int = 0) {
        let parameters = RequestParameters()
        parameters.isTaggedForUnderAgeOfConsent = false

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] error in
            DispatchQueue.main.async {
                if let error {
                    NSLog("DeviceDNA/Ads: consent update failed: %@", error.localizedDescription)
                    guard attempt < 4 else {
                        self?.startAdsIfAllowed()
                        return
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + Double(attempt + 1) * 2.0) {
                        self?.requestConsentInfo(attempt: attempt + 1)
                    }
                    return
                }
                NSLog(
                    "DeviceDNA/Ads: consent updated; canRequestAds=%@ status=%ld",
                    ConsentInformation.shared.canRequestAds.description,
                    ConsentInformation.shared.consentStatus.rawValue
                )
                self?.presentConsentFormWhenReady()
            }
        }
    }

    private func presentConsentFormWhenReady(attempt: Int = 0) {
        guard let presenter = Self.topViewController() else {
            guard attempt < 40 else {
                NSLog("DeviceDNA/Ads: no presenter available for consent form")
                startAdsIfAllowed()
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self] in
                self?.presentConsentFormWhenReady(attempt: attempt + 1)
            }
            return
        }
        ConsentForm.loadAndPresentIfRequired(from: presenter) { [weak self] formError in
            DispatchQueue.main.async {
                if let formError {
                    NSLog("DeviceDNA/Ads: consent form failed: %@", formError.localizedDescription)
                }
                self?.startAdsIfAllowed()
            }
        }
    }

    private func startAdsIfAllowed() {
        guard ConsentInformation.shared.canRequestAds else {
            publishState()
            return
        }
        if started {
            publishState()
            if interstitialAd == nil { loadInterstitial() }
            return
        }
        requestTrackingAuthorizationIfNeeded { [weak self] in self?.startAdsSdk() }
    }

    func presentPrivacyOptions() {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else { return }
            ConsentForm.presentPrivacyOptionsForm(from: presenter) { error in
                if let error {
                    NSLog("DeviceDNA/Ads: privacy options failed: %@", error.localizedDescription)
                }
                self.startAdsIfAllowed()
            }
        }
    }

    private func requestTrackingAuthorizationIfNeeded(completion: @escaping () -> Void) {
        if ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            ATTrackingManager.requestTrackingAuthorization { _ in
                DispatchQueue.main.async(execute: completion)
            }
        } else {
            completion()
        }
    }

    private func startAdsSdk() {
        guard !started, !sdkStarting else { return }
        sdkStarting = true
        Task { @MainActor [weak self] in
            _ = await MobileAds.shared.start()
            guard let self else { return }
            self.sdkStarting = false
            self.started = true
            NSLog("DeviceDNA/Ads: Mobile Ads SDK started")
            self.publishState()
            self.loadInterstitial()
            if self.bannerRequested {
                self.bannerRequested = false
                self.loadBannerWhenReady(self.bannerView)
            }
        }
    }

    private func publishState() {
        if !canShowAds { interstitialAd = nil }
        IosAdsState.shared.update(
            canShow: canShowAds,
            privacyOptionsRequired: isPrivacyOptionsRequired
        )
    }

    // MARK: - Banner (factory consumed by the Compose UIKitView slot)

    func makeBannerView() -> UIView {
        NSLog(
            "DeviceDNA/Ads: banner view requested; mode=%@",
            Self.isUsingGoogleDemoAds ? "google-demo" : "custom"
        )
        if started {
            loadBannerWhenReady(bannerView)
        } else {
            bannerRequested = true
        }
        return bannerView
    }

    private func loadBannerWhenReady(_ banner: BannerView, attempt: Int = 0) {
        guard let presenter = Self.topViewController() else {
            guard attempt < 40 else {
                NSLog("DeviceDNA/Ads: no presenter available for banner")
                IosAdsState.shared.updateBannerStatus(status: "no presenter found (gave up after 10s)")
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self, weak banner] in
                guard let banner else { return }
                self?.loadBannerWhenReady(banner, attempt: attempt + 1)
            }
            return
        }
        banner.rootViewController = presenter
        NSLog("DeviceDNA/Ads: banner request")
        IosAdsState.shared.updateBannerStatus(status: "requesting…")
        banner.load(Request())
    }

    // MARK: - Interstitial (implements the Kotlin InterstitialAds protocol)

    /// Kotlin-side handle passed into MainViewController; the shared shell owns the
    /// "show every N navigations, never for premium" cadence.
    private(set) lazy var interstitial: InterstitialAds = InterstitialBridge(host: self)

    private final class InterstitialBridge: NSObject, InterstitialAds {
        weak var host: AdsHost?
        init(host: AdsHost) { self.host = host }

        func showIfReady(onShowing: @escaping () -> Void, onDismissed: @escaping () -> Void) {
            DispatchQueue.main.async {
                self.host?.showInterstitial(onShowing: onShowing, onDismissed: onDismissed)
            }
        }
    }

    private var onInterstitialDismissed: (() -> Void)?

    private func loadInterstitial() {
        guard canShowAds else {
            IosAdsState.shared.updateInterstitialStatus(status: "skipped (canShowAds=false)")
            return
        }
        IosAdsState.shared.updateInterstitialStatus(status: "requesting…")
        Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let ad = try await InterstitialAd.load(
                    with: Self.interstitialAdUnitId,
                    request: Request()
                )
                ad.fullScreenContentDelegate = self
                self.interstitialAd = ad
                IosAdsState.shared.updateInterstitialStatus(status: "ready ✓")
            } catch {
                NSLog("DeviceDNA/Ads: interstitial failed to load: %@", error.localizedDescription)
                self.interstitialAd = nil
                IosAdsState.shared.updateInterstitialStatus(status: "failed: \(error.localizedDescription)")
            }
        }
    }

    private func showInterstitial(onShowing: @escaping () -> Void, onDismissed: @escaping () -> Void) {
        guard canShowAds, let ad = interstitialAd, let presenter = Self.topViewController() else {
            IosAdsState.shared.updateInterstitialStatus(
                status: "show requested but not ready (canShowAds=\(canShowAds), hasAd=\(interstitialAd != nil))"
            )
            return
        }
        interstitialAd = nil
        onInterstitialDismissed = onDismissed
        onShowing()
        ad.present(from: presenter)
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

// MARK: - FullScreenContentDelegate

extension AdsHost: FullScreenContentDelegate {
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        onInterstitialDismissed?()
        onInterstitialDismissed = nil
        loadInterstitial()   // preload the next one
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        onInterstitialDismissed?()
        onInterstitialDismissed = nil
        loadInterstitial()
    }
}

// MARK: - BannerViewDelegate

extension AdsHost: BannerViewDelegate {
    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        NSLog("DeviceDNA/Ads: banner loaded")
        IosAdsState.shared.updateBannerStatus(status: "loaded ✓")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        NSLog("DeviceDNA/Ads: banner failed to load: %@", error.localizedDescription)
        IosAdsState.shared.updateBannerStatus(status: "failed: \(error.localizedDescription)")
    }
}
