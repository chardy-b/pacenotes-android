package com.rich.rallypacenotes.map

const val NAVIGATION_CAMERA_ZOOM = 17.0
const val NAVIGATION_CAMERA_PITCH = 50.0

data class MapCameraSpec(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val bearingDegrees: Double,
    val pitchDegrees: Double,
)

fun cameraSpecFor(
    viewMode: MapViewMode,
    latitude: Double,
    longitude: Double,
    navigationBearingDegrees: Double,
): MapCameraSpec = MapCameraSpec(
    latitude = latitude,
    longitude = longitude,
    zoom = NAVIGATION_CAMERA_ZOOM,
    bearingDegrees = if (viewMode == MapViewMode.NORTH_UP) 0.0 else navigationBearingDegrees,
    pitchDegrees = if (viewMode == MapViewMode.NORTH_UP) 0.0 else NAVIGATION_CAMERA_PITCH,
)
