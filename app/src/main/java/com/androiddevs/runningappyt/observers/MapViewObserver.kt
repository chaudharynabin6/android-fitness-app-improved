package com.androiddevs.runningappyt.observers

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.services.PolyLineList
import com.androiddevs.runningappyt.services.Polyline
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.util.*
import kotlin.math.round

class MapViewObserver(
    private val lifecycle: Lifecycle,
    private val mapView: MapView,
    private val savedInstanceState: Bundle?,
    private val viewModel : MainViewModel,
    private val activity: FragmentActivity
) : DefaultLifecycleObserver {

    companion object {
        const val polyline_color = Color.RED
        const val polyline_width = 8f
        const val map_zoom = 15f

    }

    private var weight = 80F
    private var map: GoogleMap? = null
    private val state = MapViewObserverState()

    class MapViewObserverState {
        var pathPoints: PolyLineList = mutableListOf(mutableListOf())
        var timeRunInMillis : Long = 0L
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
            eventWhenReturnBackToFragment()
        }
        eventObservePathPoints(owner)
        eventObserveTimeInMillis(owner = owner)
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

    private fun eventObserveTimeInMillis(owner: LifecycleOwner){
        TrackingService.stateLiveData.timeRunInMillis.observe(owner){
            it?.let {
                state.timeRunInMillis = it
            }
        }
    }
// public
    fun eventZoomToSeeWholeTrack(){
        zoomToSeeWholeTrack()
    }
    fun eventEndRunAndSaveToDb(){
       endRunAndSaveToDb()
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

    private fun  zoomToSeeWholeTrack(){
        val bounds = LatLngBounds.Builder()

        for (polyline in state.pathPoints){
            for(pos in polyline){
                bounds.include(pos)
            }
        }
        try {
            map?.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds.build(),
                    mapView.width,
                    mapView.height,
                    (mapView.height * 0.05f).toInt()
                )
            )
        }
        catch (e : IllegalStateException){
            e.printStackTrace()
            Timber.e("zooming not successful due to no points in map")
        }

    }

    private fun endRunAndSaveToDb(){
        map?.snapshot {
            var distanceInMeter = 0
            for(polyline in state.pathPoints){
                distanceInMeter += calculatePolyline(polyline).toInt()
            }
            val avgSpeed = round ((distanceInMeter/1000f) / (state.timeRunInMillis / 1000f / 60f / 60f) * 10) / 10f

            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeter / 1000f) * weight).toInt()
            val run = Run(
                img = it,
                timestamp = dateTimeStamp,
                avgSpeedInKMH = avgSpeed.toFloat(),
                timeInMillis = state.timeRunInMillis,
                caloriesBurned = caloriesBurned
            )
            viewModel.insertRun(run)

            Snackbar.make(
                activity.findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun calculatePolyline(polyline: Polyline):Float{
        var distance = 0f
        for(i in 0..polyline.size - 2){
            val pos1 = polyline[i]
            val pos2 = polyline[i+1]

            val result = FloatArray(1)

            Location.distanceBetween(
                pos1.latitude,
                pos2.longitude,
                pos2.latitude,
                pos2.longitude,
                result
            )
            distance += result[0]

        }
        return distance
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