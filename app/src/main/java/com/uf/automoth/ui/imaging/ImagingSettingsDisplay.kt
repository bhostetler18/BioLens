package com.uf.automoth.ui.imaging

import android.content.Context
import com.uf.automoth.R
import com.uf.automoth.imaging.AutoStopMode
import com.uf.automoth.imaging.ImagingSettings

fun ImagingSettings.intervalDescription(context: Context): String {
    return intervalDescription(this.interval, context)
}

fun ImagingSettings.autoStopDescription(context: Context): String {
    return autoStopDescription(this.autoStopMode, this.autoStopValue, context)
}

fun intervalDescription(interval: Int, context: Context): String {
    val minutes = interval / 60
    val seconds = interval % 60
    val unitMinutes = context.resources.getQuantityString(R.plurals.unit_minutes, interval)
    val unitSeconds = context.resources.getQuantityString(R.plurals.unit_seconds, interval)
    return if (minutes > 0 && seconds > 0) {
        "$minutes $unitMinutes $seconds $unitSeconds"
    } else if (minutes > 0) {
        "$minutes $unitMinutes"
    } else {
        "$seconds $unitSeconds"
    }
}

fun autoStopDescription(mode: AutoStopMode, value: Int, context: Context): String {
    return when (mode) {
        AutoStopMode.OFF -> context.getString(R.string.auto_stop_never)
        AutoStopMode.TIME -> {
            val after = context.getString(R.string.after)
            val hours = value / 60
            val unitHours = context.resources.getQuantityString(R.plurals.unit_hours, hours)
            val minutes = value % 60
            val unitMinutes = context.resources.getQuantityString(R.plurals.unit_minutes, minutes)
            return "$after " + if (hours > 0 && minutes > 0) {
                "$hours $unitHours $minutes $unitMinutes"
            } else if (hours > 0) {
                "$hours $unitHours"
            } else {
                "$minutes $unitMinutes"
            }
        }
        AutoStopMode.IMAGE_COUNT -> {
            val after = context.getString(R.string.after)
            val unit = context.resources.getQuantityString(R.plurals.unit_images, value)
            "$after $value $unit"
        }
    }
}
