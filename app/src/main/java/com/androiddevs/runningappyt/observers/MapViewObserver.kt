package com.androiddevs.runningappyt.observers

import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.androiddevs.runningappyt.services.PolyLineList
import com.androiddevs.runningappyt.ui.fragments.TrackingFragment
import com.androiddevs.runningappyt.ui.fragments.TrackingFragmentState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.PolylineOptions

class MapViewObserver(
    private val lifecycle: Lifecycle,
    private val mapView: MapView,
    private val savedInstanceState: Bundle?,
) : DefaultLifecycleObserver {

    companion object {
        const val polyline_color = Color.RED
        const val polyline_width = 8f
        const val map_zoom = 15f
    }

    var map: GoogleMap? = null
    var state = MapViewObserverState()
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
            sendEvent(EventsMapViewObserver.WhenReturnBackTo)
        }
        sendEvent(EventsMapViewObserver.OnCreate(owner))

    }

    private fun sendEvent(event: EventsMapViewObserver) {
        when (event) {

            is EventsMapViewObserver.OnCreate -> {
                subscribeToTrackingFragmentState(owner = event.owner)
            }
            is EventsMapViewObserver.WhenReturnBackTo -> {
                addAllPolyLines()
                moveCameraToUserLocation()
            }
            is EventsMapViewObserver.CopyToInternalStateFromTrackingFragment -> {
                state = state.copy(
                    isTracking = event.state.isTracking,
                    pathPoints = event.state.pathPoints
                )
                addLatestPolyline()
                moveCameraToUserLocation()
            }
        }
    }

    private fun addAllPolyLines() {
        for (polyline in state.pathPoints) {
            val polyLineOptions = PolylineOptions()
                .color(polyline_color)
                .width(polyline_width)
                .addAll(polyline)
            map?.addPolyline(polyLineOptions)
        }
    }


    private fun moveCameraToUserLocation() {
        state.apply {
            if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        pathPoints.last().last(),
                        map_zoom
                    )
                )
            }
        }
    }

    private fun addLatestPolyline() {
        state.apply {
            if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
                val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
                val lastLatLng = pathPoints.last().last()

                val polylineOptions = PolylineOptions()
                    .color(polyline_color)
                    .width(polyline_width)
                    .add(preLastLatLng)
                    .add(lastLatLng)
                map?.addPolyline(polylineOptions)
            }
        }


    }

    private fun subscribeToTrackingFragmentState(owner: LifecycleOwner){
        TrackingFragment.trackingFragmentState.observe(owner){
            it?.apply {
                val state = it.toMapViewObserverState()
                sendEvent(EventsMapViewObserver.CopyToInternalStateFromTrackingFragment(state))
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapView.onStart()
        sendEvent(EventsMapViewObserver.WhenReturnBackTo)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mapView.onResume()
        sendEvent(EventsMapViewObserver.WhenReturnBackTo)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mapView.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapView.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mapView.onDestroy()
    }
}


sealed class EventsMapViewObserver {
   
    data class CopyToInternalStateFromTrackingFragment(val state: MapViewObserverState) :
        EventsMapViewObserver()
    object WhenReturnBackTo : EventsMapViewObserver()
    data class OnCreate(val owner: LifecycleOwner) : EventsMapViewObserver()

}


data class MapViewObserverState(
    val isTracking: Boolean = false,
    val pathPoints: PolyLineList = mutableListOf(mutableListOf())
)

fun TrackingFragmentState.toMapViewObserverState() : MapViewObserverState{

    return MapViewObserverState(
        isTracking = isTracking,
        pathPoints = pathPoints
    )
}