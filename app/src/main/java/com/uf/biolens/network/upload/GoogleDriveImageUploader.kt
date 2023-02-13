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

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.SessionCSVExporter
import com.uf.biolens.data.export.SessionCSVFormatter
import com.uf.biolens.data.export.SessionFilenameProvider
import com.uf.biolens.network.GoogleDriveHelper
import com.uf.biolens.network.MimeTypes
import java.io.File
import java.util.*

class GoogleDriveImageUploader(
    account: Account,
    applicationContext: Context
) : ImageUploader {

    private val tmpDirectory: File = applicationContext.cacheDir
    private val driveHelper: GoogleDriveHelper = initializeDrive(account, applicationContext)

    private lateinit var session: Session
    private lateinit var filenameProvider: SessionFilenameProvider
    private lateinit var sessionFolder: String

    override suspend fun initialize(session: Session, filenameProvider: SessionFilenameProvider) {
        this.session = session
        this.filenameProvider = filenameProvider
        this.sessionFolder = getSessionFolder(filenameProvider, driveHelper)
    }

    override suspend fun uploadImage(image: Image) = withInitializedSession {
        Log.d(TAG, "Uploading image ${image.index}")
        val file = BioLensRepository.resolve(image, session)
        val filename = filenameProvider.getUniqueImageId(image.index)
        driveHelper.uploadOrGetFile(file, MimeTypes.JPEG, sessionFolder, filename)
    }

    override suspend fun uploadMetadata(formatter: SessionCSVFormatter) = withInitializedSession {
        val exporter = SessionCSVExporter(formatter)
        val tmp = File(tmpDirectory, "metadata.csv")
        exporter.export(session, tmp)
        driveHelper.uploadOrOverwriteFile(tmp, MimeTypes.CSV, folderID = sessionFolder)
        tmp.delete()
    }

    private suspend fun withInitializedSession(block: suspend () -> Unit) {
        if (::session.isInitialized &&
            ::sessionFolder.isInitialized &&
            ::filenameProvider.isInitialized
        ) {
            block()
        } else {
            throw UploadException("Session upload has not been initialized")
        }
    }

    companion object {
        private const val TAG = "[GOOGLE_DRIVE_UPLOADER]"
    }
}

private fun initializeDrive(account: Account, applicationContext: Context): GoogleDriveHelper {
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

private fun getSessionFolder(
    filenameProvider: SessionFilenameProvider,
    driveHelper: GoogleDriveHelper
): String {
    val autoMothFolder = driveHelper.createOrGetFolder(driveHelper.appFolderName)
    val sessionDirectory = filenameProvider.sessionDirectory
    return driveHelper.createOrGetFolder(sessionDirectory, autoMothFolder)
}
