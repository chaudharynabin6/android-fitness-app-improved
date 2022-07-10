package com.androiddevs.runningappyt.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.androiddevs.runningappyt.notification.TrackingNotification
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    companion object{
        const val action_start_resume_service = "action_start_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"

        val state = TrackingState()
    }
    @Inject
    lateinit var trackingNotification: TrackingNotification
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when(it.action){
                action_start_resume_service -> {
                    Timber.e("service started or resumed")
                    eventStartTrackingService()
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

    override fun onCreate() {
        super.onCreate()
        eventPostInitialValues()
    }

    private fun eventPostInitialValues(){
        state.isFirstRun.postValue(true)
    }
    private fun eventStartTrackingService(){
        state.isFirstRun.observe(this){
            it?.let {
                if(it){
                    startForegroundService()
                }
            }
        }
        state.isFirstRun.postValue(false)
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

class TrackingState() {
    val isFirstRun = MutableLiveData(true)
}