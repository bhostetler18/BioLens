package com.uf.automoth.network

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.location.LocationCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SingleLocationProvider(context: Context) {
    private val locationManager: LocationManager

    init {
        locationManager =
            context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
            context,
            ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // The permission is actually checked, but the linter doesn't recognize it because the check
    // occurs inside isLocationPermissionGranted
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, fallbackOnLastKnown: Boolean): Location? {
        if (!isLocationPermissionGranted(context)) {
            Log.d(TAG, "Location permission denied")
            return null
        }

        val provider = getFirstAvailableProvider(*PREFERRED_PROVIDERS)
        if (provider == null) {
            Log.d(TAG, "No location providers enabled")
            return null
        }

        Log.d(TAG, "Using provider: $provider")
        val location = withContext(Dispatchers.IO) {
            getLocation(provider)
        }

        return if (location != null || !fallbackOnLastKnown) {
            location
        } else {
            Log.d(TAG, "Falling back on cached location, may be inaccurate")
            getNewestCachedLocation()
        }
    }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private suspend fun getLocation(provider: String): Location? =
        suspendCoroutine { continuation ->
            val executor = Executors.newSingleThreadExecutor()
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                null,
                executor
            ) { location ->
                executor.shutdown()
                continuation.resume(location)
            }
        }

    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun getNewestCachedLocation(): Location? {
        val now = SystemClock.elapsedRealtime()
        return PREFERRED_PROVIDERS.mapNotNull {
            if (locationManager.isProviderEnabled(it)) {
                locationManager.getLastKnownLocation(it)
            } else {
                null
            }
        }.minByOrNull {
            now - LocationCompat.getElapsedRealtimeMillis(it)
        }
    }

    private fun getFirstAvailableProvider(vararg preferredProviders: String): String? {
        for (provider in preferredProviders) {
            if (locationManager.isProviderEnabled(provider)) {
                return provider
            }
        }
        return null
    }

    companion object {
        private const val TAG = "[LOCATION]"
        private val PREFERRED_PROVIDERS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                LocationManager.FUSED_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
        } else {
            arrayOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
        }
    }
}
