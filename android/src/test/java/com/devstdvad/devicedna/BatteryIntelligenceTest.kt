package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingHourStatus
import com.devstdvad.devicedna.domain.batteryintelligence.buildChargingSessions
import com.devstdvad.devicedna.domain.batteryintelligence.buildHourlyTimeline
import com.devstdvad.devicedna.domain.batteryintelligence.calculateAccumulatedChargePercent
import com.devstdvad.devicedna.domain.batteryintelligence.estimateCapacityRetentionPercent
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `hourly timeline keeps minute order for charge and discharge periods`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = true, level = 20),
            snapshot(minute = 10, plugged = true, level = 30),
            snapshot(minute = 20, plugged = true, level = 30), // flat while charging -> still charging
            snapshot(minute = 30, plugged = false, level = 25),
        )

        val hour = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
        ).first()

        // Charging stays charging even where the percentage is flat; no grey Stable here.
        assertEquals(20f, hour.goodMinutes, 0.01f)
        assertEquals(0f, hour.stableMinutes, 0.01f)
        assertEquals(25f, hour.dischargeMinutes, 0.01f)
        assertEquals(4, hour.segments.size)
        assertEquals(ChargingHourStatus.GoodCharging, hour.segments[0].status)
        assertEquals(0f, hour.segments[0].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.GoodCharging, hour.segments[1].status)
        assertEquals(10f, hour.segments[1].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.Discharging, hour.segments[2].status)
        assertEquals(20f, hour.segments[2].startMinute, 0.01f)
        assertEquals(ChargingHourStatus.Discharging, hour.segments[3].status)
        assertEquals(30f, hour.segments[3].startMinute, 0.01f)
    }

    @Test
    fun `charging with a flat percentage renders as charging not stable`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = true, level = 84),
            snapshot(minute = 30, plugged = true, level = 84), // warm trickle charge, percent unchanged
        )

        val hour = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
            nowMillis = HOUR_MS,
        ).first()

        assertEquals(0f, hour.stableMinutes, 0.01f)
        assertTrue(hour.goodMinutes + hour.poorMinutes > 0f)
    }

    @Test
    fun `flat percentage while on battery is stable`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = false, level = 60),
            snapshot(minute = 20, plugged = false, level = 60), // idle, not charging
        )

        val hour = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
            nowMillis = HOUR_MS,
        ).first()

        assertTrue(hour.stableMinutes > 0f)
        assertEquals(0f, hour.goodMinutes, 0.01f)
    }

    @Test
    fun `continuous charging bridges sparse hourly samples into one block`() {
        val snapshots = listOf(
            snapshot(minute = 0, plugged = true, level = 84),
            snapshot(minute = 60, plugged = true, level = 86), // 1h later, still plugged
        )

        val timeline = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
            nowMillis = 2 * HOUR_MS,
        )

        // The whole hour between the two plugged samples is filled, not just a 20-min window.
        assertEquals(60f, timeline[0].goodMinutes + timeline[0].poorMinutes, 0.01f)
    }

    @Test
    fun `an unrecorded gap stays NoData instead of being filled`() {
        val snapshots = listOf(
            snapshotAt(HOUR_MS, plugged = true, level = 40), // 01:00 charging
            snapshotAt(9 * HOUR_MS, plugged = true, level = 95), // 09:00 charging, 8h gap (no recording)
        )

        val timeline = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
            nowMillis = 12 * HOUR_MS,
        )

        // The 8h gap (e.g. premium inactive) must not be painted by extrapolating the 01:00 state.
        assertEquals(ChargingHourStatus.NoData, timeline[3].status)
        assertEquals(ChargingHourStatus.NoData, timeline[5].status)
        // Each sample still paints a short window in its own hour.
        assertTrue(timeline[1].status != ChargingHourStatus.NoData)
        assertTrue(timeline[9].status != ChargingHourStatus.NoData)
    }

    @Test
    fun `an unrecorded overnight gap is not painted on the next day`() {
        val snapshots = listOf(
            snapshotAt(DAY_MS + 23 * HOUR_MS, plugged = true, level = 50), // 23:00 day 0 (plug-in)
            snapshotAt(2 * DAY_MS + 7 * HOUR_MS, plugged = false, level = 80), // 07:00 day 1 (unplug)
        )

        val timeline = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 2 * DAY_MS,
            timeZone = TimeZone.UTC,
            nowMillis = 2 * DAY_MS + 12 * HOUR_MS,
        )

        // Nothing was recorded overnight, so the early hours must stay empty.
        assertEquals(ChargingHourStatus.NoData, timeline[0].status)
        assertEquals(ChargingHourStatus.NoData, timeline[6].status)
        // The unplug sample still leaves a short marker in its own hour.
        assertTrue(timeline[7].status != ChargingHourStatus.NoData)
    }

    @Test
    fun `a recent pre-midnight charge bridges into the next day`() {
        val snapshots = listOf(
            snapshotAt(2 * DAY_MS - 15 * 60_000L, plugged = true, level = 90), // 23:45 day 0
            snapshotAt(2 * DAY_MS + 7 * HOUR_MS, plugged = false, level = 100), // 07:00 day 1 (unplug)
        )

        val sessions = buildChargingSessions(
            history = snapshots,
            dayStartMillis = 2 * DAY_MS,
            timeZone = TimeZone.UTC,
            nowMillis = 2 * DAY_MS + 12 * HOUR_MS,
        )

        assertEquals(1, sessions.size)
        assertEquals(2 * DAY_MS, sessions[0].startMillis)
        assertEquals(2 * DAY_MS + 7 * HOUR_MS, sessions[0].endMillis)
    }

    @Test
    fun `a stale pre-midnight charge does not fabricate a session`() {
        val snapshots = listOf(
            snapshotAt(DAY_MS + 23 * HOUR_MS, plugged = true, level = 50), // 23:00 day 0 (1h before midnight)
            snapshotAt(2 * DAY_MS + 7 * HOUR_MS, plugged = false, level = 80), // 07:00 day 1 (unplug)
        )

        val sessions = buildChargingSessions(
            history = snapshots,
            dayStartMillis = 2 * DAY_MS,
            timeZone = TimeZone.UTC,
            nowMillis = 2 * DAY_MS + 12 * HOUR_MS,
        )

        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `a recording-paused marker keeps the gap empty even while charging continues`() {
        val snapshots = listOf(
            snapshot(minute = 10, plugged = true, level = 84), // premium on, charging
            snapshot(minute = 11, plugged = true, level = 84).copy(recordingPaused = true), // premium off
            snapshot(minute = 130, plugged = true, level = 84), // premium on again ~2h later, charging
        )

        val timeline = buildHourlyTimeline(
            history = snapshots,
            dayStartMillis = 0L,
            timeZone = TimeZone.UTC,
            nowMillis = 3 * HOUR_MS,
        )

        // The hour with no recording (premium off) must stay empty, even though the phone kept charging.
        assertEquals(ChargingHourStatus.NoData, timeline[1].status)
        // Recording resumes -> charging is shown again.
        assertTrue(timeline[2].status != ChargingHourStatus.NoData)
    }

    private fun snapshot(
        minute: Int,
        plugged: Boolean,
        level: Int,
    ): BatteryHistorySnapshot = snapshotAt(minute * 60_000L, plugged, level)

    private fun snapshotAt(
        timestampMillis: Long,
        plugged: Boolean,
        level: Int,
    ): BatteryHistorySnapshot = BatteryHistorySnapshot(
        timestampMillis = timestampMillis,
        levelPercent = level,
        status = if (plugged) "Charging" else "Discharging",
        source = if (plugged) "AC" else "Unknown",
        temperatureCelsius = 30f,
        currentMa = if (plugged) 1_500 else null,
        estimatedWatts = if (plugged) 8f else null,
        chargeCycles = null,
    )

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 86_400_000L
    }
}
