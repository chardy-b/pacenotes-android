package com.rich.rallypacenotes.map

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalMapPackageImporterTest {
    @Test
    fun importsBytesIntoTheCanonicalAppPrivatePackageLocation() {
        val appFilesDir = createTempDir(prefix = "pacenotes-files")
        val packageBytes = byteArrayOf(0x53, 0x51, 0x4c, 0x69, 0x74, 0x65)

        try {
            LocalMapPackageImporter.importInto(appFilesDir, ByteArrayInputStream(packageBytes))

            val importedPackage = assertNotNull(LocalMapPackageLocator.find(appFilesDir))
            assertArrayEquals(packageBytes, importedPackage.mbtilesFile.readBytes())
            assertEquals(
                File(appFilesDir, "local-maps/norcal.mbtiles").absolutePath,
                importedPackage.mbtilesFile.absolutePath,
            )
        } finally {
            appFilesDir.deleteRecursively()
        }
    }
}
