package com.uf.automoth.network

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.Executors
import java.util.function.Consumer

class SingleLocationProvider(context: Context) {
    private val locationManager: LocationManager

    init {
        locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun getCurrentLocation(context: Context, consumer: Consumer<Location?>) {
        val executor = Executors.newSingleThreadExecutor()
        if (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            consumer.accept(null)
            return
        }
        val provider = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager.FUSED_PROVIDER
        } else {
            LocationManager.GPS_PROVIDER // TODO: handle unavailable GPS?
        }
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            null,
            executor
        ) { location ->
            if (location != null) {
                consumer.accept(location)
                Log.d("[LOCATION]", location.toString())
            }
            executor.shutdown()
        }
    }
}
