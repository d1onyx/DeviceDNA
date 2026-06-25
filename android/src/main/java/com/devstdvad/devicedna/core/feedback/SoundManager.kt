package com.devstdvad.devicedna.core.feedback

import android.media.AudioManager
import android.media.ToneGenerator

class SoundManager {

    private var toneGen: ToneGenerator? = null

    init {
        runCatching { toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 20) }
    }

    fun navClick() = play(ToneGenerator.TONE_PROP_BEEP, 25)
    fun toggleOn() = play(ToneGenerator.TONE_PROP_ACK, 40)
    fun toggleOff() = play(ToneGenerator.TONE_PROP_NACK, 30)
    fun success() = play(ToneGenerator.TONE_CDMA_CONFIRM, 80)
    fun error() = play(ToneGenerator.TONE_PROP_NACK, 100)

    private fun play(type: Int, durationMs: Int) {
        runCatching { toneGen?.startTone(type, durationMs) }
    }

    fun release() {
        runCatching { toneGen?.release() }
        toneGen = null
    }
}
