package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.domain.model.DeviceSnapshot

internal actual fun DeviceSnapshot.forBackendSync(): DeviceSnapshot = this
internal actual val includeDiskDerivedBackendData: Boolean = true
