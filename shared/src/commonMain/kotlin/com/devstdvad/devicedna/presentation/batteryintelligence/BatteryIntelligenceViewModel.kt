package com.devstdvad.devicedna.presentation.batteryintelligence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistoryTracker
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.domain.batteryintelligence.BatteryIntelligenceReport
import com.devstdvad.devicedna.domain.batteryintelligence.currentHour
import com.devstdvad.devicedna.domain.batteryintelligence.nextDayStartMillis
import com.devstdvad.devicedna.domain.batteryintelligence.previousDayStartMillis
import com.devstdvad.devicedna.domain.batteryintelligence.toBatteryIntelligenceReport
import com.devstdvad.devicedna.domain.batteryintelligence.todayStartMillis
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

data class BatteryIntelligenceUiState(
    val isLoading: Boolean = true,
    val isPremiumUnlocked: Boolean = false,
    val isChargingTrackingEnabled: Boolean = true,
    val intelligence: BatteryIntelligenceReport? = null,
    val error: String? = null,
)

class BatteryIntelligenceViewModel(
    observeBattery: ObserveBatteryUseCase,
    subscriptionRepository: SubscriptionRepository,
    private val historyStore: BatteryIntelligenceHistoryStore,
    private val historyTracker: BatteryHistoryTracker,
) : ViewModel() {
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
    private val selectedDayStartMillis = MutableStateFlow(todayStartMillis(timeZone))
    private val selectedHour = MutableStateFlow(currentHour(timeZone))

    private val batteryState = observeBattery().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    init {
        combine(
            subscriptionRepository.entitlements,
            batteryState,
            historyStore.chargingTrackingEnabled,
        ) { entitlements, batteryResult, trackingEnabled ->
            val info = (batteryResult as? AppResult.Success)?.value
            Triple(entitlements, info, trackingEnabled)
        }.onEach { (entitlements, info, trackingEnabled) ->
            historyTracker.onBatterySample(entitlements, info, trackingEnabled)
        }.launchIn(viewModelScope)
    }

    val state: StateFlow<BatteryIntelligenceUiState> = combine(
        combine(
            subscriptionRepository.entitlements,
            batteryState,
            historyStore.snapshots,
        ) { entitlements, batteryResult, history ->
            Triple(entitlements, batteryResult, history)
        },
        selectedDayStartMillis,
        selectedHour,
        historyStore.chargingTrackingEnabled,
    ) { (entitlements, batteryResult, history), dayStartMillis, hour, trackingEnabled ->
        val unlocked = entitlements.hasFeature(PremiumFeature.BatteryIntelligence)
        when (batteryResult) {
            null -> BatteryIntelligenceUiState(
                isLoading = true,
                isPremiumUnlocked = unlocked,
                isChargingTrackingEnabled = trackingEnabled,
            )
            is AppResult.Success -> BatteryIntelligenceUiState(
                isLoading = false,
                isPremiumUnlocked = unlocked,
                isChargingTrackingEnabled = trackingEnabled,
                intelligence = if (unlocked) {
                    batteryResult.value.toBatteryIntelligenceReport(
                        history = history,
                        selectedDayStartMillis = dayStartMillis,
                        selectedHour = hour,
                        timeZone = timeZone,
                    )
                } else {
                    null
                },
            )
            is AppResult.Error -> BatteryIntelligenceUiState(
                isLoading = false,
                isPremiumUnlocked = unlocked,
                isChargingTrackingEnabled = trackingEnabled,
                error = batteryResult.cause.message,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BatteryIntelligenceUiState(),
    )

    fun selectHour(hour: Int) {
        selectedHour.value = hour.coerceIn(0, 23)
    }

    fun setChargingTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            historyStore.setChargingTrackingEnabled(enabled)
        }
    }

    fun goToPreviousDay() {
        selectedDayStartMillis.update { millis ->
            previousDayStartMillis(millis, timeZone)
        }
    }

    fun goToNextDay() {
        selectedDayStartMillis.update { millis ->
            nextDayStartMillis(millis, timeZone)
        }
    }
}
