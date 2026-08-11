package com.rich.rallypacenotes.map

import android.annotation.SuppressLint
import android.location.Location
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rich.rallypacenotes.R
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val NORCAL_CENTER = LatLng(39.10964, -121.01905)
private const val NORCAL_DEFAULT_ZOOM = 7.0
private const val CAMERA_ANIMATION_DURATION_MILLIS = 500
private const val LOG_TAG = "HostedMapView"

@SuppressLint("MissingPermission")
@Composable
fun HostedMapView(
    locationPermissionGranted: Boolean,
    viewMode: MapViewMode,
    onDirectionChanged: (DirectionDecision) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var deviceHeading by remember { mutableStateOf<DeviceHeadingSample?>(null) }
    val lastReliableCourse = remember { LastReliableCourse() }
    val smoother = remember { CircularHeadingSmoother() }

    val mapView = remember(locationPermissionGranted) {
        MapView(context).also { view ->
            view.onCreate(null)
            view.addOnDidFinishRenderingMapListener { fullyRendered ->
                if (fullyRendered) Log.i(LOG_TAG, "Hosted map render completed")
            }
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(HostedMapStyle.LIBERTY_STYLE_URI)) { style ->
                    Log.i(LOG_TAG, "OpenFreeMap Liberty style loaded")
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(NORCAL_CENTER, NORCAL_DEFAULT_ZOOM))
                    if (locationPermissionGranted) {
                        val locationOptions = LocationComponentOptions.builder(context)
                            .foregroundDrawable(R.drawable.location_puck_foreground)
                            .backgroundDrawable(R.drawable.location_puck_accuracy)
                            .pulseEnabled(true)
                            .build()
                        val activationOptions = LocationComponentActivationOptions.builder(context, style)
                            .locationComponentOptions(locationOptions)
                            .useSpecializedLocationLayer(true)
                            .locationEngine(
                                PlatformGpsLocationEngine(context) { location ->
                                    view.post { lastLocation = location }
                                },
                            )
                            .useDefaultLocationEngine(false)
                            .locationEngineRequest(
                                LocationEngineRequest.Builder(1_000)
                                    .setFastestInterval(1_000)
                                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                                    .build(),
                            )
                            .build()
                        map.locationComponent.activateLocationComponent(activationOptions)
                        map.locationComponent.isLocationComponentEnabled = true
                        map.locationComponent.cameraMode = CameraMode.NONE
                    }
                }
            }
        }
    }

    val direction = remember(lastLocation, deviceHeading) {
        directionFor(lastLocation, deviceHeading, lastReliableCourse)
    }
    if (direction.source == DirectionSource.COURSE) {
        lastReliableCourse.bearingDegrees = direction.bearingDegrees
        lastReliableCourse.observedAtMillis = SystemClock.elapsedRealtime()
    }
    val smoothedBearing = remember(lastLocation, deviceHeading, direction, viewMode) {
        if (viewMode == MapViewMode.NORTH_UP) 0.0 else smoother.update(direction.bearingDegrees, SystemClock.elapsedRealtime())
    }

    LaunchedEffect(direction) {
        onDirectionChanged(direction)
    }

    LaunchedEffect(mapView, lastLocation, viewMode, direction, smoothedBearing) {
        val location = lastLocation ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            if (!map.locationComponent.isLocationComponentActivated) return@getMapAsync
            map.locationComponent.renderMode = if (direction.source == DirectionSource.COURSE) {
                RenderMode.GPS
            } else {
                RenderMode.NORMAL
            }
            val spec = cameraSpecFor(
                viewMode = viewMode,
                latitude = location.latitude,
                longitude = location.longitude,
                navigationBearingDegrees = smoothedBearing,
            )
            val camera = CameraPosition.Builder()
                .target(LatLng(spec.latitude, spec.longitude))
                .zoom(spec.zoom)
                .bearing(spec.bearingDegrees)
                .tilt(spec.pitchDegrees)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), CAMERA_ANIMATION_DURATION_MILLIS)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "hosted MapLibre map" },
    )

    DisposableEffect(mapView, lifecycleOwner) {
        val headingSource = RotationVectorHeadingSource(context) { sample ->
            mapView.post { deviceHeading = sample }
        }
        var headingStarted = false
        fun startHeading() {
            if (!headingStarted) {
                headingStarted = true
                headingSource.start()
            }
        }
        fun stopHeading() {
            if (headingStarted) {
                headingStarted = false
                headingSource.stop()
            }
        }
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    startHeading()
                }
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {
                    stopHeading()
                    mapView.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) {
                    stopHeading()
                    destroyed = true
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startHeading()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopHeading()
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
    }
}

private fun directionFor(
    location: Location?,
    heading: DeviceHeadingSample?,
    retainedCourse: LastReliableCourse,
): DirectionDecision {
    val nowMillis = SystemClock.elapsedRealtime()
    val locationAge = location?.let { nowMillis - it.elapsedRealtimeNanos / 1_000_000L } ?: Long.MAX_VALUE
    return DirectionPolicy.select(
        DirectionInput(
            courseBearingDegrees = location?.takeIf(Location::hasBearing)?.bearing?.toDouble(),
            courseBearingAccuracyDegrees = location?.takeIf(Location::hasBearingAccuracy)?.bearingAccuracyDegrees?.toDouble(),
            speedMetresPerSecond = location?.takeIf(Location::hasSpeed)?.speed?.toDouble() ?: 0.0,
            locationAgeMillis = locationAge,
            deviceHeadingDegrees = heading?.degrees,
            deviceHeadingAgeMillis = heading?.let { nowMillis - it.observedAtElapsedRealtimeMillis },
            deviceHeadingAccuracy = heading?.accuracy ?: DeviceHeadingAccuracy.UNRELIABLE,
            lastReliableCourseDegrees = retainedCourse.bearingDegrees,
            lastReliableCourseAgeMillis = retainedCourse.observedAtMillis?.let { nowMillis - it },
        ),
    )
}

private class LastReliableCourse(
    var bearingDegrees: Double? = null,
    var observedAtMillis: Long? = null,
)
