package com.uf.automoth.imaging

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import com.uf.automoth.network.SingleLocationProvider
import java.io.File
import java.lang.ref.WeakReference
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ImagingManager(
    private val settings: ImagingSettings,
    private val imageCapture: WeakReference<ImageCaptureInterface>,
    private val maxCameraRestarts: Int = DEFAULT_MAX_RESTART_COUNT,
    private val onAutoStopCallback: (() -> Unit)? = null
) : ImageCapture.OnImageSavedCallback {
    private lateinit var session: Session
    private var timer: Timer? = null
    private var imageRequestNumber: Int = 0
    private var imagesTaken: Int = 0
    private var cameraRestartCount: Int = 0

    private val sessionDirectory by lazy {
        File(AutoMothRepository.storageLocation, session.directory)
    }

    suspend fun start(
        sessionName: String,
        context: Context,
        locationProvider: SingleLocationProvider,
        initialDelay: Long = 1000
    ) {
        val start = OffsetDateTime.now()
        session = Session(
            sessionName,
            getUniqueDirectory(start),
            start,
            -1.0,
            -1.0,
            settings.interval
        )
        val sessionID = AutoMothRepository.create(session) ?: run {
            Log.d(TAG, "Failed to create new session")
            return
        }

        locationProvider.getCurrentLocation(context) { location ->
            location?.let {
                AutoMothRepository.updateSessionLocation(sessionID, it)
            }
        }

        val milliseconds: Long = settings.interval * 1000L
        timer = fixedRateTimer(TAG, false, initialDelay, milliseconds) {
            if (shouldAutoStop()) {
                stop()
            } else {
                val capture = imageCapture.get()
                if (capture != null && !capture.isRestartingCamera.get()) {
                    capture.takePhoto(getUniqueFile(), this@ImagingManager)
                } else {
                    Log.d(TAG, "Image capture was garbage collected, stopping session")
                    stop()
                }
            }
        }
    }

    fun stop() {
        val end = OffsetDateTime.now()
        timer?.cancel()
        timer = null
        AutoMothRepository.updateSessionCompletion(session.sessionID, end)
        onAutoStopCallback?.invoke()
        imagesTaken = 0
        imageRequestNumber = 0
        cameraRestartCount = 0
    }

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        val file = outputFileResults.savedUri?.toFile() ?: return
        val image = Image(file.name, OffsetDateTime.now(), session.sessionID)
        AutoMothRepository.insert(image)
        Log.d(TAG, "Image captured at $file")
        imagesTaken++
        if (shouldAutoStop()) {
            stop()
        }
    }

    override fun onError(exception: ImageCaptureException) {
        Log.d(TAG, "Image failed to capture: " + exception.localizedMessage)
        when (exception.imageCaptureError) {
            ImageCapture.ERROR_CAMERA_CLOSED,
            ImageCapture.ERROR_INVALID_CAMERA,
            ImageCapture.ERROR_CAPTURE_FAILED -> {
                tryRestartCamera()
            }
            ImageCapture.ERROR_UNKNOWN, ImageCapture.ERROR_FILE_IO -> {
                Log.d(TAG, "Stopping session due to ${exception.localizedMessage}")
                stop()
            }
        }
    }

    private fun tryRestartCamera() {
        val capture = imageCapture.get() ?: run {
            Log.d(TAG, "Failed to restart camera; image capture was garbage collected")
            stop()
            return
        }

        if (capture.isRestartingCamera.get()) {
            Log.d(TAG, "Camera restart requested while camera was already restarting")
        } else if (cameraRestartCount < maxCameraRestarts) {
            cameraRestartCount++
            Log.d(TAG, "Attempting to restart camera: attempt #$cameraRestartCount")
            capture.restartCamera()
        } else {
            Log.d(TAG, "Camera restart unsuccessful; stopping session")
            stop()
        }
    }

    private fun shouldAutoStop(): Boolean {
        return when (settings.autoStopMode) {
            AutoStopMode.OFF -> false
            AutoStopMode.IMAGE_COUNT -> imagesTaken == settings.autoStopValue
            AutoStopMode.TIME -> {
                val now = OffsetDateTime.now()
                val minutesPassed = session.started.until(now, ChronoUnit.MINUTES)
                return minutesPassed >= settings.autoStopValue
            }
        }
    }

    private fun getUniqueFile(): File {
        return File(sessionDirectory, "img${++imageRequestNumber}.jpg")
    }

    companion object {
        const val TAG = "[IMAGING]"
        const val DEFAULT_MAX_RESTART_COUNT = 3
        private val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy_MM_dd_kk_mm_ss_SSSS")

        fun getUniqueDirectory(date: OffsetDateTime): String {
            val dateString = formatter.format(date)
            return "session_$dateString"
        }
    }
}