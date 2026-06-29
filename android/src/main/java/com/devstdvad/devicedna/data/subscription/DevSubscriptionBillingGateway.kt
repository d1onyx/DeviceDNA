package com.devstdvad.devicedna.data.subscription

import java.util.concurrent.atomic.AtomicBoolean

class DevSubscriptionBillingGateway : SubscriptionBillingGateway {

    // The first purchase attempt fails on purpose (simulating a user who starts paying but aborts
    // mid-flow) so the error-handling path can be exercised; every attempt after that succeeds.
    private val firstAttemptConsumed = AtomicBoolean(false)

    override suspend fun purchasePremium(): SubscriptionPurchaseResult =
        if (firstAttemptConsumed.compareAndSet(false, true)) {
            SubscriptionPurchaseResult.Failure("Purchase cancelled.")
        } else {
            devSuccess()
        }

    override suspend fun restorePurchases(): SubscriptionPurchaseResult = devSuccess()

    // Marks the purchase as a dev entitlement; the actual validity window (10 min) is granted and
    // persisted by the backend in SubscriptionRepository.activateDevSubscription(), mirroring the
    // real Play verify flow. The local issuedAt is informational only.
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
