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

package com.uf.biolens.network

import android.accounts.Account
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
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.BioLensSessionCSVFormatter
import com.uf.biolens.data.export.SessionCSVExporter
import com.uf.biolens.ui.common.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

class GoogleDriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1)
        val accountName = inputData.getString(KEY_ACCOUNT_EMAIL)
        val accountType = inputData.getString(KEY_ACCOUNT_TYPE)
        val metadataOnly = inputData.getBoolean(KEY_METADATA_ONLY, false)
        val includeUserMetadata = inputData.getBoolean(KEY_INCLUDE_AUTOMOTH_METADATA, false)
        val includeAutoMothMetadata = inputData.getBoolean(KEY_INCLUDE_USER_METADATA, false)

        if (sessionId <= 0 || accountName == null || accountType == null) {
            return Result.failure()
        }
        val session = BioLensRepository.getSession(sessionId) ?: return Result.failure()

        val info = createForegroundInfo(session)
        setForeground(info)

        val account = Account(accountName, accountType)

        val formatter = BioLensSessionCSVFormatter(session)
        formatter.configure(
            ExportOptions(
                includeAutoMothMetadata,
                includeUserMetadata
            ),
            BioLensRepository.metadataStore
        )

        return withContext(Dispatchers.IO) {
            try {
                val driveHelper = initializeDrive(account)
                val folder = getFolderFor(session, driveHelper, formatter)
                uploadMetadata(session, formatter, driveHelper, folder)
                if (!metadataOnly) {
                    uploadImages(session, driveHelper, formatter, folder)
                }
                return@withContext Result.success()
            } catch (e: IOException) {
                Log.e(TAG, e.toString())
                return@withContext Result.failure()
            }
        }
    }

    private fun initializeDrive(account: Account): GoogleDriveHelper {
        val credential: GoogleAccountCredential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account
        val appName = applicationContext.getString(R.string.app_name)
        val drive = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(appName).build()
        return GoogleDriveHelper(drive, appName)
    }

    private fun getFolderFor(
        session: Session,
        driveHelper: GoogleDriveHelper,
        formatter: BioLensSessionCSVFormatter
    ): String {
        val autoMothFolder = driveHelper.createOrGetFolder(driveHelper.appFolderName)
        val sessionDirectory = "${session.name}-${formatter.uniqueSessionId}"
        return driveHelper.createOrGetFolder(sessionDirectory, autoMothFolder)
    }

    private suspend fun uploadMetadata(
        session: Session,
        formatter: BioLensSessionCSVFormatter,
        driveHelper: GoogleDriveHelper,
        folder: String
    ) {
        setProgress(
            workDataOf(
                KEY_METADATA to true
            )
        )
        val exporter = SessionCSVExporter(formatter)
        val tmp = File(applicationContext.cacheDir, "metadata.csv")
        exporter.export(session, tmp)
        driveHelper.uploadOrOverwriteFile(tmp, MimeTypes.CSV, folderID = folder)
        tmp.delete()
    }

    private suspend fun uploadImages(
        session: Session,
        driveHelper: GoogleDriveHelper,
        formatter: BioLensSessionCSVFormatter,
        folder: String
    ) {
        val images = BioLensRepository.getImagesInSession(session.sessionID)
        val total = images.size
        setProgress(0, total)
        images.forEachIndexed { i, image ->
            val file = BioLensRepository.resolve(image, session)
            val filename = formatter.getUniqueImageId(image)
            driveHelper.uploadOrGetFile(file, MimeTypes.JPEG, folder, filename)
            setProgress(i + 1, total)
        }
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

    companion object {
        private const val TAG = "[UPLOAD_WORKER]"
        const val KEY_SESSION_ID = "com.uf.biolens.extra.SESSION_ID"
        const val KEY_ACCOUNT_EMAIL = "com.uf.biolens.extra.ACCOUNT_EMAIL"
        const val KEY_ACCOUNT_TYPE = "com.uf.biolens.extra.ACCOUNT_TYPE"
        const val KEY_INCLUDE_AUTOMOTH_METADATA = "com.uf.biolens.extra.INCLUDE_AUTOMOTH_METADATA"
        const val KEY_INCLUDE_USER_METADATA = "com.uf.biolens.extra.INCLUDE_USER_METADATA"
        const val KEY_METADATA_ONLY = "com.uf.biolens.extra.METADATA_ONLY"
        const val KEY_PROGRESS = "com.uf.biolens.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.biolens.extra.MAX_PROGRESS"
        const val KEY_METADATA = "com.uf.biolens.extra.METADATA"

        private const val UPLOAD_CHANNEL_ID: String = "com.uf.biolens.notification.uploadChannel"

        fun uniqueWorkerTag(sessionID: Long): String {
            return "UPLOAD_$sessionID"
        }
    }
}
