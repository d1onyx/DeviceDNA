package com.devstdvad.devicedna.data.sync.model

import com.devstdvad.devicedna.domain.model.DeviceSnapshot
import kotlinx.serialization.Serializable

/** GET /v1/me response. */
@Serializable
data class CurrentAccount(
    val exists: Boolean,
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val premium: Boolean = false,
    val subscription: BackendSubscription? = null,
)

@Serializable
data class BackendSubscription(
    val status: String,
    val provider: String,
    val productId: String? = null,
    val originalTransactionId: String? = null,
    val latestTransactionId: String? = null,
    val expiresAt: String? = null,
    val updatedAt: String,
)

@Serializable
data class SubscriptionViewResponse(
    val premium: Boolean,
    val subscription: BackendSubscription? = null,
)

@Serializable
data class GooglePlaySubscriptionVerificationPayload(
    val productId: String,
    val purchaseToken: String,
)

/** POST /v1/sync body. User data is taken by the backend from the ID token, not from here. */
@Serializable
data class DeviceSyncPayload(
    val androidId: String,
    val deviceName: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val snapshotHash: String,
    val snapshot: DeviceSnapshot,
)

/** GET /v1/devices/:androidId/status response. */
@Serializable
data class DeviceSyncStatus(
    val exists: Boolean,
    val snapshotHash: String? = null,
    val lastSyncedAt: String? = null,
)

/** POST /v1/sync response. */
@Serializable
data class SyncResult(
    val synced: Boolean,
    val lastSyncedAt: String? = null,
)
