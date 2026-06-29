package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHourStatus
import com.devstdvad.devicedna.domain.batteryintelligence.buildChargingSessions
import com.devstdvad.devicedna.domain.batteryintelligence.buildHourlyTimeline
import com.devstdvad.devicedna.domain.batteryintelligence.calculateAccumulatedChargePercent
import com.devstdvad.devicedna.domain.batteryintelligence.estimateCapacityRetentionPercent
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryIntelligenceTest {

    @Test
    fun `capacity retention estimate is bounded and follows health score`() {
        assertEquals(98, estimateCapacityRetentionPercent(100))
        assertEquals(80, estimateCapacityRetentionPercent(80))
        assertEquals(8, estimateCapacityRetentionPercent(0))
    }

    @Test
    fun `charging snapshots are grouped into plug to disconnect sessions`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = false, level = 20),
            snapshot(minute = 5, plugged = true, level = 21),
            snapshot(minute = 15, plugged = true, level = 24),
            snapshot(minute = 25, plugged = false, level = 25),
            snapshot(minute = 40, plugged = true, level = 26),
        )

        val sessions = buildChargingSessions(snapshots)

        assertEquals(2, sessions.size)
        assertEquals(40L * 60_000L, sessions[0].startMillis)
        assertEquals(null, sessions[0].endMillis)
        assertEquals(5L * 60_000L, sessions[1].startMillis)
        assertEquals(25L * 60_000L, sessions[1].endMillis)
        assertEquals(3, sessions[1].levelDeltaPercent)
    }

    @Test
    fun `local cycle estimate counts accumulated charge percent`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = true, level = 20),
            snapshot(minute = 20, plugged = true, level = 80),
            snapshot(minute = 40, plugged = false, level = 40),
            snapshot(minute = 60, plugged = true, level = 70),
            snapshot(minute = 80, plugged = true, level = 90),
        )

        val accumulatedPercent = calculateAccumulatedChargePercent(snapshots)

        assertEquals(110, accumulatedPercent)
        assertEquals(1, accumulatedPercent / 100)
        assertEquals(10, accumulatedPercent % 100)
    }

    @Test
    fun `hourly timeline keeps minute order for charge stable and discharge periods`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = true, level = 20),
            snapshot(minute = 10, plugged = true, level = 30),
            snapshot(minute = 20, plugged = true, level = 30),
            snapshot(minute = 30, plugged = false, level = 25),
        )

        val hour = buildHourlyTimeline(
            daySnapshots = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
        ).first()

        assertEquals(10f, hour.goodMinutes, 0.01f)
        assertEquals(25f, hour.stableMinutes, 0.01f)
        assertEquals(10f, hour.dischargeMinutes, 0.01f)
        assertEquals(4, hour.segments.size)
        assertEquals(ChargingHourStatus.GoodCharging, hour.segments[0].status)
        assertEquals(0f, hour.segments[0].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.Stable, hour.segments[1].status)
        assertEquals(10f, hour.segments[1].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.Discharging, hour.segments[2].status)
        assertEquals(20f, hour.segments[2].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.Stable, hour.segments[3].status)
        assertEquals(30f, hour.segments[3].startMinute, 0.01f)
    }

    private fun snapshot(
        minute: Int,
        plugged: Boolean,
        level: Int,
    ): BatteryHistorySnapshot = BatteryHistorySnapshot(
        timestampMillis = minute * 60_000L,
        levelPercent = level,
        status = if (plugged) "Charging" else "Discharging",
        source = if (plugged) "AC" else "Unknown",
        temperatureCelsius = 30f,
        currentMa = if (plugged) 1_500 else null,
        estimatedWatts = if (plugged) 8f else null,
        chargeCycles = null,
    )
}
