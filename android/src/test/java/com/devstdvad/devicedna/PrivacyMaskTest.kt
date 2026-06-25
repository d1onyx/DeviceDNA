package com.devstdvad.devicedna

import com.devstdvad.devicedna.core.common.PrivacyMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyMaskTest {

    @Test
    fun `maskIpv4 hides last two octets`() {
        val masked = PrivacyMask.maskIpv4("192.168.1.100")
        assertEquals("192.168.xxx.xxx", masked)
    }

    @Test
    fun `maskIpv4 keeps first two octets visible`() {
        val masked = PrivacyMask.maskIpv4("10.0.0.1")
        assertTrue(masked.startsWith("10.0."))
    }

    @Test
    fun `maskIpv6 masks middle segments`() {
        val masked = PrivacyMask.maskIpv6("fe80::2809:96ff:fe6f:1b23")
        assertTrue(masked.contains("****"))
        assertTrue(masked.contains("::"))
        assertFalse(masked == "fe80::2809:96ff:fe6f:1b23")
    }

    @Test
    fun `maskIpv6 preserves compressed notation shape`() {
        val masked = PrivacyMask.maskIpv6("fe80::2809:96ff:fe6f:1b23")
        assertEquals("fe80::****", masked)
    }

    @Test
    fun `maskPackage retains domain prefix`() {
        val masked = PrivacyMask.maskPackage("com.example.myapp")
        assertTrue(masked.startsWith("com.example"))
        assertTrue(masked.contains("*"))
    }

    @Test
    fun `maskSsid truncates network name`() {
        val masked = PrivacyMask.maskSsid("MyHomeNetwork")
        assertTrue(masked.contains("*"))
        assertFalse(masked == "MyHomeNetwork")
    }

    @Test
    fun `maskDeviceId shows partial id`() {
        val id = "a1b2c3d4e5f6g7h8"
        val masked = PrivacyMask.maskDeviceId(id)
        assertTrue(masked.contains("****"))
        assertFalse(masked == id)
    }
}
