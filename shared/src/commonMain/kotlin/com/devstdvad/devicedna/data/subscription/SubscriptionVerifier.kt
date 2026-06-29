package com.devstdvad.devicedna.data.subscription

interface SubscriptionVerifier {
    suspend fun verifyGooglePlayPurchase(
        productId: String,
        purchaseToken: String,
    ): SubscriptionVerificationResult

    /** Dev-only: activates a short-lived Premium subscription via the backend (persisted to Neon). */
    suspend fun activateDevSubscription(): SubscriptionVerificationResult
}

sealed interface SubscriptionVerificationResult {
    data class Success(val entitlements: PremiumEntitlements) : SubscriptionVerificationResult
    data class Failure(val message: String) : SubscriptionVerificationResult
}
