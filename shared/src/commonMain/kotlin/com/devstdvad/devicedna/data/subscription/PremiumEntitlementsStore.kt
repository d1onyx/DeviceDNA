package com.devstdvad.devicedna.data.subscription

import com.devstdvad.devicedna.data.account.ClearableStore
import kotlinx.coroutines.flow.Flow

interface PremiumEntitlementsStore : ClearableStore {
    val entitlements: Flow<PremiumEntitlements>
    suspend fun save(entitlements: PremiumEntitlements)
}
