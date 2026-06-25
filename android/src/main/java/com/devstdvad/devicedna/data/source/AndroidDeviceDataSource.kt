package com.devstdvad.devicedna.data.source

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.DeviceInfo
import java.io.File

class AndroidDeviceDataSource(private val context: Context) {

    @SuppressLint("HardwareIds")
    suspend fun getDeviceInfo(): AppResult<DeviceInfo> = runCatching {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
        val suspiciousRootPaths = findSuspiciousRootPaths()
        val buildTags = Build.TAGS.orEmpty()
        val roDebuggable = readSystemProperty("ro.debuggable")
        DeviceInfo(
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            codename = Build.DEVICE,
            buildFingerprint = Build.FINGERPRINT,
            androidId = androidId,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            isRooted = suspiciousRootPaths.isNotEmpty() || buildTags.contains("test-keys", ignoreCase = true),
            bootloader = Build.BOOTLOADER,
            socName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "N/A (Android 12+)",
            serialNumber = readSerialNumber(),
            isEmulator = detectEmulator(),
            isDeveloperOptionsEnabled = isGlobalSettingEnabled(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            isAdbEnabled = isGlobalSettingEnabled(Settings.Global.ADB_ENABLED),
            buildTags = buildTags,
            buildUser = Build.USER.orEmpty(),
            buildHost = Build.HOST.orEmpty(),
            buildTimeMillis = Build.TIME,
            isTestKeysBuild = buildTags.contains("test-keys", ignoreCase = true),
            isDebuggableBuild = Build.TYPE != "user" || roDebuggable == "1",
            verifiedBootState = readSystemProperty("ro.boot.verifiedbootstate"),
            bootVerifiedState = readSystemProperty("ro.boot.bootstate"),
            vbMetaDeviceState = readSystemProperty("ro.boot.vbmeta.device_state"),
            flashLocked = readSystemProperty("ro.boot.flash.locked"),
            verityMode = readSystemProperty("ro.boot.veritymode"),
            warrantyBit = readSystemProperty("ro.boot.warranty_bit").ifBlank {
                readSystemProperty("ro.boot.efuse")
            },
            firstApiLevel = readSystemProperty("ro.product.first_api_level").toIntOrNull(),
            suspiciousRootPaths = suspiciousRootPaths,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(com.devstdvad.devicedna.core.common.AppError.Unknown(it.message ?: "Failed to read device info")) },
    )

    private fun findSuspiciousRootPaths(): List<String> {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su",
            "/cache/su",
            "/data/su",
            "/dev/su",
            "/sbin/magisk",
            "/debug_ramdisk/magisk",
            "/data/adb/magisk",
            "/data/adb/modules",
        )
        return rootPaths.filter { File(it).exists() }
    }

    private fun readSerialNumber(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            return Build.SERIAL.takeUnless { it.isBlank() || it == Build.UNKNOWN } ?: "Unknown"
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        if (!granted) return "Permission required"
        return runCatching { Build.getSerial() }
            .getOrDefault("Restricted")
            .takeUnless { it.isBlank() || it == Build.UNKNOWN }
            ?: "Restricted"
    }

    private fun isGlobalSettingEnabled(name: String): Boolean = runCatching {
        Settings.Global.getInt(context.contentResolver, name, 0) == 1
    }.getOrDefault(false)

    private fun readSystemProperty(name: String): String = runCatching {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
        val value = process.inputStream.bufferedReader().use { it.readText() }.trim()
        process.destroy()
        value
    }.getOrDefault("")

    private fun detectEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            product.contains("sdk") ||
            product.contains("emulator")
    }
}
