package com.rich.rallypacenotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rich.rallypacenotes.pacenotes.CurveCandidate
import com.rich.rallypacenotes.pacenotes.NormalizedRoute

@Composable
fun RouteCanvas(route: NormalizedRoute, currentDistanceMeters: Double, candidates: List<CurveCandidate>) = Canvas(
    modifier = Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = "Replay route canvas" },
) {
    val points = RouteCanvasProjection.project(route, size.width, size.height, 16f)
    points.zipWithNext().forEach { (from, to) -> drawLine(Color(0xFF006B5A), androidx.compose.ui.geometry.Offset(from.x, from.y), androidx.compose.ui.geometry.Offset(to.x, to.y), 6f) }
    val replay = route.samples.indices.minBy { kotlin.math.abs(route.samples[it].routeDistanceMeters - currentDistanceMeters) }
    val replayPoint = points[replay]
    drawCircle(Color(0xFFFF8A00), 10f, androidx.compose.ui.geometry.Offset(replayPoint.x, replayPoint.y))
    candidates.forEach { candidate ->
        val index = route.samples.indices.minBy { kotlin.math.abs(route.samples[it].routeDistanceMeters - candidate.startDistanceMeters) }
        val marker = points[index]
        drawCircle(Color(0xFF6A1B9A), 7f, androidx.compose.ui.geometry.Offset(marker.x, marker.y))
    }
}
