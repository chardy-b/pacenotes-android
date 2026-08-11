package com.rich.rallypacenotes.map

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.getSystemService

/**
 * Foreground-only, north-referenced heading source.
 *
 * Uses Android's fused TYPE_ROTATION_VECTOR. The game rotation vector is deliberately
 * not used because it excludes geomagnetism and is not north-referenced. A missing
 * sensor produces no heading rather than an invented compass value.
 */
class RotationVectorHeadingSource(
    context: Context,
    private val onHeading: (DeviceHeadingSample?) -> Unit,
) : SensorEventListener {
    private val sensorManager = requireNotNull(context.getSystemService<SensorManager>())
    @Suppress("DEPRECATION")
    private val displayRotation: Int
        get() = windowManager.defaultDisplay.rotation
    private val windowManager = requireNotNull(context.getSystemService<WindowManager>())
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun start() {
        if (rotationVector == null) {
            onHeading(null)
        } else {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val remappedMatrix = FloatArray(9)
        val rotation = displayRotation
        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(remappedMatrix, orientation)
        val headingDegrees = Math.toDegrees(orientation[0].toDouble()).normalize()
        onHeading(
            DeviceHeadingSample(
                degrees = headingDegrees,
                ageMillis = 0L,
                accuracy = event.accuracy.toDeviceHeadingAccuracy(),
                observedAtElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            ),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}

data class DeviceHeadingSample(
    val degrees: Double,
    val ageMillis: Long,
    val accuracy: DeviceHeadingAccuracy,
    val observedAtElapsedRealtimeMillis: Long,
)

private fun Int.toDeviceHeadingAccuracy(): DeviceHeadingAccuracy = when (this) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> DeviceHeadingAccuracy.HIGH
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> DeviceHeadingAccuracy.MEDIUM
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> DeviceHeadingAccuracy.LOW
    else -> DeviceHeadingAccuracy.UNRELIABLE
}
