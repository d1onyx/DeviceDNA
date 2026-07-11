package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.account.AccountOwnerStore
import com.devstdvad.devicedna.data.account.ClearableStore
import com.devstdvad.devicedna.data.account.LocalDataWiper
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.sync.AccountCheckOutcome
import com.devstdvad.devicedna.data.sync.AccountStatus
import com.devstdvad.devicedna.data.sync.DeviceSnapshotProvider
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.data.sync.SyncApi
import com.devstdvad.devicedna.data.sync.SyncStateStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSyncManagerRemovalTest {

    private class FakeOwnerStore(private var owner: String? = "A") : AccountOwnerStore {
        override fun getOwner(): String? = owner
        override fun setOwner(uid: String) { owner = uid }
        override fun clear() { owner = null }
    }

    private class RecordingStore : ClearableStore {
        var clears = 0
            private set
        override suspend fun clear() { clears++ }
    }

    private val authRepository: AuthGateway = mockk(relaxed = true)
    private val api: SyncApi = mockk()
    private val snapshotBuilder: DeviceSnapshotProvider = mockk(relaxed = true)
    private val stateStore: SyncStateStore = mockk(relaxed = true)

    private fun manager(wiped: RecordingStore, ownerStore: AccountOwnerStore) = DeviceSyncManager(
        authRepository = authRepository,
        snapshotBuilder = snapshotBuilder,
        api = api,
        stateStore = stateStore,
        localDataWiper = LocalDataWiper(listOf(wiped)),
        accountOwnerStore = ownerStore,
    )

    @Test
    fun `a removed account wipes local data and clears the owner`() = runTest {
        every { authRepository.uid } returns "A"
        coEvery { authRepository.getIdToken() } returns "tok"
        coEvery { api.getAccountStatus("tok") } returns AccountStatus.NotFound

        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = "A")

        val outcome = manager(wiped, ownerStore).ensureAccountExists()

        assertEquals(AccountCheckOutcome.Removed, outcome)
        assertEquals(1, wiped.clears)
        assertNull(ownerStore.getOwner())
    }

    @Test
    fun `an existing account keeps local data intact`() = runTest {
        every { authRepository.uid } returns "A"
        coEvery { authRepository.getIdToken() } returns "tok"
        coEvery { api.getAccountStatus("tok") } returns AccountStatus.Exists

        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = "A")

        val outcome = manager(wiped, ownerStore).ensureAccountExists()

        assertEquals(AccountCheckOutcome.Verified, outcome)
        assertEquals(0, wiped.clears)
        assertEquals("A", ownerStore.getOwner())
    }
}
