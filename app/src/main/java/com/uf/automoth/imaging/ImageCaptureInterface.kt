package com.uf.automoth.imaging

import androidx.camera.core.ImageCapture
import java.io.File

interface ImageCaptureInterface {
    suspend fun startCamera()
    suspend fun stopCamera()
    suspend fun takePhoto(saveLocation: File): Result<ImageCapture.OutputFileResults>
    val isCameraStarted: Boolean
}
