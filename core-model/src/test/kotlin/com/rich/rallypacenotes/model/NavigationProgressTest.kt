package com.rich.rallypacenotes.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RouteRevisionTest {
    @Test
    fun rejectsBlankRevisionId() {
        assertFailsWith<IllegalArgumentException> { RouteRevision(" \t") }
    }
}

class NavigationProgressTest {
    private val revision = RouteRevision("route-v1")
    private val position = MatchedRoutePosition(12.5, 0.8)

    @Test
    fun matchedStatusRequiresPosition() {
        assertFailsWith<IllegalArgumentException> {
            NavigationProgress(revision, NavigationStatus.MATCHED)
        }

        assertEquals(
            position,
            NavigationProgress(revision, NavigationStatus.MATCHED, position).matchedPosition,
        )
    }

    @Test
    fun nonMatchedStatusesRejectPosition() {
        val statuses = listOf(
            NavigationStatus.ACQUIRING,
            NavigationStatus.UNCERTAIN,
            NavigationStatus.OFF_ROUTE,
            NavigationStatus.WRONG_WAY,
            NavigationStatus.AMBIGUOUS,
            NavigationStatus.COMPLETED,
        )

        statuses.forEach { status ->
            assertFailsWith<IllegalArgumentException> {
                NavigationProgress(revision, status, position)
            }
        }
    }
}

class PacenoteTest {
    private fun validPacenote(
        routeId: String = "stage-1",
        revision: RouteRevision = RouteRevision("revision-1"),
        routeDistanceMeters: Double = 250.0,
        direction: PacenoteDirection = PacenoteDirection.LEFT,
        severity: Int = 4,
        confidence: Double = 0.8,
        classifierVersion: String = "classifier-1",
    ) = Pacenote.create(
        routeId = routeId,
        routeRevision = revision,
        routeDistanceMeters = routeDistanceMeters,
        direction = direction,
        severity = severity,
        confidence = confidence,
        classifierVersion = classifierVersion,
    )

    @Test
    fun sameInputsProduceDocumentedSha256Id() {
        val pacenote = validPacenote()

        assertEquals(
            "f326ce50f22e84742f7dfaed22e32473f7930f78f71ffe97099bea23ba9b2761",
            pacenote.id,
        )
        assertEquals(64, pacenote.id.length)
        assert(pacenote.id.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(pacenote.id, validPacenote().id)
    }

    @Test
    fun confidenceIsNonIdentityMetadata() {
        assertEquals(validPacenote(confidence = 0.0).id, validPacenote(confidence = 1.0).id)
    }

    @Test
    fun identityInputsChangeStableId() {
        val original = validPacenote()

        assertNotEquals(original.id, validPacenote(routeId = "stage-2").id)
        assertNotEquals(original.id, validPacenote(revision = RouteRevision("revision-2")).id)
        assertNotEquals(original.id, validPacenote(routeDistanceMeters = 251.0).id)
        assertNotEquals(original.id, validPacenote(direction = PacenoteDirection.RIGHT).id)
        assertNotEquals(original.id, validPacenote(severity = 5).id)
        assertNotEquals(original.id, validPacenote(classifierVersion = "classifier-2").id)
    }

    @Test
    fun acceptsPacenoteValueBoundaries() {
        assertEquals(0.0, validPacenote(routeDistanceMeters = 0.0, severity = 1, confidence = 0.0).routeDistanceMeters)
        assertEquals(6, validPacenote(severity = 6, confidence = 1.0).severity)
    }

    @Test
    fun rejectsInvalidEventInputs() {
        assertFailsWith<IllegalArgumentException> { validPacenote(routeId = " ") }
        assertFailsWith<IllegalArgumentException> { validPacenote(classifierVersion = " ") }
        assertFailsWith<IllegalArgumentException> { validPacenote(routeDistanceMeters = Double.NaN) }
        assertFailsWith<IllegalArgumentException> { validPacenote(routeDistanceMeters = Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { validPacenote(routeDistanceMeters = Double.NEGATIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { validPacenote(routeDistanceMeters = -0.1) }
        assertFailsWith<IllegalArgumentException> { validPacenote(severity = 0) }
        assertFailsWith<IllegalArgumentException> { validPacenote(severity = 7) }
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.01, 1.01).forEach { confidence ->
            assertFailsWith<IllegalArgumentException> { validPacenote(confidence = confidence) }
        }
    }
}

class MatchedRoutePositionTest {
    @Test
    fun acceptsFiniteNonNegativeDistanceAndBoundedConfidence() {
        val position = MatchedRoutePosition(
            routeDistanceMeters = 12.5,
            matchConfidence = 1.0,
        )

        assertEquals(12.5, position.routeDistanceMeters)
        assertEquals(1.0, position.matchConfidence)
    }

    @Test
    fun rejectsInvalidDistanceAndConfidence() {
        assertFailsWith<IllegalArgumentException> {
            MatchedRoutePosition(Double.NaN, 0.5)
        }
        assertFailsWith<IllegalArgumentException> {
            MatchedRoutePosition(Double.POSITIVE_INFINITY, 0.5)
        }
        assertFailsWith<IllegalArgumentException> {
            MatchedRoutePosition(-0.1, 0.5)
        }
        assertFailsWith<IllegalArgumentException> {
            MatchedRoutePosition(1.0, -0.01)
        }
        assertFailsWith<IllegalArgumentException> {
            MatchedRoutePosition(1.0, 1.01)
        }
    }
}
