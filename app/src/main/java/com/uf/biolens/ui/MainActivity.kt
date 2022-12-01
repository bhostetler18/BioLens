/*
 * Copyright (c) 2022 University of Florida
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.uf.biolens.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.biolens.BioLensApplication
import com.uf.automoth.R
import com.uf.biolens.data.BioLensRepository
import com.uf.biolens.data.PendingSession
import com.uf.automoth.databinding.ActivityMainBinding
import com.uf.automoth.databinding.ActivityMainFilesystemErrorBinding
import com.uf.biolens.imaging.ImagingService
import com.uf.biolens.ui.common.simpleAlertDialogWithOk
import com.uf.biolens.ui.imaging.scheduler.ImagingSchedulerActivity
import com.uf.biolens.utility.SHORT_TIME_FORMATTER

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (!BioLensRepository.isInitialized) {
            val binding = ActivityMainFilesystemErrorBinding.inflate(layoutInflater)
            binding.errorText.movementMethod = LinkMovementMethod.getInstance()
            binding.reloadButton.setOnClickListener {
                if ((application as? BioLensApplication)?.mount() == true) {
                    this.finish()
                    startActivity(intent)
                } else {
                    simpleAlertDialogWithOk(this, R.string.failed_to_mount).show()
                }
            }
            setContentView(binding.root)
            return
        }

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
//                R.id.navigation_info,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setRunningSessionIndicatorVisible(false)
        setScheduledServiceIndicator(null)

        ImagingService.IS_RUNNING.observe(this) { isRunning ->
            setRunningSessionIndicatorVisible(isRunning)
        }

        BioLensRepository.earliestPendingSessionFlow.asLiveData().observe(this) {
            setScheduledServiceIndicator(it)
        }

        binding.sessionScheduledNotification.setOnClickListener {
            startActivity(Intent(this, ImagingSchedulerActivity::class.java))
        }
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
