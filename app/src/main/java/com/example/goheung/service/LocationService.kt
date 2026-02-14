package com.example.goheung.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationService"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * 위치 권한 확인
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 위치 업데이트 시작
     * @param interval 업데이트 간격 (밀리초)
     * @param callback 위치 업데이트 콜백
     */
    fun startLocationUpdates(interval: Long, callback: (Location) -> Unit) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            interval
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "Location updated: lat=${location.latitude}, lng=${location.longitude}")
                    callback(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start location updates", e)
        }
    }

    /**
     * 위치 업데이트 중지
     */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
            Log.d(TAG, "Location updates stopped")
        }
    }

    /**
     * 마지막 위치 가져오기
     */
    fun getLastLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            callback(null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    callback(location)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get last location", e)
                    callback(null)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to get last location", e)
            callback(null)
        }
    }
}
