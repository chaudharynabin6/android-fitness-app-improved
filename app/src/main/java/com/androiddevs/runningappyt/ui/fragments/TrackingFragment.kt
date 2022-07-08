package com.androiddevs.runningappyt.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentTrackingBinding
import com.androiddevs.runningappyt.observers.MapViewObserver
import com.androiddevs.runningappyt.services.PolyLineList
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.services.TrackingServiceStates
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.PolylineOptions

class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    companion object {
        val trackingFragmentState = MutableLiveData<TrackingFragmentState>()
    }

    private lateinit var mapViewObserver: MapViewObserver
    private lateinit var binding: FragmentTrackingBinding

    var state =  TrackingFragmentState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackingBinding.inflate(inflater, container, false)

        binding.apply {

            btnToggleRun.setOnClickListener {
                sendEvent(event = EventsTrackingFragment.SendCommandToTrackingService(action = TrackingService.action_start_or_resume_service))
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

//        life cycle handling
        mapViewObserver = MapViewObserver(
            lifecycle,
            binding.mapView,
            savedInstanceState
        )
        viewLifecycleOwner.lifecycle.addObserver(mapViewObserver)

        binding.apply {
            btnToggleRun.setOnClickListener {
                toggleRun()
            }
        }

//        events
        sendEvent(EventsTrackingFragment.SubscribeObserverOfTrackingFragment)
        sendEvent(EventsTrackingFragment.WhenReturnBackTo)

        super.onViewCreated(view, savedInstanceState)
    }

    private fun sendEvent(event: EventsTrackingFragment) {
        when (event) {
            is EventsTrackingFragment.SendCommandToTrackingService -> {
                sendCommandToTrackingService(action = event.action)
            }
            is EventsTrackingFragment.WhenReturnBackTo -> {

            }
            is EventsTrackingFragment.CopyToInternalStateFromTrackingService -> {
                state = state.copy(
                    isTracking = event.state.isTracking,
                    pathPoints = event.state.pathPoints
                )
                updateTracking()
            }
            is EventsTrackingFragment.SubscribeObserverOfTrackingFragment -> {
                subscribeToObservers()
            }

        }
        trackingFragmentState.postValue(state)
    }


    private fun sendCommandToTrackingService(action: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
//            sending intent to the service
            requireContext().startService(it)
        }
    }


    private fun subscribeToObservers() {
        TrackingService.state.observe(viewLifecycleOwner) {
            it?.let {
                val state = it.toTrackingFragmentState()
                sendEvent(EventsTrackingFragment.CopyToInternalStateFromTrackingService(state))
            }

        }
    }


    private fun updateTracking() {
        state.apply {
            binding.apply {
                if (!isTracking) {
                    btnToggleRun.text = "START"
                    btnFinishRun.visibility = View.VISIBLE
                } else {
                    btnToggleRun.text = "STOP"
                    btnFinishRun.visibility = View.GONE
                }
            }
        }

    }

    private fun toggleRun() {
        state.apply {
            if (isTracking) {
                sendCommandToTrackingService(TrackingService.action_pause_service)
            } else {
                sendCommandToTrackingService(TrackingService.action_start_or_resume_service)
            }
        }
    }

}

// internal state
data class TrackingFragmentState(
    val isTracking: Boolean = false,
    val pathPoints: PolyLineList = mutableListOf(mutableListOf())
)

// mapper
private fun TrackingServiceStates.toTrackingFragmentState(): TrackingFragmentState {

    return TrackingFragmentState(
        isTracking = isTracking,
        pathPoints = pathPoints
    )
}

//events
sealed class EventsTrackingFragment {
    data class SendCommandToTrackingService(val action: String) : EventsTrackingFragment()
    data class CopyToInternalStateFromTrackingService(val state: TrackingFragmentState) :
        EventsTrackingFragment()

    object SubscribeObserverOfTrackingFragment : EventsTrackingFragment()
    object WhenReturnBackTo : EventsTrackingFragment()
}
