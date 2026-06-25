package com.devstdvad.devicedna.data.source

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.AppDetails
import com.devstdvad.devicedna.domain.model.AppListInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAppsDataSource(private val context: Context) {

    suspend fun getAppList(): AppResult<AppListInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getInstalledPackages(0)
            }

            val apps = packages.map { pkg ->
                val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                AppDetails(
                    name = runCatching { pm.getApplicationLabel(pkg.applicationInfo!!).toString() }.getOrDefault(pkg.packageName),
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "Unknown",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else @Suppress("DEPRECATION") pkg.versionCode.toLong(),
                    isSystemApp = isSystem,
                    installedAt = pkg.firstInstallTime.takeIf { it > 0 },
                    updatedAt = pkg.lastUpdateTime.takeIf { it > 0 },
                    targetSdkVersion = pkg.applicationInfo?.targetSdkVersion ?: 0,
                )
            }.sortedBy { it.name.lowercase() }

            AppListInfo(
                totalCount = apps.size,
                userCount = apps.count { !it.isSystemApp },
                systemCount = apps.count { it.isSystemApp },
                apps = apps,
            )
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Apps read failed")) },
        )
    }
}
