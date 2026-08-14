package com.rich.rallypacenotes.map

enum class MapViewMode(
    val nextActionContentDescription: String,
) {
    NAVIGATION("Switch to north-up map"),
    NORTH_UP("Switch to navigation view"),

    ;

    fun toggled(): MapViewMode =
        if (this == NAVIGATION) NORTH_UP else NAVIGATION
}
