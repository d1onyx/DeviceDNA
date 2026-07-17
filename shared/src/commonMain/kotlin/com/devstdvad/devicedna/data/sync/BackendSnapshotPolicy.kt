package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.domain.model.DeviceSnapshot

/** Applies platform privacy rules immediately before a diagnostics snapshot leaves the device. */
internal expect fun DeviceSnapshot.forBackendSync(): DeviceSnapshot
internal expect val includeDiskDerivedBackendData: Boolean
