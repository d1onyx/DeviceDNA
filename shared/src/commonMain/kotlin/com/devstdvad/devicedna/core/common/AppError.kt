package com.devstdvad.devicedna.core.common

sealed class AppError(open val message: String) {
    data class PermissionDenied(val permission: String) : AppError("Permission denied: $permission")
    data class PlatformRestricted(override val message: String = "Restricted by platform") : AppError(message)
    data class Unavailable(override val message: String = "Unavailable on this device") : AppError(message)
    data class IoError(override val message: String) : AppError(message)
    data class Unknown(override val message: String = "Unknown error") : AppError(message)
}
