package com.rich.rallypacenotes.map

import android.content.Context
import java.io.File

object BundledMapPackageProvisioner {
    private const val ASSET_NAME = "norcal.mbtiles"

    fun provisionInto(context: Context): LocalMapPackage? {
        val destination = File(context.filesDir, "local-maps/norcal.mbtiles")
        if (!destination.isFile) {
            val imported = runCatching {
                context.assets.open(ASSET_NAME).use { source ->
                    LocalMapPackageImporter.importInto(context.filesDir, source)
                }
            }.getOrNull()
            if (imported == null) return null
        }
        return runCatching { LocalMapPackage.from(destination) }.getOrNull()
    }
}
