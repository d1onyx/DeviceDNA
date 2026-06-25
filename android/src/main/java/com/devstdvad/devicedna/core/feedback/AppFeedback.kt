package com.devstdvad.devicedna.core.feedback

import androidx.compose.runtime.compositionLocalOf

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
    }
}

val LocalAppFeedback = compositionLocalOf<AppFeedback?> { null }
