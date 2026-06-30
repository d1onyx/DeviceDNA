package com.devstdvad.devicedna.presentation.batteryintelligence

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatteryAnalyticsExportState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val shareIntent: Intent? = null,
    val errorMessage: String? = null,
    val importResult: ImportResult? = null,
) {
    data class ImportResult(
        val addedCount: Int,
        val degraded: Boolean,
    )
}

class BatteryAnalyticsExportViewModel(
    private val exportManager: BatteryAnalyticsExportManager,
    private val historyStore: BatteryIntelligenceHistoryStore,
) : ViewModel() {
    private val _state = MutableStateFlow(BatteryAnalyticsExportState())
    val state: StateFlow<BatteryAnalyticsExportState> = _state.asStateFlow()

    fun export(report: BatteryIntelligenceReport, format: ExportFormat) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = BatteryAnalyticsExportState(isExporting = true)
            val rawSnapshots = historyStore.snapshots.first()
            exportManager.export(report, format, rawSnapshots).fold(
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

    fun import(uri: Uri) {
        if (_state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, errorMessage = null, importResult = null) }
            exportManager.parseImport(uri).fold(
                onSuccess = { parsed ->
                    val added = historyStore.importSnapshots(parsed.snapshots)
                    _state.update {
                        it.copy(
                            isImporting = false,
                            importResult = BatteryAnalyticsExportState.ImportResult(
                                addedCount = added,
                                degraded = parsed.degraded,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = error.message ?: "Battery analytics import failed.",
                        )
                    }
                },
            )
        }
    }

    fun clearShareIntent() {
        _state.update { it.copy(shareIntent = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearImportResult() {
        _state.update { it.copy(importResult = null) }
    }
}
