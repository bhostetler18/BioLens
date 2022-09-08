package com.uf.automoth.network

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
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Session
import com.uf.automoth.data.export.AutoMothSessionCSVFormatter
import com.uf.automoth.data.export.SessionCSVExporter
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

        if (sessionId <= 0 || accountName == null || accountType == null) {
            return Result.failure()
        }
        val session = AutoMothRepository.getSession(sessionId) ?: return Result.failure()

        val info = createForegroundInfo(session)
        setForeground(info)

        val account = Account(accountName, accountType)

        return withContext(Dispatchers.IO) {
            try {
                val driveHelper = initializeDrive(account)
                uploadSession(session, driveHelper)
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
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(appName).build()
        return GoogleDriveHelper(drive, appName)
    }

    private suspend fun uploadSession(session: Session, driveHelper: GoogleDriveHelper) {
        val formatter = AutoMothSessionCSVFormatter(session)

        val autoMothFolder = driveHelper.createOrGetFolder(driveHelper.appFolderName)
        val sessionDirectory = "${session.name}-${formatter.uniqueSessionId}"
        val folder = driveHelper.createOrGetFolder(sessionDirectory, autoMothFolder)

        uploadMetadata(session, formatter, driveHelper, folder)
        uploadImages(session, formatter, driveHelper, folder)
    }

    private suspend fun uploadMetadata(
        session: Session,
        formatter: AutoMothSessionCSVFormatter,
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
        driveHelper.uploadOrGetFile(tmp, MimeTypes.CSV, folder)
        tmp.delete()
    }

    private suspend fun uploadImages(
        session: Session,
        formatter: AutoMothSessionCSVFormatter,
        driveHelper: GoogleDriveHelper,
        folder: String
    ) {
        val images = AutoMothRepository.getImagesInSession(session.sessionID)
        val total = images.size
        setProgress(0, total)
        images.forEachIndexed { i, image ->
            val file = AutoMothRepository.resolve(image, session)
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
        const val KEY_SESSION_ID = "com.uf.automoth.extra.SESSION_ID"
        const val KEY_ACCOUNT_EMAIL = "com.uf.automoth.extra.ACCOUNT_EMAIL"
        const val KEY_ACCOUNT_TYPE = "com.uf.automoth.extra.ACCOUNT_TYPE"
        const val KEY_PROGRESS = "com.uf.automoth.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.automoth.extra.MAX_PROGRESS"
        const val KEY_METADATA = "com.uf.automoth.extra.METADATA"

        private const val UPLOAD_CHANNEL_ID: String = "com.uf.automoth.notification.uploadChannel"

        fun uniqueWorkerTag(session: Session): String {
            return "UPLOAD_${session.sessionID}"
        }
    }
}
