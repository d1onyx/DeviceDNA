package com.devstdvad.devicedna

import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryDataTest {

    private fun battery(
        level: Int = 80,
        temp: Float = 30f,
        health: BatteryHealth = BatteryHealth.Good,
        status: BatteryStatus = BatteryStatus.Discharging,
        voltage: Int = 4100,
        capacity: Int? = 4500,
        cycles: Int? = null,
    ) = BatteryInfo(
        levelPercent = level,
        status = status,
        health = health,
        source = ChargeSource.Unknown,
        technology = "Li-ion",
        temperatureCelsius = temp,
        voltageMv = voltage,
        currentMa = null,
        capacityMah = capacity,
        chargeCycles = cycles,
        isPresent = true,
    )

    @Test
    fun `battery level clamps at 100`() {
        val b = battery(level = 100)
        assertEquals(100, b.levelPercent)
    }

    @Test
    fun `battery is present by default`() {
        assertTrue(battery().isPresent)
    }

    @Test
    fun `healthy battery returns Good health`() {
        assertEquals(BatteryHealth.Good, battery(health = BatteryHealth.Good).health)
    }

    @Test
    fun `overheat battery detected when temp above threshold`() {
        val b = battery(temp = 46f)
        assertTrue(b.temperatureCelsius >= 45f)
        assertTrue(b.health != BatteryHealth.Cold)
    }

    @Test
    fun `cold battery detected`() {
        val b = battery(temp = 2f, health = BatteryHealth.Cold)
        assertEquals(BatteryHealth.Cold, b.health)
    }

    @Test
    fun `charging status correct`() {
        val b = battery(status = BatteryStatus.Charging)
        assertEquals(BatteryStatus.Charging, b.status)
    }

    @Test
    fun `full status at 100 percent`() {
        val b = battery(level = 100, status = BatteryStatus.Full)
        assertEquals(BatteryStatus.Full, b.status)
        assertEquals(100, b.levelPercent)
    }

    @Test
    fun `capacity is optional and can be null`() {
        val b = battery(capacity = null)
        assertEquals(null, b.capacityMah)
    }

    @Test
    fun `charge cycles stored correctly`() {
        val b = battery(cycles = 342)
        assertEquals(342, b.chargeCycles)
    }

    @Test
    fun `voltage millivolts stored correctly`() {
        val b = battery(voltage = 3850)
        assertEquals(3850, b.voltageMv)
    }

    @Test
    fun `technology field is preserved`() {
        assertEquals("Li-ion", battery().technology)
    }

    @Test
    fun `dead battery health detected`() {
        val b = battery(health = BatteryHealth.Dead, level = 0)
        assertEquals(BatteryHealth.Dead, b.health)
        assertEquals(0, b.levelPercent)
    }

    @Test
    fun `wireless charging source stored`() {
        val b = battery().copy(source = ChargeSource.Wireless)
        assertEquals(ChargeSource.Wireless, b.source)
    }

    @Test
    fun `estimated watts is null when not set`() {
        assertEquals(null, battery().estimatedWatts)
    }

    @Test
    fun `power saver flag defaults to false`() {
        assertFalse(battery().isPowerSaveMode)
    }

    @Test
    fun `power saver flag can be stored`() {
        assertTrue(battery().copy(isPowerSaveMode = true).isPowerSaveMode)
    }
}
