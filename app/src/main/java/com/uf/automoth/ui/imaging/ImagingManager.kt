package com.uf.automoth.ui.imaging

import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.Image
import com.uf.automoth.data.Session
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ImagingManager(
    val session: Session,
    val settings: ImagingSettings
) : ImageCapture.OnImageSavedCallback {

    var imagesTaken: Int = 0
    val sessionDirectory by lazy {
        File(AutoMothRepository.storageLocation, session.directory)
    }

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        val file = outputFileResults.savedUri?.toFile() ?: return
        val image = Image(file.name, OffsetDateTime.now(), session.sessionID)
        AutoMothRepository.insert(image)
        Log.d("[CAPTURE]", "Image captured at $file")
        imagesTaken++
    }

    override fun onError(exception: ImageCaptureException) {
        Log.d("[CAPTURE]", exception.localizedMessage ?: "")
    }

    fun shouldStop(): Boolean {
        if (!settings.autoStop) { return false }
        return when (settings.autoStopType) {
            AutoStopType.IMAGE_COUNT -> imagesTaken == settings.autoStopValue
            AutoStopType.TIME -> false // TODO: implement
        }
    }

    fun getUniqueFile(): File {
        return File(sessionDirectory, "img${imagesTaken + 1}.jpg")
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_kk_mm_ss_SSSS")
        fun getUniqueDirectory(date: OffsetDateTime): String {
            val dateString = formatter.format(date)
            return "session_$dateString"
        }
    }
}
