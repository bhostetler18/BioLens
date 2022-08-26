package com.uf.automoth.imaging

import androidx.camera.core.ImageCapture
import java.io.File

interface ImageCaptureInterface {
    fun takePhoto(saveLocation: File, onSaved: ImageCapture.OnImageSavedCallback)
    fun startCamera(onStart: (() -> Unit)?)
    fun stopCamera()
    val isCameraStarted: Boolean
}
