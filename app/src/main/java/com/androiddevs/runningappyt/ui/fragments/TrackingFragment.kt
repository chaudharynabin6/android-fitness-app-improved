package com.androiddevs.runningappyt.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentTrackingBinding
import com.androiddevs.runningappyt.observers.MapViewObserver
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.utils.TimeFormatterUtil

class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private lateinit var binding: FragmentTrackingBinding

    class TrackingFragmentState {
        var isTracking: Boolean = false
    }

    private val state = TrackingFragmentState()
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
                toggleRun()
            }

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val mapViewObserver = MapViewObserver(
            lifecycle,
            binding.mapView,
            savedInstanceState
        )

        viewLifecycleOwner.lifecycle.addObserver(mapViewObserver)


        eventSubscribeTracking()
        eventSubscribeTimerAndUpdateTimeInMillis()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun sendCommandToTrackingService(action: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
//            sending intent to the service
            requireContext().startService(it)
        }
    }

    private fun eventSubscribeTracking() {
        TrackingService.stateLiveData.isTracking.observe(viewLifecycleOwner) {
            it?.let {
                state.isTracking = it
                updateTracking()
            }
        }
    }

    private fun eventSubscribeTimerAndUpdateTimeInMillis() {
        binding.apply {
            TrackingService.stateLiveData.timeRunInMillis.observe(viewLifecycleOwner) {
                it?.let { timeRunInMillis ->
                    val formattedTime = TimeFormatterUtil.getFormattedStopWatchTime(
                        ms = timeRunInMillis,
                        isMillisIncluded = true
                    )
                    tvTimer.text = formattedTime
                }
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