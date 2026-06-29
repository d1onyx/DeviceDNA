package com.devstdvad.devicedna.data.subscription

interface SubscriptionBillingGateway {
    suspend fun purchasePremium(): SubscriptionPurchaseResult
    suspend fun restorePurchases(): SubscriptionPurchaseResult
}

sealed interface SubscriptionPurchaseResult {
    data class Success(val entitlements: PremiumEntitlements) : SubscriptionPurchaseResult
    data class Failure(val message: String) : SubscriptionPurchaseResult
}
