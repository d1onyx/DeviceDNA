package com.devstdvad.devicedna.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceState(
    val isLoading: Boolean = true,
    val info: DeviceInfo? = null,
    val error: String? = null,
)

class DeviceViewModel(private val getDeviceInfo: GetDeviceInfoUseCase) : ViewModel() {

    private val _state = MutableStateFlow(DeviceState())
    val state: StateFlow<DeviceState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getDeviceInfo()) {
                is AppResult.Success -> _state.update { it.copy(info = result.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = result.cause.message, isLoading = false) }
            }
        }
    }
}
