package com.devstdvad.devicedna.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.usecase.GetConnectivityInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class NetworkState(
    val isLoading: Boolean = true,
    val network: NetworkInfo? = null,
    val connectivity: ConnectivityInfo? = null,
    val publicIp: String? = null,
    val publicIpLoading: Boolean = false,
    val publicIpError: String? = null,
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

    fun setPublicIpLookupEnabled(enabled: Boolean) {
        if (!enabled) {
            _state.update { it.copy(publicIp = null, publicIpLoading = false, publicIpError = null) }
            return
        }
        val current = _state.value
        if (current.publicIp != null || current.publicIpLoading) return
        loadPublicIp()
    }

    private fun loadPublicIp() {
        viewModelScope.launch {
            _state.update { it.copy(publicIpLoading = true, publicIpError = null) }
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = (URL(PUBLIC_IP_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4_000
                        readTimeout = 4_000
                    }
                    try {
                        connection.inputStream.bufferedReader().use { it.readText().trim() }
                            .takeIf { it.isNotBlank() }
                            ?: error("Public IP response was empty")
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            _state.update {
                result.fold(
                    onSuccess = { publicIp -> it.copy(publicIp = publicIp, publicIpLoading = false) },
                    onFailure = { error ->
                        it.copy(
                            publicIp = null,
                            publicIpLoading = false,
                            publicIpError = error.message ?: "Public IP lookup failed",
                        )
                    },
                )
            }
        }
    }

    private companion object {
        const val PUBLIC_IP_URL = "https://api.ipify.org"
    }
}
