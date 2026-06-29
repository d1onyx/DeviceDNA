package com.devstdvad.devicedna.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.subscription.EntitlementSource
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionOperationResult
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isPremiumActive: Boolean = false,
    val features: Set<PremiumFeature> = emptySet(),
    val source: EntitlementSource = EntitlementSource.None,
    val expiresAtMillis: Long? = null,
    val errorMessage: String? = null,
) {
    val removesAds: Boolean
        get() = PremiumFeature.RemoveAds in features && isPremiumActive

    val widgets: Boolean
        get() = PremiumFeature.Widgets in features && isPremiumActive

    val batteryIntelligence: Boolean
        get() = PremiumFeature.BatteryIntelligence in features && isPremiumActive

    val smartAlerts: Boolean
        get() = PremiumFeature.SmartAlerts in features && isPremiumActive
}

private data class SubscriptionOperationState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val operationState = MutableStateFlow(SubscriptionOperationState())

    val state: StateFlow<SubscriptionUiState> = combine(
        subscriptionRepository.entitlements,
        operationState,
    ) { entitlements, operation ->
        SubscriptionUiState(
            isLoading = operation.isLoading,
            isPremiumActive = entitlements.isActive,
            features = entitlements.features,
            source = entitlements.source,
            expiresAtMillis = entitlements.expiresAtMillis,
            errorMessage = operation.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubscriptionUiState(),
    )

    fun activateDevPremium() {
        runOperation { subscriptionRepository.purchasePremium() }
    }

    fun restorePurchases() {
        runOperation { subscriptionRepository.restorePurchases() }
    }

    fun cancelDevPremium() {
        viewModelScope.launch {
            operationState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                subscriptionRepository.clearDevSubscription()
                operationState.update { it.copy(isLoading = false) }
            } catch (t: Throwable) {
                operationState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Unable to clear dev subscription.",
                    )
                }
            }
        }
    }

    fun dismissError() {
        operationState.update { it.copy(errorMessage = null) }
    }

    private fun runOperation(operation: suspend () -> SubscriptionOperationResult) {
        viewModelScope.launch {
            operationState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = operation()) {
                    SubscriptionOperationResult.Success -> operationState.update {
                        it.copy(isLoading = false)
                    }
                    is SubscriptionOperationResult.Failure -> operationState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            } catch (t: Throwable) {
                operationState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Subscription operation failed.",
                    )
                }
            }
        }
    }
}
