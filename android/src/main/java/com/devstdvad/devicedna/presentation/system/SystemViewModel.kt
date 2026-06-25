package com.devstdvad.devicedna.presentation.system

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.SystemInfo
import com.devstdvad.devicedna.domain.usecase.GetSystemInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemState(
    val isLoading: Boolean = true,
    val info: SystemInfo? = null,
    val error: String? = null,
)

class SystemViewModel(private val getSystemInfo: GetSystemInfoUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SystemState())
    val state: StateFlow<SystemState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = getSystemInfo()) {
                is AppResult.Success -> _state.update { it.copy(info = r.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = r.cause.message, isLoading = false) }
            }
        }
    }
}
