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

package com.uf.automoth.ui.imaging

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.imaging.AutoStopMode
import com.uf.automoth.imaging.ImagingSettings
import com.uf.automoth.utility.TimeUnit
import com.uf.automoth.utility.getUnit

fun ImagingSettings.intervalDescription(context: Context, shortUnits: Boolean): String {
    return intervalDescription(this.interval, context, shortUnits)
}

fun ImagingSettings.autoStopDescription(context: Context, shortUnits: Boolean): String {
    return autoStopDescription(this.autoStopMode, this.autoStopValue, context, shortUnits)
}

fun intervalDescription(interval: Int, context: Context, shortUnits: Boolean): String {
    val minutes = interval / 60
    val seconds = interval % 60
    val unitMinutes = getUnit(context, TimeUnit.MINUTE, minutes, shortUnits)
    val unitSeconds = getUnit(context, TimeUnit.SECOND, seconds, shortUnits)
    val space = if (shortUnits) "" else " "
    return if (minutes > 0 && seconds > 0) {
        "$minutes$space$unitMinutes $seconds$space$unitSeconds"
    } else if (minutes > 0) {
        "$minutes$space$unitMinutes"
    } else {
        "$seconds$space$unitSeconds"
    }
}

fun autoStopDescription(
    mode: AutoStopMode,
    value: Int,
    context: Context,
    shortUnits: Boolean
): String {
    return when (mode) {
        AutoStopMode.OFF -> context.getString(R.string.auto_stop_never)
        AutoStopMode.TIME -> {
            val after = context.getString(R.string.after)
            val hours = value / 60
            val unitHours = getUnit(context, TimeUnit.HOUR, hours, shortUnits)
            val minutes = value % 60
            val unitMinutes = getUnit(context, TimeUnit.MINUTE, minutes, shortUnits)
            val space = if (shortUnits) "" else " "
            return "$after " + if (hours > 0 && minutes > 0) {
                "$hours$space$unitHours $minutes$space$unitMinutes"
            } else if (hours > 0) {
                "$hours$space$unitHours"
            } else {
                "$minutes$space$unitMinutes"
            }
        }
        AutoStopMode.IMAGE_COUNT -> {
            val after = context.getString(R.string.after)
            val unit = context.resources.getQuantityString(R.plurals.unit_images, value)
            "$after $value $unit"
        }
    }
}
