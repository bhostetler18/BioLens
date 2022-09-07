package com.uf.automoth.network

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
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

// TODO: CoroutineWorker, long-running as in https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running
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

        // TODO: show progress "uploading metadata" or similar?
        uploadMetadata(session, formatter, driveHelper, folder)

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

    private suspend fun uploadMetadata(
        session: Session,
        formatter: AutoMothSessionCSVFormatter,
        driveHelper: GoogleDriveHelper,
        folder: String
    ) {
        val exporter = SessionCSVExporter(formatter)

        val tmp = File(applicationContext.cacheDir, "metadata.csv")
        exporter.export(session, tmp)
        driveHelper.uploadOrGetFile(tmp, MimeTypes.CSV, folder)
        tmp.delete()
    }

    private suspend fun setProgress(progress: Int, max: Int) {
        setProgress(
            workDataOf(
                KEY_PROGRESS to progress,
                KEY_MAX_PROGRESS to max
            )
        )
    }

    companion object {
        private const val TAG = "[UPLOAD_WORKER]"
        const val KEY_SESSION_ID = "com.uf.automoth.extra.SESSION_ID"
        const val KEY_ACCOUNT_EMAIL = "com.uf.automoth.extra.ACCOUNT_EMAIL"
        const val KEY_ACCOUNT_TYPE = "com.uf.automoth.extra.ACCOUNT_TYPE"
        const val KEY_PROGRESS = "com.uf.automoth.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.automoth.extra.MAX_PROGRESS"

        fun uniqueWorkerTag(session: Session): String {
            return "UPLOAD_${session.sessionID}"
        }
    }
}
