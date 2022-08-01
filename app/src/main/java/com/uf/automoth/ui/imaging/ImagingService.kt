package com.uf.automoth.ui.imaging

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
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
    private lateinit var locationProvider: SingleLocationProvider

    override fun onCreate() {
        Log.d("[SERVICE]", "On create called")
        super.onCreate()
        AutoMothRepository(this, serviceScope)
        locationProvider = SingleLocationProvider(this)
        startInForeground()
        IS_RUNNING.postValue(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("[SERVICE]", "On start command called")

        if (intent != null) {
            when (intent.action) {
                ACTION_START_SESSION -> {
                    intent.getParcelableExtra<ImagingSettings>("IMAGING_SETTINGS")?.let {
                        val name = intent.getStringExtra("SESSION_NAME")
                        startSession(name, it)
                    }
                }
                ACTION_STOP_SESSION -> stopCurrentSession()
            }
        }

        // Needed for lifecycle state to be marked correctly and camera to start
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.d("[SERVICE]", "On destroy called")
        IS_RUNNING.postValue(false)
        super.onDestroy()
    }

    private fun startInForeground() {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= 26) {
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(R.string.service_notification_title))
            .setContentText(getText(R.string.service_notification_message))
            .setSmallIcon(R.drawable.ic_camera_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startCamera(onInitialize: () -> Unit) {
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
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

            onInitialize()
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

    override fun onTakePhotoFailed(exception: ImageCaptureException) {
//        if (exception.imageCaptureError == ImageCapture.ERROR_CAMERA_CLOSED) {
//            startCamera { }
//        }
    }

    private fun startSession(name: String?, settings: ImagingSettings) {
        if (imagingManager != null) {
            Log.d(TAG, "Imaging session already in progress")
            return
        }
        startCamera {
            imagingManager = ImagingManager(settings, WeakReference(this)) {
                killService()
            }
            imagingManager?.start(
                name ?: getString(R.string.default_session_name),
                locationProvider
            )
        }
    }

    private fun stopCurrentSession() {
        imagingManager?.stop()
        imagingManager = null
        killService()
    }

    private fun killService() {
        stopForeground(true)
        stopSelf()
    }

    companion object {
        const val TAG = "[SERVICE]"
        const val SERVICE_NOTIFICATION_ID: Int = 1297044552
        const val SERVICE_CHANNEL_ID: String = "com.uf.automoth.notification.serviceChannel"
        const val ACTION_START_SESSION = "com.uf.automoth.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.uf.automoth.action.STOP_SESSION"
        val IS_RUNNING = MutableLiveData(false)
    }
}
