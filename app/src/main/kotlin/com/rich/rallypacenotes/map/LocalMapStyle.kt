package com.rich.rallypacenotes.map

object LocalMapStyle {
    fun forPackage(localMapPackage: LocalMapPackage): String {
        val mbtilesUri = localMapPackage.mapLibreUri
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return """
            {
              "version": 8,
              "sources": {
                "norcal": { "type": "vector", "url": "$mbtilesUri" }
              },
              "layers": [
                { "id": "background", "type": "background", "paint": { "background-color": "#f2efe8" } },
                { "id": "landcover", "type": "fill", "source": "norcal", "source-layer": "landcover", "paint": { "fill-color": "#dbe7c9" } },
                { "id": "water", "type": "fill", "source": "norcal", "source-layer": "water", "paint": { "fill-color": "#9dc9e8" } },
                { "id": "roads", "type": "line", "source": "norcal", "source-layer": "transportation", "paint": { "line-color": "#3e4a56", "line-width": 1.5 } }
              ]
            }
        """.trimIndent()
    }
}
