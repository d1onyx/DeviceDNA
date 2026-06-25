package com.devstdvad.devicedna.domain.model

data class AppListInfo(
    val totalCount: Int,
    val userCount: Int,
    val systemCount: Int,
    val apps: List<AppDetails>,
)

data class AppDetails(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installedAt: Long?,
    val updatedAt: Long?,
    val targetSdkVersion: Int,
)
