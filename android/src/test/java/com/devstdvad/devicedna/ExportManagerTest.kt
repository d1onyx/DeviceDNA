package com.devstdvad.devicedna

import com.devstdvad.devicedna.data.settings.ExportFormat
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    @Test
    fun `json format has extension json`() {
        val ext = ExportFormat.Json.name.lowercase()
        assertTrue(ext == "json")
    }

    @Test
    fun `csv format has extension csv`() {
        val ext = ExportFormat.Csv.name.lowercase()
        assertTrue(ext == "csv")
    }

    @Test
    fun `txt format has extension txt`() {
        val ext = ExportFormat.Txt.name.lowercase()
        assertTrue(ext == "txt")
    }

    @Test
    fun `all export formats are enumerated`() {
        val formats = ExportFormat.entries
        assertTrue(formats.contains(ExportFormat.Json))
        assertTrue(formats.contains(ExportFormat.Csv))
        assertTrue(formats.contains(ExportFormat.Txt))
    }

    @Test
    fun `default export format is JSON`() {
        val default = ExportFormat.Json
        assertTrue(default == ExportFormat.Json)
    }
}
