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

package com.uf.biolens.network.upload

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.SessionCSVFormatter
import com.uf.biolens.data.export.SessionFilenameProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SessionUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : UploadWorker(appContext, workerParams) {

    abstract suspend fun getWorkData(): UploadWorkData?
    abstract suspend fun setup(session: Session, data: UploadWorkData): UploadSpecification

    override suspend fun doWork(): Result {
        val data = getWorkData() ?: return Result.failure()
        val session = BioLensRepository.getSession(data.sessionID) ?: return Result.failure()

        val title = applicationContext.getString(R.string.uploading_session, session.name)
        makeForeground(title, data.sessionID.toInt())

        return withContext(Dispatchers.IO) {
            try {
                val (uploader, filenameProvider, formatter) = setup(session, data)
                setProgressConnecting()
                uploader.initialize(session, filenameProvider)
                setProgressUploadingMetadata()
                uploader.uploadMetadata(formatter)
                if (!data.metadataOnly) {
                    uploadImages(session, uploader)
                }
                return@withContext Result.success()
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                return@withContext Result.failure()
            }
        }
    }

    private suspend fun uploadImages(session: Session, uploader: SessionUploader) {
        val images = BioLensRepository.getImagesInSession(session.sessionID)
        val total = images.size
        setProgress(0, total)
        images.forEachIndexed { i, image ->
            uploader.uploadImage(image)
            setProgress(i + 1, total)
        }
    }

    private suspend fun setProgressConnecting() {
        setProgress(
            workDataOf(
                KEY_CONNECTING to true
            )
        )
    }

    private suspend fun setProgressUploadingMetadata() {
        setProgress(
            workDataOf(
                KEY_METADATA to true
            )
        )
    }

    interface UploadWorkData {
        val sessionID: Long
        val metadataOnly: Boolean
        fun toWorkData(): androidx.work.Data
    }

    data class UploadSpecification(
        val uploader: SessionUploader,
        val filenameProvider: SessionFilenameProvider,
        val csvFormatter: SessionCSVFormatter
    )

    companion object {
        private const val TAG = "[SESSION_UPLOAD_WORKER]"
        const val KEY_METADATA = "com.uf.biolens.extra.METADATA"
        const val KEY_CONNECTING = "com.uf.biolens.extra.CONNECTING"

        fun uniqueWorkerTag(sessionID: Long): String {
            return "UPLOAD_$sessionID"
        }
    }
}
