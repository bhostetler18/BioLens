package com.uf.automoth.imaging

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.core.app.AlarmManagerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.PendingSession
import java.time.OffsetDateTime

class ImagingScheduler(context: Context) {

    enum class SchedulingConflictType {
        WILL_CANCEL, WILL_BE_CANCELLED
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun requestExactAlarmPermissionIfNecessary(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val dialogBuilder = MaterialAlertDialogBuilder(context)
            dialogBuilder.setTitle(context.getString(R.string.schedule_session))
            dialogBuilder.setMessage(context.getString(R.string.warn_needs_alarm_permission))
            dialogBuilder.setPositiveButton(context.getString(R.string.go_to_settings)) { dialog, _ ->
                val intent = Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                dialog.dismiss()
            }
            dialogBuilder.setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            dialogBuilder.create().show()
        }
    }

    suspend fun scheduleSession(
        context: Context,
        name: String,
        settings: ImagingSettings,
        startTime: OffsetDateTime,
        cancelExisting: Boolean
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return false
        }

        val pendingSession = PendingSession(name, settings, startTime)
        val requestCode = AutoMothRepository.create(pendingSession).toInt()

        val intent = ImagingService.getStartSessionIntent(
            context,
            settings,
            cancelExisting,
            name,
            requestCode
        )
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(context, requestCode, intent, 0) // warning here is a lint bug
        }

        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            startTime.toInstant().toEpochMilli(),
            pendingIntent
        )
        return true
    }

    fun cancelPendingSession(session: PendingSession, context: Context) {
        // In order to cancel an alarm, recreate the intent and pending intent that would have been
        // used to schedule that alarm (not including extras):
        val requestCode = session.requestCode.toInt()
        val intent = Intent(context, ImagingService::class.java).apply {
            action = ImagingService.ACTION_START_SESSION
        }
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            ) // warning here is a lint bug
        }
        alarmManager.cancel(pendingIntent)
        AutoMothRepository.deletePendingSession(session.requestCode)
    }

    suspend fun checkForSchedulingConflicts(
        startTime: OffsetDateTime,
        settings: ImagingSettings
    ): Pair<SchedulingConflictType, PendingSession>? {
        val pendingSessions = AutoMothRepository.getAllPendingSessions()
        val proposedSession = PendingSession("", settings, startTime)
        for (existing in pendingSessions) {
            proposedSession.conflictWith(existing)?.let {
                return Pair(it, existing)
            }
        }
        return null
    }
}

fun PendingSession.conflictWith(other: PendingSession): ImagingScheduler.SchedulingConflictType? {
    val s1 = this.scheduledDateTime
    val e1 = this.getStopTime()
    val s2 = other.scheduledDateTime
    val e2 = other.getStopTime()
    return when {
        s1 <= s2 && (e1 == null || e1 >= s2) -> ImagingScheduler.SchedulingConflictType.WILL_BE_CANCELLED
        s1 >= s2 && (e2 == null || e2 >= s1) -> ImagingScheduler.SchedulingConflictType.WILL_CANCEL
        else -> null
    }
}
