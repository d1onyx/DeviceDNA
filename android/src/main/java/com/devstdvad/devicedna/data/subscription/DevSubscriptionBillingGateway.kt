package com.devstdvad.devicedna.data.subscription

class DevSubscriptionBillingGateway : SubscriptionBillingGateway {

    override suspend fun purchasePremium(): SubscriptionPurchaseResult = devSuccess()

    override suspend fun restorePurchases(): SubscriptionPurchaseResult = devSuccess()

    private fun devSuccess(): SubscriptionPurchaseResult.Success =
        SubscriptionPurchaseResult.Success(
            PremiumEntitlements(
                features = PremiumFeature.entries.toSet(),
                issuedAtMillis = System.currentTimeMillis(),
                expiresAtMillis = null,
                source = EntitlementSource.Dev,
            ),
        )
}
