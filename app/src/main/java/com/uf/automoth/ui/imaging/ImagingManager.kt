package com.uf.automoth.ui.imaging

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.ref.WeakReference
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ImagingManager(
    private val settings: ImagingSettings,
    private val capturer: WeakReference<ImageCapturerInterface>
) : ImageCapture.OnImageSavedCallback {
    private lateinit var session: Session
    private var timer: Timer? = null
    private var imagesTaken: Int = 0
    private val sessionDirectory by lazy {
        File(AutoMothRepository.storageLocation, session.directory)
    }

    fun start(sessionName: String, locationProvider: SingleLocationProvider) {
        val start = OffsetDateTime.now()
        session = Session(
            sessionName,
            getUniqueDirectory(start),
            start,
            -1.0,
            -1.0,
            settings.interval
        )
        // Need to get session primary key before continuing, so block for this call
        runBlocking {
            AutoMothRepository.create(session)
        }

        locationProvider.getCurrentLocation {
            AutoMothRepository.updateSessionLocation(session.sessionID, it)
        }

        val milliseconds: Long = settings.interval * 1000L
        timer = fixedRateTimer(TAG, false, 0, milliseconds) {
            if (shouldStop()) {
                stop()
            } else {
                val capturer = capturer.get()
                if (capturer == null ||
                    !capturer.takePhoto(getUniqueFile(), this@ImagingManager)
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
    }

    override fun onError(exception: ImageCaptureException) {
        Log.d(TAG, exception.localizedMessage ?: "")
    }

    private fun shouldStop(): Boolean {
        if (!settings.autoStop) { return false }
        return when (settings.autoStopType) {
            AutoStopType.IMAGE_COUNT -> imagesTaken == settings.autoStopValue
            AutoStopType.TIME -> false // TODO: implement
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
