package com.devstdvad.devicedna.data.sync.model

import com.devstdvad.devicedna.domain.model.DeviceSnapshot
import kotlinx.serialization.Serializable

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
