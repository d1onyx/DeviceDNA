package com.devstdvad.devicedna.core.feedback

import android.media.AudioManager
import android.media.ToneGenerator

class AndroidSoundManager : SoundManager {

    private var toneGen: ToneGenerator? = null

    init {
        runCatching { toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 20) }
    }

    override fun navClick() = play(ToneGenerator.TONE_PROP_BEEP, 25)
    override fun toggleOn() = play(ToneGenerator.TONE_PROP_ACK, 40)
    override fun toggleOff() = play(ToneGenerator.TONE_PROP_NACK, 30)
    override fun success() = play(ToneGenerator.TONE_CDMA_CONFIRM, 80)
    override fun error() = play(ToneGenerator.TONE_PROP_NACK, 100)

    private fun play(type: Int, durationMs: Int) {
        runCatching { toneGen?.startTone(type, durationMs) }
    }

    override fun release() {
        runCatching { toneGen?.release() }
        toneGen = null
    }
}
