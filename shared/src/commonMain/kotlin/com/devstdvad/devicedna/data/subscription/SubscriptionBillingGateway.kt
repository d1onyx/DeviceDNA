package com.devstdvad.devicedna.data.subscription

interface SubscriptionBillingGateway {
    suspend fun purchasePremium(): SubscriptionPurchaseResult
    suspend fun restorePurchases(): SubscriptionPurchaseResult
    suspend fun productInfo(): SubscriptionProductInfo? = null
    fun openSubscriptionManagement() = Unit
}

data class SubscriptionProductInfo(
    val displayName: String,
    val displayPrice: String,
    val periodUnit: String,
    val periodValue: Int,
)

sealed interface SubscriptionPurchaseResult {
    data class Success(val entitlements: PremiumEntitlements) : SubscriptionPurchaseResult
    data class Failure(val message: String) : SubscriptionPurchaseResult
}
