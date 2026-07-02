package com.devstdvad.devicedna.platform

/**
 * Platform abstraction for picking a file and reading its text content. Lets shared ViewModels
 * trigger an import without touching Android's ActivityResult APIs or iOS document pickers.
 *   • Android → AndroidFileImporter (ActivityResultRegistry + contentResolver)
 *   • iOS     → UIDocumentPickerViewController, later
 * Returns null if the user cancels or the file cannot be read.
 */
interface FileImporter {
    suspend fun importText(mimeTypes: List<String>): String?
}
