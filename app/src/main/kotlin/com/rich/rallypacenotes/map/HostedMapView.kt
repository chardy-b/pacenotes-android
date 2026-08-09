package com.rich.rallypacenotes.map

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
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

@SuppressLint("MissingPermission")
@Composable
fun HostedMapView(
    locationPermissionGranted: Boolean,
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { context ->
            MapView(context).also { view ->
                view.onCreate(null)
                mapView = view
                view.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(HostedMapStyle.LIBERTY_STYLE_URI)) { style ->
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(NORCAL_CENTER, NORCAL_DEFAULT_ZOOM))
                        if (locationPermissionGranted) {
                            val locationComponent = map.locationComponent
                            val locationOptions = LocationComponentOptions.builder(context)
                                .pulseEnabled(true)
                                .build()
                            val activationOptions = LocationComponentActivationOptions.builder(context, style)
                                .locationComponentOptions(locationOptions)
                                .useDefaultLocationEngine(true)
                                .locationEngineRequest(
                                    LocationEngineRequest.Builder(1_000)
                                        .setFastestInterval(1_000)
                                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                                        .build(),
                                )
                                .build()
                            locationComponent.activateLocationComponent(activationOptions)
                            locationComponent.isLocationComponentEnabled = true
                            locationComponent.cameraMode = CameraMode.TRACKING
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "hosted MapLibre map" },
    )

    DisposableEffect(mapView) {
        mapView?.onStart()
        mapView?.onResume()
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }
}
