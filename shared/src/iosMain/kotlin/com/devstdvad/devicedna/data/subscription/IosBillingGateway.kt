package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Result shape the Swift StoreKit 2 layer reports back through the bridge closures.
 * Null [expiresAtMillis] means a non-expiring entitlement; empty [productId] + `active=false`
 * means no entitlement (purchase cancelled / nothing to restore).
 */
data class StoreKitOutcome(
    val active: Boolean,
    val productId: String,
    val transactionId: String,
    val purchasedAtMillis: Long,
    val expiresAtMillis: Long?,
    val errorMessage: String?,
)

/**
 * iOS [SubscriptionBillingGateway] bridged to Swift StoreKit 2 via injected closures
 * (StoreKit 2 is a Swift-only async API, unreachable from Kotlin/Native directly).
 * App Store review: all premium features are unlocked exclusively through Apple IAP —
 * no external purchase links (guideline 3.1.1).
 *
 * Swift wiring (see ios/DeviceDNAApp/StoreKitBilling.swift): purchase/restore run the
 * StoreKit 2 flows and call back with a [StoreKitOutcome].
 */
class IosBillingGateway(
    private val purchaseAction: (onResult: (StoreKitOutcome) -> Unit) -> Unit,
    private val restoreAction: (onResult: (StoreKitOutcome) -> Unit) -> Unit,
) : SubscriptionBillingGateway {

    override suspend fun purchasePremium(): SubscriptionPurchaseResult =
        await(purchaseAction).toPurchaseResult(defaultError = "Purchase did not complete.")

    override suspend fun restorePurchases(): SubscriptionPurchaseResult =
        await(restoreAction).toPurchaseResult(defaultError = "No active subscription found.")

    /** Swift-visible mapper for StoreKit entitlement cache updates. */
    fun toEntitlements(outcome: StoreKitOutcome): PremiumEntitlements = outcome.toEntitlements()

    private suspend fun await(
        action: (onResult: (StoreKitOutcome) -> Unit) -> Unit,
    ): StoreKitOutcome = suspendCancellableCoroutine { cont ->
        action { outcome -> if (cont.isActive) cont.resume(outcome) }
    }

    private fun StoreKitOutcome.toPurchaseResult(defaultError: String): SubscriptionPurchaseResult =
        if (active) {
            SubscriptionPurchaseResult.Success(toEntitlements())
        } else {
            SubscriptionPurchaseResult.Failure(errorMessage ?: defaultError)
        }
}

/** Maps a verified StoreKit transaction to the shared entitlement model (all features, one tier). */
fun StoreKitOutcome.toEntitlements(): PremiumEntitlements = PremiumEntitlements(
    userId = null,
    features = PremiumFeature.entries.toSet(),
    issuedAtMillis = purchasedAtMillis,
    expiresAtMillis = expiresAtMillis,
    // Backend source keeps SubscriptionRepository from re-verifying through the
    // Google-Play-only backend endpoint; StoreKit itself already verified the purchase.
    source = EntitlementSource.Backend,
    productId = productId,
    purchaseToken = transactionId,
)
