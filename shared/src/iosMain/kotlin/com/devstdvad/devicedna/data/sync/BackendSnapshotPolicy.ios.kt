package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.domain.model.DeviceSnapshot

/**
 * Apple Required Reason 85F4.1 permits disk-space APIs only for an on-device display.
 * Keep sync enabled, but never transmit storage or the health score derived from storage.
 */
internal actual fun DeviceSnapshot.forBackendSync(): DeviceSnapshot = copy(
    storage = null,
    health = null,
)

internal actual val includeDiskDerivedBackendData: Boolean = false
