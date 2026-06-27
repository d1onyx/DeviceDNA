package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.subscription.EntitlementSource
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionOperationResult
import com.devstdvad.devicedna.data.subscription.SubscriptionPurchaseResult
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRepositoryTest {

    @Test
    fun `dev purchase grants remove ads entitlement`() = runTest {
        val store = FakePremiumEntitlementsStore()
        val repository = SubscriptionRepository(
            store = store,
            billingGateway = FakeBillingGateway(
                PremiumEntitlements(
                    features = setOf(PremiumFeature.RemoveAds),
                    issuedAtMillis = 1_000L,
                    source = EntitlementSource.Dev,
                ),
            ),
        )

        val result = repository.purchasePremium()

        assertTrue(result is SubscriptionOperationResult.Success)
        assertTrue(repository.entitlements.first().hasFeature(PremiumFeature.RemoveAds))
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
}
