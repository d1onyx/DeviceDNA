package com.devstdvad.devicedna.data.export

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.devstdvad.devicedna.core.CurrentActivityHolder
import com.devstdvad.devicedna.platform.FileImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.resume

/**
 * Android [FileImporter]: opens the system document picker via the current Activity's
 * ActivityResultRegistry (no pre-registration needed) and reads the chosen file's text.
 */
class AndroidFileImporter(
    private val context: Context,
    private val activityHolder: CurrentActivityHolder,
) : FileImporter {

    override suspend fun importText(mimeTypes: List<String>): String? {
        val activity = activityHolder.current as? ComponentActivity ?: return null
        val uri = pickDocument(activity, mimeTypes.toTypedArray()) ?: return null
        return withContext(Dispatchers.IO) {
            val declaredSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            require(declaredSize == null || declaredSize < 0L || declaredSize <= MAX_IMPORT_BYTES) {
                "The selected file is larger than ${MAX_IMPORT_BYTES / (1024 * 1024)} MB."
            }
            context.contentResolver.openInputStream(uri)?.use {
                it.readTextWithLimit(MAX_IMPORT_BYTES)
            }
        }
    }

    private suspend fun pickDocument(activity: ComponentActivity, mimeTypes: Array<String>): Uri? =
        suspendCancellableCoroutine { cont ->
            val key = "devicedna_import_${activity.hashCode()}_${System.nanoTime()}"
            lateinit var launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
            launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                launcher.unregister()
                if (cont.isActive) cont.resume(uri)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(mimeTypes)
        }

    private companion object {
        const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
    }
}

internal fun InputStream.readTextWithLimit(maxBytes: Int): String {
    require(maxBytes > 0) { "maxBytes must be positive." }
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "The selected file is too large." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray().decodeToString(throwOnInvalidSequence = true)
}
