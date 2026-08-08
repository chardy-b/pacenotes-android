package com.rich.rallypacenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.rich.rallypacenotes.map.BundledMapPackageProvisioner
import com.rich.rallypacenotes.map.LocalMapPackageImporter
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    private var mapPackageVersion by mutableIntStateOf(0)
    private var locationPermissionGranted by mutableStateOf(false)

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    private val selectMapPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openInputStream(uri)?.let { source ->
            runCatching { LocalMapPackageImporter.importInto(filesDir, source) }
                .onSuccess { mapPackageVersion++ }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(applicationContext)
        BundledMapPackageProvisioner.provisionInto(this)
        locationPermissionGranted = hasLocationPermission()
        setContent {
            ReplayAlphaApp(
                appFilesDir = filesDir,
                mapPackageVersion = mapPackageVersion,
                locationPermissionGranted = locationPermissionGranted,
                onRequestLocationPermission = { requestLocationPermission.launch(LOCATION_PERMISSIONS) },
                onImportMapPackage = { selectMapPackage.launch(arrayOf("application/octet-stream", "*/*")) },
            )
        }
    }

    private fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
