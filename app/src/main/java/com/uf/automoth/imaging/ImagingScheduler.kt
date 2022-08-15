package com.uf.automoth.imaging

import android.app.AlarmManager
import android.content.Context

class ImagingScheduler {

    fun scheduleSession(
        context: Context,
        name: String,
        settings: ImagingSettings,
        autoRepeat: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        alarmManager.setExact()
    }
}
