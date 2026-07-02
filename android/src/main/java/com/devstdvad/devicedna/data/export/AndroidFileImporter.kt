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
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        }
    }

    private suspend fun pickDocument(activity: ComponentActivity, mimeTypes: Array<String>): Uri? =
        suspendCancellableCoroutine { cont ->
            val key = "devicedna_import_${activity.hashCode()}_${System.nanoTime()}"
            val launcher = activity.activityResultRegistry.register(
                key,
                ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (cont.isActive) cont.resume(uri)
            }
            cont.invokeOnCancellation { launcher.unregister() }
            launcher.launch(mimeTypes)
        }
}
