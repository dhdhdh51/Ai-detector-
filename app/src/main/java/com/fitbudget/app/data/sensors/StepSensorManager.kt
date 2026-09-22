package com.fitbudget.app.data.sensors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.fitbudget.app.data.repository.StepRepository
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reads the hardware step counter when one is available.
 *
 * `TYPE_STEP_COUNTER` is maintained by the device since boot, so the app does not need a
 * foreground service: every time the user opens FitBudget we read the counter and credit the
 * difference since the last reading. Devices without the sensor fall back to manual entry, which
 * the UI offers explicitly.
 */
class StepSensorManager(
    private val context: Context,
    private val stepRepository: StepRepository,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(SensorManager::class.java)

    private val stepCounter: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var listening = false

    val isSensorAvailable: Boolean get() = stepCounter != null

    val hasPermission: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /** True when the app can actually count steps automatically right now. */
    val isUsable: Boolean get() = isSensorAvailable && hasPermission

    fun start() {
        if (listening || !isUsable) return
        val manager = sensorManager ?: return
        val sensor = stepCounter ?: return
        listening = try {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (error: SecurityException) {
            Log.w(TAG, "Step counter registration denied", error)
            false
        }
    }

    fun stop() {
        if (!listening) return
        runCatching { sensorManager?.unregisterListener(this) }
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val value = event?.values?.firstOrNull() ?: return
        if (value <= 0f || !value.isFinite()) return
        val counter = value.toLong()
        scope.launch {
            runCatching {
                stepRepository.recordSensorReading(counter, DateTimeUtils.todayEpochDay())
            }.onFailure { Log.e(TAG, "Unable to store step reading", it) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "StepSensorManager"
    }
}
