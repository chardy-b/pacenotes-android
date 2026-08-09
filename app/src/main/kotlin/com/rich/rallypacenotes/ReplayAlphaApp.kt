package com.rich.rallypacenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rich.rallypacenotes.map.LocalMapPackageLocator
import com.rich.rallypacenotes.map.OfflineMapView
import java.io.File

@Composable
fun ReplayAlphaApp(
    appFilesDir: File? = null,
    mapPackageVersion: Int = 0,
    locationPermissionGranted: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    onImportMapPackage: () -> Unit = {},
) {
    val localMapPackage = remember(appFilesDir, mapPackageVersion) {
        appFilesDir?.let(LocalMapPackageLocator::find)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (localMapPackage == null) {
                MissingMapPackage(onImportMapPackage)
            } else {
                FullScreenOfflineMap(
                    localMapPackage = localMapPackage,
                    locationPermissionGranted = locationPermissionGranted,
                    onRequestLocationPermission = onRequestLocationPermission,
                    onImportMapPackage = onImportMapPackage,
                )
            }
        }
    }
}

@Composable
private fun FullScreenOfflineMap(
    localMapPackage: com.rich.rallypacenotes.map.LocalMapPackage,
    locationPermissionGranted: Boolean,
    onRequestLocationPermission: () -> Unit,
    onImportMapPackage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OfflineMapView(
            localMapPackage = localMapPackage,
            locationPermissionGranted = locationPermissionGranted,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ) {
            if (locationPermissionGranted) {
                Text(
                    text = "Current location active",
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics { contentDescription = "current location marker" },
                )
            } else {
                Button(
                    onClick = onRequestLocationPermission,
                    modifier = Modifier.semantics { contentDescription = "Enable current location" },
                ) { Text("Enable current location") }
            }
        }

        Button(
            onClick = onImportMapPackage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .semantics { contentDescription = "Import offline map package" },
        ) { Text("Import map") }

        Text(
            text = "© OpenStreetMap contributors · © OpenMapTiles",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .semantics { contentDescription = "map attribution" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun MissingMapPackage(onImportMapPackage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Offline map unavailable")
        Text("Import a local Northern California map package to view the basemap.")
        Button(
            onClick = onImportMapPackage,
            modifier = Modifier.semantics { contentDescription = "Import offline map package" },
        ) { Text("Import offline map") }
    }
}
