package com.uf.automoth.ui.imaging

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File

interface ImageCapturerInterface {
    fun takePhoto(saveLocation: File, onSaved: ImageCapture.OnImageSavedCallback): Boolean
    fun onTakePhotoFailed(exception: ImageCaptureException)
}