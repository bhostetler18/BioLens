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
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkInfo
import androidx.work.WorkManager

interface SessionUploadWorkerObserver {
    fun onActiveWork()
    fun onUploadPending()
    fun onUploadConnecting()
    fun onUploadingMetadata()
    fun onUploadProgress(progress: Int, max: Int)
    fun onUploadSuccess()
    fun onUploadCancellation()
    fun onUploadFailure()
    fun onNoUploadInfo()

    fun observe(owner: LifecycleOwner, context: Context, sessionID: Long) {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(
            SessionUploadWorker.uniqueWorkerTag(sessionID)
        ).observe(owner) { infoList: List<WorkInfo>? ->
            if (infoList != null && infoList.isNotEmpty()) {
                parse(infoList[0])
            } else {
                parse(null)
            }
        }
    }

    fun cancelWorker(context: Context, sessionID: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(
            SessionUploadWorker.uniqueWorkerTag(sessionID)
        )
    }

    private fun parse(info: WorkInfo?) {
        if (info == null) {
            onNoUploadInfo()
            return
        }

        onActiveWork()
        when (info.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                onUploadPending()
            }
            WorkInfo.State.RUNNING -> {
                if (info.progress.keyValueMap.containsKey(SessionUploadWorker.KEY_METADATA)) {
                    onUploadingMetadata()
                } else if (info.progress.keyValueMap.containsKey(SessionUploadWorker.KEY_CONNECTING)) {
                    onUploadConnecting()
                } else if (info.progress.keyValueMap.containsKey(UploadWorker.KEY_PROGRESS)) {
                    val progress =
                        info.progress.keyValueMap[UploadWorker.KEY_PROGRESS] as? Int ?: 0
                    val maxProgress =
                        info.progress.keyValueMap[UploadWorker.KEY_MAX_PROGRESS] as? Int ?: 0
                    onUploadProgress(progress, maxProgress)
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                onUploadSuccess()
            }
            WorkInfo.State.CANCELLED -> {
                onUploadCancellation()
            }
            WorkInfo.State.FAILED -> {
                onUploadFailure()
            }
        }
    }
}
