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

    // Grants a dev entitlement valid for 24 hours (matches the iOS dev gateway). In local mode
    // (DEV_SUBSCRIPTION_USE_BACKEND = false) the repository stores this directly. In backend mode the
    // repository instead calls activateDevSubscription() and this local window is replaced by the
    // backend-issued one.
    private fun devSuccess(): SubscriptionPurchaseResult.Success {
        val now = System.currentTimeMillis()
        return SubscriptionPurchaseResult.Success(
            PremiumEntitlements(
                features = PremiumFeature.entries.toSet(),
                issuedAtMillis = now,
                expiresAtMillis = now + DEV_SUBSCRIPTION_DURATION_MS,
                source = EntitlementSource.Dev,
            ),
        )
    }

    private companion object {
        const val DEV_SUBSCRIPTION_DURATION_MS = 24 * 60 * 60 * 1000L
    }
}
