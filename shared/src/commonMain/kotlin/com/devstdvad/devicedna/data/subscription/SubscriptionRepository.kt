package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(
    private val store: PremiumEntitlementsStore,
    private val billingGateway: SubscriptionBillingGateway,
) {
    val entitlements: Flow<PremiumEntitlements> = store.entitlements

    suspend fun purchasePremium(): SubscriptionOperationResult =
        when (val result = billingGateway.purchasePremium()) {
            is SubscriptionPurchaseResult.Success -> {
                store.save(result.entitlements)
                SubscriptionOperationResult.Success
            }
            is SubscriptionPurchaseResult.Failure -> SubscriptionOperationResult.Failure(result.message)
        }

    suspend fun restorePurchases(): SubscriptionOperationResult =
        when (val result = billingGateway.restorePurchases()) {
            is SubscriptionPurchaseResult.Success -> {
                store.save(result.entitlements)
                SubscriptionOperationResult.Success
            }
            is SubscriptionPurchaseResult.Failure -> SubscriptionOperationResult.Failure(result.message)
        }

    suspend fun clearDevSubscription() {
        store.clear()
    }
}

sealed interface SubscriptionOperationResult {
    data object Success : SubscriptionOperationResult
    data class Failure(val message: String) : SubscriptionOperationResult
}
