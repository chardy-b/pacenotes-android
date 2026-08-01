package com.rich.rallypacenotes.model

import java.security.MessageDigest

enum class PacenoteDirection {
    LEFT,
    RIGHT,
}

class Pacenote private constructor(
    val id: String,
    val routeId: String,
    val routeRevision: RouteRevision,
    val routeDistanceMeters: Double,
    val direction: PacenoteDirection,
    val severity: Int,
    val confidence: Double,
    val classifierVersion: String,
) {
    companion object {
        fun create(
            routeId: String,
            routeRevision: RouteRevision,
            routeDistanceMeters: Double,
            direction: PacenoteDirection,
            severity: Int,
            confidence: Double,
            classifierVersion: String,
        ): Pacenote {
            require(routeId.isNotBlank()) { "Route ID must not be blank" }
            require(routeDistanceMeters.isFinite() && routeDistanceMeters >= 0.0) {
                "Route distance must be finite and non-negative"
            }
            require(severity in 1..6) { "Severity must be between 1 and 6" }
            require(confidence.isFinite() && confidence in 0.0..1.0) {
                "Confidence must be finite and between 0 and 1"
            }
            require(classifierVersion.isNotBlank()) { "Classifier version must not be blank" }

            val canonicalIdentity = listOf(
                routeId,
                routeRevision.value,
                routeDistanceMeters.toString(),
                direction.name,
                severity.toString(),
                classifierVersion,
            ).joinToString(separator = "") { "${it.length}:$it" }
            val id = MessageDigest.getInstance("SHA-256")
                .digest(canonicalIdentity.encodeToByteArray())
                .joinToString(separator = "") { byte -> "%02x".format(byte) }

            return Pacenote(
                id = id,
                routeId = routeId,
                routeRevision = routeRevision,
                routeDistanceMeters = routeDistanceMeters,
                direction = direction,
                severity = severity,
                confidence = confidence,
                classifierVersion = classifierVersion,
            )
        }
    }
}
