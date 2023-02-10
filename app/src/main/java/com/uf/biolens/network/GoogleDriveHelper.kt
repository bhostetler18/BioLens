/*
 * Copyright (c) 2022-2023 University of Florida
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

package com.uf.biolens.network

import android.util.Log
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import java.io.File
import java.util.*
import com.google.api.services.drive.model.File as DriveFile

class GoogleDriveHelper(private val drive: Drive, val appFolderName: String) {

    fun uploadOrGetFile(
        file: File,
        mimeType: String,
        folderID: String = "root",
        overrideFilename: String? = null,
        progressListener: MediaHttpUploaderProgressListener = DUMMY_LISTENER
    ): String {
        val filename = overrideFilename ?: file.name
        return getFileIdIfExists(filename, mimeType, folderID)
            ?: upload(file, mimeType, folderID, overrideFilename, progressListener)
    }

    fun uploadOrOverwriteFile(
        file: File,
        mimeType: String,
        folderID: String = "root",
        overrideFilename: String? = null,
        progressListener: MediaHttpUploaderProgressListener = DUMMY_LISTENER
    ) {
        if (!update(file, mimeType, folderID, overrideFilename, progressListener)) {
            upload(file, mimeType, folderID, overrideFilename, progressListener)
        }
    }

    fun upload(
        file: File,
        mimeType: String,
        folderID: String = "root",
        overrideFilename: String? = null,
        progressListener: MediaHttpUploaderProgressListener = DUMMY_LISTENER
    ): String {
        val (fileMetadata, fileContent) = makeFileRepresentation(file, mimeType, folderID, overrideFilename)
        val request = drive.files().create(fileMetadata, fileContent).setFields("id")
        request.mediaHttpUploader.progressListener = progressListener
        val result = request.execute()
        Log.d(TAG, "Uploaded file with id: ${result.id}")
        return result.id
    }

    fun update(
        file: File,
        mimeType: String,
        folderID: String = "root",
        overrideFilename: String? = null,
        progressListener: MediaHttpUploaderProgressListener = DUMMY_LISTENER
    ): Boolean {
        val filename = overrideFilename ?: file.name
        getFileIdIfExists(filename, mimeType, folderID)?.let { id ->
            // folderID is null because 'The parents field is not directly writable in update requests'
            // and we don't want to change it anyway
            val (fileMetadata, fileContent) = makeFileRepresentation(file, mimeType, null, overrideFilename)
            val request = drive.files().update(id, fileMetadata, fileContent)
            request.mediaHttpUploader.progressListener = progressListener
            request.execute()
            Log.d(TAG, "Updated file with id: $id")
            return true
        }
        Log.d(TAG, "Could not update file: $filename does not exist")
        return false
    }

    private fun makeFileRepresentation(
        file: File,
        mimeType: String,
        folderID: String?,
        overrideFilename: String? = null
    ): Pair<DriveFile, FileContent> {
        val filename = overrideFilename ?: file.name
        val fileContent = FileContent(mimeType, file)
        val fileMetadata = DriveFile()
        folderID?.let {
            fileMetadata.parents = Collections.singletonList(it)
        }
        fileMetadata.name = filename
        fileMetadata.mimeType = mimeType
        return Pair(fileMetadata, fileContent)
    }

    fun createOrGetFolder(name: String, parent: String = "root"): String {
        return getFileIdIfExists(name, MimeTypes.GDRIVE_FOLDER, parent) ?: createFolder(
            name,
            parent
        )
    }

    fun getFileIdIfExists(name: String, mimeType: String, parent: String): String? {
        val request = drive.files().list().setQ(
            "name='$name' and '$parent' in parents and mimeType='$mimeType' and trashed=false"
        )
        val result = request.execute()
        return if (result.files.isEmpty()) {
            null
        } else {
            result.files[0].id
        }
    }

    fun createFolder(name: String, parent: String): String {
        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = name
        fileMetadata.mimeType = MimeTypes.GDRIVE_FOLDER
        fileMetadata.parents = Collections.singletonList(parent)
        val file = drive.files().create(fileMetadata)
            .setFields("id")
            .execute()
        return file.id
    }

    companion object {
        private const val TAG = "[DRIVE_HELPER]"
        val DUMMY_LISTENER = MediaHttpUploaderProgressListener { uploader ->
            when (uploader.uploadState) {
                MediaHttpUploader.UploadState.INITIATION_STARTED -> Log.d(TAG, "Initiation started")
                MediaHttpUploader.UploadState.INITIATION_COMPLETE -> Log.d(
                    TAG,
                    "Initiation complete"
                )
                MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS -> Log.d(
                    TAG,
                    "Progress: ${uploader.progress}"
                )
                MediaHttpUploader.UploadState.MEDIA_COMPLETE -> Log.d(TAG, "Upload complete!")
                else -> {}
            }
        }
    }
}
