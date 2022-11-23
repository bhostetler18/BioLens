/*
 * Copyright (c) 2022 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.automoth.utility

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

suspend fun saveImageToMediaStore(
    image: File,
    name: String,
    mimeType: String,
    contentResolver: ContentResolver
): Boolean = coroutineScope {
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

fun File.copyTo(file: File) {
    inputStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}
