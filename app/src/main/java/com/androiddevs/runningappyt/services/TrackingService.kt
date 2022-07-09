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
import com.androiddevs.runningappyt.other.utils.time_formatter.TimeFormatterUtil
import com.androiddevs.runningappyt.permission.location.LocationPermission
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

typealias  Polyline = MutableList<LatLng>
typealias  PolyLineList = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var trackingNotification: TrackingNotification

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    private lateinit var notificationManager: NotificationManager

    companion object {
        val trackingState = MutableLiveData<TrackingServiceStates>()
        val trackingTimerState = MutableLiveData<TrackingServiceTimerState>()

        //        actions
        const val action_start_or_resume_service = "action_start_or_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"

        //        location update time
        const val location_update_interval = 5000L
        const val location_fastest_interval = 2000L

        //        timer
        const val timer_update_interval = 100L

        var timeRunInMillis = MutableLiveData<Long>(0L)
        var timeRunInSeconds = MutableLiveData<Long>(0L)

    }

    private var state = TrackingServiceStates()
    private var timerState = TrackingServiceTimerState()


    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        currentNotificationBuilder = trackingNotification.getNotificationBuilder()
        notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        //        events
        sendEvent(event = EventsTrackingService.PostInitialValues)
        sendEvent(event = EventsTrackingService.ObserveTrackingAndUpdateActionState)
        sendEvent(event = EventsTrackingService.ObserveTrackingTime)
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
                    sendEvent(event = EventsTrackingService.PauseTrackingService)
                }
                action_stop_service -> {
                    Timber.e("service stopped")
                    sendEvent(EventsTrackingService.KillService)

                }
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun sendEvent(event: EventsTrackingService) {
        when (event) {
            is EventsTrackingService.PostInitialValues -> {

                //                posting new value
                state = state.copy(
                    isFirsRun = true,
                    isTracking = false,
                    pathPoints = mutableListOf(mutableListOf()),
                    isServiceKilled = false
                )
                timerState = timerState.copy(
                    isTimerEnable = false,
                )
                timeRunInMillis.postValue(0L)
                timeRunInSeconds.postValue(0L)

            }
            is EventsTrackingService.StartTrackingService -> {

                startForegroundService()

                //  posting new value
                state = state.copy(
                    isFirsRun = false,

                    )
                timerState = timerState.copy(
                    isTimerEnable = true
                )

            }
            is EventsTrackingService.PauseTrackingService -> {
                state = state.copy(
                    isTracking = false,
                )
                timerState = timerState.copy(
                    isTimerEnable = false
                )
            }
            is EventsTrackingService.AddPathPoints -> {
                state = state.copy(
                    pathPoints = event.pathPoints
                )
            }
            is EventsTrackingService.EnableLocationTrackingAndTimer -> {
                state = state.copy(
                    isTracking = true,
                )
                timerState = timerState.copy(
                    isTimerEnable = true
                )
                updateLocationTracking()
                startTimer()
            }
            is EventsTrackingService.AddEmptyPolyLine -> {
                state = state.copy(
                    pathPoints = event.pathPoints
                )
            }
            is EventsTrackingService.ObserveTrackingAndUpdateActionState -> {
                trackingState.observe(
                    this
                ) {
                    it?.let {
                        val isTracking = it.isTracking
                        updateNotificationTrackingState(isTracking)
                    }

                }
            }
            is EventsTrackingService.ObserveTrackingTime -> {
                timeRunInSeconds.observe(this) {
                    it?.let {
                        changeTrackingTimer(it)
                    }
                }
            }
            is EventsTrackingService.KillService -> {
                state = state.copy(
                    isServiceKilled = true,
                    isFirsRun = true
                )
                killService()
            }
        }

        trackingState.postValue(state)
    }


    private fun changeTrackingTimer(time: Long) {
        if (!state.isServiceKilled && state.isTracking) {
            val notification = currentNotificationBuilder
                .setContentText(
                    TimeFormatterUtil.getFormattedStopWatchTime(time * 1000L)
                )
            notificationManager.notify(
                TrackingNotification.notification_id,
                notification.build()
            )
        }
    }

    private fun startForegroundService() {
        if (state.isFirsRun) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackingNotification.createNotificationChannel(notificationManager)
            }

            val notificationBuilder = trackingNotification.getNotificationBuilder()

            startForeground(
                TrackingNotification.notification_id,
                notificationBuilder.build()
            )
        } else {
            //            resume service
            addEmptyPolyline()
        }
        sendEvent(EventsTrackingService.EnableLocationTrackingAndTimer)
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
                        Timber.d("NEW LOCATION ${location.latitude} , ${location.longitude} ")
                    }
                }
            }
        }
    }

    // adding polyline point as the last of the pathPoints
    fun addPathPoint(location: Location) {
        location.let {
            val position = LatLng(location.latitude, location.longitude)
            state.pathPoints.let { polyLineList ->

                val polyline: Polyline = polyLineList.last()
                //               inserting co-ordinate
                polyline.add(position)

                sendEvent(EventsTrackingService.AddPathPoints(polyLineList))
            }
        }
    }

    private fun addEmptyPolyline() {
        state.pathPoints.apply {
            //        inside polylineList adding another list of polyline co-ordinates
            val polyLineList: PolyLineList = this

            val polyline = mutableListOf<LatLng>()

            polyLineList.add(polyline)

            sendEvent(EventsTrackingService.AddEmptyPolyLine(this))

        }
    }


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

                timeRunInMillis.postValue(timeRun + lapTime)

                trackingTimerState.postValue(timerState)
                if (timeRunInMillis.value!! >= lastSecondTimeStamp + 1000L) {

                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
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

        if (!state.isServiceKilled && isTracking) {

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
    }
    private fun killService(){
        sendEvent(EventsTrackingService.PauseTrackingService)
        sendEvent(EventsTrackingService.PostInitialValues)
        stopForeground(true)
        stopSelf()
    }
}


data class TrackingServiceStates(
    val isFirsRun: Boolean = true,
    val isTracking: Boolean = false,
    val pathPoints: PolyLineList = mutableListOf(mutableListOf()),
    val isServiceKilled : Boolean = false
)

data class TrackingServiceTimerState(
    val isTimerEnable: Boolean = true
)


sealed class EventsTrackingService {
    object StartTrackingService : EventsTrackingService()
    object PauseTrackingService : EventsTrackingService()
    object PostInitialValues : EventsTrackingService()
    object EnableLocationTrackingAndTimer : EventsTrackingService()
    data class AddPathPoints(val pathPoints: PolyLineList) : EventsTrackingService()
    data class AddEmptyPolyLine(val pathPoints: PolyLineList) : EventsTrackingService()
    object ObserveTrackingAndUpdateActionState : EventsTrackingService()
    object ObserveTrackingTime : EventsTrackingService()
    object KillService : EventsTrackingService()
}