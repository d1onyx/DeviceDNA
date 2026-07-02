package com.devstdvad.devicedna.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AndroidHapticManager(context: Context) : HapticManager {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun navTap() = oneShot(6L, 60)
    override fun toggle() = oneShot(10L, 90)
    override fun light() = oneShot(5L, 50)
    override fun confirm() = waveform(longArrayOf(0, 8, 60, 12), intArrayOf(0, 100, 0, 70))
    override fun error() = waveform(longArrayOf(0, 15, 80, 15), intArrayOf(0, 200, 0, 180))

    private fun oneShot(ms: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(ms)
        }
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(timings, -1)
        }
    }
}
