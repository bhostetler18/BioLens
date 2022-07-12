package com.uf.automoth.ui.imaging

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.uf.automoth.MainActivity
import com.uf.automoth.R
import com.uf.automoth.data.AutoMothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ImagingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        Log.d("[SERVICE]", "On create called")
        super.onCreate()
        AutoMothRepository(this, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("[SERVICE]", "On start command called")
        startInForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        Log.d("[SERVICE]", "On destroy called")
        super.onDestroy()
    }

    private fun startInForeground() {
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            SERVICE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        serviceChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getText(R.string.service_notification_title))
            .setContentText(getText(R.string.service_notification_message))
            .setSmallIcon(R.drawable.ic_camera_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val SERVICE_NOTIFICATION_ID: Int = 1297044552
        const val SERVICE_CHANNEL_ID: String = "com.uf.automoth.serviceNotification"
        const val SERVICE_CHANNEL_NAME: String = "AutoMoth Imaging Service"
    }
}
