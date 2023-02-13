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

package com.uf.biolens.ui.common

import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.uf.biolens.R
import com.uf.biolens.network.upload.SessionUploadWorkerObserver

class UploadProgressBarListener(
    private val progressBar: UploadProgressBar,
    private val sessionID: Long,
    private val onDismiss: () -> Unit,
    private val onRetry: () -> Unit
) : SessionUploadWorkerObserver {
    private val resources = progressBar.resources

    fun onBeginWorker() {
        progressBar.reset()
        progressBar.configureActionButton(resources.getString(R.string.cancel)) {
            cancelWorker(progressBar.context, sessionID)
        }
    }

    override fun onActiveWork() {
        progressBar.isVisible = true
    }

    override fun onUploadPending() {
        setLabel(R.string.waiting_internet)
    }

    override fun onUploadConnecting() {
        setLabel(R.string.connecting_to_drive)
    }

    override fun onUploadingMetadata() {
        setLabel(R.string.exporting_metadata)
    }

    override fun onUploadProgress(progress: Int, max: Int) {
        setLabel(R.string.uploading_images)
        progressBar.maxProgress = max
        progressBar.setProgress(progress)
    }

    override fun onUploadSuccess() {
        setLabel(R.string.upload_complete)
        // The Result.Success() WorkInfo update may occur before the final update to KEY_PROGRESS,
        // so just force the progress bar to show fully complete
        progressBar.showComplete()
        if (!progressBar.hasSetMaxProgress) {
            // The Result.Success() WorkInfo contains no progress information, and when
            // navigating back to a completed upload the progress bar may not have been
            // configured with the proper max value and would just show 100/100. In this
            // case, just hide the numbers
            progressBar.showNumericProgress(false)
        }

        progressBar.configureActionButton(resources.getString(R.string.dismiss)) {
            onDismiss()
        }
    }

    override fun onUploadCancellation() {
        setLabel(R.string.upload_cancelled)
        progressBar.configureActionButton(resources.getString(R.string.dismiss)) {
            onDismiss()
        }
    }

    override fun onUploadFailure() {
        setLabel(R.string.upload_failed)
        progressBar.configureActionButton(resources.getString(R.string.retry)) {
            onRetry()
        }
    }

    override fun onNoUploadInfo() {
        progressBar.isVisible = false
    }

    private fun setLabel(@StringRes res: Int) {
        progressBar.setLabel(resources.getString(res))
    }
}
