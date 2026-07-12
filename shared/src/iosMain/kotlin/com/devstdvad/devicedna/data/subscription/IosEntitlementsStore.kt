package com.devstdvad.devicedna.data.subscription

import com.devstdvad.devicedna.data.auth.AuthGateway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS [PremiumEntitlementsStore] persisted in NSUserDefaults as JSON. This is a cache only —
 * the authoritative entitlement source on iOS is StoreKit's Transaction.currentEntitlements,
 * which the Swift billing bridge re-pushes on launch and on every transaction update, so a
 * tampered cache cannot grant premium (App Store review-safe without Keychain complexity).
 *
 * Premium is scoped to the signed-in account: stored under a per-uid key so switching accounts
 * (and back) preserves each account's premium and one account never sees another's. When signed
 * out it reports Empty.
 */
class IosEntitlementsStore(
    private val authGateway: AuthGateway,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : PremiumEntitlementsStore {

    @Serializable
    private data class Persisted(
        val userId: String? = null,
        val featureKeys: List<String> = emptyList(),
        val issuedAtMillis: Long = 0L,
        val expiresAtMillis: Long? = null,
        val source: String = EntitlementSource.None.name,
        val productId: String? = null,
        val purchaseToken: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    // Bumped on every save/clear so the combined flow re-reads NSUserDefaults for the current account.
    private val reload = MutableStateFlow(0)

    override val entitlements: Flow<PremiumEntitlements> =
        combine(authGateway.currentUser, reload) { user, _ -> load(user?.uid) }

    private fun load(uid: String?): PremiumEntitlements {
        val raw = uid?.let { defaults.stringForKey(keyFor(it)) } ?: return PremiumEntitlements.Empty
        val p = runCatching { json.decodeFromString<Persisted>(raw) }.getOrNull()
            ?: return PremiumEntitlements.Empty
        return PremiumEntitlements(
            userId = p.userId,
            features = p.featureKeys.mapNotNull { PremiumFeature.fromKey(it) }.toSet(),
            issuedAtMillis = p.issuedAtMillis,
            expiresAtMillis = p.expiresAtMillis,
            source = EntitlementSource.entries.firstOrNull { it.name == p.source } ?: EntitlementSource.None,
            productId = p.productId,
            purchaseToken = p.purchaseToken,
        )
    }

    override suspend fun save(entitlements: PremiumEntitlements) {
        val uid = authGateway.uid ?: return
        val persisted = Persisted(
            userId = entitlements.userId,
            featureKeys = entitlements.features.map { it.key },
            issuedAtMillis = entitlements.issuedAtMillis,
            expiresAtMillis = entitlements.expiresAtMillis,
            source = entitlements.source.name,
            productId = entitlements.productId,
            purchaseToken = entitlements.purchaseToken,
        )
        defaults.setObject(json.encodeToString(persisted), keyFor(uid))
        reload.value += 1
    }

    override suspend fun clear() {
        val uid = authGateway.uid ?: return
        defaults.removeObjectForKey(keyFor(uid))
        reload.value += 1
    }

    private fun keyFor(uid: String) = "$KEY_PREFIX$uid"

    private companion object {
        const val KEY_PREFIX = "premium_entitlements_"
    }
}
