package com.devstdvad.devicedna.data.subscription

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.subscriptionDataStore by preferencesDataStore("device_dna_subscription")

class SubscriptionStore(
    private val context: Context,
) : PremiumEntitlementsStore {

    override val entitlements: Flow<PremiumEntitlements> = context.subscriptionDataStore.data.map { prefs ->
        val storedFeatures = prefs[FEATURES]
            ?.mapNotNull(PremiumFeature::fromKey)
            ?.toSet()
            .orEmpty()
        val source = prefs[SOURCE]?.toEnumOrDefault(EntitlementSource.None) ?: EntitlementSource.None
        val features = if (source == EntitlementSource.Dev && storedFeatures.isNotEmpty()) {
            PremiumFeature.entries.toSet()
        } else {
            storedFeatures
        }
        PremiumEntitlements(
            userId = prefs[USER_ID],
            features = features,
            issuedAtMillis = prefs[ISSUED_AT_MILLIS] ?: 0L,
            expiresAtMillis = prefs[EXPIRES_AT_MILLIS],
            source = source,
        )
    }

    override suspend fun save(entitlements: PremiumEntitlements) {
        context.subscriptionDataStore.edit { prefs ->
            val userId = entitlements.userId
            val expiresAtMillis = entitlements.expiresAtMillis
            if (userId == null) {
                prefs.remove(USER_ID)
            } else {
                prefs[USER_ID] = userId
            }
            prefs[FEATURES] = entitlements.features.map { it.key }.toSet()
            prefs[ISSUED_AT_MILLIS] = entitlements.issuedAtMillis
            if (expiresAtMillis == null) {
                prefs.remove(EXPIRES_AT_MILLIS)
            } else {
                prefs[EXPIRES_AT_MILLIS] = expiresAtMillis
            }
            prefs[SOURCE] = entitlements.source.name
        }
    }

    override suspend fun clear() {
        context.subscriptionDataStore.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    private companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val FEATURES = stringSetPreferencesKey("features")
        val ISSUED_AT_MILLIS = longPreferencesKey("issued_at_millis")
        val EXPIRES_AT_MILLIS = longPreferencesKey("expires_at_millis")
        val SOURCE = stringPreferencesKey("source")
    }
}
