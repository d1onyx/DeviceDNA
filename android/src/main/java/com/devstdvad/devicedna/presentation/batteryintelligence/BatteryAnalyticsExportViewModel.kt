package com.devstdvad.devicedna.presentation.batteryintelligence

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BatteryAnalyticsExportState(
    val isExporting: Boolean = false,
    val shareIntent: Intent? = null,
    val errorMessage: String? = null,
)

class BatteryAnalyticsExportViewModel(
    private val exportManager: BatteryAnalyticsExportManager,
) : ViewModel() {
    private val _state = MutableStateFlow(BatteryAnalyticsExportState())
    val state: StateFlow<BatteryAnalyticsExportState> = _state.asStateFlow()

    fun export(report: BatteryIntelligenceReport, format: ExportFormat) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = BatteryAnalyticsExportState(isExporting = true)
            exportManager.export(report, format).fold(
                onSuccess = { uri ->
                    _state.value = BatteryAnalyticsExportState(
                        shareIntent = exportManager.buildShareIntent(uri, format),
                    )
                },
                onFailure = { error ->
                    _state.value = BatteryAnalyticsExportState(
                        errorMessage = error.message ?: "Battery analytics export failed.",
                    )
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
