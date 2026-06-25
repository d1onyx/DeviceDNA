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
                    ?.take(20)
                    ?.forEach { zoneDir ->
                        val typeName = runCatching { File(zoneDir, "type").readText().trim() }.getOrDefault("unknown")
                        val temp = runCatching { File(zoneDir, "temp").readText().trim().toFloat() / 1000f }.getOrNull()
                        zones.add(ThermalZone(name = typeName, type = classifyZone(typeName), temperatureCelsius = temp))
                    }
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
}
