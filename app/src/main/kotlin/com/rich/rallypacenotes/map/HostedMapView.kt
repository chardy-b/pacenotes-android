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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rich.rallypacenotes.R
import java.util.Locale
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
    var foregroundStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var freshnessTick by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lastReliableCourse = remember { LastReliableCourse() }
    val smoother = remember { CircularHeadingSmoother() }

    val mapView = remember(locationPermissionGranted) {
        MapView(context).also { view ->
            view.onCreate(null)
            var pendingCameraFrame: String? = null
            view.addOnDidFinishRenderingMapListener { fullyRendered ->
                if (fullyRendered) {
                    Log.i(LOG_TAG, "Hosted map render completed")
                    pendingCameraFrame?.let { geometry ->
                        Log.i(LOG_TAG, "Camera frame rendered $geometry")
                        pendingCameraFrame = null
                    }
                }
            }
            view.getMapAsync { map ->
                map.addOnCameraIdleListener {
                    val position = map.cameraPosition
                    val target = position.target ?: return@addOnCameraIdleListener
                    val geometry = String.format(
                        Locale.US,
                        "latitude=%.7f longitude=%.7f zoom=%.1f pitch=%.1f bearing=%.1f",
                        target.latitude,
                        target.longitude,
                        position.zoom,
                        position.tilt,
                        position.bearing,
                    )
                    Log.i(LOG_TAG, "Camera idle $geometry")
                    pendingCameraFrame = geometry
                }
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
                                    view.post {
                                        updateRetainedCourseFromDeliveredLocation(location, lastReliableCourse)
                                        lastLocation = location
                                    }
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

    LaunchedEffect(foregroundStarted, lastLocation, deviceHeading) {
        if (!foregroundStarted) return@LaunchedEffect
        while (true) {
            freshnessTick = SystemClock.elapsedRealtime()
            delay(250)
        }
    }

    val direction = remember(lastLocation, deviceHeading, freshnessTick) {
        directionFor(lastLocation, deviceHeading, lastReliableCourse)
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
            Log.i(
                LOG_TAG,
                "Camera request mode=$viewMode latitude=${spec.latitude} longitude=${spec.longitude} " +
                    "bearing=${spec.bearingDegrees} pitch=${spec.pitchDegrees}",
            )
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
        var mapStarted = false
        var mapResumed = false
        fun startMap() {
            if (!mapStarted) {
                mapStarted = true
                mapView.onStart()
            }
        }
        fun resumeMap() {
            startMap()
            if (!mapResumed) {
                mapResumed = true
                mapView.onResume()
            }
        }
        fun pauseMap() {
            if (mapResumed) {
                mapResumed = false
                mapView.onPause()
            }
        }
        fun stopMap() {
            pauseMap()
            if (mapStarted) {
                mapStarted = false
                mapView.onStop()
            }
        }
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    foregroundStarted = true
                    startMap()
                    startHeading()
                }
                Lifecycle.Event.ON_RESUME -> resumeMap()
                Lifecycle.Event.ON_PAUSE -> pauseMap()
                Lifecycle.Event.ON_STOP -> {
                    foregroundStarted = false
                    stopHeading()
                    stopMap()
                }
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) {
                    foregroundStarted = false
                    stopHeading()
                    stopMap()
                    destroyed = true
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        when {
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                foregroundStarted = true
                startHeading()
                resumeMap()
            }
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> {
                foregroundStarted = true
                startHeading()
                startMap()
            }
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            foregroundStarted = false
            stopHeading()
            stopMap()
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
    }
}

private fun updateRetainedCourseFromDeliveredLocation(
    location: Location,
    retainedCourse: LastReliableCourse,
) {
    val decision = directionFor(location, heading = null, retainedCourse = retainedCourse)
    if (decision.source == DirectionSource.COURSE) {
        retainedCourse.bearingDegrees = decision.bearingDegrees
        // Bind retention to the actual Android fix time, never a Compose recomposition time.
        retainedCourse.observedAtMillis = location.elapsedRealtimeNanos / 1_000_000L
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
