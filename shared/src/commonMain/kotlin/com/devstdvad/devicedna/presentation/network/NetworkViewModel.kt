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
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

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
    private val publicIpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 4_000
            connectTimeoutMillis = 4_000
        }
    }
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
            val result = runCatching {
                publicIpClient.get(PUBLIC_IP_URL).bodyAsText().trim()
                    .takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException()
            }
            _state.update {
                result.fold(
                    onSuccess = { publicIp -> it.copy(publicIp = publicIp, publicIpLoading = false) },
                    onFailure = { error ->
                        // No localized message here — this is a plain ViewModel, not a
                        // @Composable, so it can't call stringRes(). The empty-message case
                        // (including the empty-body guard above) is translated by the screen.
                        it.copy(
                            publicIp = null,
                            publicIpLoading = false,
                            publicIpError = error.message?.takeIf { msg -> msg.isNotBlank() },
                        )
                    },
                )
            }
        }
    }

    override fun onCleared() {
        publicIpClient.close()
        super.onCleared()
    }

    private companion object {
        const val PUBLIC_IP_URL = "https://api.ipify.org"
    }
}
