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
            onSuccess = { toVerificationResult(it, EntitlementSource.Backend) },
            onFailure = { error ->
                SubscriptionVerificationResult.Failure(
                    error.message ?: "Unable to verify Premium with the backend.",
                )
            },
        )
    }

    override suspend fun activateDevSubscription(): SubscriptionVerificationResult {
        val idToken = authRepository.getIdToken()
            ?: return SubscriptionVerificationResult.Failure("Sign in before activating Premium.")

        return runCatching {
            syncApi.activateDevSubscription(idToken)
        }.fold(
            onSuccess = { toVerificationResult(it, EntitlementSource.Dev) },
            onFailure = { error ->
                SubscriptionVerificationResult.Failure(
                    error.message ?: "Unable to activate the dev subscription.",
                )
            },
        )
    }

    private fun toVerificationResult(
        response: SubscriptionViewResponse,
        source: EntitlementSource,
    ): SubscriptionVerificationResult {
        if (!response.premium) {
            return SubscriptionVerificationResult.Failure("Backend did not confirm an active Premium subscription.")
        }

        val subscription = response.subscription
            ?: return SubscriptionVerificationResult.Failure("Backend response did not include subscription details.")

        return SubscriptionVerificationResult.Success(subscription.toEntitlements(authRepository.uid, source))
    }

    private fun BackendSubscription.toEntitlements(userId: String?, source: EntitlementSource): PremiumEntitlements =
        PremiumEntitlements(
            userId = userId,
            features = PremiumFeature.entries.toSet(),
            issuedAtMillis = Clock.System.now().toEpochMilliseconds(),
            expiresAtMillis = expiresAt?.let { Instant.parse(it).toEpochMilliseconds() },
            source = source,
            productId = productId,
            purchaseToken = null,
        )
}
