package com.example.dealernest.core.location

data class LocationPoint(val latitude: Double, val longitude: Double)

/**
 * One-shot current-location capture. Callers must only invoke this after location permission
 * has been granted (see [LocationPermissionState]) - the SRS forbids manual lat/long entry
 * anywhere in the app, so this is the only source of coordinates for Vendor create/edit.
 */
interface LocationProvider {
    suspend fun getCurrentLocation(): LocationPoint?
}
