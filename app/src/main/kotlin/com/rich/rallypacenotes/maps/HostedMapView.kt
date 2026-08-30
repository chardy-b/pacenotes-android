package com.rich.rallypacenotes.maps

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val TAG = "HostedMapView"
private const val HOSTED_MAP_RENDER_TIMEOUT_MILLIS = 20_000L

sealed interface HostedMapState {
    data object Loading : HostedMapState
    data object Ready : HostedMapState
    data class Error(val message: String) : HostedMapState
}

@Composable
fun HostedMapView(
    locationPermissionGranted: Boolean,
    modifier: Modifier = Modifier,
    onStateChanged: (HostedMapState) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStateChanged by rememberUpdatedState(onStateChanged)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var styleLoaded by remember { mutableStateOf(false) }
    var fullyRendered by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var locationComponentActivated by remember { mutableStateOf(false) }
    val mapView = remember(context) {
        MapView(context).also { view ->
            view.onCreate(null)
            view.addOnDidFinishRenderingMapListener { rendered ->
                if (rendered) {
                    fullyRendered = true
                    Log.i(TAG, "HOSTED_MAP_FULLY_RENDERED")
                }
            }
            view.addOnDidFailLoadingMapListener { message ->
                loadFailed = true
                Log.w(TAG, "HOSTED_MAP_LOAD_FAILED")
                currentOnStateChanged(HostedMapState.Error(message))
            }
            view.getMapAsync { loadedMap ->
                map = loadedMap
                Log.i(TAG, "HOSTED_MAP_STYLE_LOADING ${HostedMapStyle.libertyStyleUrl}")
                loadedMap.setStyle(Style.Builder().fromUri(HostedMapStyle.libertyStyleUrl)) { loadedStyle ->
                    style = loadedStyle
                    styleLoaded = true
                    loadedMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(HostedMapStyle.initialLatitude, HostedMapStyle.initialLongitude),
                            HostedMapStyle.initialZoom,
                        ),
                    )
                    Log.i(TAG, "HOSTED_MAP_STYLE_LOADED")
                }
            }
        }
    }

    val mapReady = styleLoaded && fullyRendered
    LaunchedEffect(mapReady) {
        if (mapReady) currentOnStateChanged(HostedMapState.Ready)
    }

    LaunchedEffect(mapReady, loadFailed) {
        if (!mapReady && !loadFailed) {
            delay(HOSTED_MAP_RENDER_TIMEOUT_MILLIS)
            currentOnStateChanged(HostedMapState.Error("Hosted map did not render in time"))
        }
    }

    LaunchedEffect(map, style, locationPermissionGranted) {
        val activeMap = map ?: return@LaunchedEffect
        val activeStyle = style ?: return@LaunchedEffect
        if (locationPermissionGranted) {
            activeMap.locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, activeStyle).build(),
            )
            activeMap.locationComponent.isLocationComponentEnabled = true
            locationComponentActivated = true
            Log.i(TAG, "HOSTED_MAP_LOCATION_ENABLED")
        } else if (locationComponentActivated) {
            activeMap.locationComponent.isLocationComponentEnabled = false
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.semantics { contentDescription = "hosted MapLibre map" },
    )

    DisposableEffect(mapView, lifecycleOwner) {
        var destroyed = false
        fun startForCurrentLifecycleState() {
            when (lifecycleOwner.lifecycle.currentState) {
                Lifecycle.State.RESUMED -> {
                    mapView.onStart()
                    mapView.onResume()
                }
                Lifecycle.State.STARTED -> mapView.onStart()
                else -> Unit
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) {
                    destroyed = true
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        startForCurrentLifecycleState()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                destroyed = true
                mapView.onDestroy()
            }
        }
    }
}
