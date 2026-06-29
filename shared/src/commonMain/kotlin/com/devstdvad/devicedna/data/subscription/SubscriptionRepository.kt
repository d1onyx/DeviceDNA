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

    private suspend fun saveVerified(entitlements: PremiumEntitlements): SubscriptionOperationResult =
        when (entitlements.source) {
            EntitlementSource.Play -> verifyPlayPurchase(entitlements)
            EntitlementSource.Dev -> activateDevSubscription()
            else -> {
                store.save(entitlements)
                SubscriptionOperationResult.Success
            }
        }

    private suspend fun verifyPlayPurchase(entitlements: PremiumEntitlements): SubscriptionOperationResult {
        val productId = entitlements.productId
        val purchaseToken = entitlements.purchaseToken
        if (productId.isNullOrBlank() || purchaseToken.isNullOrBlank()) {
            return SubscriptionOperationResult.Failure("Google Play did not return purchase verification data.")
        }

        val verification = verifier
            ?: return SubscriptionOperationResult.Failure("Backend subscription verification is not configured.")

        return verification.verifyGooglePlayPurchase(productId, purchaseToken).persist()
    }

    // Dev subscriptions are also persisted server-side (Neon) so the dev flow mirrors production;
    // the backend grants a short-lived entitlement and we store whatever it returns.
    private suspend fun activateDevSubscription(): SubscriptionOperationResult {
        val verification = verifier
            ?: return SubscriptionOperationResult.Failure("Backend subscription verification is not configured.")

        return verification.activateDevSubscription().persist()
    }

    private suspend fun SubscriptionVerificationResult.persist(): SubscriptionOperationResult =
        when (this) {
            is SubscriptionVerificationResult.Success -> {
                store.save(entitlements)
                SubscriptionOperationResult.Success
            }
            is SubscriptionVerificationResult.Failure -> SubscriptionOperationResult.Failure(message)
        }
}

sealed interface SubscriptionOperationResult {
    data object Success : SubscriptionOperationResult
    data class Failure(val message: String) : SubscriptionOperationResult
}
