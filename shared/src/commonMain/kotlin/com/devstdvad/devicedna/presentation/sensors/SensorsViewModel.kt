package com.devstdvad.devicedna.presentation.sensors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.SensorDetails
import com.devstdvad.devicedna.domain.model.SensorInfo
import com.devstdvad.devicedna.domain.usecase.GetSensorsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SensorsState(
    val isLoading: Boolean = true,
    val info: SensorInfo? = null,
    val filtered: List<SensorDetails> = emptyList(),
    val query: String = "",
    val error: String? = null,
)

class SensorsViewModel(private val getSensors: GetSensorsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(SensorsState())
    val state: StateFlow<SensorsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = getSensors()) {
                is AppResult.Success -> _state.update { it.copy(info = r.value, filtered = r.value.sensors, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = r.cause.message, isLoading = false) }
            }
        }
    }

    fun onQuery(q: String) {
        _state.update { s ->
            val filtered = s.info?.sensors?.filter { it.name.contains(q, ignoreCase = true) || it.typeName.contains(q, ignoreCase = true) } ?: emptyList()
            s.copy(query = q, filtered = filtered)
        }
    }
}
