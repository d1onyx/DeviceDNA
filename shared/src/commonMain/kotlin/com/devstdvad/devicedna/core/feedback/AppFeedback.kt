package com.devstdvad.devicedna.core.feedback

import androidx.compose.runtime.compositionLocalOf

/** Platform haptics. Implemented per platform (Android Vibrator, iOS UIFeedbackGenerator). */
interface HapticManager {
    fun navTap()
    fun toggle()
    fun light()
    fun confirm()
    fun error()
}

/** Platform sound cues. Implemented per platform (Android ToneGenerator, iOS AudioServices). */
interface SoundManager {
    fun navClick()
    fun toggleOn()
    fun toggleOff()
    fun success()
    fun error()
    /** Release native audio resources (Android ToneGenerator); no-op where unneeded. */
    fun release()
}

/**
 * Pure feedback orchestration shared by both platforms. Holds the platform managers and
 * gates them on the user's haptic/sound preferences.
 */
data class AppFeedback(
    val haptic: HapticManager,
    val sound: SoundManager,
    val hapticEnabled: Boolean,
    val soundEnabled: Boolean,
) {
    fun navTap() {
        if (hapticEnabled) haptic.navTap()
        if (soundEnabled) sound.navClick()
    }

    fun toggle(on: Boolean) {
        if (hapticEnabled) haptic.toggle()
        if (soundEnabled) { if (on) sound.toggleOn() else sound.toggleOff() }
    }

    fun confirm() {
        if (hapticEnabled) haptic.confirm()
        if (soundEnabled) sound.success()
    }

    fun error() {
        if (hapticEnabled) haptic.error()
        if (soundEnabled) sound.error()
    }

    fun light() {
        if (hapticEnabled) haptic.light()
        if (soundEnabled) sound.navClick()
    }
}

val LocalAppFeedback = compositionLocalOf<AppFeedback?> { null }
