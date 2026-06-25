package com.devstdvad.devicedna.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CpuCluster
import com.devstdvad.devicedna.domain.model.CpuCore
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.GpuInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

class AndroidCpuDataSource(private val context: Context) {

    private data class ProcStatSample(val work: Long, val total: Long)

    suspend fun getCpuInfo(): AppResult<CpuInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val coreCount = Runtime.getRuntime().availableProcessors()
            val cores = (0 until coreCount).map { buildCore(it) }
            val allMaxFreqs = cores.map { it.maxFrequencyKhz }
            val minFreqMhz = (cores.minOfOrNull { it.minFrequencyKhz } ?: 0L).div(1000).toInt()
            val maxFreqMhz = (allMaxFreqs.maxOrNull() ?: 0L).div(1000).toInt()

            val usagePercent = measureCpuUsage()
            val instructionSets = buildInstructionSets()
            val processCount = getProcessCount()
            val chipset = detectChipset()

            CpuInfo(
                chipsetName = chipset,
                architecture = Build.SUPPORTED_ABIS.firstOrNull()?.substringBefore("-") ?: "Unknown",
                coreCount = coreCount,
                cores = cores,
                clusters = buildClusters(cores),
                governor = readCpuGovernor(),
                gpu = detectGpu(),
                usagePercent = usagePercent,
                instructionSets = instructionSets,
                processCount = processCount,
                minFreqMhz = minFreqMhz,
                maxFreqMhz = maxFreqMhz,
            )
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Failed to read CPU info")) },
        )
    }

    private fun buildCore(index: Int): CpuCore {
        val basePath = "/sys/devices/system/cpu/cpu$index/cpufreq"
        val current = readLongFile("$basePath/scaling_cur_freq")
        val min = readLongFile("$basePath/cpuinfo_min_freq") ?: 0L
        val max = readLongFile("$basePath/cpuinfo_max_freq") ?: 2_000_000L
        val online = File("/sys/devices/system/cpu/cpu$index/online").run {
            !exists() || readText().trim() == "1"
        }
        return CpuCore(index, current, min, max, online)
    }

    private fun buildClusters(cores: List<CpuCore>): List<CpuCluster> {
        val entries = cores.groupBy { it.maxFrequencyKhz }.entries.sortedBy { it.key }
        val lastIndex = entries.size - 1
        return entries.mapIndexed { i, (_, clusterCores) ->
            val label = when {
                i == 0 -> "Efficiency"
                i == lastIndex -> "Prime"
                else -> "Performance"
            }
            CpuCluster(
                name = "$label (${clusterCores.size} cores)",
                coreIndices = clusterCores.map { it.index },
                minFrequencyMhz = (clusterCores.first().minFrequencyKhz / 1000).toInt(),
                maxFrequencyMhz = (clusterCores.first().maxFrequencyKhz / 1000).toInt(),
            )
        }
    }

    private fun readCpuGovernor(): String = runCatching {
        File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
    }.getOrDefault("Unknown")

    private fun detectChipset(): String = runCatching {
        // Try /proc/cpuinfo Hardware field first
        val hardware = File("/proc/cpuinfo").readLines()
            .firstOrNull { it.startsWith("Hardware") }
            ?.substringAfter(":")?.trim()
            ?.takeUnless { it.isBlank() || it == "0" }

        // Try SoC model (Android 12+)
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeUnless { it.isBlank() || it == Build.UNKNOWN }
        } else null

        socModel ?: hardware ?: Build.HARDWARE
    }.getOrDefault(Build.HARDWARE)

    private fun detectGpu(): GpuInfo {
        // Read renderer from /proc/cpuinfo or system properties as fallback
        val renderer = runCatching {
            val lines = File("/proc/cpuinfo").readLines()
            val hardware = lines.firstOrNull { it.startsWith("Hardware") }
                ?.substringAfter(":")?.trim() ?: ""
            when {
                "snapdragon" in hardware.lowercase() || "qcom" in Build.HARDWARE.lowercase() -> "Adreno GPU"
                "mali" in hardware.lowercase() || "exynos" in hardware.lowercase() -> "Mali GPU"
                "mediatek" in hardware.lowercase() || "mt" in Build.HARDWARE.lowercase() -> "Mali / IMG GPU"
                "tensor" in hardware.lowercase() -> "Pixel GPU"
                "apple" in hardware.lowercase() -> "Apple GPU"
                else -> detectGpuFromBrand()
            }
        }.getOrDefault(detectGpuFromBrand())

        return GpuInfo(
            renderer = renderer,
            vendor = Build.MANUFACTURER,
            version = "OpenGL ES 3.2",
        )
    }

    private fun detectGpuFromBrand(): String = when (Build.MANUFACTURER.lowercase()) {
        "samsung" -> "Adreno / Mali GPU"
        "google" -> "Pixel GPU"
        "oneplus", "oppo", "realme", "vivo" -> "Adreno GPU"
        "xiaomi", "poco" -> "Adreno / Mali GPU"
        else -> "GPU"
    }

    private fun buildInstructionSets(): List<String> {
        val sets = mutableListOf<String>()
        Build.SUPPORTED_ABIS.forEach { abi ->
            when {
                abi.contains("arm64") && "AArch64" !in sets -> sets.add("AArch64")
                abi.contains("armeabi-v7a") && "ARM" !in sets -> sets.add("ARM")
                abi.contains("x86_64") && "x86-64" !in sets -> sets.add("x86-64")
                abi.contains("x86") && !abi.contains("64") && "x86" !in sets -> sets.add("x86")
            }
        }
        // Check for NEON support
        runCatching {
            if (File("/proc/cpuinfo").readText().contains("neon", ignoreCase = true)) {
                sets.add("NEON")
            }
        }
        return sets
    }

    private suspend fun measureCpuUsage(): Float? = runCatching {
        val s1 = readProcStatSample() ?: return null
        delay(250)
        val s2 = readProcStatSample() ?: return null
        val deltaWork = s2.work - s1.work
        val deltaTotal = s2.total - s1.total
        if (deltaTotal <= 0) null else (deltaWork.toFloat() / deltaTotal * 100f).coerceIn(0f, 100f)
    }.getOrNull()

    private fun readProcStatSample(): ProcStatSample? = runCatching {
        val line = File("/proc/stat").readLines().firstOrNull { it.startsWith("cpu ") } ?: return null
        val vals = line.trim().split("\\s+".toRegex()).drop(1).map { it.toLongOrNull() ?: 0L }
        val user = vals.getOrElse(0) { 0L }
        val nice = vals.getOrElse(1) { 0L }
        val system = vals.getOrElse(2) { 0L }
        val idle = vals.getOrElse(3) { 0L }
        val iowait = vals.getOrElse(4) { 0L }
        val irq = vals.getOrElse(5) { 0L }
        val softirq = vals.getOrElse(6) { 0L }
        val steal = vals.getOrElse(7) { 0L }
        val work = user + nice + system + irq + softirq + steal
        val total = work + idle + iowait
        ProcStatSample(work, total)
    }.getOrNull()

    private fun getProcessCount(): Int? = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses?.size
    }.getOrNull()

    private fun readLongFile(path: String): Long? = runCatching {
        File(path).readText().trim().toLong()
    }.getOrNull()
}
