package com.androiddevs.runningappyt.services

import android.content.Intent
import androidx.lifecycle.LifecycleService
import timber.log.Timber

class TrackingService : LifecycleService() {

    companion object{
        const val action_start_resume_service = "action_start_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when(it.action){
                action_start_resume_service -> {
                    Timber.e("service started or resumed")

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
}