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

    /// Test ad units — replace with production ids before release.
    static let bannerAdUnitId = "ca-app-pub-3940256099942544/2934735716"
    static let interstitialAdUnitId = "ca-app-pub-3940256099942544/4411468910"

    private var started = false
    private var interstitialAd: InterstitialAd?

    // MARK: - Startup: consent → ATT → SDK

    /// Runs the UMP consent flow, then starts the Google Mobile Ads SDK. Safe to call
    /// once from didFinishLaunching; presentation happens after the first frame.
    func startWhenReady() {
        let parameters = RequestParameters()
        parameters.isTaggedForUnderAgeOfConsent = false

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] _ in
            DispatchQueue.main.async {
                guard let presenter = Self.topViewController() else {
                    self?.startAdsSdk()
                    return
                }
                ConsentForm.loadAndPresentIfRequired(from: presenter) { _ in
                    self?.requestTrackingAuthorizationIfNeeded {
                        self?.startAdsSdk()
                    }
                }
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
        guard !started else { return }
        started = true
        Task { @MainActor [weak self] in
            _ = await MobileAds.shared.start()
            self?.loadInterstitial()
        }
    }

    // MARK: - Banner (factory consumed by the Compose UIKitView slot)

    func makeBannerView() -> UIView {
        let banner = BannerView(adSize: AdSizeBanner)
        banner.adUnitID = Self.bannerAdUnitId
        banner.rootViewController = Self.topViewController()
        if started {
            banner.load(Request())
        }
        return banner
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
        guard let ad = interstitialAd, let presenter = Self.topViewController() else { return }
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
