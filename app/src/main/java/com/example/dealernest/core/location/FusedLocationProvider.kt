package com.example.dealernest.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    // Safe: every call site sits behind the LocationGate, which only lets the app past once
    // hasLocationPermission() is true.
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationPoint? {
        val location = client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .await()
        return location?.let { LocationPoint(it.latitude, it.longitude) }
    }
}
