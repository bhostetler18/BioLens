package com.uf.automoth.imaging

import androidx.camera.core.ImageCapture
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

interface ImageCaptureInterface {
    var isRestartingCamera: AtomicBoolean
    fun takePhoto(saveLocation: File, onSaved: ImageCapture.OnImageSavedCallback)
    fun restartCamera()
}
