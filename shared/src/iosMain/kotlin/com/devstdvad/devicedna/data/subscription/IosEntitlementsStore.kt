package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS [PremiumEntitlementsStore] persisted in NSUserDefaults as JSON. This is a cache only —
 * the authoritative entitlement source on iOS is StoreKit's Transaction.currentEntitlements,
 * which the Swift billing bridge re-pushes on launch and on every transaction update, so a
 * tampered cache cannot grant premium (App Store review-safe without Keychain complexity).
 */
class IosEntitlementsStore(
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
    private val state = MutableStateFlow(load())

    override val entitlements: Flow<PremiumEntitlements> = state

    private fun load(): PremiumEntitlements {
        val raw = defaults.stringForKey(KEY) ?: return PremiumEntitlements.Empty
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
        val persisted = Persisted(
            userId = entitlements.userId,
            featureKeys = entitlements.features.map { it.key },
            issuedAtMillis = entitlements.issuedAtMillis,
            expiresAtMillis = entitlements.expiresAtMillis,
            source = entitlements.source.name,
            productId = entitlements.productId,
            purchaseToken = entitlements.purchaseToken,
        )
        defaults.setObject(json.encodeToString(persisted), KEY)
        state.value = entitlements
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(KEY)
        state.value = PremiumEntitlements.Empty
    }

    private companion object {
        const val KEY = "premium_entitlements"
    }
}
