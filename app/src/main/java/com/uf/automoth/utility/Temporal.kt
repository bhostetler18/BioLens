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
