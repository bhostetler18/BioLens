package com.uf.automoth.network

import android.accounts.Account
import android.content.Context
import androidx.work.Worker
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
import kotlinx.coroutines.runBlocking
import java.util.*

// TODO: CoroutineWorker, long-running as in https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running
class GoogleDriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    // TODO: handle connectivity issues
    override fun doWork(): Result {
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1)
        val accountName = inputData.getString(KEY_ACCOUNT_NAME)
        val accountType = inputData.getString(KEY_ACCOUNT_TYPE)

        if (sessionId <= 0 || accountName == null || accountType == null) {
            return Result.failure()
        }

        val session = runBlocking {
            AutoMothRepository.getSession(sessionId)
        } ?: return Result.failure()

        val account = Account(accountName, accountType)
        val driveHelper = initializeDrive(account)
        uploadSession(session, driveHelper)
        return Result.success()
    }

    private fun initializeDrive(account: Account): GoogleDriveHelper {
        // TODO: handle exceptions
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

    private fun uploadSession(session: Session, driveHelper: GoogleDriveHelper) {
        setProgress(0.0)
        val autoMothFolder = driveHelper.createOrGetFolder(GoogleDriveHelper.APP_FOLDER_NAME)
        val folder = driveHelper.createOrGetFolder(session.directory, autoMothFolder)
        val images = AutoMothRepository.getImagesInSessionBlocking(session.sessionID)
        val total = images.size
        images.forEachIndexed { i, image ->
            val file = AutoMothRepository.resolve(image, session)
            driveHelper.upload(file, MimeTypes.JPEG, folder) // TODO: check if exists?
            setProgress(i.toDouble() / total.toDouble())
        }
    }

    private fun setProgress(percent: Double) {
        setProgressAsync(workDataOf(KEY_PROGRESS to percent))
    }

    companion object {
        const val KEY_SESSION_ID = "com.uf.automoth.extra.SESSION_ID"
        const val KEY_ACCOUNT_NAME = "com.uf.automoth.extra.ACCOUNT_NAME"
        const val KEY_ACCOUNT_TYPE = "com.uf.automoth.extra.ACCOUNT_TYPE"
        const val KEY_PROGRESS = "com.uf.automoth.extra.PROGRESS"
    }
}
