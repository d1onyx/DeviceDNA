package com.devstdvad.devicedna.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDisplayInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetSystemInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(
    private val context: Context,
    private val getDevice: GetDeviceInfoUseCase,
    private val getCpu: GetCpuInfoUseCase,
    private val getSystem: GetSystemInfoUseCase,
    private val getStorage: GetStorageInfoUseCase,
    private val getNetwork: GetNetworkInfoUseCase,
    private val getDisplay: GetDisplayInfoUseCase,
    private val observeBattery: ObserveBatteryUseCase,
) {

    suspend fun export(format: ExportFormat): Result<Uri> = runCatching {
        val data = collectData()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val ext = format.name.lowercase()
        val file = File(context.cacheDir, "DeviceDNA_$timestamp.$ext")

        withContext(Dispatchers.IO) {
            file.writeText(
                when (format) {
                    ExportFormat.Json -> renderJson(data)
                    ExportFormat.Csv -> renderCsv(data)
                    ExportFormat.Txt -> renderTxt(data)
                },
            )
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildShareIntent(uri: Uri, format: ExportFormat): Intent {
        val mime = when (format) {
            ExportFormat.Json -> "application/json"
            ExportFormat.Csv -> "text/csv"
            ExportFormat.Txt -> "text/plain"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DeviceDNA Diagnostics Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private suspend fun collectData(): Map<String, Map<String, String>> = coroutineScope {
        val deviceDef = async { (getDevice() as? AppResult.Success)?.value }
        val cpuDef = async { (getCpu() as? AppResult.Success)?.value }
        val systemDef = async { (getSystem() as? AppResult.Success)?.value }
        val storageDef = async { (getStorage() as? AppResult.Success)?.value }
        val networkDef = async { (getNetwork() as? AppResult.Success)?.value }
        val displayDef = async { (getDisplay() as? AppResult.Success)?.value }
        val batteryDef = async { (observeBattery().first() as? AppResult.Success)?.value }

        val device = deviceDef.await()
        val cpu = cpuDef.await()
        val system = systemDef.await()
        val storage = storageDef.await()
        val network = networkDef.await()
        val display = displayDef.await()
        val battery = batteryDef.await()

        buildMap {
            put("device", buildMap {
                device?.let {
                    put("manufacturer", it.manufacturer)
                    put("model", it.model)
                    put("brand", it.brand)
                    put("hardware", it.hardware)
                    put("board", it.board)
                    put("soc", it.socName.ifBlank { "n/a" })
                    put("abis", it.supportedAbis.joinToString(", "))
                    put("rooted", it.isRooted.toString())
                    put("emulator", it.isEmulator.toString())
                    put("developer_options", it.isDeveloperOptionsEnabled.toString())
                    put("adb_enabled", it.isAdbEnabled.toString())
                    put("build_tags", it.buildTags)
                    put("test_keys", it.isTestKeysBuild.toString())
                    put("debuggable_build", it.isDebuggableBuild.toString())
                    put("verified_boot_state", it.verifiedBootState)
                    put("vbmeta_state", it.vbMetaDeviceState)
                    put("flash_locked", it.flashLocked)
                    put("verity_mode", it.verityMode)
                    put("warranty_bit", it.warrantyBit)
                    put("suspicious_root_paths", it.suspiciousRootPaths.joinToString(", "))
                }
            })
            put("cpu", buildMap {
                cpu?.let {
                    put("chipset", it.chipsetName)
                    put("architecture", it.architecture)
                    put("cores", it.coreCount.toString())
                    put("min_freq_mhz", it.minFreqMhz.toString())
                    put("max_freq_mhz", it.maxFreqMhz.toString())
                    put("usage_percent", it.usagePercent?.let { u -> "%.1f".format(u) } ?: "n/a")
                    put("governor", it.governor)
                    put("gpu_renderer", it.gpu.renderer)
                    put("gpu_vendor", it.gpu.vendor)
                }
            })
            put("system", buildMap {
                system?.let {
                    put("android_version", it.androidVersion)
                    put("api_level", it.apiLevel.toString())
                    put("security_patch", it.securityPatchLevel)
                    put("build_number", it.buildNumber)
                    put("kernel", it.kernelVersion)
                    put("encrypted", it.isEncrypted.toString())
                    put("uptime_hours", "%.1f".format(it.uptimeMillis / 3_600_000f))
                    put("package_name", it.packageName)
                    put("app_version", "${it.appVersionName} (${it.appVersionCode})")
                    put("app_debuggable", it.isAppDebuggable.toString())
                    put("installer", it.installerPackageName ?: "n/a")
                    put("known_store", it.isInstalledFromKnownStore.toString())
                    put("signing_sha256", it.signingCertificateSha256 ?: "n/a")
                }
            })
            put("storage", buildMap {
                storage?.let {
                    put("total_bytes", it.totalBytes.toString())
                    put("used_bytes", it.usedBytes.toString())
                    put("free_bytes", it.freeBytes.toString())
                    put("used_percent", "%.1f".format(it.usedPercent * 100f))
                }
            })
            put("network", buildMap {
                network?.let {
                    put("type", it.connectionType.name)
                    put("ssid", it.ssid ?: "n/a")
                    put("link_speed_mbps", it.linkSpeedMbps?.toString() ?: "n/a")
                    put("ip_v4", it.localIpv4 ?: "n/a")
                    put("wifi_standard", it.wifiStandard ?: "n/a")
                    put("vpn_active", it.isVpnActive.toString())
                    put("validated_internet", it.isValidatedInternet.toString())
                    put("captive_portal", it.isCaptivePortal.toString())
                    put("transports", it.activeTransports.joinToString(", "))
                    put("private_dns", it.privateDnsServerName ?: "n/a")
                    put("http_proxy", it.httpProxyHost?.let { host -> "$host:${it.httpProxyPort ?: 0}" } ?: "n/a")
                }
            })
            put("display", buildMap {
                display?.let {
                    put("width_px", it.widthPx.toString())
                    put("height_px", it.heightPx.toString())
                    put("density_dpi", it.densityDpi.toString())
                    put("refresh_rate_hz", it.refreshRateHz.toString())
                    put("display_type", it.displayType)
                    put("hdr", it.hdrCapabilities.joinToString(", ").ifBlank { "none" })
                    put("wide_color_gamut", it.isWideColorGamut.toString())
                }
            })
            put("battery", buildMap {
                battery?.let {
                    put("level_percent", it.levelPercent.toString())
                    put("status", it.status.name)
                    put("health", it.health.name)
                    put("temperature_celsius", "%.1f".format(it.temperatureCelsius))
                    put("voltage_mv", it.voltageMv.toString())
                    it.estimatedWatts?.let { watts -> put("estimated_watts", "%.2f".format(watts)) }
                    it.chargeTimeRemainingMs?.let { remaining -> put("charge_time_remaining_ms", remaining.toString()) }
                    put("power_saver", it.isPowerSaveMode.toString())
                    put("technology", it.technology)
                    it.capacityMah?.let { c -> put("capacity_mah", c.toString()) }
                    it.chargeCycles?.let { c -> put("charge_cycles", c.toString()) }
                }
            })
        }
    }

    private fun renderJson(data: Map<String, Map<String, String>>): String {
        val sb = StringBuilder("{\n")
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        sb.append("  \"exported_at\": \"$timestamp\",\n")
        data.entries.forEachIndexed { sectionIdx, (section, fields) ->
            sb.append("  \"$section\": {\n")
            fields.entries.forEachIndexed { i, (k, v) ->
                val comma = if (i < fields.size - 1) "," else ""
                val safeV = v.replace("\\", "\\\\").replace("\"", "\\\"")
                val jsonVal = if (v == "n/a") "null" else "\"$safeV\""
                sb.append("    \"$k\": $jsonVal$comma\n")
            }
            val comma = if (sectionIdx < data.size - 1) "," else ""
            sb.append("  }$comma\n")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun renderCsv(data: Map<String, Map<String, String>>): String {
        val sb = StringBuilder("section,key,value\n")
        data.forEach { (section, fields) ->
            fields.forEach { (k, v) ->
                sb.append("$section,$k,\"${v.replace("\"", "\"\"")}\"\n")
            }
        }
        return sb.toString()
    }

    private fun renderTxt(data: Map<String, Map<String, String>>): String {
        val sb = StringBuilder("DeviceDNA Diagnostics Report\n")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sb.append("Generated: $timestamp\n")
        sb.append("=".repeat(40)).append("\n\n")
        data.forEach { (section, fields) ->
            sb.append("[${section.uppercase()}]\n")
            fields.forEach { (k, v) ->
                sb.append("  ${k.padEnd(22)} $v\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
