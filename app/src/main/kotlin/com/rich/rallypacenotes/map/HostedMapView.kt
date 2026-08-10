package com.rich.rallypacenotes.map

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val NORCAL_CENTER = LatLng(39.10964, -121.01905)
private const val NORCAL_DEFAULT_ZOOM = 7.0
private const val LOG_TAG = "HostedMapView"

@SuppressLint("MissingPermission")
@Composable
fun HostedMapView(
    locationPermissionGranted: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(locationPermissionGranted) {
        MapView(context).also { view ->
            view.onCreate(null)
            view.addOnDidFinishRenderingMapListener { fullyRendered ->
                if (fullyRendered) {
                    Log.i(LOG_TAG, "Hosted map render completed")
                }
            }
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(HostedMapStyle.LIBERTY_STYLE_URI)) { style ->
                    Log.i(LOG_TAG, "OpenFreeMap Liberty style loaded")
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(NORCAL_CENTER, NORCAL_DEFAULT_ZOOM))
                    if (locationPermissionGranted) {
                        val locationOptions = LocationComponentOptions.builder(context)
                            .pulseEnabled(true)
                            .build()
                        val activationOptions = LocationComponentActivationOptions.builder(context, style)
                            .locationComponentOptions(locationOptions)
                            .locationEngine(
                                PlatformGpsLocationEngine(context) { delivered ->
                                    val component = map.locationComponent
                                    Log.i(
                                        LOG_TAG,
                                        "MAPLIBRE_AFTER_CALLBACK input=${delivered.latitude},${delivered.longitude} " +
                                            "componentLast=${component.lastKnownLocation?.latitude}," +
                                            "${component.lastKnownLocation?.longitude} " +
                                            "enabled=${component.isLocationComponentEnabled} " +
                                            "cameraMode=${component.cameraMode} " +
                                            "camera=${map.cameraPosition.target.latitude}," +
                                            "${map.cameraPosition.target.longitude}",
                                    )
                                    view.post {
                                        Log.i(
                                            LOG_TAG,
                                            "MAPLIBRE_AFTER_FRAME componentLast=${component.lastKnownLocation} " +
                                                "camera=${map.cameraPosition.target} mode=${component.cameraMode}",
                                        )
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
                        map.locationComponent.cameraMode = CameraMode.TRACKING
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "hosted MapLibre map" },
    )

    DisposableEffect(mapView, lifecycleOwner) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    if (!destroyed) {
                        destroyed = true
                        mapView.onDestroy()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
    }
}
