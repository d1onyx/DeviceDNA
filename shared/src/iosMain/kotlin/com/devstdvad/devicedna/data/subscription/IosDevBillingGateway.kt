package com.devstdvad.devicedna.data.subscription

import com.devstdvad.devicedna.core.common.currentTimeMillis

/**
 * Dev-only billing gateway for iOS development artifacts. Grants a local, time-boxed entitlement so
 * premium-gated screens can be exercised without a real StoreKit purchase (App Store sandbox
 * accounts are not available in a plain `xcodebuild` dev run). The host selects this explicitly;
 * CI uses an optimized Release configuration even for sideloaded development artifacts.
 */
class IosDevBillingGateway : SubscriptionBillingGateway {

    override suspend fun purchasePremium(): SubscriptionPurchaseResult = devSuccess()

    override suspend fun restorePurchases(): SubscriptionPurchaseResult = devSuccess()

    private fun devSuccess(): SubscriptionPurchaseResult.Success {
        val now = currentTimeMillis()
        return SubscriptionPurchaseResult.Success(
            PremiumEntitlements(
                features = PremiumFeature.entries.filterNot { it == PremiumFeature.BatteryIntelligence }.toSet(),
                issuedAtMillis = now,
                expiresAtMillis = now + DEV_SUBSCRIPTION_DURATION_MS,
                source = EntitlementSource.Dev,
            ),
        )
    }

    private companion object {
        const val DEV_SUBSCRIPTION_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}
