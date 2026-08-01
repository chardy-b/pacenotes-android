package com.rich.rallypacenotes.ui

import com.rich.rallypacenotes.model.GeoPoint
import com.rich.rallypacenotes.pacenotes.NormalizedRoute
import com.rich.rallypacenotes.pacenotes.NormalizedRouteSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteCanvasProjectionTest {
    @Test
    fun projectsRouteIntoPaddedViewportWhilePreservingSampleOrder() {
        val route = NormalizedRoute(
            sourceRouteId = "projection-fixture",
            samples = listOf(
                NormalizedRouteSample(GeoPoint(45.0, 7.0), 0.0),
                NormalizedRouteSample(GeoPoint(45.001, 7.002), 100.0),
            ),
        )

        val points = RouteCanvasProjection.project(route, width = 300f, height = 200f, padding = 20f)

        assertEquals(2, points.size)
        assertTrue(points.all { it.x in 20f..280f && it.y in 20f..180f })
        assertTrue(points[0].x < points[1].x)
        assertTrue(points[0].y > points[1].y)
    }

    @Test
    fun rejectsInvalidViewportDimensions() {
        val route = NormalizedRoute(
            sourceRouteId = "projection-fixture",
            samples = listOf(NormalizedRouteSample(GeoPoint(45.0, 7.0), 0.0), NormalizedRouteSample(GeoPoint(45.001, 7.002), 100.0)),
        )

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            RouteCanvasProjection.project(route, width = 0f, height = 200f, padding = 20f)
        }
    }
}
