package com.uf.automoth.ui.imaging

import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.util.concurrent.Executors
import java.util.function.Consumer

class SingleLocationProvider(context: Context) {
    val locationManager: LocationManager

    init {
        locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun getCurrentLocation(consumer: Consumer<Location>) {
        // TODO: probably not the best to create and shutdown an executor every time this runs
        val executor = Executors.newSingleThreadExecutor()
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        locationManager.getCurrentLocation(provider, null, executor) {
            executor.shutdown()
            consumer.accept(it)
        }
    }
}
