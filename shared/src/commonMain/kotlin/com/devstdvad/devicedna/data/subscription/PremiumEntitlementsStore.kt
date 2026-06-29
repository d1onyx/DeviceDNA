package com.devstdvad.devicedna.data.subscription

import kotlinx.coroutines.flow.Flow

interface PremiumEntitlementsStore {
    val entitlements: Flow<PremiumEntitlements>
    suspend fun save(entitlements: PremiumEntitlements)
    suspend fun clear()
}
