package com.androiddevs.runningappyt.observers

import android.graphics.Color
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.androiddevs.runningappyt.services.PolyLineList
import com.androiddevs.runningappyt.services.TrackingService
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

    private var map: GoogleMap? = null
    private val state = MapViewObserverState()

    class MapViewObserverState {
        var pathPoints: PolyLineList = mutableListOf(mutableListOf())
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
            eventWhenReturnBackToFragment()
        }
        eventObservePathPoints(owner)
    }

    private fun eventObservePathPoints(owner: LifecycleOwner) {
        TrackingService.stateLiveData.pathPoints.observe(owner) {
            it?.let {
                state.pathPoints = it
                addLatestPolyline()
                moveCameraToUserLocation()
            }
        }
    }

    private fun eventWhenReturnBackToFragment(){
        addAllPolyLines()
        moveCameraToUserLocation()
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

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapView.onStart()
        eventWhenReturnBackToFragment()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mapView.onResume()
        eventWhenReturnBackToFragment()
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