package com.rich.rallypacenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.rich.rallypacenotes.replay.ReplayAlphaFixture
import com.rich.rallypacenotes.replay.ReplayController
import com.rich.rallypacenotes.ui.RouteCanvas

@Composable
fun ReplayAlphaApp() {
    val controller = remember { ReplayController(ReplayAlphaFixture.route.samples.last().routeDistanceMeters) }
    var state by remember { mutableStateOf(controller.state) }
    val candidateText = ReplayAlphaFixture.candidates.firstOrNull()?.let {
        "Detected candidate: ${it.direction.name.lowercase()} curve, severity ${it.severity}, ${it.startDistanceMeters.toInt()}-${it.endDistanceMeters.toInt()} m"
    } ?: "No geometry-only curve candidate detected"

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Replay Alpha — Not for Driving", style = MaterialTheme.typography.headlineSmall)
                Text("Route fixture: ${ReplayAlphaFixture.name}", modifier = Modifier.semantics { contentDescription = "route fixture name" })
                RouteCanvas(
                    route = ReplayAlphaFixture.route,
                    currentDistanceMeters = state.currentDistanceMeters,
                    candidates = ReplayAlphaFixture.candidates,
                )
                Text("Status: ${state.status.name.lowercase()} | Current distance: ${"%.1f".format(state.currentDistanceMeters)} m", modifier = Modifier.semantics { contentDescription = "replay status and current distance" })
                Text(candidateText, modifier = Modifier.semantics { contentDescription = "geometry-only detected candidate" })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                    Button(
                        onClick = {
                            controller.start()
                            controller.advanceBy(20.0)
                            state = controller.state
                        },
                        modifier = Modifier.semantics { contentDescription = "Start replay" },
                    ) { Text("Start") }
                    Button(onClick = { controller.pause(); state = controller.state }, modifier = Modifier.semantics { contentDescription = "Pause replay" }) { Text("Pause") }
                    Button(onClick = { controller.reset(); state = controller.state }, modifier = Modifier.semantics { contentDescription = "Reset replay" }) { Text("Reset") }
                }
            }
        }
    }
}
