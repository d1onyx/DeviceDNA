package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.alerts.SmartAlertEvaluator
import com.devstdvad.devicedna.data.alerts.SmartAlertType
import com.devstdvad.devicedna.data.alerts.SmartAlertsStateStore
import com.devstdvad.devicedna.data.widget.WidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAlertEvaluatorTest {

    @Test
    fun `cpu overheating is active at fixed thresholds`() {
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.CpuOverheating, snapshot(cpuTempC = 45f)))
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.CpuOverheating, snapshot(thermalMaxC = 48f)))
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.CpuOverheating, snapshot(thermalStatus = 3)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.CpuOverheating, snapshot(cpuTempC = 44.9f, thermalMaxC = 47.9f, thermalStatus = 2)))
    }

    @Test
    fun `low battery is active only while unplugged below or at fifteen percent`() {
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.LowBattery, snapshot(batteryLevel = 15, batteryCharging = false)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.LowBattery, snapshot(batteryLevel = 16, batteryCharging = false)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.LowBattery, snapshot(batteryLevel = 10, batteryCharging = true)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.LowBattery, snapshot(batteryLevel = 0, batteryCharging = false)))
    }

    @Test
    fun `storage and ram alerts are active at ninety percent`() {
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.StorageFull, snapshot(storageUsedPercent = 0.90f)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.StorageFull, snapshot(storageUsedPercent = 0.89f)))
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.HighRam, snapshot(ramUsedPercent = 0.90f)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.HighRam, snapshot(ramUsedPercent = 0.89f)))
    }

    @Test
    fun `slow charging is active only for low positive watts below eighty percent`() {
        assertTrue(SmartAlertEvaluator.isActive(SmartAlertType.SlowCharging, snapshot(batteryCharging = true, batteryWatts = 5f, batteryLevel = 79)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.SlowCharging, snapshot(batteryCharging = true, batteryWatts = 5.1f, batteryLevel = 79)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.SlowCharging, snapshot(batteryCharging = true, batteryWatts = 0f, batteryLevel = 79)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.SlowCharging, snapshot(batteryCharging = true, batteryWatts = 4f, batteryLevel = 80)))
        assertFalse(SmartAlertEvaluator.isActive(SmartAlertType.SlowCharging, snapshot(batteryCharging = false, batteryWatts = 4f, batteryLevel = 50)))
    }

    @Test
    fun `evaluate filters disabled alert types`() {
        val result = SmartAlertEvaluator.evaluate(
            snapshot(cpuTempC = 50f, batteryLevel = 10, batteryCharging = false),
            enabled = setOf(SmartAlertType.LowBattery),
        )

        assertEquals(listOf(SmartAlertType.LowBattery), result)
    }

    @Test
    fun `should notify on active edge and after cooldown only`() {
        val cooldown = 100L

        assertTrue(SmartAlertsStateStore.shouldNotify(isActive = true, wasActive = false, lastNotifiedMillis = 0L, nowMillis = 10L, cooldownMs = cooldown))
        assertFalse(SmartAlertsStateStore.shouldNotify(isActive = true, wasActive = true, lastNotifiedMillis = 10L, nowMillis = 50L, cooldownMs = cooldown))
        assertTrue(SmartAlertsStateStore.shouldNotify(isActive = true, wasActive = true, lastNotifiedMillis = 10L, nowMillis = 110L, cooldownMs = cooldown))
        assertFalse(SmartAlertsStateStore.shouldNotify(isActive = false, wasActive = true, lastNotifiedMillis = 10L, nowMillis = 200L, cooldownMs = cooldown))
    }

    private fun snapshot(
        cpuTempC: Float = 0f,
        thermalMaxC: Float = 0f,
        thermalStatus: Int = -1,
        batteryLevel: Int = 50,
        batteryCharging: Boolean = false,
        batteryWatts: Float = 0f,
        storageUsedPercent: Float = 0f,
        ramUsedPercent: Float = 0f,
    ): WidgetSnapshot = WidgetSnapshot(
        cpuTempC = cpuTempC,
        thermalMaxC = thermalMaxC,
        thermalStatus = thermalStatus,
        batteryLevel = batteryLevel,
        batteryCharging = batteryCharging,
        batteryWatts = batteryWatts,
        storageUsedPercent = storageUsedPercent,
        ramUsedPercent = ramUsedPercent,
    )
}
