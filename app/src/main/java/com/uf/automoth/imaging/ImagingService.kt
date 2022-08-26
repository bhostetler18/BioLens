package com.uf.automoth.imaging

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.work.await
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.network.SingleLocationProvider
import com.uf.automoth.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

class ImagingService : LifecycleService(), ImageCaptureInterface {

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture = ImageCapture.Builder().build()
    private var imagingManager: ImagingManager? = null
    private lateinit var locationProvider: SingleLocationProvider

    override val isCameraStarted: Boolean
        get() = cameraProvider?.isBound(imageCapture) ?: false

    override fun onCreate() {
        Log.d(TAG, "On create called")
        super.onCreate()
        AutoMothRepository(this, serviceScope)
        locationProvider = SingleLocationProvider(this)
        startInForeground()
        IS_RUNNING.postValue(true)
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
                        AutoMothRepository.deletePendingSession(requestCode.toLong())
                    }
                    startSession(name, settings, shouldCancel)
                    return START_REDELIVER_INTENT
                }
            }
            ACTION_STOP_SESSION -> {
                stopCurrentSession("Service received stop action")
                killService()
                return START_REDELIVER_INTENT
            }
        }

        // In the case of an unrecognized or malformed intent, just stop the service so it doesn't
        // run indefinitely in the background (as long as there isn't an active session)
        killServiceIfInactive()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "On destroy called")
        IS_RUNNING.postValue(false)
        super.onDestroy()
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

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(R.string.service_notification_title))
            .setContentText(getText(R.string.service_notification_message))
            .setSmallIcon(R.drawable.ic_camera_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    override fun startCamera(onStart: (() -> Unit)?) {
        cameraProvider?.let {
            bindCaptureUseCase(it, onStart)
            return@startCamera
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProviderFuture.get()?.let {
                this.cameraProvider = it
                bindCaptureUseCase(it, onStart)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCaptureUseCase(cameraProvider: ProcessCameraProvider, onStart: (() -> Unit)?) {
        imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageCapture
            )
        } catch (exc: Exception) {
            stopCurrentSession("Image capture use case binding failed")
            killService()
            return
        }

        onStart?.invoke()
    }

    override fun stopCamera() {
        cameraProvider?.unbindAll()
    }

    override fun takePhoto(
        saveLocation: File,
        onSaved: ImageCapture.OnImageSavedCallback
    ) {
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            onSaved
        )
    }

    private fun startSession(name: String?, settings: ImagingSettings, cancelExisting: Boolean) {
        if (cancelExisting) {
            stopCurrentSession("Cancelled by new session $name")
        } else if (imagingManager != null) {
            Log.w(
                TAG,
                "Imaging session $name will not be started because a session is already in progress and cancelExisting=false"
            )
            return
        }
        startCamera {
            imagingManager = ImagingManager(settings, WeakReference(this)) {
                killService()
            }
            lifecycleScope.launch {
                imagingManager?.start(
                    name ?: getString(R.string.default_session_name),
                    this@ImagingService,
                    locationProvider,
                    0L
                )
            }
        }
    }

    private fun stopCurrentSession(reason: String) {
        imagingManager?.stop(reason)
        imagingManager = null
    }

    private fun killServiceIfInactive() {
        if (imagingManager == null) {
            killService()
        }
    }

    private fun killService() {
        stopForeground(true)
        stopSelf()
    }

    companion object {
        private const val TAG = "[SERVICE]"
        private const val SERVICE_NOTIFICATION_ID: Int = 1297044552
        private const val SERVICE_CHANNEL_ID: String = "com.uf.automoth.notification.serviceChannel"
        const val ACTION_START_SESSION = "com.uf.automoth.action.START_SESSION"
        const val ACTION_STOP_SESSION = "com.uf.automoth.action.STOP_SESSION"
        private const val KEY_IMAGING_SETTINGS = "com.uf.automoth.extra.IMAGING_SETTINGS"
        private const val KEY_SESSION_NAME = "com.uf.automoth.extra.SESSION_NAME"
        private const val KEY_CANCEL_EXISTING = "com.automoth.extra.CANCEL_EXISTING"
        private const val KEY_REQUEST_CODE = "com.uf.automoth.extra.REQUEST_CODE"
        val IS_RUNNING = MutableLiveData(false)

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
