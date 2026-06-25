package com.devstdvad.devicedna.data.source

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AndroidRamStorageDataSource(private val context: Context) {

    fun observeRam(intervalMs: Long = 3_000): Flow<AppResult<RamInfo>> = flow {
        while (true) { emit(getRamSnapshot()); delay(intervalMs) }
    }.flowOn(Dispatchers.IO)

    fun getRamSnapshot(): AppResult<RamInfo> = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val used = info.totalMem - info.availMem
        val cached = readCachedMemory()
        RamInfo(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            usedBytes = used,
            usedPercent = if (info.totalMem > 0) used.toFloat() / info.totalMem else 0f,
            isLowMemory = info.lowMemory,
            cachedBytes = cached,
            thresholdBytes = info.threshold,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "RAM read failed")) },
    )

    fun getStorageInfo(): AppResult<StorageInfo> = runCatching {
        val stat = StatFs(android.os.Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free

        val extStat = runCatching {
            val extDir = context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.parentFile
            extDir?.let { StatFs(it.path) }
        }.getOrNull()

        StorageInfo(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            usedPercent = if (total > 0) used.toFloat() / total else 0f,
            externalTotalBytes = extStat?.totalBytes ?: 0L,
            externalFreeBytes = extStat?.availableBytes ?: 0L,
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Storage read failed")) },
    )

    private fun readCachedMemory(): Long = runCatching {
        java.io.File("/proc/meminfo").readLines()
            .firstOrNull { it.startsWith("Cached:") }
            ?.split("\\s+".toRegex())
            ?.getOrNull(1)
            ?.toLong()
            ?.times(1024L) ?: 0L
    }.getOrDefault(0L)
}
