package com.uf.automoth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.automoth.data.AutoMothRepository
import com.uf.automoth.data.PendingSession
import com.uf.automoth.databinding.ActivityMainBinding
import com.uf.automoth.imaging.ImagingService
import com.uf.automoth.ui.imaging.scheduler.ImagingSchedulerActivity
import com.uf.automoth.utility.SHORT_TIME_FORMATTER

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBar.toolbar)

        val navView: BottomNavigationView = binding.bottomNavBar

        // See https://stackoverflow.com/questions/58703451/fragmentcontainerview-as-navhostfragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.main_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_imaging,
                R.id.navigation_data,
                R.id.navigation_other
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        ImagingService.IS_RUNNING.observe(this) { isRunning ->
            setRunningSessionIndicatorVisible(isRunning)
        }

        AutoMothRepository.earliestPendingSessionFlow.asLiveData().observe(this) {
            setScheduledServiceIndicator(it)
        }

        binding.sessionScheduledNotification.setOnClickListener {
            startActivity(Intent(this, ImagingSchedulerActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onRestart() {
        super.onRestart()
        refreshUI()
    }

    private fun refreshUI() {
        setRunningSessionIndicatorVisible(ImagingService.IS_RUNNING.value ?: false)
    }

    private fun setRunningSessionIndicatorVisible(visible: Boolean) {
        binding.sessionRunningNotification.isVisible = visible
    }

    private fun setScheduledServiceIndicator(pendingSession: PendingSession?) {
        binding.sessionScheduledNotification.isVisible = pendingSession != null
        pendingSession?.let {
            val time = pendingSession.scheduledDateTime.toLocalTime().format(SHORT_TIME_FORMATTER)
            binding.sessionScheduledNotification.text =
                getString(R.string.scheduled_session_indication, time)
        }
    }
}
