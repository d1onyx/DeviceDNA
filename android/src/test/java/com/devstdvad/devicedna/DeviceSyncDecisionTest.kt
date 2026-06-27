package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.sync.SnapshotHasher
import com.devstdvad.devicedna.data.sync.SyncDecision
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.DeviceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSyncDecisionTest {

    private val now = 1_000_000_000_000L

    @Test
    fun `push when device missing on server`() {
        assertTrue(
            SyncDecision.shouldPush(
                force = false,
                serverExists = false,
                serverHash = null,
                localHash = "h1",
                lastSyncTimeMs = now,
                nowMs = now,
            ),
        )
    }

    @Test
    fun `push when hash differs`() {
        assertTrue(
            SyncDecision.shouldPush(
                force = false,
                serverExists = true,
                serverHash = "server",
                localHash = "local",
                lastSyncTimeMs = now,
                nowMs = now,
            ),
        )
    }

    @Test
    fun `push when stale beyond 24h`() {
        assertTrue(
            SyncDecision.shouldPush(
                force = false,
                serverExists = true,
                serverHash = "same",
                localHash = "same",
                lastSyncTimeMs = now - (25L * 60 * 60 * 1000),
                nowMs = now,
            ),
        )
    }

    @Test
    fun `skip when synced recently and hash matches`() {
        assertFalse(
            SyncDecision.shouldPush(
                force = false,
                serverExists = true,
                serverHash = "same",
                localHash = "same",
                lastSyncTimeMs = now - 1000,
                nowMs = now,
            ),
        )
    }

    @Test
    fun `force always pushes`() {
        assertTrue(
            SyncDecision.shouldPush(
                force = true,
                serverExists = true,
                serverHash = "same",
                localHash = "same",
                lastSyncTimeMs = now,
                nowMs = now,
            ),
        )
    }

    @Test
    fun `stable hash ignores volatile fields but reacts to identity changes`() {
        val base = DeviceSnapshot(
            device = DeviceInfo(
                name = "Pixel",
                model = "Pixel 8",
                manufacturer = "Google",
                brand = "google",
                board = "b",
                hardware = "h",
                codename = "c",
                buildFingerprint = "fp-1",
                androidId = "aid-1",
                supportedAbis = listOf("arm64-v8a"),
                isRooted = false,
                bootloader = "bl",
            ),
        )
        val sameIdentity = base.copy(
            device = base.device!!.copy(), // identity unchanged
        )
        val changedFingerprint = base.copy(
            device = base.device!!.copy(buildFingerprint = "fp-2"),
        )

        assertEquals(SnapshotHasher.stableHash(base), SnapshotHasher.stableHash(sameIdentity))
        assertNotEquals(SnapshotHasher.stableHash(base), SnapshotHasher.stableHash(changedFingerprint))
    }
}
