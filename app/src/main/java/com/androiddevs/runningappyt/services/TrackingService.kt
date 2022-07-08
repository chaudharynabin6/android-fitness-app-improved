package com.androiddevs.runningappyt.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LifecycleService
import com.androiddevs.runningappyt.notification.TrackingNotification
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var trackingNotification: TrackingNotification


    companion object {
        const val action_start_or_resume_service = "action_start_or_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"

        var trackingStates = TrackingServiceStates()
    }

    override fun onCreate() {
        super.onCreate()
        sendEvent(event = EventsTrackingService.PostInitialValues)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                action_start_or_resume_service -> {
                    Timber.e("service started or resumed")
                    sendEvent(event = EventsTrackingService.StartTrackingService)
                }
                action_pause_service -> {
                    Timber.e("service  paused")
                }
                action_stop_service -> {
                    Timber.e("service stopped")

                }
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun sendEvent(event: EventsTrackingService) {
        when (event) {
            is EventsTrackingService.PostInitialValues -> {

//                posting new value
               trackingStates = trackingStates.copy(
                   isFirsRun = true
               )
            }
            is EventsTrackingService.StartTrackingService -> {
                if (trackingStates.isFirsRun) {
                    startForegroundService()
                }


                //  posting new value
                trackingStates = trackingStates.copy(
                    isFirsRun = false
                )
            }

        }
    }

    private fun startForegroundService() {

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            trackingNotification.createNotificationChannel(notificationManager)
        }

        val notificationBuilder = trackingNotification.getNotificationBuilder()

        startForeground(
            TrackingNotification.notification_id,
            notificationBuilder.build()
        )
    }


}


data class TrackingServiceStates(
    val isFirsRun: Boolean = true
)

sealed class EventsTrackingService {
    object StartTrackingService : EventsTrackingService()
    object PostInitialValues : EventsTrackingService()
}