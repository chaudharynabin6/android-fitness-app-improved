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

        val stateLiveData = TrackingStateAsLiveData()
    }

    class TrackingStateAsLiveData {
        val isFirstRun = MutableLiveData(true)
        val isTracking = MutableLiveData(false)
        val pathPoints = MutableLiveData<PolyLineList>(mutableListOf(mutableListOf()))
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
    }

    @Inject
    lateinit var trackingNotification: TrackingNotification

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

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
                    eventPauseTrackingService()

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
        eventPostInitialValues()
    }

    private fun eventPostInitialValues() {
        state.isFirstRun = true
        state.isTracking = false
        state.pathPoints = mutableListOf(mutableListOf())
    }

    private fun eventStartTrackingService() {
        startForegroundService()

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

    private fun eventPauseTrackingService() {
        state.isTracking = false
    }

    private fun startForegroundService() {
        if (state.isFirstRun) {
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
            addEmptyPolyline()
        }
        eventEnableLocationTracking()
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
}
