package com.rich.rallypacenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.rich.rallypacenotes.maps.HostedMapState
import com.rich.rallypacenotes.maps.HostedMapStyle
import com.rich.rallypacenotes.maps.HostedMapView
import com.rich.rallypacenotes.replay.ReplayAlphaFixture
import com.rich.rallypacenotes.replay.ReplayController
import com.rich.rallypacenotes.ui.RouteCanvas

@Composable
fun ReplayAlphaApp(
    locationPermissionGranted: Boolean,
    onRequestLocationPermission: () -> Unit,
) {
    var mapState by remember { mutableStateOf<HostedMapState>(HostedMapState.Loading) }
    val controller = remember { ReplayController(ReplayAlphaFixture.route.samples.last().routeDistanceMeters) }
    var replayState by remember { mutableStateOf(controller.state) }
    val candidateText = ReplayAlphaFixture.candidates.firstOrNull()?.let {
        "Detected candidate: ${it.direction.name.lowercase()} curve, severity ${it.severity}, ${it.startDistanceMeters.toInt()}-${it.endDistanceMeters.toInt()} m"
    } ?: "No geometry-only curve candidate detected"
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                HostedMapView(
                    locationPermissionGranted = locationPermissionGranted,
                    modifier = Modifier.fillMaxSize(),
                ) { mapState = it }
                Column(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Rally Pacenotes", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        when (mapState) {
                            HostedMapState.Loading -> "Map status: loading"
                            HostedMapState.Ready -> "Map status: ready"
                            is HostedMapState.Error -> "Map status: error"
                        },
                        modifier = Modifier.semantics { contentDescription = "hosted map status" },
                    )
                    Text(
                        HostedMapStyle.attribution,
                        modifier = Modifier.semantics { contentDescription = "map attribution" },
                    )
                    if (!locationPermissionGranted) {
                        Button(
                            onClick = onRequestLocationPermission,
                            modifier = Modifier.semantics { contentDescription = "Enable location" },
                        ) { Text("Enable location") }
                    }
                    Text(
                        if (locationPermissionGranted) {
                            "Location enabled"
                        } else {
                            "Location disabled"
                        },
                        modifier = Modifier.semantics { contentDescription = "current location status" },
                    )
                    Text(
                        "Route fixture: ${ReplayAlphaFixture.name}",
                        modifier = Modifier.semantics { contentDescription = "route fixture name" },
                    )
                    RouteCanvas(
                        route = ReplayAlphaFixture.route,
                        currentDistanceMeters = replayState.currentDistanceMeters,
                        candidates = ReplayAlphaFixture.candidates,
                    )
                    Text(
                        "Status: ${replayState.status.name.lowercase()} | Current distance: ${"%.1f".format(replayState.currentDistanceMeters)} m",
                        modifier = Modifier.semantics { contentDescription = "replay status and current distance" },
                    )
                    Text(candidateText, modifier = Modifier.semantics { contentDescription = "geometry-only detected candidate" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                controller.start()
                                controller.advanceBy(20.0)
                                replayState = controller.state
                            },
                            modifier = Modifier.semantics { contentDescription = "Start replay" },
                        ) { Text("Start") }
                        Button(
                            onClick = { controller.pause(); replayState = controller.state },
                            modifier = Modifier.semantics { contentDescription = "Pause replay" },
                        ) { Text("Pause") }
                        Button(
                            onClick = { controller.reset(); replayState = controller.state },
                            modifier = Modifier.semantics { contentDescription = "Reset replay" },
                        ) { Text("Reset") }
                    }
                }
            }
        }
    }
}
