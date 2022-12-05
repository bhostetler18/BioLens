/*
 * Copyright (c) 2022 University of Florida
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
