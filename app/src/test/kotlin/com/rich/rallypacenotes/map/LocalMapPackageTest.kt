package com.rich.rallypacenotes.map

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun rejectsAnImportedPathThatDoesNotExist() {
        val missing = File.createTempFile("norcal-missing", ".mbtiles")
        missing.delete()

        assertThrows(IllegalArgumentException::class.java) {
            LocalMapPackage.from(missing)
        }
    }

    @Test
    fun styleUsesTheLocalMbtilesUriWithoutAnyRemoteUrl() {
        val mbtiles = File.createTempFile("norcal", ".mbtiles")
        try {
            val style = LocalMapStyle.forPackage(LocalMapPackage.from(mbtiles))

            assertTrue(
                Regex("\\\"url\\\"\\s*:\\s*\\\"${Regex.escape("mbtiles://${mbtiles.absolutePath}")}\\\"")
                    .containsMatchIn(style),
            )
            assertTrue(
                Regex("\\\"source-layer\\\"\\s*:\\s*\\\"transportation\\\"")
                    .containsMatchIn(style),
            )
            assertFalse(style.contains("http://"))
            assertFalse(style.contains("https://"))
        } finally {
            mbtiles.delete()
        }
    }
}
