package com.androiddevs.runningappyt.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.androiddevs.runningappyt.R


class TrackingNotification (
    private val context: Context,
    private val pendingIntent: PendingIntent
) {


    companion object {
        const val notification_channel_id = "tracking_channel"
        const val notification_channel_name = "notification_channel_name"
        const val notification_id = 1
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            notification_channel_id,
            notification_channel_name,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun getNotificationBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(
            context,
            notification_channel_id
        ).setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
            .setContentTitle("Running App")
            .setContentText("00:00:00")
            .setContentIntent(pendingIntent)
    }


}