package com.uf.automoth.imaging

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

enum class AutoStopMode {
    OFF, TIME, IMAGE_COUNT
}

@Serializable
@Parcelize
data class ImagingSettings(
    var interval: Int = 60,
    var autoStopMode: AutoStopMode = AutoStopMode.TIME,
    var autoStopValue: Int = 720,
    var shutdownCameraWhenPossible: Boolean = false
) : Parcelable
