package com.uf.automoth.ui.imaging

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.uf.automoth.MainActivity
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.lang.ref.WeakReference

class ImagingService : LifecycleService(), ImageCapturerInterface {

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var imageCapture: ImageCapture? = null
    private var imagingManager: ImagingManager? = null
    private val locationProvider = SingleLocationProvider(this)

    override fun onCreate() {
        Log.d("[SERVICE]", "On create called")
        super.onCreate()
        AutoMothRepository(this, serviceScope)
        IS_RUNNING = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("[SERVICE]", "On start command called")
        startInForeground()
        // Needed for lifecycle state to be marked correctly
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.d("[SERVICE]", "On destroy called")
        IS_RUNNING = false
        super.onDestroy()
    }

    private fun startInForeground() {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            getString(R.string.service_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(R.string.service_notification_title))
            .setContentText(getText(R.string.service_notification_message))
            .setSmallIcon(R.drawable.ic_camera_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageCapture
                )
                Log.d(TAG, this.lifecycle.currentState.toString())
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun takePhoto(saveLocation: File, onSaved: ImageCapture.OnImageSavedCallback): Boolean {
        val imageCapture = imageCapture ?: return false

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            onSaved
        )
        return true
    }

    fun startSession(settings: ImagingSettings) {
        imagingManager = ImagingManager(settings, WeakReference(this))
        imagingManager?.start(getString(R.string.default_session_name), locationProvider)
    }

    fun stopCurrentSession() {
        imagingManager?.stop()
        imagingManager = null
    }

    companion object {
        const val TAG = "[SERVICE]"
        const val SERVICE_NOTIFICATION_ID: Int = 1297044552
        const val SERVICE_CHANNEL_ID: String = "com.uf.automoth.serviceNotification"
        var IS_RUNNING = false
    }
}
