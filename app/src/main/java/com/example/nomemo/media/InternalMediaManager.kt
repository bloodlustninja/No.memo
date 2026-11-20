package com.example.nomemo.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object InternalMediaManager {

    private const val DIRECTORY_NAME = "board_media"

    /**
     * Copies an image from a content:// or file:// Uri into internal app storage
     * (context.filesDir/board_media/<UUID>.jpg) on Dispatchers.IO so it persists permanently
     * across app restarts without requiring persistable Uri permissions.
     */
    suspend fun persistImageToInternalStorage(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val mediaDir = File(context.filesDir, DIRECTORY_NAME).apply { mkdirs() }
        val destFile = File(mediaDir, "${UUID.randomUUID()}.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot open input stream for $sourceUri")

        Uri.fromFile(destFile).toString()
    }

    /**
     * Deletes the local file if it resides in internal storage.
     */
    suspend fun deleteInternalImage(context: Context, imageUriString: String?) = withContext(Dispatchers.IO) {
        if (imageUriString.isNullOrBlank()) return@withContext
        try {
            val uri = Uri.parse(imageUriString)
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null && path.contains(DIRECTORY_NAME)) {
                    File(path).delete()
                }
            } else if (imageUriString.contains(DIRECTORY_NAME)) {
                val filename = File(imageUriString).name
                val targetFile = File(File(context.filesDir, DIRECTORY_NAME), filename)
                if (targetFile.exists()) {
                    targetFile.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
