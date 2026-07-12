package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.export.readTextWithLimit
import java.io.ByteArrayInputStream
import java.nio.charset.CharacterCodingException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LimitedTextImportTest {

    @Test
    fun `reader accepts a UTF-8 document within the limit`() {
        val input = ByteArrayInputStream("DeviceDNA ✓".encodeToByteArray())

        assertEquals("DeviceDNA ✓", input.readTextWithLimit(64))
    }

    @Test
    fun `reader rejects a document that exceeds the limit`() {
        val input = ByteArrayInputStream(ByteArray(65) { 'x'.code.toByte() })

        assertThrows(IllegalArgumentException::class.java) {
            input.readTextWithLimit(64)
        }
    }

    @Test
    fun `reader rejects malformed UTF-8`() {
        val input = ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28))

        assertThrows(CharacterCodingException::class.java) {
            input.readTextWithLimit(64)
        }
    }
}
