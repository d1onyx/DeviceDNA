package com.devstdvad.devicedna.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Observable consent-derived ad UI state pushed by the native UMP bridge. */
object IosAdsState {
    private val canShowFlow = MutableStateFlow(false)
    private val privacyOptionsFlow = MutableStateFlow(false)
    private val bannerStatusFlow = MutableStateFlow<String?>(null)
    private val interstitialStatusFlow = MutableStateFlow<String?>(null)

    val canShowAds: StateFlow<Boolean> = canShowFlow
    val privacyOptionsRequired: StateFlow<Boolean> = privacyOptionsFlow
    val bannerStatus: StateFlow<String?> = bannerStatusFlow
    val interstitialStatus: StateFlow<String?> = interstitialStatusFlow

    fun update(canShow: Boolean, privacyOptionsRequired: Boolean) {
        canShowFlow.value = canShow
        privacyOptionsFlow.value = privacyOptionsRequired
    }

    fun updateBannerStatus(status: String?) {
        bannerStatusFlow.value = status
    }

    fun updateInterstitialStatus(status: String?) {
        interstitialStatusFlow.value = status
    }
}
