package com.uf.automoth.ui.imaging

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
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
    private val onAutoStopCallback: (() -> Unit)? = null
) : ImageCapture.OnImageSavedCallback {
    private lateinit var session: Session
    private var timer: Timer? = null
    private var imagesTaken: Int = 0
    private val sessionDirectory by lazy {
        File(AutoMothRepository.storageLocation, session.directory)
    }

    suspend fun start(sessionName: String, context: Context, locationProvider: SingleLocationProvider, initialDelay: Long = 1000) {
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
                onAutoStopCallback?.invoke()
            } else {
                val capture = imageCapture.get()
                if (capture == null ||
                    !capture.takePhoto(getUniqueFile(), this@ImagingManager)
                ) {
                    Log.d(TAG, "Failed to capture image; stopping session")
                    stop()
                }
            }
        }
    }

    fun stop() {
        val end = OffsetDateTime.now()
        timer?.cancel()
        AutoMothRepository.updateSessionCompletion(session.sessionID, end)
    }

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        val file = outputFileResults.savedUri?.toFile() ?: return
        val image = Image(file.name, OffsetDateTime.now(), session.sessionID)
        AutoMothRepository.insert(image)
        Log.d(TAG, "Image captured at $file")
        imagesTaken++
        if (shouldAutoStop()) {
            stop()
            onAutoStopCallback?.invoke()
        }
    }

    override fun onError(exception: ImageCaptureException) {
        Log.d(TAG, "Image failed to capture: " + exception.localizedMessage)
        // TODO: implement below
//        capturer.get()?.onTakePhotoFailed(exception)
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
        return File(sessionDirectory, "img${imagesTaken + 1}.jpg")
    }

    companion object {
        const val TAG = "[IMAGING]"
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_kk_mm_ss_SSSS")
        fun getUniqueDirectory(date: OffsetDateTime): String {
            val dateString = formatter.format(date)
            return "session_$dateString"
        }
    }
}
