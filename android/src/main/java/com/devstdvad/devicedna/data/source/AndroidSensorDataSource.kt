package com.devstdvad.devicedna.data.source

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.SensorDetails
import com.devstdvad.devicedna.domain.model.SensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSensorDataSource(private val context: Context) {

    suspend fun getSensorInfo(): AppResult<SensorInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensors = sm.getSensorList(Sensor.TYPE_ALL).map { sensor ->
                SensorDetails(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    type = sensor.type,
                    typeName = sensorTypeName(sensor.type),
                    version = sensor.version,
                    powerMa = sensor.power,
                    resolution = sensor.resolution,
                    maxRange = sensor.maximumRange,
                    isWakeUp = sensor.isWakeUpSensor,
                    isDynamic = sensor.isDynamicSensor,
                )
            }
            SensorInfo(sensors = sensors)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Sensor read failed")) },
        )
    }

    private fun sensorTypeName(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
        Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
        Sensor.TYPE_GYROSCOPE -> "Gyroscope"
        Sensor.TYPE_LIGHT -> "Light"
        Sensor.TYPE_PRESSURE -> "Pressure"
        Sensor.TYPE_PROXIMITY -> "Proximity"
        Sensor.TYPE_GRAVITY -> "Gravity"
        Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
        Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature"
        Sensor.TYPE_STEP_COUNTER -> "Step Counter"
        Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
        Sensor.TYPE_HEART_RATE -> "Heart Rate"
        Sensor.TYPE_STATIONARY_DETECT -> "Stationary Detect"
        Sensor.TYPE_MOTION_DETECT -> "Motion Detect"
        Sensor.TYPE_POSE_6DOF -> "6DOF Pose"
        else -> "Type $type"
    }
}
