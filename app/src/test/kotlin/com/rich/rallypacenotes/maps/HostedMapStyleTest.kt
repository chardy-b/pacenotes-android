package com.rich.rallypacenotes.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class HostedMapStyleTest {
    @Test
    fun usesCredentialFreeLibertyStyleAndNorthernCaliforniaCamera() {
        assertEquals("https://tiles.openfreemap.org/styles/liberty", HostedMapStyle.libertyStyleUrl)
        assertEquals("© OpenFreeMap · © OpenStreetMap contributors · © OpenMapTiles", HostedMapStyle.attribution)
        assertEquals(-122.4194, HostedMapStyle.initialLongitude, 0.0)
        assertEquals(38.5816, HostedMapStyle.initialLatitude, 0.0)
        assertEquals(8.0, HostedMapStyle.initialZoom, 0.0)
    }
}
