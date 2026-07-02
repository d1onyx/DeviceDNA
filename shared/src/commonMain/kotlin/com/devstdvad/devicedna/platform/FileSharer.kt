package com.devstdvad.devicedna.platform

/**
 * Platform abstraction for exporting a text artifact and presenting the system share UI.
 * Lets shared ViewModels trigger sharing without touching Android `Intent` / iOS
 * `UIActivityViewController`. Implemented per platform and injected via Koin:
 *   • Android → AndroidFileSharer (FileProvider + ACTION_SEND)
 *   • iOS     → IosFileSharer (UIActivityViewController)
 */
interface FileSharer {
    suspend fun shareText(
        fileName: String,
        mimeType: String,
        subject: String,
        content: String,
    )
}
