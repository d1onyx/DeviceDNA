package com.devstdvad.devicedna.data.subscription

interface SubscriptionVerifier {
    suspend fun verifyGooglePlayPurchase(
        productId: String,
        purchaseToken: String,
    ): SubscriptionVerificationResult

    /** Dev-only: activates a short-lived Premium subscription via the backend (persisted to Neon). */
    suspend fun activateDevSubscription(): SubscriptionVerificationResult

    /** Re-reads the authoritative entitlement state from the backend (source of truth). */
    suspend fun fetchCurrentEntitlements(): SubscriptionRefreshResult
}

sealed interface SubscriptionVerificationResult {
    data class Success(val entitlements: PremiumEntitlements) : SubscriptionVerificationResult
    data class Failure(val message: String) : SubscriptionVerificationResult
}

sealed interface SubscriptionRefreshResult {
    /** Backend confirms an active subscription. */
    data class Active(val entitlements: PremiumEntitlements) : SubscriptionRefreshResult

    /** Backend confirms there is no active subscription — local entitlements must be cleared. */
    data object Inactive : SubscriptionRefreshResult

    /** Could not reach the backend (offline/error); local entitlements should be kept as-is. */
    data class Unavailable(val message: String) : SubscriptionRefreshResult
}
