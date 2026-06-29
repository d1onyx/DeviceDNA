package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.subscription.EntitlementSource
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionOperationResult
import com.devstdvad.devicedna.data.subscription.SubscriptionPurchaseResult
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.subscription.SubscriptionVerificationResult
import com.devstdvad.devicedna.data.subscription.SubscriptionVerifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRepositoryTest {

    @Test
    fun `dev purchase in local mode stores entitlement directly`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = PremiumFeature.entries.toSet(),
                    issuedAtMillis = 1_000L,
                    expiresAtMillis = 600_000L,
                    source = EntitlementSource.Dev,
                ),
            ),
            // devUsesBackend defaults to false → local unlock, no verifier needed.
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Success)
        val entitlements = repository.entitlements.first()
        assertTrue(entitlements.hasFeature(PremiumFeature.RemoveAds, nowMillis = 1L))
        assertTrue(entitlements.source == EntitlementSource.Dev)
    }

    @Test
    fun `dev purchase in backend mode saves backend entitlement`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = PremiumFeature.entries.toSet(),
                    issuedAtMillis = 1_000L,
                    source = EntitlementSource.Dev,
                ),
            ),
            verifier = FakeVerifier(
                devResult = SubscriptionVerificationResult.Success(
                    PremiumEntitlements(
                        features = PremiumFeature.entries.toSet(),
                        issuedAtMillis = 2_000L,
                        expiresAtMillis = 600_000L,
                        source = EntitlementSource.Dev,
                        productId = "devicedna_premium",
                    ),
                ),
            ),
            devUsesBackend = true,
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Success)
        val entitlements = repository.entitlements.first()
        assertTrue(entitlements.hasFeature(PremiumFeature.RemoveAds, nowMillis = 1L))
        assertTrue(entitlements.source == EntitlementSource.Dev)
    }

    @Test
    fun `dev purchase in backend mode without verifier fails`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = PremiumFeature.entries.toSet(),
                    issuedAtMillis = 1_000L,
                    source = EntitlementSource.Dev,
                ),
            ),
            devUsesBackend = true,
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Failure)
        assertFalse(repository.entitlements.first().hasFeature(PremiumFeature.RemoveAds, nowMillis = 1L))
    }

    @Test
    fun `clear dev subscription removes remove ads entitlement`() = runTest {
        val store = FakePremiumEntitlementsStore(
            PremiumEntitlements(
                features = setOf(PremiumFeature.RemoveAds),
                issuedAtMillis = 1_000L,
                source = EntitlementSource.Dev,
            ),
        )
        val repository = SubscriptionRepository(store, FakeBillingGateway(PremiumEntitlements.Empty))

        repository.clearDevSubscription()

        assertFalse(repository.entitlements.first().hasFeature(PremiumFeature.RemoveAds))
    }

    @Test
    fun `play purchase saves backend verified entitlement`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = PremiumFeature.entries.toSet(),
                    issuedAtMillis = 1_000L,
                    source = EntitlementSource.Play,
                    productId = "devicedna_premium",
                    purchaseToken = "purchase-token",
                ),
            ),
            verifier = FakeVerifier(
                SubscriptionVerificationResult.Success(
                    PremiumEntitlements(
                        features = PremiumFeature.entries.toSet(),
                        issuedAtMillis = 2_000L,
                        source = EntitlementSource.Backend,
                        productId = "devicedna_premium",
                    ),
                ),
            ),
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Success)
        val entitlements = repository.entitlements.first()
        assertTrue(entitlements.hasFeature(PremiumFeature.RemoveAds))
        assertTrue(entitlements.source == EntitlementSource.Backend)
    }

    @Test
    fun `play purchase without verifier does not save local premium`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = PremiumFeature.entries.toSet(),
                    issuedAtMillis = 1_000L,
                    source = EntitlementSource.Play,
                    productId = "devicedna_premium",
                    purchaseToken = "purchase-token",
                ),
            ),
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Failure)
        assertFalse(repository.entitlements.first().hasFeature(PremiumFeature.RemoveAds))
    }

    @Test
    fun `expired entitlement does not grant feature`() {
        val entitlements = PremiumEntitlements(
            features = setOf(PremiumFeature.RemoveAds),
            issuedAtMillis = 1_000L,
            expiresAtMillis = 2_000L,
            source = EntitlementSource.Backend,
        )

        assertFalse(entitlements.hasFeature(PremiumFeature.RemoveAds, nowMillis = 2_001L))
    }

    private class FakePremiumEntitlementsStore(
        initial: PremiumEntitlements = PremiumEntitlements.Empty,
    ) : PremiumEntitlementsStore {
        private val state = MutableStateFlow(initial)

        override val entitlements: Flow<PremiumEntitlements> = state

        override suspend fun save(entitlements: PremiumEntitlements) {
            state.value = entitlements
        }

        override suspend fun clear() {
            state.value = PremiumEntitlements.Empty
        }
    }

    private class FakeBillingGateway(
        private val entitlements: PremiumEntitlements,
    ) : SubscriptionBillingGateway {
        override suspend fun purchasePremium(): SubscriptionPurchaseResult =
            SubscriptionPurchaseResult.Success(entitlements)

        override suspend fun restorePurchases(): SubscriptionPurchaseResult =
            SubscriptionPurchaseResult.Success(entitlements)
    }

    private class FakeVerifier(
        private val playResult: SubscriptionVerificationResult =
            SubscriptionVerificationResult.Failure("no play result"),
        private val devResult: SubscriptionVerificationResult =
            SubscriptionVerificationResult.Failure("no dev result"),
    ) : SubscriptionVerifier {
        override suspend fun verifyGooglePlayPurchase(
            productId: String,
            purchaseToken: String,
        ): SubscriptionVerificationResult = playResult

        override suspend fun activateDevSubscription(): SubscriptionVerificationResult = devResult
    }
}
