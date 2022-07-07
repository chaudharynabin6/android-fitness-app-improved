package com.androiddevs.runningappyt.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.androiddevs.runningappyt.R
import com.androiddevs.runningappyt.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
//
//        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        binding.apply {

            setSupportActionBar(toolbar)
            val navController =
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
    }

}


