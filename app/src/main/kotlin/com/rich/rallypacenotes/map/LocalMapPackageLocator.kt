package com.rich.rallypacenotes.map

import java.io.File

object LocalMapPackageLocator {
    private const val DIRECTORY_NAME = "local-maps"
    private const val FILE_NAME = "norcal.mbtiles"

    fun find(appFilesDir: File): LocalMapPackage? {
        val importedMap = File(appFilesDir, "$DIRECTORY_NAME/$FILE_NAME")
        return if (importedMap.isFile) LocalMapPackage.from(importedMap) else null
    }
}
