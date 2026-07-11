package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.account.AccountOwnerStore
import com.devstdvad.devicedna.data.account.AccountScopeGuard
import com.devstdvad.devicedna.data.account.ClearableStore
import com.devstdvad.devicedna.data.account.LocalDataWiper
import com.devstdvad.devicedna.data.auth.AuthGateway
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountScopeGuardTest {

    private class FakeOwnerStore(private var owner: String? = null) : AccountOwnerStore {
        override fun getOwner(): String? = owner
        override fun setOwner(uid: String) { owner = uid }
        override fun clear() { owner = null }
    }

    private class RecordingStore : ClearableStore {
        var clears = 0
            private set
        override suspend fun clear() { clears++ }
    }

    private val authGateway: AuthGateway = mockk(relaxed = true)

    @Test
    fun `first sign-in claims ownership without wiping`() = runTest {
        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = null)
        val guard = AccountScopeGuard(authGateway, ownerStore, LocalDataWiper(listOf(wiped)))

        guard.onAccountChanged("A")

        assertEquals(0, wiped.clears)
        assertEquals("A", ownerStore.getOwner())
    }

    @Test
    fun `re-login of the same account does not wipe`() = runTest {
        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = "A")
        val guard = AccountScopeGuard(authGateway, ownerStore, LocalDataWiper(listOf(wiped)))

        guard.onAccountChanged("A")

        assertEquals(0, wiped.clears)
        assertEquals("A", ownerStore.getOwner())
    }

    @Test
    fun `a different account wipes and takes ownership`() = runTest {
        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = "A")
        val guard = AccountScopeGuard(authGateway, ownerStore, LocalDataWiper(listOf(wiped)))

        guard.onAccountChanged("B")

        assertEquals(1, wiped.clears)
        assertEquals("B", ownerStore.getOwner())
    }

    @Test
    fun `sign-out keeps the owner and wipes nothing`() = runTest {
        val wiped = RecordingStore()
        val ownerStore = FakeOwnerStore(owner = "A")
        val guard = AccountScopeGuard(authGateway, ownerStore, LocalDataWiper(listOf(wiped)))

        guard.onAccountChanged(null)

        assertEquals(0, wiped.clears)
        assertEquals("A", ownerStore.getOwner())
    }
}
