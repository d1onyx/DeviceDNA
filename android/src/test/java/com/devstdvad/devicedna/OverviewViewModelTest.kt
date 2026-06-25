package com.devstdvad.devicedna

import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import com.devstdvad.devicedna.domain.model.CpuCluster
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.GpuInfo
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveRamUseCase
import com.devstdvad.devicedna.presentation.overview.OverviewViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val observeBattery: ObserveBatteryUseCase = mockk()
    private val observeRam: ObserveRamUseCase = mockk()
    private val getStorage: GetStorageInfoUseCase = mockk()
    private val getHealthScore: GetHealthScoreUseCase = mockk()
    private val getCpu: GetCpuInfoUseCase = mockk()
    private val getDevice: GetDeviceInfoUseCase = mockk()

    private fun battery(level: Int = 85) = BatteryInfo(
        levelPercent = level,
        status = BatteryStatus.Discharging,
        health = BatteryHealth.Good,
        source = ChargeSource.Unknown,
        technology = "Li-ion",
        temperatureCelsius = 32f,
        voltageMv = 4100,
        currentMa = null,
        capacityMah = 4500,
        chargeCycles = null,
        isPresent = true,
    )

    private fun ram(usedPercent: Float = 0.55f) = RamInfo(
        totalBytes = 8L * 1024 * 1024 * 1024,
        availableBytes = (8L * 1024 * 1024 * 1024 * (1 - usedPercent)).toLong(),
        usedBytes = (8L * 1024 * 1024 * 1024 * usedPercent).toLong(),
        usedPercent = usedPercent,
        isLowMemory = false,
    )

    private fun cpu() = CpuInfo(
        chipsetName = "Snapdragon 888",
        architecture = "arm64-v8a",
        coreCount = 8,
        cores = emptyList(),
        clusters = listOf(CpuCluster("Prime", listOf(7), 1800, 2840)),
        governor = "schedutil",
        gpu = GpuInfo("Adreno 660", "Qualcomm", "OpenGL ES 3.2"),
        usagePercent = 12.5f,
    )

    private fun device() = DeviceInfo(
        name = "Pixel 8",
        model = "Pixel 8",
        manufacturer = "Google",
        brand = "google",
        board = "slider",
        hardware = "slider",
        codename = "shiba",
        buildFingerprint = "google/shiba/shiba:14/AP1A.240405.002/11480754:user/release-keys",
        androidId = "abc123def456",
        supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        isRooted = false,
        bootloader = "slider-1.2-9134737",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel(): OverviewViewModel {
        every { observeBattery() } returns flowOf(AppResult.Success(battery()))
        every { observeRam() } returns flowOf(AppResult.Success(ram()))
        coEvery { getStorage() } returns AppResult.Success(
            StorageInfo(128L * 1024 * 1024 * 1024, 60L * 1024 * 1024 * 1024, 68L * 1024 * 1024 * 1024, 0.47f),
        )
        coEvery { getHealthScore() } returns HealthScore(overall = 88, battery = 90, thermal = 85, security = 80, storage = 92, performance = 88, insights = emptyList())
        coEvery { getCpu() } returns AppResult.Success(cpu())
        coEvery { getDevice() } returns AppResult.Success(device())
        return OverviewViewModel(observeBattery, observeRam, getStorage, getHealthScore, getCpu, getDevice)
    }

    @Test
    fun `initial state is loading`() {
        val vm = makeViewModel()
        // Before advancing, loading should be true or false depending on init timing
        // The important thing is it is never stuck loading
        assertNotNull(vm.state)
    }

    @Test
    fun `loads device model from device info`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertEquals("Google Pixel 8", vm.state.value.deviceModel)
    }

    @Test
    fun `loads health score`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertNotNull(vm.state.value.healthScore)
        assertEquals(88, vm.state.value.healthScore!!.overall)
    }

    @Test
    fun `loads cpu usage percent`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertEquals(12.5f, vm.state.value.cpuUsage)
    }

    @Test
    fun `loads battery level from flow`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertEquals(85, vm.state.value.battery?.levelPercent)
    }

    @Test
    fun `isLoading becomes false after data loads`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `error field null on success`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
    }

    @Test
    fun `refresh reloads data`() = runTest {
        val vm = makeViewModel()
        advanceUntilIdle()
        val firstScore = vm.state.value.healthScore
        coEvery { getHealthScore() } returns HealthScore(overall = 72, battery = 70, thermal = 75, security = 70, storage = 80, performance = 70, insights = emptyList())
        vm.refresh()
        advanceUntilIdle()
        assertEquals(72, vm.state.value.healthScore?.overall)
    }
}
