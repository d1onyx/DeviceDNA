package com.devstdvad.devicedna.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.export.ExportManager
import com.devstdvad.devicedna.data.settings.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExportState(
    val isExporting: Boolean = false,
    val shareIntent: Intent? = null,
    val errorMessage: String? = null,
)

class ExportViewModel(private val exportManager: ExportManager) : ViewModel() {

    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun export(format: ExportFormat) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            exportManager.export(format).fold(
                onSuccess = { uri ->
                    val intent = exportManager.buildShareIntent(uri, format)
                    _state.value = ExportState(shareIntent = intent)
                },
                onFailure = { e ->
                    _state.value = ExportState(errorMessage = e.message ?: "Export failed")
                },
            )
        }
    }

    fun clearShareIntent() {
        _state.value = _state.value.copy(shareIntent = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
