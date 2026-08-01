package com.rich.rallypacenotes.model

@JvmInline
value class RouteRevision(val value: String) {
    init {
        require(value.isNotBlank()) { "Route revision must not be blank" }
    }
}
