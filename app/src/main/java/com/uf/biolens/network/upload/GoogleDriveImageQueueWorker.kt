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
import com.uf.biolens.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.BioLensFilenameProvider
import com.uf.biolens.network.GoogleSignInHelper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// TODO: finish and use this in the future (rather than queues in ImagingService)?
class GoogleDriveImageQueueWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : UploadWorker(appContext, workerParams), ImageUploadQueueListener {

    override suspend fun doWork(): Result {
        val sessionID = inputData.getLong("id", -1)
        val session = BioLensRepository.getSession(sessionID) ?: return Result.failure()
        val account = GoogleSignInHelper.getGoogleAccountIfValid(applicationContext)?.account
            ?: return Result.failure()
        val title = applicationContext.getString(R.string.uploading_session, session.name)
        makeForeground(title, session.sessionID.toInt())

        coroutineScope {
            val uploader = GoogleDriveSessionUploader(account, applicationContext)
            val queue = SessionUploadQueue(this, uploader)
            queue.start(session, BioLensFilenameProvider(session))
            queue.listener = this@GoogleDriveImageQueueWorker
            val imageFlow = BioLensRepository.getImagesInSessionFlow(sessionID)
                .stateIn(this)
            val sessionFlow = BioLensRepository.getSessionFlow(sessionID).stateIn(this)
            launch {
                sessionFlow.collect { session ->
                    session?.completed?.let {
                        Log.d(TAG, "Finalized")
                        queue.finalize()
                    }
                }
            }
            launch {
                imageFlow.collect { images ->
                    Log.d(TAG, "Enqueueing image ${images.lastOrNull()?.index}")
                    images.lastOrNull()?.let {
                        queue.enqueue(it)
                    }
                }
            }

            queue.uploadJob?.join()
        }

        return Result.success()
    }

    override fun onFinishUpload(session: Session?) {
        TODO("Not yet implemented")
    }

    override fun onFailUpload(session: Session?) {
        TODO("Not yet implemented")
    }

    override fun onCancelUpload(session: Session?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "[UPLOAD_QUEUE_WORKER]"
        fun uniqueWorkerTag(sessionID: Long): String {
            return "AUTO_UPLOAD_$sessionID"
        }
    }
}
