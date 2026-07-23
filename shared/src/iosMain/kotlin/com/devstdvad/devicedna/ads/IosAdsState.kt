package com.devstdvad.devicedna.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Observable consent-derived ad UI state pushed by the native UMP bridge. */
object IosAdsState {
    private val canShowFlow = MutableStateFlow(false)
    private val privacyOptionsFlow = MutableStateFlow(false)

    val canShowAds: StateFlow<Boolean> = canShowFlow
    val privacyOptionsRequired: StateFlow<Boolean> = privacyOptionsFlow

    fun update(canShow: Boolean, privacyOptionsRequired: Boolean) {
        canShowFlow.value = canShow
        privacyOptionsFlow.value = privacyOptionsRequired
    }
}
