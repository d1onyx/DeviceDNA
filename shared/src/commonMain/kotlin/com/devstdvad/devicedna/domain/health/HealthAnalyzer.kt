package com.devstdvad.devicedna.domain.health

import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.FraudRiskLevel
import com.devstdvad.devicedna.domain.model.FraudRiskScore
import com.devstdvad.devicedna.domain.model.FraudSignal
import com.devstdvad.devicedna.domain.model.FraudSignalSeverity
import com.devstdvad.devicedna.domain.model.HealthInsight
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.InsightSeverity
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.RecommendedAction
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.SystemInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.domain.repository.HealthRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Rule-based health scoring. Lives in shared so Android and iOS use identical logic.
 */
class HealthAnalyzer : HealthRepository {

    override suspend fun getHealthScore(
        battery: BatteryInfo?,
        ram: RamInfo?,
        storage: StorageInfo?,
        thermal: ThermalInfo?,
        device: DeviceInfo?,
        system: SystemInfo?,
        network: NetworkInfo?,
    ): HealthScore {
        val insights = mutableListOf<HealthInsight>()

        val batteryScore  = scoreBattery(battery, insights)
        val perfScore     = scorePerformance(ram, insights)
        val storageScore  = scoreStorage(storage, insights)
        val thermalScore  = scoreThermal(thermal, insights)
        val securityScore = scoreSecurity(device, system, insights)
        val fraudRisk = scoreFraudRisk(device, system, network, insights)

        val overall = (batteryScore * 0.25f + perfScore * 0.20f + storageScore * 0.20f +
            thermalScore * 0.20f + securityScore * 0.15f).toInt()

        if (insights.isEmpty()) {
            insights += HealthInsight(
                id = "ok",
                title = "Device looks healthy",
                summary = "No significant issues detected. Your device is running well.",
                severity = InsightSeverity.Good,
                confidence = 0.9f,
                actions = emptyList(),
            )
        }

        return HealthScore(
            overall = overall,
            battery = batteryScore,
            performance = perfScore,
            storage = storageScore,
            security = securityScore,
            thermal = thermalScore,
            insights = insights,
            fraudRisk = fraudRisk,
        )
    }

    // ── Subscores ──────────────────────────────────────────────────────────

    private fun scoreBattery(battery: BatteryInfo?, insights: MutableList<HealthInsight>): Int {
        if (battery == null) return 70
        var score = 100

        if (battery.temperatureCelsius >= 45f) {
            score -= if (battery.temperatureCelsius >= 55f) 30 else 20
            insights += HealthInsight(
                id = "bat_hot",
                title = "Battery temperature is high",
                summary = "Battery at ${Formatters.formatCelsius(battery.temperatureCelsius)}. Sustained heat degrades capacity and may indicate a charging issue.",
                severity = if (battery.temperatureCelsius >= 55f) InsightSeverity.Critical else InsightSeverity.Warning,
                confidence = 0.95f,
                actions = listOf(
                    RecommendedAction("Remove case while charging", "Cases trap heat during charging."),
                    RecommendedAction("Avoid using while charging", "Reduces combined thermal load."),
                ),
            )
        } else if (battery.temperatureCelsius >= 38f) {
            score -= 10
        }

        if (battery.health == BatteryHealth.Overheat || battery.health == BatteryHealth.Dead) {
            score -= 25
            insights += HealthInsight(
                id = "bat_health",
                title = "Battery health warning",
                summary = "Battery health reported as ${battery.health.name}. Consider servicing your device.",
                severity = InsightSeverity.Warning,
                confidence = 0.85f,
                actions = listOf(RecommendedAction("Check battery usage", "Identify apps causing excess drain.")),
            )
        }

        if (battery.levelPercent <= 15 && battery.status == BatteryStatus.Discharging) {
            score -= 15
        }

        return score.coerceIn(0, 100)
    }

