package com.devstdvad.devicedna.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.usecase.GetConnectivityInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NetworkState(
    val isLoading: Boolean = true,
    val network: NetworkInfo? = null,
    val connectivity: ConnectivityInfo? = null,
    val error: String? = null,
)

class NetworkViewModel(
    private val getNetwork: GetNetworkInfoUseCase,
    private val getConnectivity: GetConnectivityInfoUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(NetworkState())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val conn = async { getConnectivity() }
            _state.update { it.copy(connectivity = (conn.await() as? AppResult.Success)?.value, isLoading = false) }
        }
        getNetwork.observe().onEach { result ->
            if (result is AppResult.Success) _state.update { it.copy(network = result.value) }
        }.launchIn(viewModelScope)
    }
}
