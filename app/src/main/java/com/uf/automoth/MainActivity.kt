package com.uf.automoth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uf.automoth.databinding.ActivityMainBinding
import com.uf.automoth.ui.imaging.ImagingService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        val navView: BottomNavigationView = binding.bottomNavBar

        // See https://stackoverflow.com/questions/58703451/fragmentcontainerview-as-navhostfragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
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
            setServiceIndicatorBarVisible(isRunning)
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
        setServiceIndicatorBarVisible(ImagingService.IS_RUNNING.value ?: false)
    }

    fun setServiceIndicatorBarVisible(visible: Boolean) {
        binding.activeServiceBar.isVisible = visible
    }
}
