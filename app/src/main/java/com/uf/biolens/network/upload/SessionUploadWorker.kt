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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
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
) : CoroutineWorker(appContext, workerParams) {

    abstract suspend fun getWorkData(): UploadWorkData?
    abstract suspend fun setup(session: Session, data: UploadWorkData): UploadSpecification

    override suspend fun doWork(): Result {
        val data = getWorkData() ?: return Result.failure()
        val session = BioLensRepository.getSession(data.sessionID) ?: return Result.failure()

        val info = createForegroundInfo(session)
        setForeground(info)

        return withContext(Dispatchers.IO) {
            try {
                val (uploader, filenameProvider, formatter) = setup(session, data)
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

    private suspend fun uploadImages(session: Session, uploader: ImageUploader) {
        val images = BioLensRepository.getImagesInSession(session.sessionID)
        val total = images.size
        setProgress(0, total)
        images.forEachIndexed { i, image ->
            uploader.uploadImage(image)
            setProgress(i + 1, total)
        }
    }

    private suspend fun setProgressUploadingMetadata() {
        setProgress(
            workDataOf(
                KEY_METADATA to true
            )
        )
    }

    private suspend fun setProgress(progress: Int, max: Int) {
        setProgress(
            workDataOf(
                KEY_PROGRESS to progress,
                KEY_MAX_PROGRESS to max
            )
        )
    }

    private fun createForegroundInfo(session: Session): ForegroundInfo {
        val title = applicationContext.getString(R.string.uploading_session, session.name)
        val cancel = applicationContext.getString(R.string.cancel)

        val cancelIntent =
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(this.id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val uploadChannel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                applicationContext.getString(R.string.upload_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            uploadChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(uploadChannel)
        }

        val notification = NotificationCompat.Builder(applicationContext, UPLOAD_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setSmallIcon(R.drawable.ic_cloud_upload_24)
            .setOngoing(true)
            .addAction(R.drawable.ic_cancel_24, cancel, cancelIntent)
            .build()

        return ForegroundInfo(session.sessionID.toInt(), notification)
    }

    interface UploadWorkData {
        val sessionID: Long
        val metadataOnly: Boolean
        fun toWorkData(): androidx.work.Data
    }

    data class UploadSpecification(
        val uploader: ImageUploader,
        val filenameProvider: SessionFilenameProvider,
        val csvFormatter: SessionCSVFormatter
    )

    companion object {
        private const val TAG = "[UPLOAD_WORKER]"

        const val KEY_PROGRESS = "com.uf.biolens.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.biolens.extra.MAX_PROGRESS"
        const val KEY_METADATA = "com.uf.biolens.extra.METADATA"

        private const val UPLOAD_CHANNEL_ID: String = "com.uf.biolens.notification.uploadChannel"

        fun uniqueWorkerTag(sessionID: Long): String {
            return "UPLOAD_$sessionID"
        }
    }
}
