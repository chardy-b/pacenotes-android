package com.rich.rallypacenotes.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun OfflineMapView(localMapPackage: LocalMapPackage) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { context ->
            MapView(context).also { view ->
                mapView = view
                view.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromJson(LocalMapStyle.forPackage(localMapPackage)))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "offline MapLibre map" },
    )

    DisposableEffect(mapView) {
        mapView?.onStart()
        onDispose {
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }
}
