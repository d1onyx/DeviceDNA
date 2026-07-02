package com.devstdvad.devicedna.presentation.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.DisplayInfo
import com.devstdvad.devicedna.domain.usecase.GetDisplayInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DisplayState(val isLoading: Boolean = true, val info: DisplayInfo? = null, val error: String? = null)

class DisplayViewModel(private val getDisplay: GetDisplayInfoUseCase) : ViewModel() {
    private val _state = MutableStateFlow(DisplayState())
    val state: StateFlow<DisplayState> = _state.asStateFlow()
    init {
        viewModelScope.launch {
            when (val r = getDisplay()) {
                is AppResult.Success -> _state.update { it.copy(info = r.value, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = r.cause.message, isLoading = false) }
            }
        }
    }
}
