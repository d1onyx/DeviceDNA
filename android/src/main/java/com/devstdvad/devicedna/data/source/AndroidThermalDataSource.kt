package com.devstdvad.devicedna.data.source

import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZone
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class AndroidThermalDataSource {

    fun observeThermal(intervalMs: Long = 5_000): Flow<AppResult<ThermalInfo>> = flow {
        while (true) {
            emit(getThermalInfo())
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getThermalInfo(): AppResult<ThermalInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val zones = mutableListOf<ThermalZone>()
            val thermalBase = File("/sys/class/thermal")
            if (thermalBase.exists()) {
                thermalBase.listFiles()
                    ?.filter { it.name.startsWith("thermal_zone") }
                    ?.sortedBy { it.name.removePrefix("thermal_zone").toIntOrNull() ?: 0 }
                    ?.mapNotNull(::readThermalZone)
                    ?.forEach(zones::add)
            }
            if (zones.isEmpty()) {
                zones.add(ThermalZone("battery", ThermalZoneType.Battery, null))
                zones.add(ThermalZone("cpu", ThermalZoneType.Cpu, null))
            }
            ThermalInfo(zones = zones)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Thermal read failed")) },
        )
    }

    private fun readThermalZone(zoneDir: File): ThermalZone? {
        val typeName = runCatching { File(zoneDir, "type").readText().trim() }.getOrDefault("unknown")
        if (typeName.isControlOrNonTemperatureZone()) return null

        val temp = runCatching {
            File(zoneDir, "temp").readText().trim().toFloat()
        }.getOrNull()?.toValidTemperatureCelsius() ?: return null

        return ThermalZone(name = typeName, type = classifyZone(typeName), temperatureCelsius = temp)
    }

    private fun classifyZone(type: String): ThermalZoneType {
        val lower = type.lowercase()
        return when {
            "battery" in lower || "bat" in lower -> ThermalZoneType.Battery
            "cpu" in lower || "soc" in lower || "core" in lower -> ThermalZoneType.Cpu
            "cam" in lower -> ThermalZoneType.Camera
            "charg" in lower || "usb" in lower -> ThermalZoneType.Charger
            "audio" in lower || "sound" in lower -> ThermalZoneType.Audio
            "modem" in lower || "mdm" in lower || "lte" in lower -> ThermalZoneType.Modem
            "conn" in lower || "ufs" in lower || "nand" in lower -> ThermalZoneType.Connector
            "board" in lower || "pmic" in lower -> ThermalZoneType.Board
            else -> ThermalZoneType.Unknown
        }
    }

    private fun String.isControlOrNonTemperatureZone(): Boolean {
        val lower = lowercase()
        return NON_TEMPERATURE_ZONE_MARKERS.any { it in lower }
    }

    private fun Float.toValidTemperatureCelsius(): Float? {
        val celsius = this / 1000f
        return celsius.takeIf { it in MIN_VALID_TEMP_C..MAX_VALID_TEMP_C }
    }

    private companion object {
        const val MIN_VALID_TEMP_C = -20f
        const val MAX_VALID_TEMP_C = 125f
        val NON_TEMPERATURE_ZONE_MARKERS = listOf(
            "bcl",
            "ibat",
            "vbat",
            "vph",
            "-lvl",
            "_lvl",
            "dcvs",
            "-step",
            "_step",
            "-lowf",
            "_lowf",
        )
    }
}
