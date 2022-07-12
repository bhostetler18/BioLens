package com.uf.automoth.ui.imaging

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.Executors
import java.util.function.Consumer

class SingleLocationProvider(context: Context) {
    private val locationManager: LocationManager

    init {
        locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun getCurrentLocation(consumer: Consumer<Location>) {
//        val provider = if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
//            LocationManager.FUSED_PROVIDER
//        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            LocationManager.GPS_PROVIDER
//        } else {
//            LocationManager.NETWORK_PROVIDER
//        }
//        if (Build.VERSION.SDK_INT >= 30) {
//            // TODO: probably not the best to create and shutdown an executor every time this runs
//            val executor = Executors.newSingleThreadExecutor()
//            locationManager.getCurrentLocation(provider, null, executor) {
//                executor.shutdown()
//                consumer.accept(it)
//            }
//        } else {
//            locationManager.requestSingleUpdate(provider, { location ->
//                consumer.accept(location)
//            }, null)
//        }

        val executor = Executors.newSingleThreadExecutor()
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            LocationManager.FUSED_PROVIDER,
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
