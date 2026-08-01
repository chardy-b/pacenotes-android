package com.rich.rallypacenotes.model

enum class NavigationStatus {
    ACQUIRING,
    MATCHED,
    UNCERTAIN,
    OFF_ROUTE,
    WRONG_WAY,
    AMBIGUOUS,
    COMPLETED,
}

data class NavigationProgress(
    val routeRevision: RouteRevision,
    val status: NavigationStatus,
    val matchedPosition: MatchedRoutePosition? = null,
) {
    init {
        require((status == NavigationStatus.MATCHED) == (matchedPosition != null)) {
            "Only matched navigation progress may include a matched route position"
        }
    }
}
