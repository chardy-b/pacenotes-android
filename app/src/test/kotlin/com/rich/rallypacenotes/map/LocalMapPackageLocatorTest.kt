package com.rich.rallypacenotes.map

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMapPackageLocatorTest {
    @Test
    fun findsTheCanonicalImportedMapInAppPrivateStorage() {
        val appFilesDir = createTempDir(prefix = "pacenotes-files")
        val imported = File(appFilesDir, "local-maps/norcal.mbtiles")
        imported.parentFile!!.mkdirs()
        imported.writeBytes(byteArrayOf())

        try {
            val found = LocalMapPackageLocator.find(appFilesDir)

            assertEquals(imported.absolutePath, found!!.mbtilesFile.absolutePath)
        } finally {
            appFilesDir.deleteRecursively()
        }
    }

    @Test
    fun reportsNoMapBeforeAnImportHasCompleted() {
        val appFilesDir = createTempDir(prefix = "pacenotes-files")
        try {
            assertNull(LocalMapPackageLocator.find(appFilesDir))
        } finally {
            appFilesDir.deleteRecursively()
        }
    }
}
