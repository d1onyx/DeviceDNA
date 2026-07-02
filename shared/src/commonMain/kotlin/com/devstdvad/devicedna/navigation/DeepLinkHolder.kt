package com.devstdvad.devicedna.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform carrier for a pending deep-link route (widget tap, notification tap).
 * The platform host pushes a route; the shared App shell consumes it and clears it.
 * Android uses Intent extras today; iOS pushes here from the notification delegate
 * and WidgetKit URL handling.
 */
object DeepLinkHolder {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun push(route: String) {
        _route.value = route
    }

    fun consume() {
        _route.value = null
    }
}
