package com.devstdvad.devicedna.presentation.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.AppDetails
import com.devstdvad.devicedna.domain.model.AppListInfo
import com.devstdvad.devicedna.domain.usecase.GetAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppsState(
    val isLoading: Boolean = true,
    val info: AppListInfo? = null,
    val filtered: List<AppDetails> = emptyList(),
    val showSystem: Boolean = false,
    val query: String = "",
    val error: String? = null,
)

class AppsViewModel(private val getApps: GetAppsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = getApps()) {
                is AppResult.Success -> _state.update { it.copy(info = r.value, filtered = r.value.apps.filter { a -> !a.isSystemApp }, isLoading = false) }
                is AppResult.Error -> _state.update { it.copy(error = r.cause.message, isLoading = false) }
            }
        }
    }

    fun onQuery(q: String) = applyFilter(q, _state.value.showSystem)
    fun toggleSystem() = applyFilter(_state.value.query, !_state.value.showSystem)

    private fun applyFilter(q: String, showSystem: Boolean) {
        _state.update { s ->
            val base = s.info?.apps ?: emptyList()
            val filtered = base.filter { a ->
                (showSystem || !a.isSystemApp) && (q.isEmpty() || a.name.contains(q, ignoreCase = true) || a.packageName.contains(q, ignoreCase = true))
            }
            s.copy(query = q, showSystem = showSystem, filtered = filtered)
        }
    }
}
