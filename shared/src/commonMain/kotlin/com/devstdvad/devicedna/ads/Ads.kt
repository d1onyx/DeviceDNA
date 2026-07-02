package com.devstdvad.devicedna.ads

/**
 * Platform-agnostic interstitial ads seam. Android wraps AdMob's InterstitialAdManager; iOS (and
 * premium users) use [NoOpInterstitialAds]. The "show every N navigations" cadence lives in shared
 * App() — this only exposes the show trigger.
 */
interface InterstitialAds {
    fun showIfReady(onShowing: () -> Unit = {}, onDismissed: () -> Unit = {})
}

object NoOpInterstitialAds : InterstitialAds {
    override fun showIfReady(onShowing: () -> Unit, onDismissed: () -> Unit) {}
}