    private fun scorePerformance(ram: RamInfo?, insights: MutableList<HealthInsight>): Int {
        if (ram == null) return 70
        var score = 100

        if (ram.usedPercent >= 0.90f) {
            score -= 30
            insights += HealthInsight(
                id = "ram_high",
                title = "RAM usage is very high",
                summary = "${Formatters.formatPercent(ram.usedPercent)} used (${Formatters.formatBytes(ram.usedBytes)} / ${Formatters.formatBytes(ram.totalBytes)}). This may cause slowdowns and app crashes.",
                severity = InsightSeverity.Warning,
                confidence = 0.9f,
                actions = listOf(RecommendedAction("Close background apps", "Free up memory by closing unused apps.")),
            )
        } else if (ram.usedPercent >= 0.75f) {
            score -= 15
        }

        if (ram.isLowMemory) {
            score -= 20
            insights += HealthInsight(
                id = "low_mem",
                title = "System low memory warning",
                summary = "OS flagged a low memory condition. Background apps will be terminated aggressively.",
                severity = InsightSeverity.Critical,
                confidence = 0.99f,
                actions = listOf(RecommendedAction("Restart device", "Clears memory and restores normal operation.")),
            )
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreStorage(storage: StorageInfo?, insights: MutableList<HealthInsight>): Int {
        if (storage == null) return 70
        var score = 100
        val pct = (storage.usedPercent * 100).toInt()

        if (storage.usedPercent >= 0.95f) {
            score -= 40
            insights += HealthInsight(
                id = "storage_full",
                title = "Storage almost full",
                summary = "$pct% used. A full device blocks updates, camera captures, and data sync.",
                severity = InsightSeverity.Critical,
                confidence = 0.99f,
                actions = listOf(
                    RecommendedAction("Delete unused apps", "Remove apps you no longer use."),
                    RecommendedAction("Clear cache", "Settings → Storage → Cached Data"),
                ),
            )
        } else if (storage.usedPercent >= 0.85f) {
            score -= 20
            insights += HealthInsight(
                id = "storage_warn",
                title = "Storage is getting full",
                summary = "$pct% used. Consider freeing space soon.",
                severity = InsightSeverity.Warning,
                confidence = 0.95f,
                actions = listOf(RecommendedAction("Review photos and videos", "Transfer to cloud or computer.")),
            )
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreThermal(thermal: ThermalInfo?, insights: MutableList<HealthInsight>): Int {
        if (thermal == null) return 70
        var score = 100

        val maxCpuTemp = thermal.zones
            .filter { it.type == ThermalZoneType.Cpu }
            .mapNotNull { it.temperatureCelsius }
            .maxOrNull()

        val coarseState = thermal.zones.firstOrNull { it.type == ThermalZoneType.Cpu }
            ?.name
            ?.lowercase()

        if (maxCpuTemp != null && maxCpuTemp >= 80f) {
            score -= 35
            insights += HealthInsight(
                id = "cpu_thermal",
                title = "CPU is running hot",
                summary = "CPU at ${Formatters.formatCelsius(maxCpuTemp)}. Thermal throttling is likely reducing performance.",
                severity = InsightSeverity.Warning,
                confidence = 0.9f,
                actions = listOf(RecommendedAction("Stop intensive apps", "Gaming or crypto apps cause high CPU temps.")),
            )
        } else if (coarseState == "serious" || coarseState == "critical") {
            score -= if (coarseState == "critical") 35 else 20
            insights += HealthInsight(
                id = "system_thermal",
                title = "System thermal pressure is ${coarseState}",
                summary = "The operating system reports elevated thermal pressure. Numeric component temperatures are not exposed on this platform.",
                severity = if (coarseState == "critical") InsightSeverity.Critical else InsightSeverity.Warning,
                confidence = 0.95f,
                actions = listOf(RecommendedAction("Let the device cool", "Stop intensive work and move the device away from direct heat.")),
            )
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreSecurity(
        device: DeviceInfo?,
        system: SystemInfo?,
        insights: MutableList<HealthInsight>,
    ): Int {
        var score = 100

        if (device?.isRooted == true) {
            score -= 30
            insights += HealthInsight(
                id = "rooted",
                title = "Device appears to be rooted/jailbroken",
                summary = "Elevated privileges bypass OS security protections. Banking apps may refuse to run.",
                severity = InsightSeverity.Warning,
                confidence = 0.8f,
                actions = listOf(RecommendedAction("Review root/jailbreak usage", "Ensure only trusted apps have elevated access.")),
            )
        }

        if (system != null) {
            val monthsOld = runCatching {
                val patchDate = LocalDate.parse(system.securityPatchLevel)
                val today = Instant.fromEpochMilliseconds(currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
                val months = (today.year - patchDate.year) * 12L + (today.monthNumber - patchDate.monthNumber)
                months.coerceAtLeast(0L)
            }.getOrNull()

            if (monthsOld != null && monthsOld >= 6L) {
                score -= 20
                insights += HealthInsight(
                    id = "sec_patch",
                    title = "Security patch is outdated",
                    summary = "Last patch: ${system.securityPatchLevel} (${monthsOld} months ago). Your device may be missing critical security fixes.",
                    severity = if (monthsOld >= 12L) InsightSeverity.Critical else InsightSeverity.Warning,
                    confidence = 0.95f,
                    actions = listOf(RecommendedAction("Check for system updates", "Settings → System → Software update")),
                )
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreFraudRisk(
        device: DeviceInfo?,
        system: SystemInfo?,
        network: NetworkInfo?,
        insights: MutableList<HealthInsight>,
    ): FraudRiskScore {
        val signals = mutableListOf<FraudSignal>()

        fun add(
            id: String,
            label: String,
            severity: FraudSignalSeverity,
            evidence: String,
        ) {
            signals += FraudSignal(id, label, severity, evidence)
        }

        if (device?.isEmulator == true) {
            add("emulator", "Emulator environment", FraudSignalSeverity.Critical, "Build/model/hardware match emulator heuristics")
        }
        if (device?.isRooted == true) {
            add("root", "Root access indicators", FraudSignalSeverity.Critical, "Root binaries or superuser paths detected")
        }
        if (device?.suspiciousRootPaths?.isNotEmpty() == true) {
            add("root_paths", "Suspicious root paths", FraudSignalSeverity.High, device.suspiciousRootPaths.joinToString(", "))
        }
        if (device?.isTestKeysBuild == true) {
            add("test_keys", "Test-keys build", FraudSignalSeverity.High, "Build tags: ${device.buildTags}")
        }
        if (device?.isDebuggableBuild == true) {
            add("debuggable_os", "Debuggable OS build", FraudSignalSeverity.High, "ro.debuggable or build type indicates debug environment")
        }
        if (device?.isDeveloperOptionsEnabled == true) {
            add("developer_options", "Developer options enabled", FraudSignalSeverity.Medium, "Development settings are enabled")
        }
        if (device?.isAdbEnabled == true) {
            add("adb_enabled", "ADB enabled", FraudSignalSeverity.High, "USB/Wi-Fi debugging can permit device manipulation")
        }
        if (device?.vbMetaDeviceState.equals("unlocked", ignoreCase = true) ||
            device?.flashLocked == "0" ||
            device?.verifiedBootState.equals("orange", ignoreCase = true)
        ) {
            add("bootloader_unlocked", "Unlocked boot chain", FraudSignalSeverity.Critical, "Verified boot/device state indicates unlocked or modified boot chain")
        }
        if (device?.verifiedBootState.equals("red", ignoreCase = true) ||
            device?.verifiedBootState.equals("yellow", ignoreCase = true)
        ) {
            add("verified_boot_warning", "Verified boot warning", FraudSignalSeverity.High, "Verified boot state: ${device?.verifiedBootState}")
        }
        if (device?.verityMode.equals("eio", ignoreCase = true) ||
            device?.verityMode.equals("disabled", ignoreCase = true)
        ) {
            add("verity_disabled", "dm-verity not enforcing", FraudSignalSeverity.High, "Verity mode: ${device?.verityMode}")
        }
        if (device?.warrantyBit == "1") {
            add("warranty_bit", "Warranty/tamper bit set", FraudSignalSeverity.High, "Vendor tamper flag is set")
        }

        if (system?.isAppDebuggable == true) {
            add("debuggable_app", "Debuggable app package", FraudSignalSeverity.Critical, "Installed app has FLAG_DEBUGGABLE")
        }
        if (system?.supportsInstallSourceInspection == true && !system.isInstalledFromKnownStore) {
            add(
                id = "unknown_installer",
                label = "Unknown installer",
                severity = FraudSignalSeverity.Medium,
                evidence = system.installerPackageName ?: "Installer package unavailable",
            )
        }
        if (system?.supportsAppSignatureInspection == true && system.signingCertificateSha256.isNullOrBlank()) {
            add("missing_signature", "Signature hash unavailable", FraudSignalSeverity.Low, "Could not read signing certificate hash")
        }

        if (network?.isVpnActive == true) {
            add("vpn", "VPN active", FraudSignalSeverity.Medium, "Active transports: ${network.activeTransports.joinToString(", ")}")
        }
        if (network?.isCaptivePortal == true) {
            add("captive_portal", "Captive portal detected", FraudSignalSeverity.Low, "Network capability reports captive portal")
        }
        if (network?.isValidatedInternet == false && network.connectionType.name != "None") {
            add("not_validated", "Network not validated", FraudSignalSeverity.Low, "Android did not validate internet connectivity")
        }
        if (!network?.httpProxyHost.isNullOrBlank()) {
            add("proxy", "HTTP proxy configured", FraudSignalSeverity.Medium, "${network?.httpProxyHost}:${network?.httpProxyPort ?: 0}")
        }

        val score = signals.sumOf {
            when (it.severity) {
                FraudSignalSeverity.Info -> 0
                FraudSignalSeverity.Low -> 5
                FraudSignalSeverity.Medium -> 15
                FraudSignalSeverity.High -> 25
                FraudSignalSeverity.Critical -> 35
            }
        }.coerceIn(0, 100)

        if (score >= 35) {
            insights += HealthInsight(
                id = "fraud_risk",
                title = "Device integrity review recommended",
                summary = "${signals.size} integrity/network signal(s) indicate this environment may be modified, automated, proxied, or running with development settings.",
                severity = if (score >= 70) InsightSeverity.Critical else InsightSeverity.Warning,
                confidence = 0.86f,
                actions = listOf(
                    RecommendedAction("Review integrity signals", "Check boot, root, emulator, app signature, installer, VPN and proxy indicators."),
                    RecommendedAction("Use additional verification", "For sensitive actions, request extra confirmation or manual review."),
                ),
            )
        }

        val level = when {
            score >= 75 -> FraudRiskLevel.Critical
            score >= 50 -> FraudRiskLevel.High
            score >= 20 -> FraudRiskLevel.Medium
            else -> FraudRiskLevel.Low
        }

        return FraudRiskScore(score = score, level = level, signals = signals)
    }
}
