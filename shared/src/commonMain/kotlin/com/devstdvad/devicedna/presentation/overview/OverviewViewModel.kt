package com.devstdvad.devicedna.presentation.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetThermalInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveRamUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OverviewState(
    val isLoading: Boolean = true,
    val battery: BatteryInfo? = null,
    val ram: RamInfo? = null,
    val storage: StorageInfo? = null,
    val healthScore: HealthScore? = null,
    val cpuUsage: Float? = null,
    val deviceModel: String? = null,
    val network: NetworkInfo? = null,
    val thermal: ThermalInfo? = null,
    val error: String? = null,
)

class OverviewViewModel(
    private val observeBattery: ObserveBatteryUseCase,
    private val observeRam: ObserveRamUseCase,
    private val getStorage: GetStorageInfoUseCase,
    private val getHealthScore: GetHealthScoreUseCase,
    private val getCpuInfo: GetCpuInfoUseCase,
    private val getDeviceInfo: GetDeviceInfoUseCase,
    private val getNetwork: GetNetworkInfoUseCase,
    private val getThermal: GetThermalInfoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state.asStateFlow()

    init {
        observeBattery().onEach { result ->
            if (result is AppResult.Success) _state.update { it.copy(battery = result.value) }
        }.launchIn(viewModelScope)

        observeRam().onEach { result ->
            if (result is AppResult.Success) _state.update { it.copy(ram = result.value) }
        }.launchIn(viewModelScope)

        getNetwork.observe().onEach { result ->
            if (result is AppResult.Success) _state.update { it.copy(network = result.value) }
        }.launchIn(viewModelScope)

        getThermal.observe().onEach { result ->
            if (result is AppResult.Success) _state.update { it.copy(thermal = result.value) }
            else if (result is AppResult.Error) _state.update { it.copy(thermal = null) }
        }.launchIn(viewModelScope)

        load()
    }

    fun refresh() = load()

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val storageDeferred = async { getStorage() }
            val scoreDeferred = async { getHealthScore() }
            val cpuDeferred = async { getCpuInfo() }
            val deviceDeferred = async { getDeviceInfo() }

            val storage = (storageDeferred.await() as? AppResult.Success)?.value
            val score = scoreDeferred.await()
            val cpuResult = cpuDeferred.await()
            val deviceResult = deviceDeferred.await()

            val cpu = (cpuResult as? AppResult.Success)?.value
            val device = (deviceResult as? AppResult.Success)?.value

            val errorMsg = when {
                cpuResult is AppResult.Error && deviceResult is AppResult.Error ->
                    "Some hardware data unavailable"
                else -> null
            }

            _state.update {
                it.copy(
                    storage = storage,
                    healthScore = score,
                    cpuUsage = cpu?.usagePercent,
                    deviceModel = device?.let { d -> "${d.manufacturer} ${d.model}" },
                    isLoading = false,
                    error = errorMsg,
                )
            }
        }
    }
}
