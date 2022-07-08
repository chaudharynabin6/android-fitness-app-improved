package com.androiddevs.runningappyt.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.ActivityMainBinding
import com.androiddevs.runningappyt.permission.location.Constants
import com.androiddevs.runningappyt.permission.location.LocationPermission
import com.androiddevs.runningappyt.ui.fragments.TrackingFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    companion object {
        const val action_show_tracking_fragment = "action_show_tracking_fragment"

    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
//
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        binding.apply {

            setSupportActionBar(toolbar)
            navController =
                Navigation.findNavController(this@MainActivity, R.id.navHostFragment)
            bottomNavigationView.setupWithNavController(navController = navController)

            navController
                .addOnDestinationChangedListener { _, destination, _ ->
                    when (destination.id) {
                        R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                            bottomNavigationView.visibility = View.VISIBLE
                        else -> bottomNavigationView.visibility = View.GONE
                    }
                }
        }

// on start events
        sendEvent(events = MainActivityEvents.NavigateToTrackingFragmentIfNecessary(intent = intent))
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {

//      starting events to show
        sendEvent(MainActivityEvents.RequestLocationPermission)
        return super.onCreateView(parent, name, context, attrs)
    }

    private fun sendEvent(events: MainActivityEvents) {
        when (events) {
            is MainActivityEvents.RequestLocationPermission -> {
                requestPermission()
            }
            is MainActivityEvents.NavigateToTrackingFragmentIfNecessary -> {
                navigateToTrackingFragmentIfNeeded(intent = events.intent)
            }

        }
    }

    private fun requestPermission() {
        if (LocationPermission.hasLocationPermission(this)) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permission to use the app",
                Constants.REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permission to use the app",
                Constants.REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
//        no operation
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)
        ) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {

        intent?.let {
            if (
                it.action == action_show_tracking_fragment
            ) {
                val action = TrackingFragmentDirections.actionGlobalTrackingFragment()
                navController.navigate(action)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }
}


internal sealed class MainActivityEvents(){
    object RequestLocationPermission : MainActivityEvents()
    data class NavigateToTrackingFragmentIfNecessary(val intent: Intent?) : MainActivityEvents()
}
