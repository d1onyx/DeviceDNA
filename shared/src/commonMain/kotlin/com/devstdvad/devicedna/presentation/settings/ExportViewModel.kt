package com.devstdvad.devicedna.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.export.DiagnosticsExporter
import com.devstdvad.devicedna.data.settings.ExportFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExportState(
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Triggers a diagnostics export. Sharing is delegated to the platform FileSharer inside
 * [DiagnosticsExporter], so no Android Intent leaks into the shared UI layer.
 */
class ExportViewModel(private val exporter: DiagnosticsExporter) : ViewModel() {

    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun export(format: ExportFormat) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = ExportState(isExporting = true)
            runCatching { exporter.export(format) }.fold(
                onSuccess = { _state.value = ExportState() },
                onFailure = { e -> _state.value = ExportState(errorMessage = e.message ?: "Export failed") },
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
