package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(
    private val store: PremiumEntitlementsStore,
    private val billingGateway: SubscriptionBillingGateway,
    private val verifier: SubscriptionVerifier? = null,
    // When true, dev purchases are activated through the backend (persisted to Neon) instead of
    // being unlocked locally. Lets the dev build exercise the real client→backend→Neon flow.
    private val devUsesBackend: Boolean = false,
    // When true, the backend is the source of truth for entitlements, so refreshEntitlements()
    // can clear local premium if the backend reports none. False for pure local dev unlock (where
    // the subscription only exists on-device and must not be wiped by a backend check).
    private val serverAuthoritative: Boolean = false,
) {
    val entitlements: Flow<PremiumEntitlements> = store.entitlements

    /**
     * Re-checks the authoritative entitlement state with the backend and updates local storage.
     * The server wins: an active subscription is saved, a missing one clears local premium. On a
     * network error the local cache is kept (offline tolerance). No-op when the backend is not the
     * source of truth (pure local dev unlock) or no verifier is configured.
     */
    suspend fun refreshEntitlements() {
        if (!serverAuthoritative) return
        val verification = verifier ?: return
        when (val result = verification.fetchCurrentEntitlements()) {
            is SubscriptionRefreshResult.Active -> store.save(result.entitlements)
            SubscriptionRefreshResult.Inactive -> store.clear()
            is SubscriptionRefreshResult.Unavailable -> Unit
        }
    }

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
            EntitlementSource.Dev -> if (devUsesBackend) {
                activateDevSubscription()
            } else {
                store.save(entitlements)
                SubscriptionOperationResult.Success
            }
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
