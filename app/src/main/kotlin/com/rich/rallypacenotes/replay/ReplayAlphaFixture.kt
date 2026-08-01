package com.rich.rallypacenotes.replay

import com.rich.rallypacenotes.model.GeoPoint
import com.rich.rallypacenotes.pacenotes.CurveCandidate
import com.rich.rallypacenotes.pacenotes.CurveDetector
import com.rich.rallypacenotes.pacenotes.NormalizedRoute
import com.rich.rallypacenotes.pacenotes.NormalizedRouteSample
import kotlin.math.cos
import kotlin.math.sin

/** Deterministic, synthetic geometry-only route used by Replay Alpha. */
object ReplayAlphaFixture {
    const val name = "Synthetic right curve"

    val route: NormalizedRoute = run {
        val headings = listOf(0.0, 20.0, 40.0, 60.0, 80.0)
        val points = mutableListOf(GeoPoint(45.0, 7.0))
        headings.forEach { heading ->
            val previous = points.last()
            val lengthDegrees = 20.0 / 111_000.0
            points += GeoPoint(
                latitude = previous.latitude + lengthDegrees * cos(Math.toRadians(heading)),
                longitude = previous.longitude + lengthDegrees * sin(Math.toRadians(heading)),
            )
        }
        NormalizedRoute(
            sourceRouteId = "synthetic-right-curve",
            samples = points.mapIndexed { index, point ->
                NormalizedRouteSample(point, index * 20.0)
            },
        )
    }

    val candidates: List<CurveCandidate> = CurveDetector.detect(route)
}
