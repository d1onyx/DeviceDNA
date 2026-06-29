package com.devstdvad.devicedna.data.subscription

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.devstdvad.devicedna.BuildConfig
import com.devstdvad.devicedna.core.CurrentActivityHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Real Google Play Billing implementation of [SubscriptionBillingGateway] for a single premium
 * subscription product. Selected in place of [DevSubscriptionBillingGateway] when
 * `BuildConfig.USE_REAL_BILLING` is true (see AppModule). A single active subscription unlocks
 * every [PremiumFeature], mirroring the dev gateway.
 *
 * Note: client-side BillingClient cannot reveal the renewal/expiry date, so an active acknowledged
 * purchase is treated as entitled and re-validated via [restorePurchases]/queryPurchases. For
 * production, validate the purchase token server-side with the Play Developer API.
 */
class PlayBillingGateway(
    context: Context,
    private val activityHolder: CurrentActivityHolder,
    private val productId: String = BuildConfig.PREMIUM_SUB_PRODUCT_ID,
) : SubscriptionBillingGateway {

    private val purchaseUpdates = MutableSharedFlow<PurchasesUpdate>(extraBufferCapacity = 1)
    private val connectMutex = Mutex()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .setListener { result, purchases ->
            purchaseUpdates.tryEmit(PurchasesUpdate(result, purchases.orEmpty()))
        }
        .build()

    override suspend fun purchasePremium(): SubscriptionPurchaseResult = runCatching {
        val activity = activityHolder.current
            ?: return SubscriptionPurchaseResult.Failure("No active screen to start the purchase.")
        ensureConnected()

        // Already subscribed (e.g. on another device)? Restore instead of charging again.
        queryActiveEntitlements()?.let { return SubscriptionPurchaseResult.Success(it) }

        val product = queryPremiumProduct()
            ?: return SubscriptionPurchaseResult.Failure("Premium product '$productId' not found in Google Play.")
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return SubscriptionPurchaseResult.Failure("No subscription offer available for '$productId'.")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        val launch = billingClient.launchBillingFlow(activity, flowParams)
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            return SubscriptionPurchaseResult.Failure(launch.debugMessage.ifBlank { "Could not start purchase." })
        }

        // The purchase result arrives asynchronously via the PurchasesUpdatedListener.
        val update = withTimeoutOrNull(PURCHASE_TIMEOUT_MS) { purchaseUpdates.first() }
            ?: return SubscriptionPurchaseResult.Failure("Purchase timed out.")

        when (update.result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = update.purchases.firstOrNull { it.products.contains(productId) }
                    ?: return SubscriptionPurchaseResult.Failure("Purchase completed but no matching product.")
                handlePurchase(purchase)
                    ?.let { SubscriptionPurchaseResult.Success(it) }
                    ?: SubscriptionPurchaseResult.Failure("Purchase is pending approval.")
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                SubscriptionPurchaseResult.Failure("Purchase cancelled.")
            else ->
                SubscriptionPurchaseResult.Failure(update.result.debugMessage.ifBlank { "Purchase failed." })
        }
    }.getOrElse { t -> SubscriptionPurchaseResult.Failure(t.message ?: "Billing error.") }

    override suspend fun restorePurchases(): SubscriptionPurchaseResult = runCatching {
        ensureConnected()
        queryActiveEntitlements()
            ?.let { SubscriptionPurchaseResult.Success(it) }
            ?: SubscriptionPurchaseResult.Failure("No active subscription found.")
    }.getOrElse { t -> SubscriptionPurchaseResult.Failure(t.message ?: "Billing error.") }

    private suspend fun queryActiveEntitlements(): PremiumEntitlements? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val purchase = billingClient.queryPurchasesAsync(params).purchasesList
            .firstOrNull { it.products.contains(productId) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            ?: return null
        return handlePurchase(purchase)
    }

    /** Acknowledges (if needed) and maps a PURCHASED subscription to entitlements; null if pending. */
    private suspend fun handlePurchase(purchase: Purchase): PremiumEntitlements? {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return null
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams)
        }
        return PremiumEntitlements(
            features = PremiumFeature.entries.toSet(),
            issuedAtMillis = purchase.purchaseTime,
            expiresAtMillis = null,
            source = EntitlementSource.Play,
        )
    }

    private suspend fun queryPremiumProduct(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()
        return billingClient.queryProductDetails(params).productDetailsList?.firstOrNull()
    }

    private suspend fun ensureConnected() = connectMutex.withLock {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) return@withLock
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (!cont.isActive) return
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            IllegalStateException(result.debugMessage.ifBlank { "Google Play Billing is unavailable." }),
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Reconnected lazily on the next call via ensureConnected().
                }
            })
        }
    }

    private data class PurchasesUpdate(val result: BillingResult, val purchases: List<Purchase>)

    companion object {
        private const val PURCHASE_TIMEOUT_MS = 5L * 60L * 1000L
    }
}
