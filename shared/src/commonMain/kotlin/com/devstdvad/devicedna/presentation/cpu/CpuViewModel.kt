package com.devstdvad.devicedna.presentation.cpu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class CpuState(
    val isLoading: Boolean = true,
    val info: CpuInfo? = null,
    val error: String? = null,
)

class CpuViewModel(private val getCpuInfo: GetCpuInfoUseCase) : ViewModel() {
    private val _state = MutableStateFlow(CpuState())
    val state: StateFlow<CpuState> = _state.asStateFlow()

    init {
        getCpuInfo.observe().onEach { result ->
            when (result) {
                is AppResult.Success -> _state.update { it.copy(info = result.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = result.cause.message, isLoading = false) }
            }
        }.launchIn(viewModelScope)
    }
}
