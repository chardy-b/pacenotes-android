package com.rich.rallypacenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    private var locationPermissionGranted by mutableStateOf(false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeMapLibreOnce()
        locationPermissionGranted = hasLocationPermission()
        setContent {
            ReplayAlphaApp(
                locationPermissionGranted = locationPermissionGranted,
                onRequestLocationPermission = {
                    locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                },
            )
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun initializeMapLibreOnce() {
        synchronized(initializationLock) {
            if (!mapLibreInitialized) {
                MapLibre.getInstance(applicationContext)
                mapLibreInitialized = true
            }
        }
    }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        val initializationLock = Any()
        var mapLibreInitialized = false
    }
}
