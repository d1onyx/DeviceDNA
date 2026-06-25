package com.devstdvad.devicedna

import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CpuCluster
import com.devstdvad.devicedna.domain.model.CpuCore
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.GpuInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.repository.CpuRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareUseCaseTest {

    // --- CPU Use Case ---

    private val cpuRepo: CpuRepository = mockk()
    private val getCpuUseCase = GetCpuInfoUseCase(cpuRepo)

    private fun fakeCpu(
        chipset: String = "Snapdragon 8 Gen 3",
        cores: Int = 8,
        usage: Float? = 15f,
    ) = CpuInfo(
        chipsetName = chipset,
        architecture = "arm64-v8a",
        coreCount = cores,
        cores = (0 until cores).map {
            CpuCore(it, 2400_000L, 300_000L, 3200_000L, true)
        },
        clusters = listOf(CpuCluster("Prime", listOf(7), 1000, 3200)),
        governor = "schedutil",
        gpu = GpuInfo("Adreno 750", "Qualcomm", "OpenGL ES 3.2"),
        usagePercent = usage,
        minFreqMhz = 300,
        maxFreqMhz = 3200,
    )

    @Test
    fun `getCpuInfo returns success when repo succeeds`() = runTest {
        val expected = fakeCpu()
        coEvery { cpuRepo.getCpuInfo() } returns AppResult.Success(expected)

        val result = getCpuUseCase()

        assertTrue(result is AppResult.Success)
        assertEquals("Snapdragon 8 Gen 3", (result as AppResult.Success).value.chipsetName)
    }

    @Test
    fun `getCpuInfo returns error when repo fails`() = runTest {
        coEvery { cpuRepo.getCpuInfo() } returns AppResult.Error(AppError.IoError("read error"))

        val result = getCpuUseCase()

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `getCpuInfo invokes repo exactly once`() = runTest {
        coEvery { cpuRepo.getCpuInfo() } returns AppResult.Success(fakeCpu())
        coEvery { cpuRepo.observeCpuCores() } returns emptyFlow()

        getCpuUseCase()

        coVerify(exactly = 1) { cpuRepo.getCpuInfo() }
    }

    @Test
    fun `cpu usage percent is null when unavailable`() = runTest {
        coEvery { cpuRepo.getCpuInfo() } returns AppResult.Success(fakeCpu(usage = null))

        val result = getCpuUseCase()

        assertNull((result as AppResult.Success).value.usagePercent)
    }

    @Test
    fun `cpu core count matches`() = runTest {
        coEvery { cpuRepo.getCpuInfo() } returns AppResult.Success(fakeCpu(cores = 12))

        val result = (getCpuUseCase() as AppResult.Success).value

        assertEquals(12, result.coreCount)
        assertEquals(12, result.cores.size)
    }

    // --- Storage Use Case ---

    private val storageRepo: StorageRepository = mockk()
    private val getStorageUseCase = GetStorageInfoUseCase(storageRepo)

    private fun fakeStorage(
        total: Long = 256L * 1024 * 1024 * 1024,
        used: Long = 100L * 1024 * 1024 * 1024,
    ) = StorageInfo(
        totalBytes = total,
        usedBytes = used,
        freeBytes = total - used,
        usedPercent = used.toFloat() / total,
    )

    @Test
    fun `getStorageInfo success path returns correct used percent`() = runTest {
        coEvery { storageRepo.getStorageInfo() } returns AppResult.Success(fakeStorage())

        val result = (getStorageUseCase() as AppResult.Success).value

        val expectedPercent = 100f / 256f
        assertEquals(expectedPercent, result.usedPercent, 0.01f)
    }

    @Test
    fun `getStorageInfo free bytes equals total minus used`() = runTest {
        val total = 256L * 1024 * 1024 * 1024
        val used = 80L * 1024 * 1024 * 1024
        coEvery { storageRepo.getStorageInfo() } returns AppResult.Success(fakeStorage(total, used))

        val result = (getStorageUseCase() as AppResult.Success).value

        assertEquals(total - used, result.freeBytes)
    }

    @Test
    fun `getStorageInfo error propagates`() = runTest {
        coEvery { storageRepo.getStorageInfo() } returns AppResult.Error(AppError.Unavailable("storage unavailable"))

        val result = getStorageUseCase()

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `storage nearly full is detected by high usedPercent`() = runTest {
        val total = 128L * 1024 * 1024 * 1024
        val used = (total * 0.97f).toLong()
        coEvery { storageRepo.getStorageInfo() } returns AppResult.Success(fakeStorage(total, used))

        val result = (getStorageUseCase() as AppResult.Success).value

        assertTrue("Storage should be nearly full", result.usedPercent >= 0.95f)
    }
}
