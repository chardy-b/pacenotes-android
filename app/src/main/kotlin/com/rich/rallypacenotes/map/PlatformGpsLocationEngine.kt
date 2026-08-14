package com.rich.rallypacenotes.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Location engine for MapLibre that consumes the platform GNSS provider directly.
 *
 * Pacenotes needs a high-accuracy outdoor fix. Pinning the provider avoids the
 * framework's criteria-based selection choosing a fused/passive provider that does
 * not receive emulator GNSS fixes or produce a MapLibre puck.
 */
class PlatformGpsLocationEngine(
    context: Context,
    private val onLocationDelivered: (Location) -> Unit = {},
) : LocationEngine {
    private companion object {
        const val LOG_TAG = "PlatformGpsLocationEngine"
    }

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val listeners = mutableMapOf<LocationEngineCallback<LocationEngineResult>, LocationListener>()

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            callback.onFailure(IllegalStateException("GPS last location unavailable"))
        } else {
            callback.onSuccess(LocationEngineResult.create(location))
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?,
    ) {
        removeLocationUpdates(callback)
        val listener = LocationListener { location ->
            Log.i(
                LOG_TAG,
                "GPS fix latitude=${location.latitude} longitude=${location.longitude} " +
                    "speed=${location.speed} bearing=${location.bearing} elapsedNanos=${location.elapsedRealtimeNanos}",
            )
            onLocationDelivered(location)
            callback.onSuccess(LocationEngineResult.create(location))
        }
        listeners[callback] = listener
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            request.interval,
            request.displacement,
            listener,
            looper,
        )
        Log.i(LOG_TAG, "GPS provider/listener registered provider=${LocationManager.GPS_PROVIDER}")
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent) {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            request.interval,
            request.displacement,
            pendingIntent,
        )
    }

    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        listeners.remove(callback)?.let(locationManager::removeUpdates)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        locationManager.removeUpdates(pendingIntent)
    }
}
