package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.domain.model.DeviceSnapshot

/**
 * Builds a full [DeviceSnapshot] from the platform data sources. Implemented per platform
 * (Android aggregates the AndroidXxxDataSources); lets the shared DeviceSyncManager stay
 * platform-agnostic.
 */
interface DeviceSnapshotProvider {
    suspend fun build(): DeviceSnapshot
}
