package com.devstdvad.devicedna.presentation.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CameraInfo
import com.devstdvad.devicedna.domain.usecase.GetCameraInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraState(val isLoading: Boolean = true, val info: CameraInfo? = null, val error: String? = null)

class CameraViewModel(private val getCamera: GetCameraInfoUseCase) : ViewModel() {
    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()
    init {
        viewModelScope.launch {
            when (val r = getCamera()) {
                is AppResult.Success -> _state.update { it.copy(info = r.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = r.cause.message, isLoading = false) }
            }
        }
    }
}
