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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.uf.biolens.R

abstract class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    protected suspend fun setProgress(progress: Int, max: Int) {
        setProgress(
            workDataOf(
                KEY_PROGRESS to progress,
                KEY_MAX_PROGRESS to max
            )
        )
    }

    suspend fun makeForeground(title: String, id: Int) {
        setForeground(createForegroundInfo(title, id))
    }

    private fun createForegroundInfo(title: String, id: Int): ForegroundInfo {
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

        return ForegroundInfo(id, notification)
    }

    companion object {
        const val KEY_PROGRESS = "com.uf.biolens.extra.PROGRESS"
        const val KEY_MAX_PROGRESS = "com.uf.biolens.extra.MAX_PROGRESS"

        private const val UPLOAD_CHANNEL_ID: String = "com.uf.biolens.notification.uploadChannel"
    }
}
