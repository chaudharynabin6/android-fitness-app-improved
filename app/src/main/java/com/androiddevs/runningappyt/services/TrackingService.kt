package com.androiddevs.runningappyt.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.androiddevs.runningappyt.notification.TrackingNotification
import com.androiddevs.runningappyt.permission.location.LocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

typealias  Polyline = MutableList<LatLng>
typealias  PolyLineList = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var trackingNotification: TrackingNotification

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        val state = MutableLiveData<TrackingServiceStates>()

        //        actions
        const val action_start_or_resume_service = "action_start_or_resume_service"
        const val action_pause_service = "action_pause_service"
        const val action_stop_service = "action_stop_service"

        //        location update time
        const val location_update_interval = 5000L
        const val location_fastest_interval = 2000L


    }

    private var trackingStates = TrackingServiceStates()

    override fun onCreate() {
        super.onCreate()
        fusedLocationProviderClient = FusedLocationProviderClient(this)
//        events
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
                   isFirsRun = true,
                   isTracking = false,
                   pathPoints = mutableListOf(mutableListOf())
               )
            }
            is EventsTrackingService.StartTrackingService -> {

                startForegroundService()

                //  posting new value
                trackingStates = trackingStates.copy(
                    isFirsRun = false
                )

            }
            is EventsTrackingService.AddPathPoints -> {
                trackingStates = trackingStates.copy(
                    pathPoints = event.pathPoints
                )
            }
            is EventsTrackingService.EnableLocationTracking -> {
                trackingStates = trackingStates.copy(
                    isTracking = true
                )
                updateLocationTracking()
            }
            is EventsTrackingService.AddEmptyPolyLine -> {
                trackingStates = trackingStates.copy(
                    pathPoints = event.pathPoints
                )
            }
        }

        state.postValue(trackingStates)
    }

    private fun startForegroundService() {
        if (trackingStates.isFirsRun) {

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
        } else {
//            resume service
            addEmptyPolyline()
        }
        sendEvent(EventsTrackingService.EnableLocationTracking)
    }

    private fun updateLocationTracking() {
        if (trackingStates.isTracking) {
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
            if (trackingStates.isTracking) {
                p0.locations.let { locations ->
                    for (location in locations) {
                        addPathPoint(location)
                        Timber.e("NEW LOCATION ${location.toString()} ")
                    }
                }
            }
        }
    }

    // adding polyline point as the last of the pathPoints
    fun addPathPoint(location: Location) {
        location.let {
            val position = LatLng(location.latitude, location.longitude)
            trackingStates.pathPoints.let { polyLineList ->

                val polyline: Polyline = polyLineList.last()
//               inserting co-ordinate
                polyline.add(position)

                sendEvent(EventsTrackingService.AddPathPoints(polyLineList))
            }
        }
    }

    private fun addEmptyPolyline() {
        trackingStates.pathPoints.apply {
//        inside polylineList adding another list of polyline co-ordinates
            val polyLineList: PolyLineList = this

            val polyline = mutableListOf<LatLng>()

            polyLineList.add(polyline)

            sendEvent(EventsTrackingService.AddEmptyPolyLine(this))

        }
    }

}


data class TrackingServiceStates(
    val isFirsRun: Boolean = true,
    val isTracking: Boolean = false,
    val pathPoints: PolyLineList = mutableListOf(mutableListOf())
)

sealed class EventsTrackingService {
    object StartTrackingService : EventsTrackingService()
    object PostInitialValues : EventsTrackingService()
    object EnableLocationTracking : EventsTrackingService()
    data class AddPathPoints(val pathPoints: PolyLineList) : EventsTrackingService()
    data class AddEmptyPolyLine(val pathPoints: PolyLineList) : EventsTrackingService()
}