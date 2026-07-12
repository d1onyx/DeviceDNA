package com.devstdvad.devicedna.data.source

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.SystemInfo
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone

class AndroidSystemDataSource(private val context: Context) {

    suspend fun getSystemInfo(): AppResult<SystemInfo> = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        val appInfo = readAppIntegrityInfo()

        SystemInfo(
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatchLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else "Unknown",
            buildNumber = Build.DISPLAY,
            kernelVersion = readKernelVersion(),
            javaVm = "${System.getProperty("java.vm.name") ?: "ART"} ${System.getProperty("java.vm.version") ?: ""}".trim(),
            openGlVersion = readOpenGlVersion(),
            baseband = Build.getRadioVersion() ?: "Unknown",
            bootloader = Build.BOOTLOADER,
            language = Locale.getDefault().displayName,
            timeZone = TimeZone.getDefault().id,
            releaseName = Build.VERSION.CODENAME,
            uptimeMillis = SystemClock.elapsedRealtime(),
            buildType = Build.TYPE,
            runningProcessCount = am.runningAppProcesses?.size ?: 0,
            seLinuxStatus = readSeLinuxStatus(),
            isEncrypted = isDeviceEncrypted(),
            glEsVersion = readGlEsVersion(),
            totalRamGb = totalRamGb,
            isAppDebuggable = appInfo.isDebuggable,
            installerPackageName = appInfo.installerPackageName,
            signingCertificateSha256 = appInfo.signingCertificateSha256,
            appVersionName = appInfo.versionName,
            appVersionCode = appInfo.versionCode,
            packageName = context.packageName,
            isInstalledFromKnownStore = appInfo.isKnownInstaller,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Failed to read system info")) },
    )

    private fun readKernelVersion(): String = runCatching {
        File("/proc/version").readText().trim()
            .substringAfter("version ")
            .substringBefore(" (")
            .take(80)
    }.getOrDefault("Unknown")

    private fun readOpenGlVersion(): String = runCatching {
        val pm = context.packageManager
        val featureInfo = pm.systemAvailableFeatures
        featureInfo?.firstOrNull { it.name == null }?.let { info ->
            val major = (info.reqGlEsVersion shr 16) and 0xFF
            val minor = info.reqGlEsVersion and 0xFF
            "OpenGL ES $major.$minor"
        } ?: "OpenGL ES 3.2"
    }.getOrDefault("OpenGL ES 3.2")

    private fun readSeLinuxStatus(): String = runCatching {
        val process = Runtime.getRuntime().exec("getenforce")
        val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
        process.destroy()
        result.ifBlank { "Unknown" }
    }.getOrDefault("Unknown")

    private fun isDeviceEncrypted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getSystemService(android.app.KeyguardManager::class.java)
                ?.isDeviceSecure == true
        }
        return true
    }

    private fun readGlEsVersion(): String = runCatching {
        val pm = context.packageManager
        val features = pm.systemAvailableFeatures ?: return "Unknown"
        val glInfo = features.firstOrNull { it.name == null } ?: return "Unknown"
        val major = (glInfo.reqGlEsVersion shr 16) and 0xFF
        val minor = glInfo.reqGlEsVersion and 0xFF
        "$major.$minor"
    }.getOrDefault("3.2")

    private data class AppIntegrityInfo(
        val isDebuggable: Boolean,
        val installerPackageName: String?,
        val signingCertificateSha256: String?,
        val versionName: String,
        val versionCode: Long,
        val isKnownInstaller: Boolean,
    )

    private fun readAppIntegrityInfo(): AppIntegrityInfo {
        val pm = context.packageManager
        val packageName = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }
        val applicationInfo = packageInfo.applicationInfo
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { pm.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
        val knownInstallers = setOf(
            "com.android.vending",
            "com.google.android.packageinstaller",
            "com.google.android.apps.nbu.files",
            "com.sec.android.app.samsungapps",
            "com.amazon.venezia",
        )
        return AppIntegrityInfo(
            isDebuggable = (applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            installerPackageName = installer,
            signingCertificateSha256 = readSigningCertificateSha256(packageInfo),
            versionName = packageInfo.versionName ?: "",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            isKnownInstaller = installer in knownInstallers,
        )
    }

    private fun readSigningCertificateSha256(packageInfo: android.content.pm.PackageInfo): String? = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return@runCatching null
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val signature = signatures?.firstOrNull() ?: return@runCatching null
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        digest.joinToString(":") { "%02X".format(it) }
    }.getOrNull()
}
