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
    private val productInfoAction: (onResult: (SubscriptionProductInfo?) -> Unit) -> Unit,
    private val manageAction: () -> Unit,
) : SubscriptionBillingGateway {

    override suspend fun purchasePremium(): SubscriptionPurchaseResult =
        await(purchaseAction).toPurchaseResult(defaultError = "Purchase did not complete.")

    override suspend fun restorePurchases(): SubscriptionPurchaseResult =
        await(restoreAction).toPurchaseResult(defaultError = "No active subscription found.")

    override suspend fun productInfo(): SubscriptionProductInfo? =
        suspendCancellableCoroutine { cont ->
            productInfoAction { info -> if (cont.isActive) cont.resume(info) }
        }

    override fun openSubscriptionManagement() = manageAction()

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
    features = PremiumFeature.entries.filterNot { it == PremiumFeature.BatteryIntelligence }.toSet(),
    issuedAtMillis = purchasedAtMillis,
    expiresAtMillis = expiresAtMillis,
    source = EntitlementSource.AppStore,
    productId = productId,
    purchaseToken = transactionId,
)
