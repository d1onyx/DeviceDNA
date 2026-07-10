package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.account.ClearableStore
import com.devstdvad.devicedna.data.account.LocalDataWiper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataWiperTest {

    private class RecordingStore(private val failing: Boolean = false) : ClearableStore {
        var cleared = false
            private set

        override suspend fun clear() {
            if (failing) error("store unavailable")
            cleared = true
        }
    }

    @Test
    fun `wipeAll clears every store`() = runTest {
        val stores = List(3) { RecordingStore() }

        LocalDataWiper(stores).wipeAll()

        assertTrue(stores.all { it.cleared })
    }

    @Test
    fun `wipeAll keeps clearing after a store fails`() = runTest {
        val first = RecordingStore()
        val failing = RecordingStore(failing = true)
        val last = RecordingStore()

        LocalDataWiper(listOf(first, failing, last)).wipeAll()

        assertTrue(first.cleared)
        assertTrue(last.cleared)
    }
}
