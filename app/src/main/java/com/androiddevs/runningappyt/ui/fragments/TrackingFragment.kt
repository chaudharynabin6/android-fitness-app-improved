package com.androiddevs.runningappyt.ui.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.FragmentTrackingBinding
import com.androiddevs.runningappyt.observers.MapViewObserver
import com.androiddevs.runningappyt.services.TrackingService
import com.androiddevs.runningappyt.ui.viewmodels.MainViewModel
import com.androiddevs.runningappyt.utils.TimeFormatterUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private lateinit var binding: FragmentTrackingBinding

    private lateinit var activityRef: FragmentActivity
    private var menu: Menu? = null


    private val viewModel: MainViewModel by viewModels()

    private lateinit var mapViewObserver: MapViewObserver

    class TrackingFragmentState {
        var isTracking: Boolean = false
        var isFirstRun: Boolean = true
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
        activityRef = requireActivity()
        binding = FragmentTrackingBinding.inflate(inflater, container, false)

        binding.apply {
            btnToggleRun.setOnClickListener {
                toggleRun()
            }

            btnFinishRun.setOnClickListener{
                CoroutineScope(Dispatchers.Main).launch {
                    mapViewObserver.eventZoomToSeeWholeTrack()
                    mapViewObserver.eventEndRunAndSaveToDb()
                    delay(1000)
                    stopRun()
                }
            }
        }

        activityRef.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                TrackingService.stateLiveData.timeRunInMillis.observe(viewLifecycleOwner) { time ->
                    time?.let {
                        if (it > 0L) {
                            this@TrackingFragment.menu?.findItem(R.id.miCancelTracking)?.isVisible =
                                true
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

       mapViewObserver = MapViewObserver(
            lifecycle,
            binding.mapView,
            savedInstanceState,
            viewModel = viewModel,
            activity = activityRef
        )

        viewLifecycleOwner.lifecycle.addObserver(mapViewObserver)


        eventSubscribeTracking()
        eventSubscribeIsFirstRun()
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

    private fun eventSubscribeIsFirstRun() {
        TrackingService.stateLiveData.isFirstRun.observe(viewLifecycleOwner) {
            it?.let {
                state.isFirstRun = it
            }
        }
    }

    private fun updateTracking() {
        state.apply {
            binding.apply {
                if (!isTracking) {
                    btnToggleRun.text = "START"
                    if (!isFirstRun) {
                        btnFinishRun.visibility = View.VISIBLE
                    }
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


}