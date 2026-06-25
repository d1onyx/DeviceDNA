package com.devstdvad.devicedna.presentation.thermal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.usecase.GetThermalInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class ThermalState(val isLoading: Boolean = true, val info: ThermalInfo? = null, val error: String? = null)

class ThermalViewModel(private val getThermal: GetThermalInfoUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ThermalState())
    val state: StateFlow<ThermalState> = _state.asStateFlow()
    init {
        getThermal.observe().onEach { result ->
            when (result) {
                is AppResult.Success -> _state.update { it.copy(info = result.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = result.cause.message, isLoading = false) }
            }
        }.launchIn(viewModelScope)
    }
}
