package com.androiddevs.runningappyt.ui.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentTrackingBinding
import com.androiddevs.runningappyt.observers.MapViewObserver
import com.androiddevs.runningappyt.other.utils.time_formatter.TimeFormatterUtil
import com.androiddevs.runningappyt.services.PolyLineList
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.services.TrackingServiceStates
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    companion object {
        val trackingFragmentState = MutableLiveData<TrackingFragmentState>()
    }

    private lateinit var mapViewObserver: MapViewObserver
    private lateinit var binding: FragmentTrackingBinding

    var state = TrackingFragmentState()

    private lateinit var  activityRef :FragmentActivity
    private var menu: Menu? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activityRef = requireActivity()
        binding = FragmentTrackingBinding.inflate(inflater, container, false)

        binding.apply {

            btnToggleRun.setOnClickListener {
                sendEvent(event = EventsTrackingFragment.SendCommandToTrackingService(action = TrackingService.action_start_or_resume_service))
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
// for option menu

        activityRef.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                TrackingService.timeRunInMillis.observe(viewLifecycleOwner) { time ->
                    time?.let {
                        if (it > 0L) {
                            menu.getItem(0)?.isVisible = true
                        }
                    }
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_tracking_menu, menu)
                this@TrackingFragment.menu = menu
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {

                return when (menuItem.itemId) {
                    R.id.miCancelTracking -> {
                        showCancelTrackingDialog()
                        true
                    }
                    else -> false
                }

            }

            override fun onMenuClosed(menu: Menu) {
                super.onMenuClosed(menu)
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

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
        sendEvent(EventsTrackingFragment.ObserveTimer)

        super.onViewCreated(view, savedInstanceState)
    }

    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all its data")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(
                "Yes"
            ) { dialog: DialogInterface, which: Int ->

                stopRun()

            }.setNegativeButton(
                "No"
            ) { dialog, which ->
                dialog.cancel()
            }.create()
        dialog.show()
    }
    private fun stopRun() {
        sendCommandToTrackingService(TrackingService.action_stop_service)
        val action = TrackingFragmentDirections.actionTrackingFragmentToRunFragment()
        findNavController().navigate(action)
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
                    pathPoints = event.state.pathPoints,

                    )
                updateTracking()

            }
            is EventsTrackingFragment.SubscribeObserverOfTrackingFragment -> {
                subscribeToObservers()
            }

            is EventsTrackingFragment.ObserveTimer -> {
                observeTimer()
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
        TrackingService.trackingState.observe(viewLifecycleOwner) {
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
                    menu?.getItem(0)?.isVisible = true
                }
            }
        }

    }

    private fun toggleRun() {
        state.apply {
            if (isTracking) {
                sendCommandToTrackingService(TrackingService.action_pause_service)
//                menu?.getItem(0)?.isVisible = true
            } else {
                sendCommandToTrackingService(TrackingService.action_start_or_resume_service)
            }
        }
    }

    private fun observeTimer() {
        binding.apply {
            TrackingService.timeRunInMillis.observe(viewLifecycleOwner) {
                it?.let {
                    val formattedTime = TimeFormatterUtil.getFormattedStopWatchTime(
                        ms = it,
                        isMillisIncluded = true
                    )
                    tvTimer.text = formattedTime
                }

            }
        }
    }
}

// internal state
data class TrackingFragmentState(
    val isTracking: Boolean = false,
    val pathPoints: PolyLineList = mutableListOf(mutableListOf()),
)

// mapper
private fun TrackingServiceStates.toTrackingFragmentState(): TrackingFragmentState {

    return TrackingFragmentState(
        isTracking = isTracking,
        pathPoints = pathPoints,
    )
}

//events
sealed class EventsTrackingFragment {
    data class SendCommandToTrackingService(val action: String) : EventsTrackingFragment()
    data class CopyToInternalStateFromTrackingService(val state: TrackingFragmentState) :
        EventsTrackingFragment()

    object SubscribeObserverOfTrackingFragment : EventsTrackingFragment()
    object WhenReturnBackTo : EventsTrackingFragment()
    object ObserveTimer : EventsTrackingFragment()
}
