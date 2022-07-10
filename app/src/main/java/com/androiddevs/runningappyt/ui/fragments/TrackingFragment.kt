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

class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private lateinit var binding: FragmentTrackingBinding

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
                sendCommandToTrackingService(TrackingService.action_start_resume_service)
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
        super.onViewCreated(view, savedInstanceState)
    }

    private fun sendCommandToTrackingService(action : String){
        Intent(requireContext(),TrackingService::class.java).also {
            it.action = action
//            sending intent to the service
            requireContext().startService(it)
        }
    }
}