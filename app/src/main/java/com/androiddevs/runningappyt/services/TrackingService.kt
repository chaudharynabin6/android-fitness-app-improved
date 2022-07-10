package com.androiddevs.runningappyt.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.notification.TrackingNotification
import com.androiddevs.runningappyt.permission.location.LocationPermission
import com.androiddevs.runningappyt.utils.TimeFormatterUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

typealias  Polyline = MutableList<LatLng>
typealias  PolyLineList = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    companion object {
        const val action_start_or_resume_service = "action_start_or_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"

        //        location update time
        const val location_update_interval = 5000L
        const val location_fastest_interval = 2000L

        //         timer
        const val timer_update_interval = 200L
        val stateLiveData = TrackingStateAsLiveData()
    }

    class TrackingStateAsLiveData {
        val isFirstRun = MutableLiveData(true)
        val isTracking = MutableLiveData(false)
        val pathPoints = MutableLiveData<PolyLineList>(mutableListOf(mutableListOf()))
        val timeRunInMillis = MutableLiveData(0L)
        val timeRunInSeconds = MutableLiveData(0L)
    }

    class TrackingState {
        var isFirstRun: Boolean by Delegates.observable(true) { _, _, newValue ->
            stateLiveData.isFirstRun.postValue(newValue)
        }
        var isTracking: Boolean by Delegates.observable(false) { _, _, newValue ->
            stateLiveData.isTracking.postValue(newValue)
        }
        var pathPoints: PolyLineList by Delegates.observable(mutableListOf(mutableListOf())) { _, _, newValue ->
            stateLiveData.pathPoints.postValue(newValue)
        }
        var timeRunInMillis: Long by Delegates.observable(0L) { _, _, newValue ->
            stateLiveData.timeRunInMillis.postValue(newValue)
        }
        var timeRunInSeconds: Long by Delegates.observable(0L) { _, _, newValue ->
            stateLiveData.timeRunInSeconds.postValue(newValue)
        }
    }

    @Inject
    lateinit var trackingNotification: TrackingNotification

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    private lateinit var notificationManager: NotificationManager
    private var state = TrackingState()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when (it.action) {
                action_start_or_resume_service -> {
                    Timber.e("service started or resumed")
                    eventStartTrackingService()
                }
                action_pause_service -> {
                    Timber.e("service  paused")
                    eventPauseTrackingServiceAndTimer()

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
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currentNotificationBuilder = trackingNotification.getNotificationBuilder()
        notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        eventPostInitialValues()
    }

    private fun eventPostInitialValues() {
        state.isFirstRun = true
        state.isTracking = false
        state.pathPoints = mutableListOf(mutableListOf())
        state.timeRunInMillis = 0L
        state.timeRunInSeconds = 0L
    }

    private fun eventStartTrackingService() {
        startForegroundService()
        eventEnableLocationTracking()
        eventStartTimer()
        eventUpdateTimerOnNotification()
        eventUpdateTrackingStateOnNotification()
        state.isFirstRun = false
    }

    private fun eventAddPathPoints(p: PolyLineList) {
        state.pathPoints = p
    }

    private fun eventEnableLocationTracking() {
        state.isTracking = true
        updateLocationTracking()
    }

    private fun eventAddEmptyPathPoint(p: PolyLineList) {
        state.pathPoints = p
    }

    private fun eventPauseTrackingServiceAndTimer() {
        state.isTracking = false
    }

    private fun eventStartTimer() {
        startTimer()
    }

    private fun eventUpdateTimerOnNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            while (state.isTracking) {
                changeTrackingTimer(state.timeRunInSeconds)
                delay(1000L)
            }
        }
    }

    private fun eventUpdateTrackingStateOnNotification() {
        stateLiveData.isTracking.observe(this) {
            it?.let {
                updateNotificationTrackingState(it)
            }
        }
    }

    private fun startForegroundService() {
        if (state.isFirstRun) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackingNotification.createNotificationChannel(notificationManager)
            }

            startForeground(
                TrackingNotification.notification_id,
                currentNotificationBuilder.build()
            )
        } else {
            addEmptyPolyline()
        }

    }


    private fun updateLocationTracking() {


        if (state.isTracking) {

            if (LocationPermission.hasLocationPermission(this)) {
                val request = LocationRequest().apply {
                    interval = location_update_interval
                    fastestInterval = location_fastest_interval
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }

    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            if (state.isTracking) {
                p0.locations.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.e("NEW LOCATION ${location.latitude}  ${location.longitude}")
                    }
                }
            }
        }
    }


    // adding polyline point as the last of the pathPoints
    fun addPathPoint(location: Location) {
        location.let {
            val position = LatLng(location.latitude, location.longitude)
//            pathPoints are updated each time it called
            state.pathPoints.let {
                val polyline: Polyline = it.last()
//               inserting co-ordinate
                polyline.add(position)
                eventAddPathPoints(it)
            }
        }
    }

    private fun addEmptyPolyline() {
        state.pathPoints.apply {
//        inside polylineList adding another list of polyline co-ordinates
            val polyLineList: PolyLineList = this

            val polyline = mutableListOf<LatLng>()

            polyLineList.add(polyline)

            eventAddEmptyPathPoint(this)
        }
    }

    //     this state are local state
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimeStamp = 0L
    private fun startTimer() {
        timeStarted = System.currentTimeMillis()
        CoroutineScope(Dispatchers.Main).launch {
            while (state.isTracking) {
//              time difference between the each start time and system time
                lapTime = System.currentTimeMillis() - timeStarted

//                 post the new lapTime
//                total run is

                state.timeRunInMillis = (timeRun + lapTime)


                if (state.timeRunInMillis >= lastSecondTimeStamp + 1000L) {

                    state.timeRunInSeconds = (state.timeRunInSeconds + 1)
                    lastSecondTimeStamp += 1000L

                }
                delay(timer_update_interval)
            }
//             updating total time only when
            timeRun += lapTime

        }

    }

    // this only update the pause or resume state
    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) "Pause" else "Resume"

        val pendingIntent: PendingIntent = if (isTracking) {
            val pauseIntent = Intent(
                this,
                TrackingService::class.java
            ).apply {
                action = action_pause_service
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(
                this,
                TrackingService::class.java
            ).apply {
                action = action_start_or_resume_service
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

//         remove previous action buttons
        currentNotificationBuilder.javaClass.getField("mActions").apply {
            isAccessible = true
            set(currentNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        currentNotificationBuilder = currentNotificationBuilder
            .addAction(
                R.drawable.ic_pause_black_24dp,
                notificationActionText,
                pendingIntent
            )
        notificationManager.notify(
            TrackingNotification.notification_id,
            currentNotificationBuilder.build()
        )
    }

    private fun changeTrackingTimer(time: Long) {
        val notificationBuilder = currentNotificationBuilder
            .setContentText(
                TimeFormatterUtil.getFormattedStopWatchTime(time * 1000L)
            )
        notificationManager.notify(
            TrackingNotification.notification_id,
            notificationBuilder.build()
        )
    }
}
