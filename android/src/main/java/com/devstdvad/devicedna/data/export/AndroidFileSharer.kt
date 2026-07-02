package com.devstdvad.devicedna.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.devstdvad.devicedna.platform.FileSharer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android [FileSharer]: writes the artifact to cacheDir, exposes it via FileProvider and
 * launches an ACTION_SEND chooser. Replaces the Intent/Uri plumbing previously embedded in
 * the export ViewModels so those can move to commonMain.
 */
class AndroidFileSharer(private val context: Context) : FileSharer {
    override suspend fun shareText(
        fileName: String,
        mimeType: String,
        subject: String,
        content: String,
    ) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, fileName).apply { writeText(content) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
