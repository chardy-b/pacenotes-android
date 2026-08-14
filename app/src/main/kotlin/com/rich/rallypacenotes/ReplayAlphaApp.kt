package com.rich.rallypacenotes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rich.rallypacenotes.map.DirectionDecision
import com.rich.rallypacenotes.map.DirectionSource
import com.rich.rallypacenotes.map.HostedMapStyle
import com.rich.rallypacenotes.map.HostedMapView
import com.rich.rallypacenotes.map.MapViewMode

@Composable
fun ReplayAlphaApp(
    locationPermissionGranted: Boolean = false,
) {
    var viewMode by remember { mutableStateOf(MapViewMode.NAVIGATION) }
    var direction by remember {
        mutableStateOf(DirectionDecision(0.0, DirectionSource.NORTH_UP))
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                HostedMapView(
                    locationPermissionGranted = locationPermissionGranted,
                    viewMode = viewMode,
                    onDirectionChanged = { direction = it },
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Text(
                        text = locationStatus(locationPermissionGranted, direction),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .semantics {
                                contentDescription = if (locationPermissionGranted) {
                                    "location permission granted"
                                } else {
                                    "location permission unavailable"
                                }
                            },
                    )
                }

                Text(
                    text = HostedMapStyle.ATTRIBUTION,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .semantics { contentDescription = "map attribution" },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                )

                FloatingActionButton(
                    onClick = { viewMode = viewMode.toggled() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .semantics { contentDescription = viewMode.nextActionContentDescription },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Explore,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private fun locationStatus(
    locationPermissionGranted: Boolean,
    direction: DirectionDecision,
): String = when {
    !locationPermissionGranted -> "Location permission unavailable"
    direction.source == DirectionSource.NORTH_UP -> "Location permission granted · awaiting direction"
    else -> "Location permission granted · ${direction.source.displayName()}"
}

private fun DirectionSource.displayName(): String = when (this) {
    DirectionSource.COURSE -> "travel direction"
    DirectionSource.DEVICE_HEADING -> "device heading"
    DirectionSource.RETAINED_COURSE -> "last travel direction"
    DirectionSource.NORTH_UP -> "north-up"
}
