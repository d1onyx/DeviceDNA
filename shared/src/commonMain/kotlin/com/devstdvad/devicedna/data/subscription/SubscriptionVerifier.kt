package com.devstdvad.devicedna.data.subscription

interface SubscriptionVerifier {
    suspend fun verifyGooglePlayPurchase(
        productId: String,
        purchaseToken: String,
    ): SubscriptionVerificationResult
}

sealed interface SubscriptionVerificationResult {
    data class Success(val entitlements: PremiumEntitlements) : SubscriptionVerificationResult
    data class Failure(val message: String) : SubscriptionVerificationResult
}
