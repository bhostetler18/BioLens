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
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.BioLensSessionCSVFormatter
import com.uf.biolens.ui.common.ExportOptions

class GoogleDriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : SessionUploadWorker(appContext, workerParams) {

    override suspend fun setup(session: Session, data: UploadWorkData): UploadSpecification {
        val uploadData =
            data as? GoogleDriveWorkData ?: throw UploadException("Failed to load work data")
        val formatter = BioLensSessionCSVFormatter(session)
        formatter.configure(
            ExportOptions(
                uploadData.includeAutoMothMetadata,
                uploadData.includeUserMetadata
            ),
            BioLensRepository.metadataStore
        )
        return UploadSpecification(
            GoogleDriveSessionUploader(
                uploadData.account,
                applicationContext
            ),
            formatter,
            formatter
        )
    }

    override suspend fun getWorkData(): UploadWorkData? {
        return GoogleDriveWorkData.fromData(inputData)
    }

    data class GoogleDriveWorkData(
        override val sessionID: Long,
        override val metadataOnly: Boolean,
        val account: Account,
        val includeAutoMothMetadata: Boolean,
        val includeUserMetadata: Boolean
    ) : UploadWorkData {
        override fun toWorkData(): Data {
            return workDataOf(
                KEY_SESSION_ID to sessionID,
                KEY_METADATA_ONLY to metadataOnly,
                KEY_ACCOUNT_EMAIL to account.name,
                KEY_ACCOUNT_TYPE to account.type,
                KEY_INCLUDE_AUTOMOTH_METADATA to includeAutoMothMetadata,
                KEY_INCLUDE_USER_METADATA to includeUserMetadata
            )
        }

        companion object {
            private const val KEY_SESSION_ID = "com.uf.biolens.extra.SESSION_ID"
            private const val KEY_METADATA_ONLY = "com.uf.biolens.extra.METADATA_ONLY"
            private const val KEY_ACCOUNT_EMAIL = "com.uf.biolens.extra.ACCOUNT_EMAIL"
            private const val KEY_ACCOUNT_TYPE = "com.uf.biolens.extra.ACCOUNT_TYPE"
            private const val KEY_INCLUDE_AUTOMOTH_METADATA =
                "com.uf.biolens.extra.INCLUDE_AUTOMOTH_METADATA"
            private const val KEY_INCLUDE_USER_METADATA =
                "com.uf.biolens.extra.INCLUDE_USER_METADATA"

            fun fromData(workData: Data): GoogleDriveWorkData? {
                val sessionID = workData.getLong(KEY_SESSION_ID, -1)
                if (sessionID < 0) {
                    return null
                }
                val metadataOnly = workData.getBoolean(KEY_METADATA_ONLY, false)
                val email = workData.getString(KEY_ACCOUNT_EMAIL) ?: return null
                val type = workData.getString(KEY_ACCOUNT_TYPE) ?: return null
                return GoogleDriveWorkData(
                    sessionID,
                    metadataOnly,
                    Account(email, type),
                    workData.getBoolean(KEY_INCLUDE_AUTOMOTH_METADATA, true),
                    workData.getBoolean(KEY_INCLUDE_USER_METADATA, true)
                )
            }
        }
    }
}
