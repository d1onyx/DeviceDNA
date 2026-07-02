package com.devstdvad.devicedna.presentation.batteryintelligence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import com.devstdvad.devicedna.platform.FileImporter
import com.devstdvad.devicedna.platform.FileSharer
import com.devstdvad.devicedna.resources.AppLanguage
import com.devstdvad.devicedna.resources.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BatteryAnalyticsExportState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val importResult: ImportResult? = null,
) {
    data class ImportResult(
        val addedCount: Int,
        val degraded: Boolean,
    )
}

/**
 * Battery-analytics export/import, fully platform-agnostic: renders via the shared
 * [BatteryAnalyticsExporter], shares via [FileSharer], and imports via [FileImporter].
 * No Android Intent/Uri leaks into the shared UI layer.
 */
class BatteryAnalyticsExportViewModel(
    private val exporter: BatteryAnalyticsExporter,
    private val historyStore: BatteryIntelligenceHistoryStore,
    private val fileSharer: FileSharer,
    private val fileImporter: FileImporter,
) : ViewModel() {

    private val _state = MutableStateFlow(BatteryAnalyticsExportState())
    val state: StateFlow<BatteryAnalyticsExportState> = _state.asStateFlow()

    fun export(report: BatteryIntelligenceReport, format: ExportFormat) {
        if (_state.value.isExporting) return
        viewModelScope.launch {
            _state.value = BatteryAnalyticsExportState(isExporting = true)
            try {
                val rawSnapshots = historyStore.snapshots.first()
                val strings = stringsFor(AppLanguage.En)
                val adviceText = report.chargingAdvice.map { strings[it.adviceKey] }
                val nowMillis = currentTimeMillis()
                val content = exporter.render(report, format, rawSnapshots, adviceText, nowMillis)
                fileSharer.shareText(
                    fileName = exporter.fileName(nowMillis, format),
                    mimeType = exporter.mimeType(format),
                    subject = "DeviceDNA Battery Analytics",
                    content = content,
                )
                _state.value = BatteryAnalyticsExportState()
            } catch (t: Throwable) {
                _state.value = BatteryAnalyticsExportState(
                    errorMessage = t.message ?: "Battery analytics export failed.",
                )
            }
        }
    }

    fun import() {
        if (_state.value.isImporting) return
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, errorMessage = null, importResult = null) }
            try {
                val text = fileImporter.importText(listOf("application/json"))
                if (text == null) {
                    _state.update { it.copy(isImporting = false) }
                    return@launch
                }
                val parsed = exporter.parseImport(text)
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
            } catch (t: Throwable) {
                _state.update {
                    it.copy(isImporting = false, errorMessage = t.message ?: "Battery analytics import failed.")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearImportResult() {
        _state.update { it.copy(importResult = null) }
    }
}
