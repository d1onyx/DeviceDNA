package com.devstdvad.devicedna.data.subscription

data class PremiumEntitlements(
    val userId: String? = null,
    val features: Set<PremiumFeature> = emptySet(),
    val issuedAtMillis: Long = 0L,
    val expiresAtMillis: Long? = null,
    val source: EntitlementSource = EntitlementSource.None,
) {
    val isActive: Boolean
        get() = features.isNotEmpty() && !isExpired()

    fun hasFeature(feature: PremiumFeature, nowMillis: Long = System.currentTimeMillis()): Boolean =
        feature in features && !isExpired(nowMillis)

    fun isExpired(nowMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtMillis?.let { it <= nowMillis } ?: false

    companion object {
        val Empty = PremiumEntitlements()
    }
}

enum class EntitlementSource {
    None,
    Dev,
    Backend,
    Play,
}
