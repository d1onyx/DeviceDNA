package com.devstdvad.devicedna.data.subscription

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.subscriptionDataStore by preferencesDataStore("device_dna_subscription")

class SubscriptionStore(
    private val context: Context,
) : PremiumEntitlementsStore {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val entitlements: Flow<PremiumEntitlements> = context.subscriptionDataStore.data.map { prefs ->
        val encrypted = prefs[ENCRYPTED_ENTITLEMENTS] ?: return@map PremiumEntitlements.Empty
        runCatching {
            json.decodeFromString<StoredEntitlements>(decrypt(encrypted)).toEntitlements()
        }.getOrDefault(PremiumEntitlements.Empty)
    }

    override suspend fun save(entitlements: PremiumEntitlements) {
        context.subscriptionDataStore.edit { prefs ->
            prefs.clear()
            prefs[ENCRYPTED_ENTITLEMENTS] = encrypt(json.encodeToString(entitlements.toStored()))
        }
    }

    override suspend fun clear() {
        context.subscriptionDataStore.edit { it.clear() }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.encodeToByteArray())
        val payload = ByteArray(1 + iv.size + cipherText.size)
        payload[0] = iv.size.toByte()
        iv.copyInto(payload, destinationOffset = 1)
        cipherText.copyInto(payload, destinationOffset = 1 + iv.size)

        return "$PAYLOAD_PREFIX${Base64.encodeToString(payload, Base64.NO_WRAP)}"
    }

    private fun decrypt(encrypted: String): String {
        require(encrypted.startsWith(PAYLOAD_PREFIX)) { "Unsupported subscription payload." }
        val payload = Base64.decode(encrypted.removePrefix(PAYLOAD_PREFIX), Base64.NO_WRAP)
        val ivSize = payload.firstOrNull()?.toInt() ?: error("Missing IV.")
        require(ivSize > 0 && payload.size > 1 + ivSize) { "Invalid subscription payload." }
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val cipherText = payload.copyOfRange(1 + ivSize, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))

        return cipher.doFinal(cipherText).decodeToString()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)

        return generator.generateKey()
    }

    private fun PremiumEntitlements.toStored(): StoredEntitlements =
        StoredEntitlements(
            userId = userId,
            features = features.map { it.key }.toSet(),
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            source = source.name,
            productId = productId,
        )

    private fun StoredEntitlements.toEntitlements(): PremiumEntitlements {
        val source = enumValues<EntitlementSource>().firstOrNull { it.name == source } ?: EntitlementSource.None
        val storedFeatures = features.mapNotNull(PremiumFeature::fromKey).toSet()
        val resolvedFeatures = if (source == EntitlementSource.Dev && storedFeatures.isNotEmpty()) {
            PremiumFeature.entries.toSet()
        } else {
            storedFeatures
        }

        return PremiumEntitlements(
            userId = userId,
            features = resolvedFeatures,
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = expiresAtMillis,
            source = source,
            productId = productId,
            purchaseToken = null,
        )
    }

    @Serializable
    private data class StoredEntitlements(
        val userId: String? = null,
        val features: Set<String> = emptySet(),
        val issuedAtMillis: Long = 0L,
        val expiresAtMillis: Long? = null,
        val source: String = EntitlementSource.None.name,
        val productId: String? = null,
    )

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "device_dna_subscription_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PAYLOAD_PREFIX = "v1:"
        val ENCRYPTED_ENTITLEMENTS = stringPreferencesKey("encrypted_entitlements")
    }
}
