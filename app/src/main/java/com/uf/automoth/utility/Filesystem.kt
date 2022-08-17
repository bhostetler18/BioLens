package com.uf.automoth.utility

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

suspend fun saveImageToMediaStore(image: File, name: String, mimeType: String, contentResolver: ContentResolver): Boolean = coroutineScope {
    val imageStore =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    val imageDetails = ContentValues().apply {
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
    }

    return@coroutineScope runCatching {
        withContext(Dispatchers.IO) {
            val uri = contentResolver.insert(imageStore, imageDetails) ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                contentResolver.openOutputStream(uri)?.use {
                    Files.copy(image.toPath(), it)
                } ?: return@withContext false
            } else {
                contentResolver.openFileDescriptor(uri, "w")?.use {
                    val ostream = FileOutputStream(it.fileDescriptor)
                    image.inputStream().use { istream ->
                        istream.channel.transferTo(0, istream.channel.size(), ostream.channel)
                    }
                } ?: return@withContext false
            }
            return@withContext true
        }
    }.getOrElse { false }


}
