package com.devstdvad.devicedna.ads

/** Android [InterstitialAds] backed by AdMob's [InterstitialAdManager]. */
class AndroidInterstitialAds(
    private val manager: InterstitialAdManager,
) : InterstitialAds {
    override fun showIfReady(onShowing: () -> Unit, onDismissed: () -> Unit) {
        manager.showIfReady(onShowing = onShowing, onDismissed = onDismissed)
    }
}
