package com.uf.automoth.utility

import android.content.Context
import com.uf.automoth.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val SHORT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
val SHORT_DATE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

fun LocalDate.formatDateTodayTomorrow(context: Context): String {
    val now = LocalDate.now()
    return if (now == this) {
        context.getString(R.string.today)
    } else if (now.plusDays(1) == this) {
        context.getString(R.string.tomorrow)
    } else {
        SHORT_DATE_FORMATTER.format(this)
    }
}

enum class TimeUnit {
    HOUR, MINUTE, SECOND
}

fun getUnit(context: Context, unit: TimeUnit, value: Int, short: Boolean): String {
    if (short) {
        return when (unit) {
            TimeUnit.HOUR -> context.getString(R.string.unit_hours_short)
            TimeUnit.MINUTE -> context.getString(R.string.unit_minutes_short)
            TimeUnit.SECOND -> context.getString(R.string.unit_seconds_short)
        }
    }
    return when (unit) {
        TimeUnit.HOUR -> context.resources.getQuantityString(R.plurals.unit_hours, value)
        TimeUnit.MINUTE -> context.resources.getQuantityString(R.plurals.unit_minutes, value)
        TimeUnit.SECOND -> context.resources.getQuantityString(R.plurals.unit_seconds, value)
    }
}