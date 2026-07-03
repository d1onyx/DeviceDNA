package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatchLevel: String,
    val buildNumber: String,
    val kernelVersion: String,
    val javaVm: String,
    val openGlVersion: String,
    val baseband: String,
    val bootloader: String,
    val language: String,
    val timeZone: String,
    val releaseName: String,
    val uptimeMillis: Long = 0L,
    val buildType: String = "",
    val runningProcessCount: Int = 0,
    val seLinuxStatus: String = "",
    val isEncrypted: Boolean = true,
    val glEsVersion: String = "",
    val totalRamGb: Float = 0f,
    val isAppDebuggable: Boolean = false,
    val installerPackageName: String? = null,
    val signingCertificateSha256: String? = null,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val packageName: String = "",
    val isInstalledFromKnownStore: Boolean = false,
    val isPowerSaveMode: Boolean = false,
)
