package com.rich.rallypacenotes.map

import kotlin.math.abs

/**
 * Truthful bearing selection for a foreground map. Course is preferred only while
 * the receiver is moving and Android reports a recent, sufficiently accurate bearing.
 */
object DirectionPolicy {
    const val MIN_TRAVEL_SPEED_METRES_PER_SECOND = 1.5
    const val MAX_LOCATION_AGE_MILLIS = 5_000L
    const val MAX_BEARING_ACCURACY_DEGREES = 35.0
    const val MAX_DEVICE_HEADING_AGE_MILLIS = 2_000L
    const val MAX_RETAINED_COURSE_AGE_MILLIS = 3_000L

    fun select(input: DirectionInput): DirectionDecision {
        val course = input.courseBearingDegrees
        val courseIsReliable = course.isBearing() &&
            input.speedMetresPerSecond >= MIN_TRAVEL_SPEED_METRES_PER_SECOND &&
            input.locationAgeMillis <= MAX_LOCATION_AGE_MILLIS &&
            (input.courseBearingAccuracyDegrees == null ||
                input.courseBearingAccuracyDegrees <= MAX_BEARING_ACCURACY_DEGREES)
        if (courseIsReliable) {
            return DirectionDecision(course!!.normalize(), DirectionSource.COURSE)
        }

        val deviceHeading = input.deviceHeadingDegrees
        val headingIsReliable = deviceHeading.isBearing() &&
            input.deviceHeadingAgeMillis != null &&
            input.deviceHeadingAgeMillis <= MAX_DEVICE_HEADING_AGE_MILLIS &&
            input.deviceHeadingAccuracy.isReliable
        if (headingIsReliable) {
            return DirectionDecision(deviceHeading!!.normalize(), DirectionSource.DEVICE_HEADING)
        }

        val retainedCourse = input.lastReliableCourseDegrees
        if (retainedCourse.isBearing() &&
            input.lastReliableCourseAgeMillis != null &&
            input.lastReliableCourseAgeMillis <= MAX_RETAINED_COURSE_AGE_MILLIS
        ) {
            return DirectionDecision(retainedCourse!!.normalize(), DirectionSource.RETAINED_COURSE)
        }

        return DirectionDecision(0.0, DirectionSource.NORTH_UP)
    }
}

data class DirectionInput(
    val courseBearingDegrees: Double?,
    val courseBearingAccuracyDegrees: Double?,
    val speedMetresPerSecond: Double,
    val locationAgeMillis: Long,
    val deviceHeadingDegrees: Double?,
    val deviceHeadingAgeMillis: Long?,
    val deviceHeadingAccuracy: DeviceHeadingAccuracy,
    val lastReliableCourseDegrees: Double? = null,
    val lastReliableCourseAgeMillis: Long? = null,
)

data class DirectionDecision(
    val bearingDegrees: Double,
    val source: DirectionSource,
)

enum class DirectionSource {
    COURSE,
    DEVICE_HEADING,
    RETAINED_COURSE,
    NORTH_UP,
}

enum class DeviceHeadingAccuracy(val isReliable: Boolean) {
    UNRELIABLE(false),
    LOW(false),
    MEDIUM(true),
    HIGH(true),
}

/**
 * Bounded exponential circular-angle smoother. It rejects sub-three-degree jitter
 * and accepts at most one update every 250 ms, preventing a sensor callback loop
 * from continuously spinning the map.
 */
class CircularHeadingSmoother(
    private val minimumUpdateIntervalMillis: Long = 250L,
    private val deadbandDegrees: Double = 3.0,
    private val alpha: Double = 0.35,
) {
    private var previousHeading: Double? = null
    private var previousUpdateMillis: Long? = null

    fun update(candidateDegrees: Double, nowMillis: Long): Double {
        val candidate = candidateDegrees.normalize()
        val previous = previousHeading
        val previousTime = previousUpdateMillis
        if (previous == null || previousTime == null) {
            return candidate.also {
                previousHeading = it
                previousUpdateMillis = nowMillis
            }
        }
        if (nowMillis - previousTime < minimumUpdateIntervalMillis) return previous

        val delta = signedAngleDelta(previous, candidate)
        val next = if (abs(delta) < deadbandDegrees) previous else (previous + alpha * delta).normalize()
        previousHeading = next
        previousUpdateMillis = nowMillis
        return next
    }
}

internal fun signedAngleDelta(fromDegrees: Double, toDegrees: Double): Double =
    ((toDegrees - fromDegrees + 540.0) % 360.0) - 180.0

private fun Double?.isBearing(): Boolean = this != null && isFinite() && this >= 0.0 && this < 360.0

internal fun Double.normalize(): Double = ((this % 360.0) + 360.0) % 360.0
