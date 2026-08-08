package com.rich.rallypacenotes.map

import java.io.File

class LocalMapPackage private constructor(
    val mbtilesFile: File,
) {
    val mapLibreUri: String
        get() = "mbtiles://${mbtilesFile.absolutePath}"

    companion object {
        fun from(mbtilesFile: File): LocalMapPackage {
            require(mbtilesFile.isFile) { "Imported map package must be an existing file" }
            return LocalMapPackage(mbtilesFile)
        }
    }
}
