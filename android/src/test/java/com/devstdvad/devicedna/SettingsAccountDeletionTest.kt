package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.account.ClearableStore
import com.devstdvad.devicedna.data.account.LocalDataWiper
import com.devstdvad.devicedna.data.auth.AccountDeletionResult
import com.devstdvad.devicedna.data.auth.AccountDeletionReadiness
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.presentation.settings.AccountDeletionUi
import com.devstdvad.devicedna.presentation.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAccountDeletionTest {

    private val testDispatcher = StandardTestDispatcher()

    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val authRepository: AuthGateway = mockk(relaxed = true)
    private val syncManager: DeviceSyncManager = mockk()

    private val localStore = object : ClearableStore {
        var cleared = false
            private set

        override suspend fun clear() {
            cleared = true
        }
    }

    private fun viewModel() = SettingsViewModel(
        settingsStore = settingsStore,
        authRepository = authRepository,
        syncManager = syncManager,
        localDataWiper = LocalDataWiper(listOf(localStore)),
    )

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        every { settingsStore.settings } returns emptyFlow()
        every { authRepository.currentUser } returns emptyFlow()
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `local data is wiped once the account is deleted`() = runTest(testDispatcher) {
        coEvery { authRepository.prepareAccountDeletion() } returns AccountDeletionReadiness.Ready
        coEvery { syncManager.deleteAccountData() } returns true
        coEvery { authRepository.deleteAccount() } returns AccountDeletionResult.Deleted

        val vm = viewModel()
        vm.deleteAccount()
        advanceUntilIdle()

        assertTrue(localStore.cleared)
        assertEquals(AccountDeletionUi.Idle, vm.accountDeletion.value)
    }

    @Test
    fun `local data survives a deletion that needs re-authentication`() = runTest(testDispatcher) {
        coEvery { authRepository.prepareAccountDeletion() } returns AccountDeletionReadiness.ReauthRequired

        val vm = viewModel()
        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(localStore.cleared)
        assertEquals(AccountDeletionUi.ReauthRequired, vm.accountDeletion.value)
        coVerify(exactly = 0) { syncManager.deleteAccountData() }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `local data survives a failed server-side purge`() = runTest(testDispatcher) {
        coEvery { authRepository.prepareAccountDeletion() } returns AccountDeletionReadiness.Ready
        coEvery { syncManager.deleteAccountData() } returns false

        val vm = viewModel()
        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(localStore.cleared)
        assertEquals(AccountDeletionUi.Failed, vm.accountDeletion.value)
    }

    @Test
    fun `readiness failure cannot leave deletion stuck or purge backend data`() = runTest(testDispatcher) {
        coEvery { authRepository.prepareAccountDeletion() } throws IllegalStateException("network")

        val vm = viewModel()
        vm.deleteAccount()
        advanceUntilIdle()

        assertFalse(localStore.cleared)
        assertEquals(AccountDeletionUi.Failed, vm.accountDeletion.value)
        coVerify(exactly = 0) { syncManager.deleteAccountData() }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }
}
