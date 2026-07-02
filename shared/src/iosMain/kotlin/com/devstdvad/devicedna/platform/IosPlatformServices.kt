@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.devstdvad.devicedna.platform

import com.devstdvad.devicedna.core.feedback.HapticManager
import com.devstdvad.devicedna.core.feedback.SoundManager
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

/*
 * iOS implementations of the shared platform-service interfaces.
 * COMPILES ONLY ON macOS (Kotlin/Native iOS target).
 */

/** Topmost view controller to present system sheets from. */
internal fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.windows
        .filterIsInstance<UIWindow>()
        .firstOrNull { it.keyWindow } ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

// ── FileSharer: UIActivityViewController ─────────────────────────────────────

class IosFileSharer : FileSharer {
    override suspend fun shareText(
        fileName: String,
        mimeType: String,
        subject: String,
        content: String,
    ) {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        @Suppress("CAST_NEVER_SUCCEEDS")
        (content as NSString).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)

        dispatch_async(dispatch_get_main_queue()) {
            val controller = UIActivityViewController(
                activityItems = listOf(url),
                applicationActivities = null,
            )
            // iPad requires a popover anchor; anchoring to the root view satisfies it.
            val presenter = topViewController()
            controller.popoverPresentationController?.sourceView = presenter?.view
            presenter?.presentViewController(controller, animated = true, completion = null)
        }
    }
}

// ── FileImporter: UIDocumentPickerViewController ─────────────────────────────

class IosFileImporter : FileImporter {

    // Delegate must be retained for the duration of the presentation.
    private var activeDelegate: PickerDelegate? = null

    private inner class PickerDelegate(
        val onPicked: (NSURL?) -> Unit,
    ) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            onPicked(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
            activeDelegate = null
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onPicked(null)
            activeDelegate = null
        }
    }

    override suspend fun importText(mimeTypes: List<String>): String? =
        suspendCancellableCoroutine { cont ->
            dispatch_async(dispatch_get_main_queue()) {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeJSON),
                    asCopy = true,
                )
                val delegate = PickerDelegate { url ->
                    val text = url?.let { picked ->
                        // Security-scoped access around the read (asCopy usually exempts,
                        // but scoping is harmless and reviewer-friendly).
                        val scoped = picked.startAccessingSecurityScopedResource()
                        val result = NSString.stringWithContentsOfURL(
                            picked, encoding = NSUTF8StringEncoding, error = null,
                        ) as String?
                        if (scoped) picked.stopAccessingSecurityScopedResource()
                        result
                    }
                    if (cont.isActive) cont.resume(text)
                }
                activeDelegate = delegate
                picker.delegate = delegate
                topViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
}

// ── Feedback: haptics + sound ────────────────────────────────────────────────

class IosHapticManager : HapticManager {
    override fun navTap() = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    override fun toggle() = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    override fun light() = impact(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

    override fun confirm() {
        onMain { UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess) }
    }

    override fun error() {
        onMain { UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError) }
    }

    private fun impact(style: UIImpactFeedbackStyle) {
        onMain { UIImpactFeedbackGenerator(style = style).impactOccurred() }
    }

    private fun onMain(block: () -> Unit) = dispatch_async(dispatch_get_main_queue()) { block() }
}

class IosSoundManager : SoundManager {
    // System sound ids: 1104 keyboard tap, 1057 tink, 1025 confirm-ish, 1073 error-ish.
    override fun navClick() = play(1104u)
    override fun toggleOn() = play(1057u)
    override fun toggleOff() = play(1057u)
    override fun success() = play(1025u)
    override fun error() = play(1073u)
    override fun release() { /* no retained audio resources on iOS */ }

    private fun play(id: UInt) = AudioServicesPlaySystemSound(id)
}

/** Selection haptic helper for pickers (parity with Android's selection feedback). */
fun selectionHaptic() {
    dispatch_async(dispatch_get_main_queue()) { UISelectionFeedbackGenerator().selectionChanged() }
}
