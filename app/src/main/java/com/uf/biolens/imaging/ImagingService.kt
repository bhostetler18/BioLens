/*
 * Copyright (c) 2022 University of Florida
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

package com.uf.biolens.imaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.work.await
import com.uf.automoth.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.network.SingleLocationProvider
import com.uf.biolens.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImagingService : LifecycleService(), ImageCaptureInterface {

    private val serviceScope = CoroutineScope(SupervisorJob())

    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var imageCapture: ImageCapture = ImageCapture.Builder().build()
    private var imagingManager: ImagingManager? = null

    private lateinit var locationProvider: SingleLocationProvider

    private val isSessionRunning: Boolean
        get() = imagingManager != null
    private var isWaitingForScheduledSession = false

    override val isCameraStarted: Boolean
        get() = cameraProvider?.isBound(imageCapture) ?: false

    override fun onCreate() {
        Log.d(TAG, "On create called")
        super.onCreate()
        val storageLocation = getExternalFilesDir(null)
        if (storageLocation != null) {
            BioLensRepository(this, storageLocation, serviceScope)
            locationProvider = SingleLocationProvider(this)
            startInForeground()
        } else {
            Log.d(TAG, "Failed to access external directory")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Needed for lifecycle state to be marked correctly and camera to start
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "On start command called")

        when (intent?.action) {
            ACTION_START_SESSION -> {
                intent.getParcelableExtra<ImagingSettings>(KEY_IMAGING_SETTINGS)?.let { settings ->
                    val name = intent.getStringExtra(KEY_SESSION_NAME)
                    val shouldCancel = intent.getBooleanExtra(KEY_CANCEL_EXISTING, false)
                    (intent.extras?.get(KEY_REQUEST_CODE) as? Int)?.let { requestCode ->
                        // If this session was triggered by an alarm, remove the record of its pending intent
                        BioLensRepository.deletePendingSession(requestCode.toLong())
                    }
                    lifecycleScope.launch {
                        startSession(name, settings, shouldCancel)
                    }
                    return START_REDELIVER_INTENT
                }
            }
            ACTION_STOP_SESSION -> {
                lifecycleScope.launch {
                    stopCurrentSession("Service received stop action")
                    killServiceIfInactive()
                }
                return START_REDELIVER_INTENT
            }
            ACTION_WAIT_FOR_SCHEDULED_SESSION -> {
                waitForScheduledSessions()
                return START_REDELIVER_INTENT
            }
        }

        // In the case of an unrecognized or malformed intent, just stop the service so it doesn't
        // run indefinitely in the background (as long as there isn't an active session)
        killServiceIfInactive()
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            val flags = 0
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                flags // warning here is a lint bug
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }

        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(R.string.service_waiting_session_notification_title))
            .setContentText(getText(R.string.service_waiting_session_notification_message))
            .setSmallIcon(R.drawable.ic_camera_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun setNotificationContent(
        @StringRes title: Int,
        @StringRes message: Int,
        @DrawableRes icon: Int
    ) {
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(title))
            .setContentText(getText(message))
            .setSmallIcon(icon)
            .build()
        NotificationManagerCompat.from(this).notify(SERVICE_NOTIFICATION_ID, notification)
    }

    override suspend fun startCamera() {
        cameraProvider?.let {
            bindCaptureUseCase(it)
            return@startCamera
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.await()?.let {
            this.cameraProvider = it
            bindCaptureUseCase(it)
        }
    }

    private suspend fun bindCaptureUseCase(cameraProvider: ProcessCameraProvider) =
        withContext(Dispatchers.Main) {
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@ImagingService,
                    cameraSelector,
                    imageCapture
                )
            } catch (exc: Exception) {
                stopCurrentSession("Image capture use case binding failed")
                killServiceIfInactive()
            }
        }

    override suspend fun stopCamera() {
        withContext(Dispatchers.Main) {
            cameraProvider?.unbindAll()
        }
    }

    override suspend fun takePhoto(saveLocation: File): Result<ImageCapture.OutputFileResults> =
        suspendCoroutine { continuation ->
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(saveLocation)
                .build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        continuation.resume(Result.success(outputFileResults))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(Result.failure(exception))
                    }
                }
            )
        }

    private suspend fun startSession(
        name: String?,
        settings: ImagingSettings,
        cancelExisting: Boolean
    ) {
        if (cancelExisting) {
            stopCurrentSession("Cancelled by new session $name")
        } else if (isSessionRunning) {
            Log.w(
                TAG,
                "Imaging session $name will not be started because a session is already in progress and cancelExisting=false"
            )
            return
        }

        setNotificationContent(
            R.string.service_active_session_notification_title,
            R.string.service_active_session_notification_message,
            R.drawable.ic_camera_24
        )

        IS_RUNNING.postValue(true)

        imagingManager = ImagingManager(settings, WeakReference(this)) {
            lifecycleScope.launch {
                stopCurrentSession("Auto-stop")
                killServiceIfInactive()
            }
        }
        currentImagingSettings.postValue(settings)

        startCamera()
        imagingManager!!.start(
            name ?: getString(R.string.default_session_name),
            this@ImagingService,
            locationProvider,
            0L
        )
    }

    private suspend fun stopCurrentSession(reason: String) {
        imagingManager?.stop(reason)
        imagingManager = null
        IS_RUNNING.postValue(false)
        currentImagingSettings.postValue(null)
        if (isWaitingForScheduledSession) {
            setNotificationContent(
                R.string.service_waiting_session_notification_title,
                R.string.service_waiting_session_notification_message,
                R.drawable.ic_launcher_foreground
            )
        }
    }

    private fun waitForScheduledSessions() {
        Log.d(TAG, "Waiting for scheduled session")
        if (!isWaitingForScheduledSession) {
            isWaitingForScheduledSession = true
            if (!isSessionRunning) {
                setNotificationContent(
                    R.string.service_waiting_session_notification_title,
                    R.string.service_waiting_session_notification_message,
                    R.drawable.ic_launcher_foreground
                )
            }
            BioLensRepository.allPendingSessionsFlow.asLiveData().observe(this) {
                if (it.isEmpty()) {
                    Log.d(TAG, "No pending sessions to wait for – stopping service if necessary")
                    isWaitingForScheduledSession = false
                    killServiceIfInactive()
                }
            }
        }
    }

    private fun killServiceIfInactive() {
        if (!isSessionRunning && !isWaitingForScheduledSession) {
            killService()
        }
    }

    private fun killService() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(TAG, "On destroy called")
        IS_RUNNING.postValue(false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "[SERVICE]"

        // Just for fun: this is "MOTH" in ASCII when expressed as four byte unsigned binary
        private const val SERVICE_NOTIFICATION_ID: Int = 1297044552
        private const val SERVICE_CHANNEL_ID: String = "com.uf.automoth.notification.serviceChannel"

        const val ACTION_START_SESSION = "com.uf.automoth.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.uf.automoth.action.STOP_SESSION"
        const val ACTION_WAIT_FOR_SCHEDULED_SESSION =
            "com.uf.automoth.action.WAIT_FOR_SCHEDULED_SESSION"

        private const val KEY_IMAGING_SETTINGS = "com.uf.automoth.extra.IMAGING_SETTINGS"
        private const val KEY_SESSION_NAME = "com.uf.automoth.extra.SESSION_NAME"
        private const val KEY_CANCEL_EXISTING = "com.automoth.extra.CANCEL_EXISTING"
        private const val KEY_REQUEST_CODE = "com.uf.automoth.extra.REQUEST_CODE"

        val IS_RUNNING = MutableLiveData(false)
        val currentImagingSettings = MutableLiveData<ImagingSettings?>(null)

        fun getStartSessionIntent(
            context: Context,
            settings: ImagingSettings,
            cancelExisting: Boolean,
            name: String?,
            requestCode: Int? = null
        ): Intent {
            return Intent(context, ImagingService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(KEY_IMAGING_SETTINGS, settings)
                putExtra(KEY_CANCEL_EXISTING, cancelExisting)
                name?.let {
                    putExtra(KEY_SESSION_NAME, it)
                }
                requestCode?.let {
                    putExtra(KEY_REQUEST_CODE, it)
                }
            }
        }
    }
}
