package com.devstdvad.devicedna.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

class InterstitialAdManager(
    private val adUnitId: String,
    private val activity: Activity?,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    // Set to true when show was requested but ad wasn't ready yet;
    // the ad will be shown as soon as it finishes loading.
    private var showPending = false

    fun loadIfNeeded() {
        if (interstitialAd != null || isLoading || adUnitId.isBlank() || activity == null) return
        isLoading = true
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    if (showPending) {
                        showPending = false
                        showIfReady()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    isLoading = false
                    showPending = false
                }
            },
        )
    }

    fun showIfReady(onShowing: () -> Unit = {}, onDismissed: () -> Unit = {}) {
        if (adUnitId.isBlank() || activity == null) return
        val ad = interstitialAd ?: run {
            showPending = true
            loadIfNeeded()
            return
        }
        showPending = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                onShowing()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onDismissed()
                loadIfNeeded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                onDismissed()
                loadIfNeeded()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val TAG = "AdMobInterstitial"
    }
}

@Composable
fun rememberInterstitialAdManager(adUnitId: String): InterstitialAdManager {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val manager = remember(adUnitId, activity) {
        InterstitialAdManager(adUnitId, activity)
    }
    LaunchedEffect(manager) {
        manager.loadIfNeeded()
    }
    return manager
}
