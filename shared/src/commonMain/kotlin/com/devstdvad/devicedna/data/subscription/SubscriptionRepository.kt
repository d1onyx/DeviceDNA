package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(
    private val store: PremiumEntitlementsStore,
    private val billingGateway: SubscriptionBillingGateway,
    private val verifier: SubscriptionVerifier? = null,
) {
    val entitlements: Flow<PremiumEntitlements> = store.entitlements

    suspend fun purchasePremium(): SubscriptionOperationResult =
        when (val result = billingGateway.purchasePremium()) {
            is SubscriptionPurchaseResult.Success -> saveVerified(result.entitlements)
            is SubscriptionPurchaseResult.Failure -> SubscriptionOperationResult.Failure(result.message)
        }

    suspend fun restorePurchases(): SubscriptionOperationResult =
        when (val result = billingGateway.restorePurchases()) {
            is SubscriptionPurchaseResult.Success -> saveVerified(result.entitlements)
            is SubscriptionPurchaseResult.Failure -> SubscriptionOperationResult.Failure(result.message)
        }

    suspend fun clearDevSubscription() {
        store.clear()
    }

    private suspend fun saveVerified(entitlements: PremiumEntitlements): SubscriptionOperationResult {
        if (entitlements.source != EntitlementSource.Play) {
            store.save(entitlements)
            return SubscriptionOperationResult.Success
        }

        val productId = entitlements.productId
        val purchaseToken = entitlements.purchaseToken
        if (productId.isNullOrBlank() || purchaseToken.isNullOrBlank()) {
            return SubscriptionOperationResult.Failure("Google Play did not return purchase verification data.")
        }

        val verification = verifier
            ?: return SubscriptionOperationResult.Failure("Backend subscription verification is not configured.")

        return when (val result = verification.verifyGooglePlayPurchase(productId, purchaseToken)) {
            is SubscriptionVerificationResult.Success -> {
                store.save(result.entitlements)
                SubscriptionOperationResult.Success
            }
            is SubscriptionVerificationResult.Failure -> SubscriptionOperationResult.Failure(result.message)
        }
    }
}

sealed interface SubscriptionOperationResult {
    data object Success : SubscriptionOperationResult
    data class Failure(val message: String) : SubscriptionOperationResult
}
