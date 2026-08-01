package com.rich.rallypacenotes.ui

import com.rich.rallypacenotes.pacenotes.NormalizedRoute

data class RouteCanvasPoint(val x: Float, val y: Float)

object RouteCanvasProjection {
    fun project(route: NormalizedRoute, width: Float, height: Float, padding: Float): List<RouteCanvasPoint> {
        require(width.isFinite() && height.isFinite() && width > 2 * padding && height > 2 * padding) {
            "Viewport must exceed padding"
        }
        require(padding.isFinite() && padding >= 0f) { "Padding must be finite and non-negative" }

        val latitudes = route.samples.map { it.point.latitude }
        val longitudes = route.samples.map { it.point.longitude }
        val latitudeSpan = (latitudes.max() - latitudes.min()).takeIf { it > 0.0 } ?: 1.0
        val longitudeSpan = (longitudes.max() - longitudes.min()).takeIf { it > 0.0 } ?: 1.0
        val drawableWidth = width - 2 * padding
        val drawableHeight = height - 2 * padding

        return route.samples.map { sample ->
            RouteCanvasPoint(
                x = (padding + (sample.point.longitude - longitudes.min()) / longitudeSpan * drawableWidth).toFloat(),
                y = (height - padding - (sample.point.latitude - latitudes.min()) / latitudeSpan * drawableHeight).toFloat(),
            )
        }
    }
}
