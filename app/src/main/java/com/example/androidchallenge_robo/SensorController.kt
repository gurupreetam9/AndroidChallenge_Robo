package com.example.androidchallenge_robo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sqrt

data class RoboSensorState(
    val pitch: Float = 0f, // Up/Down tilt (radians)
    val roll: Float = 0f,  // Left/Right tilt (radians)
    val isFaceDown: Boolean = false,
    val isProximityNear: Boolean = false,
    val isShaking: Boolean = false,
    val rotationRateZ: Float = 0f // From Gyroscope
)

class SensorController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _state = MutableStateFlow(RoboSensorState())
    val state: StateFlow<RoboSensorState> = _state.asStateFlow()

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null // For accurate orientation if needed, but Gravity/RotationVector is better
    private var proximitySensor: Sensor? = null
    private var gyroscope: Sensor? = null

    // For Shake Detection
    private var lastAcceleration = 0f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var shakeThreshold = 12f // Sensitivity

    // For Orientation
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var hasAccelerometer = false
    private var hasMagnetometer = false

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    fun startListening() {
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                hasAccelerometer = true
                detectShake(event.values)
                updateOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                hasMagnetometer = true
                updateOrientation()
            }
            Sensor.TYPE_PROXIMITY -> {
                // event.values[0] is distance in cm
                // event.sensor.maximumRange is the max check
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                // Some sensors return binary (0 or 5/max).
                // Usually near is < 5cm or < maxRange
                val isNear = distance < 5f && distance < maxRange
                _state.update { it.copy(isProximityNear = isNear) }
            }
            Sensor.TYPE_GYROSCOPE -> {
                // event.values[2] is rotation around Z axis (rad/s)
                val rateZ = event.values[2]
                // Apply simple smoothing or just pass raw? Raw is fine for effects.
                _state.update { it.copy(rotationRateZ = rateZ) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun detectShake(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val accel = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta = accel - currentAcceleration
        currentAcceleration = currentAcceleration * 0.9f + accel
        
        // Increased threshold to prevent false positives during normal interaction
        val shakeThreshold = 14f 
        
        // Calculate g-force.
        val gForce = accel / SensorManager.GRAVITY_EARTH
        if (gForce > 2.8f) { // Shake threshold raised from 2.5
            _state.update { it.copy(isShaking = true) }
        } else {
             _state.update { it.copy(isShaking = false) }
        }
    }

    private fun updateOrientation() {
        if (hasAccelerometer && hasMagnetometer) {
             // Compute orientation
             val success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
             if (success) {
                 SensorManager.getOrientation(rotationMatrix, orientationAngles)
                 val pitch = orientationAngles[1]
                 val roll = orientationAngles[2]
                 
                 // Apply Stronger Smoothness (Low Pass Filter)
                 // Previous: 0.6/0.4 was too jittery.
                 // New: 0.9/0.1 for very smooth but slightly lazy movement.
                 _state.update { current ->
                     val smoothPitch = current.pitch * 0.9f + pitch * 0.1f
                     val smoothRoll = current.roll * 0.9f + roll * 0.1f
                     current.copy(pitch = smoothPitch, roll = smoothRoll)
                 }
             }
        } else if (hasAccelerometer) {
            // Fallback estimation
            val x = accelerometerReading[0]
            val y = accelerometerReading[1]
            val z = accelerometerReading[2]
            
            val g = sqrt((x*x + y*y + z*z).toDouble()).toFloat()
            if (g > 0) {
                 val rawRoll = -(x/g) * (Math.PI/2).toFloat()
                 val rawPitch = (y/g) * (Math.PI/2).toFloat()
                 
                 // Fallback also needs to be very smooth because raw accel is noisy
                 _state.update { current ->
                     val smoothPitch = current.pitch * 0.92f + rawPitch * 0.08f
                     val smoothRoll = current.roll * 0.92f + rawRoll * 0.08f
                     current.copy(pitch = smoothPitch, roll = smoothRoll)
                 }
            }
        }
    }
}
