package com.uf.automoth.network

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import java.io.File
import java.util.*

class GoogleDriveHelper(private val drive: Drive) {

    fun upload(
        file: File,
        mimeType: String,
        folderID: String? = null,
        progressListener: MediaHttpUploaderProgressListener = DUMMY_LISTENER
    ): String {
        val fileContent = FileContent(mimeType, file)

        val fileMetadata = com.google.api.services.drive.model.File()
        folderID?.let {
            fileMetadata.parents = Collections.singletonList(folderID)
        }
        fileMetadata.name = file.name
        fileMetadata.mimeType = mimeType

        try {
            val request = drive.files().create(fileMetadata, fileContent).setFields("id")
            request.mediaHttpUploader.progressListener = progressListener
            val result = request.execute()
            Log.d(TAG, "Uploaded file with id: $result.id")
            return result.id
        } catch (e: GoogleJsonResponseException) {
            TODO("Handle exception")
        }
    }

    fun createOrGetFolder(name: String, parent: String = "root"): String {
        val request = drive.files().list().setQ(
            "mimeType='${MimeTypes.GDRIVE_FOLDER}' and trashed=false and name='$name' and '$parent' in parents"
        )
        try {
            val result = request.execute()
            return if (result.files.isEmpty()) {
                createFolder(name, parent)
            } else {
                result.files[0].id
            }
        } catch (e: GoogleJsonResponseException) {
            TODO("Handle exception")
        }
    }

    fun createFolder(name: String, parent: String): String {
        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = name
        fileMetadata.mimeType = MimeTypes.GDRIVE_FOLDER
        fileMetadata.parents = Collections.singletonList(parent)
        return try {
            val file = drive.files().create(fileMetadata)
                .setFields("id")
                .execute()
            file.id
        } catch (e: GoogleJsonResponseException) {
            TODO("Handle create exception")
        }
    }

    companion object {
        private const val TAG = "[DRIVE_HELPER]"
        const val ROOT_FOLDER_NAME = "AutoMoth"
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