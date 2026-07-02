package com.devstdvad.devicedna.presentation.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class BatteryState(
    val isLoading: Boolean = true,
    val info: BatteryInfo? = null,
    val error: String? = null,
)

class BatteryViewModel(private val observeBattery: ObserveBatteryUseCase) : ViewModel() {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    init {
        observeBattery().onEach { result ->
            when (result) {
                is AppResult.Success -> _state.update { it.copy(info = result.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = result.cause.message, isLoading = false) }
            }
        }.launchIn(viewModelScope)
    }
}
