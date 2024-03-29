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

package com.uf.biolens.imaging

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.net.toFile
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.Image
import com.uf.biolens.data.Session
import com.uf.biolens.data.export.BioLensFilenameProvider
import com.uf.biolens.data.export.SessionFilenameProvider
import com.uf.biolens.network.SingleLocationProvider
import com.uf.biolens.network.upload.SessionUploadBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.lang.ref.WeakReference
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

interface ImagingManagerListener {
    fun onSessionCreation(session: Session, settings: ImagingSettings)
    fun onAutoStop(session: Session)
}

class ImagingManager(
    private val settings: ImagingSettings,
    private val imageCapture: WeakReference<ImageCaptureInterface>,
    private val maxCameraRestarts: Int = DEFAULT_MAX_RESTART_COUNT,
    private val minShutdownInterval: Int = DEFAULT_MIN_SHUTDOWN_INTERVAL,
    private val uploadBuffer: SessionUploadBuffer? = null,
    private val listener: ImagingManagerListener? = null
) {

    private var session: Session? = null
    private var filenameProvider: SessionFilenameProvider? = null

    private var executor: ScheduledExecutorService? = null

    private var locationJob: Job? = null

    private var imagesTaken: Int = 0

    private var cameraRestartCount: Int = 0

    private val sessionDirectory: File
        get() {
            return File(BioLensRepository.storageLocation, session!!.directory)
        }

    suspend fun start(
        sessionName: String,
        context: Context,
        locationProvider: SingleLocationProvider,
        initialDelay: Int
    ) = coroutineScope {
        if (executor != null) {
            Log.d(TAG, "Manager was already started")
            return@coroutineScope
        }

        val start = OffsetDateTime.now()
        session = Session(
            sessionName,
            getUniqueDirectory(start),
            start,
            null,
            null,
            settings.interval
        )
        val sessionID = BioLensRepository.create(session!!) ?: run {
            Log.d(TAG, "Failed to create new session")
            return@coroutineScope
        }

        listener?.onSessionCreation(session!!, settings)

        filenameProvider = BioLensFilenameProvider(session!!)

        locationJob = launch {
            val maxAge = BioLensRepository.getLocationToleranceSeconds(context)
            locationProvider.getCurrentLocation(context, true, maxAge)?.let {
                Log.d(TAG, "Setting session location to $it")
                BioLensRepository.updateSessionLocation(sessionID, it.latitude, it.longitude)
            }
        }

        if (settings.automaticUpload) {
            uploadBuffer?.start(session!!, filenameProvider!!)
        }

        executor = Executors.newSingleThreadScheduledExecutor()
        executor!!.scheduleAtFixedRate({
            // The runBlocking call is an acceptable bridge between the coroutine-oriented image
            // capture methods and a traditional timer. It will only ever block the timer thread,
            // which is actually desirable since each image capture should complete before a new one
            // begins. scheduleAtFixedRate was used instead of a coroutine-based delay() because (at
            // least as of now) a coroutine "timer" implementation is not overly precise.
            runBlocking { onTimerTick() }
        }, initialDelay.toLong(), settings.interval.toLong(), TimeUnit.SECONDS)
    }

    private suspend fun onTimerTick() {
        autoStopIfNecessary()
        val capture = imageCapture.get()
        if (capture != null) {
            runCapture(capture)
        } else {
            stop("Image capture was garbage collected")
        }
        if (settings.autoStopMode == AutoStopMode.IMAGE_COUNT) {
            // check again after capturing image rather than waiting until next tick
            autoStopIfNecessary()
        }
    }

    private suspend fun runCapture(capture: ImageCaptureInterface) {
        if (shouldShutdownCamera()) {
            Log.d(TAG, "Starting camera")
            capture.startCamera()
        }

        takePhoto(capture)

        if (shouldShutdownCamera()) {
            Log.d(TAG, "Stopping camera")
            capture.stopCamera()
        }
    }

    private suspend fun takePhoto(capture: ImageCaptureInterface) {
        val requestNumber = imagesTaken + 1
        val result = capture.takePhoto(getUniqueFile(requestNumber))
        result.onSuccess {
            onImageSaved(requestNumber, it)
        }.onFailure {
            (it as? ImageCaptureException)?.let { error ->
                onError(error)
            }
        }
    }

    private fun onImageSaved(
        requestNumber: Int,
        outputFileResults: ImageCapture.OutputFileResults
    ) {
        val file = outputFileResults.savedUri?.toFile() ?: return
        val image = Image(requestNumber, file.name, OffsetDateTime.now(), session!!.sessionID)
        BioLensRepository.insert(image)
        Log.d(TAG, "Image captured at $file")
        imagesTaken++

        if (settings.automaticUpload) {
            uploadBuffer?.enqueue(image)
        }
    }

    private suspend fun onError(exception: ImageCaptureException) {
        Log.d(TAG, "Image failed to capture: " + exception.localizedMessage)
        when (exception.imageCaptureError) {
            ImageCapture.ERROR_CAMERA_CLOSED,
            ImageCapture.ERROR_INVALID_CAMERA,
            ImageCapture.ERROR_CAPTURE_FAILED -> {
                tryRestartCamera()
            }
            ImageCapture.ERROR_UNKNOWN,
            ImageCapture.ERROR_FILE_IO -> {
                stop(exception.localizedMessage ?: "Unknown exception")
            }
        }
    }

    suspend fun stopAndAwaitTermination() {
        executor?.shutdown()
        // Wait for execution of currently running capture (if any)
        runInterruptible(Dispatchers.IO) {
            executor?.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
            Log.d(TAG, "Last capture completed")
        }
        finish()
    }

    private suspend fun stop(reason: String) {
        Log.d(TAG, "Stopping session: $reason")
        finish()
        executor?.shutdown()
    }

    private suspend fun finish() {
        locationJob?.cancel()
        locationJob = null
        imagesTaken = 0
        cameraRestartCount = 0
        session?.sessionID?.let {
            BioLensRepository.updateSessionCompletion(it)
        }
//        session = null
        filenameProvider = null
        uploadBuffer?.finalize()
        Log.d(TAG, "Session stopped successfully")
    }

    private suspend fun tryRestartCamera() {
        val capture = imageCapture.get() ?: run {
            stop("Failed to restart camera; image capture was garbage collected")
            return
        }

        if (cameraRestartCount < maxCameraRestarts) {
            cameraRestartCount++
            Log.d(TAG, "Attempting to restart camera: attempt #$cameraRestartCount")
            capture.startCamera()
        } else {
            stop("Camera restart unsuccessful")
        }
    }

    private suspend fun autoStopIfNecessary() {
        if (shouldAutoStop()) {
            stop("Auto-stop")
            // call this last as it may do something like kill the containing service
            listener?.onAutoStop(session!!)
        }
    }

    private fun shouldAutoStop(): Boolean {
        return when (settings.autoStopMode) {
            AutoStopMode.OFF -> false
            AutoStopMode.IMAGE_COUNT -> imagesTaken == settings.autoStopValue
            AutoStopMode.TIME -> {
                val now = OffsetDateTime.now()
                val minutesPassed = session!!.started.until(now, ChronoUnit.MINUTES)
                return minutesPassed >= settings.autoStopValue
            }
        }
    }

    private fun shouldShutdownCamera(): Boolean {
        return settings.shutdownCameraWhenPossible && settings.interval >= minShutdownInterval
    }

    private fun getUniqueFile(requestNumber: Int): File {
        val filename = filenameProvider!!.getUniqueImageId(requestNumber)
        return File(sessionDirectory, "$filename.jpg")
    }

    companion object {
        private const val TAG = "[IMAGING]"
        const val DEFAULT_MAX_RESTART_COUNT = 3
        const val DEFAULT_MIN_SHUTDOWN_INTERVAL = 60
        private val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy_MM_dd_kk_mm_ss_SSSS")

        fun getUniqueDirectory(date: OffsetDateTime): String {
            val dateString = formatter.format(date)
            return "session_$dateString"
        }
    }
}
