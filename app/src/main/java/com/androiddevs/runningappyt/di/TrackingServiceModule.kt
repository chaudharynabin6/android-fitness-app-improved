package com.androiddevs.runningappyt.di

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.androiddevs.runningappyt.notification.TrackingNotification
import com.androiddevs.runningappyt.ui.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped


@Module
@InstallIn(ServiceComponent::class)
object TrackingServiceModule {

    @Provides
    @ServiceScoped
    fun providesTrackingNotification(
        @ApplicationContext context: Context,
        pendingIntent: PendingIntent
    ): TrackingNotification {
        return TrackingNotification(context, pendingIntent)
    }

    @Provides
    @ServiceScoped
    @SuppressLint("UnspecifiedImmutableFlag")
    fun getMainActivityPendingIntent(
        @ApplicationContext context: Context
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).also {
                it.action = MainActivity.action_show_tracking_fragment
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}