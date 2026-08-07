package com.rich.rallypacenotes.map

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMapPackageTest {
    @Test
    fun mapLibreUriUsesOnlyTheImportedLocalMbtilesFile() {
        val mbtiles = File.createTempFile("norcal", ".mbtiles")
        try {
            val packageUnderTest = LocalMapPackage.from(mbtiles)

            assertEquals("mbtiles://${mbtiles.absolutePath}", packageUnderTest.mapLibreUri)
        } finally {
            mbtiles.delete()
        }
    }
}
