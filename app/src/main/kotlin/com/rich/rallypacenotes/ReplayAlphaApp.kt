package com.rich.rallypacenotes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rich.rallypacenotes.map.HostedMapStyle
import com.rich.rallypacenotes.map.HostedMapView

@Composable
fun ReplayAlphaApp(
    locationPermissionGranted: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                HostedMapView(locationPermissionGranted = locationPermissionGranted)

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    if (locationPermissionGranted) {
                        Text(
                            text = "Location permission granted",
                            color = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .semantics { contentDescription = "location permission granted" },
                        )
                    } else {
                        Button(
                            onClick = onRequestLocationPermission,
                            modifier = Modifier.semantics { contentDescription = "Enable current location" },
                        ) { Text("Enable current location") }
                    }
                }

                Text(
                    text = HostedMapStyle.ATTRIBUTION,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .semantics { contentDescription = "map attribution" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
