package com.rich.rallypacenotes.map

import java.io.File
import java.io.InputStream

object LocalMapPackageImporter {
    private const val DIRECTORY_NAME = "local-maps"
    private const val FILE_NAME = "norcal.mbtiles"

    fun importInto(appFilesDir: File, source: InputStream): LocalMapPackage {
        val destinationDirectory = File(appFilesDir, DIRECTORY_NAME)
        require(destinationDirectory.exists() || destinationDirectory.mkdirs()) {
            "Unable to create app-private map package directory"
        }

        val destination = File(destinationDirectory, FILE_NAME)
        source.use { input ->
            destination.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
        return LocalMapPackage.from(destination)
    }
}
