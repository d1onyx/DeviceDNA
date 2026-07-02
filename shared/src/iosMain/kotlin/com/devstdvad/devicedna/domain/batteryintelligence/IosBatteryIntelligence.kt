package com.devstdvad.devicedna.domain.batteryintelligence

import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.domain.model.BatteryInfo
import kotlinx.datetime.TimeZone

/**
 * Swift-friendly facade over the battery-intelligence extension functions. Hides
 * `kotlinx.datetime.TimeZone`, which is not exported into the iOS framework, so Swift
 * only passes primitives and gets back the fully-built [BatteryIntelligenceReport].
 */
object IosBatteryIntelligence {

    fun report(
        battery: BatteryInfo,
        history: List<BatteryHistorySnapshot>,
        selectedDayStartMillis: Long,
        selectedHour: Int,
        nowMillis: Long,
    ): BatteryIntelligenceReport =
        battery.toBatteryIntelligenceReport(
            history = history,
            selectedDayStartMillis = selectedDayStartMillis,
            selectedHour = selectedHour,
            timeZone = TimeZone.currentSystemDefault(),
            nowMillis = nowMillis,
        )

    fun todayStartMillis(nowMillis: Long): Long =
        todayStartMillis(TimeZone.currentSystemDefault(), nowMillis)

    fun currentHour(nowMillis: Long): Int =
        currentHour(TimeZone.currentSystemDefault(), nowMillis)
}
