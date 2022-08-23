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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

// TODO: CoroutineWorker, long-running as in https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running
class GoogleDriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1)
        val accountName = inputData.getString(KEY_ACCOUNT_NAME)
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
                e.localizedMessage?.let {
                    Log.d(TAG, it)
                }
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
        val drive = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName(applicationContext.getString(R.string.app_name)).build()
        return GoogleDriveHelper(drive)
    }

    private suspend fun uploadSession(session: Session, driveHelper: GoogleDriveHelper) {
        val autoMothFolder = driveHelper.createOrGetFolder(GoogleDriveHelper.APP_FOLDER_NAME)
        val folder = driveHelper.createOrGetFolder(session.directory, autoMothFolder)
        val images = AutoMothRepository.getImagesInSessionBlocking(session.sessionID)
        val total = images.size
        setProgress(0, total)
        images.forEachIndexed { i, image ->
            val file = AutoMothRepository.resolve(image, session)
            driveHelper.uploadOrGetFile(file, MimeTypes.JPEG, folder)
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

    companion object {
        private const val TAG = "[UPLOAD_WORKER]"
        const val KEY_SESSION_ID = "com.uf.automoth.extra.SESSION_ID"
        const val KEY_ACCOUNT_NAME = "com.uf.automoth.extra.ACCOUNT_NAME"
        const val KEY_ACCOUNT_TYPE = "com.uf.automoth.extra.ACCOUNT_TYPE"
        const val KEY_PROGRESS = "com.uf.automoth.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.automoth.extra.MAX_PROGRESS"

        fun uniqueWorkerTag(session: Session): String {
            return "UPLOAD_${session.sessionID}"
        }
    }
}
