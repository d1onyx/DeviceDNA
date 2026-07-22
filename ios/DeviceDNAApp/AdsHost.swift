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

    static let bannerAdUnitId = AppConfig.adMobBannerAdUnitId
    static let interstitialAdUnitId = AppConfig.adMobInterstitialAdUnitId

    private var started = false
    private var sdkStarting = false
    private var consentFlowStarted = false
    private var interstitialAd: InterstitialAd?
    private var pendingBanners: [BannerView] = []

    var isPrivacyOptionsRequired: Bool {
        ConsentInformation.shared.privacyOptionsRequirementStatus == .required
    }

    var canShowAds: Bool { started && ConsentInformation.shared.canRequestAds }

    // MARK: - Startup: consent → ATT → SDK

    /// Runs the UMP consent flow, then starts the Google Mobile Ads SDK. Safe to call
    /// once from didFinishLaunching; presentation happens after the first frame.
    func startWhenReady() {
        guard !consentFlowStarted else { return }
        consentFlowStarted = true
        let parameters = RequestParameters()
        parameters.isTaggedForUnderAgeOfConsent = false

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] error in
            DispatchQueue.main.async {
                if let error {
                    NSLog("DeviceDNA/Ads: consent update failed: %@", error.localizedDescription)
                    self?.startAdsIfAllowed()
                    return
                }
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
            self.publishState()
            self.loadInterstitial()
            self.pendingBanners.forEach { self.loadBannerWhenReady($0) }
            self.pendingBanners.removeAll()
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
        let banner = BannerView(adSize: AdSizeBanner)
        banner.adUnitID = Self.bannerAdUnitId
        if started {
            loadBannerWhenReady(banner)
        } else {
            pendingBanners.append(banner)
        }
        return banner
    }

    private func loadBannerWhenReady(_ banner: BannerView, attempt: Int = 0) {
        guard let presenter = Self.topViewController() else {
            guard attempt < 40 else {
                NSLog("DeviceDNA/Ads: no presenter available for banner")
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self, weak banner] in
                guard let banner else { return }
                self?.loadBannerWhenReady(banner, attempt: attempt + 1)
            }
            return
        }
        banner.rootViewController = presenter
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
        guard canShowAds else { return }
        Task { @MainActor [weak self] in
            guard let self else { return }
            do {
                let ad = try await InterstitialAd.load(
                    with: Self.interstitialAdUnitId,
                    request: Request()
                )
                ad.fullScreenContentDelegate = self
                self.interstitialAd = ad
            } catch {
                self.interstitialAd = nil
            }
        }
    }

    private func showInterstitial(onShowing: @escaping () -> Void, onDismissed: @escaping () -> Void) {
        guard canShowAds, let ad = interstitialAd, let presenter = Self.topViewController() else { return }
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
