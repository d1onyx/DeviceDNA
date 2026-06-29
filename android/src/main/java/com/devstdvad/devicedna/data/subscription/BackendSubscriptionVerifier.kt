package com.devstdvad.devicedna.data.subscription

import com.devstdvad.devicedna.data.auth.AuthRepository
import com.devstdvad.devicedna.data.sync.SyncApi
import com.devstdvad.devicedna.data.sync.model.BackendSubscription
import com.devstdvad.devicedna.data.sync.model.GooglePlaySubscriptionVerificationPayload
import com.devstdvad.devicedna.data.sync.model.SubscriptionViewResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class BackendSubscriptionVerifier(
    private val authRepository: AuthRepository,
    private val syncApi: SyncApi,
) : SubscriptionVerifier {

    override suspend fun verifyGooglePlayPurchase(
        productId: String,
        purchaseToken: String,
    ): SubscriptionVerificationResult {
        val idToken = authRepository.getIdToken()
            ?: return SubscriptionVerificationResult.Failure("Sign in before activating Premium.")

        return runCatching {
            syncApi.verifyGooglePlaySubscription(
                idToken = idToken,
                payload = GooglePlaySubscriptionVerificationPayload(
                    productId = productId,
                    purchaseToken = purchaseToken,
                ),
            )
        }.fold(
            onSuccess = ::toVerificationResult,
            onFailure = { error ->
                SubscriptionVerificationResult.Failure(
                    error.message ?: "Unable to verify Premium with the backend.",
                )
            },
        )
    }

    private fun toVerificationResult(response: SubscriptionViewResponse): SubscriptionVerificationResult {
        if (!response.premium) {
            return SubscriptionVerificationResult.Failure("Backend did not confirm an active Premium subscription.")
        }

        val subscription = response.subscription
            ?: return SubscriptionVerificationResult.Failure("Backend response did not include subscription details.")

        return SubscriptionVerificationResult.Success(subscription.toEntitlements(authRepository.uid))
    }

    private fun BackendSubscription.toEntitlements(userId: String?): PremiumEntitlements =
        PremiumEntitlements(
            userId = userId,
            features = PremiumFeature.entries.toSet(),
            issuedAtMillis = Clock.System.now().toEpochMilliseconds(),
            expiresAtMillis = expiresAt?.let { Instant.parse(it).toEpochMilliseconds() },
            source = EntitlementSource.Backend,
            productId = productId,
            purchaseToken = null,
        )
}
