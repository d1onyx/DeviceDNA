package com.devstdvad.devicedna.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.devstdvad.devicedna.core.design.AppTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobTopBanner(
    adUnitId: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled || adUnitId.isBlank()) return

    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    if (widthDp <= 0) return

    val adView = remember(context, adUnitId, widthDp) {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(anchoredAdaptiveBannerSize(context, widthDp))
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Banner failed to load: ${error.message}")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val TAG = "AdMobBanner"

@Suppress("DEPRECATION")
private fun anchoredAdaptiveBannerSize(
    context: android.content.Context,
    widthDp: Int,
): AdSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
