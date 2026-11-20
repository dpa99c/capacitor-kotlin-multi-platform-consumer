package io.github.capacitorkmp.consumer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.device.RawDeviceInfo
import io.github.capacitorkmp.consumer.motion.AccelerationUnit
import io.github.capacitorkmp.consumer.motion.AccelerometerSource
import io.github.capacitorkmp.consumer.motion.AxisConvention
import io.github.capacitorkmp.consumer.motion.RawAcceleration
import io.github.capacitorkmp.core.KmpAndroid
import io.github.capacitorkmp.core.KmpErrorCode
import io.github.capacitorkmp.core.KmpException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

// The Android application Context is provided by the base plugin (KmpCapacitorPlugin calls
// KmpAndroid.init(context) in load()).

actual fun platformDeviceInfoSource(): DeviceInfoSource = DeviceInfoSource {
    val metrics = KmpAndroid.applicationContext.resources.displayMetrics
    RawDeviceInfo(
        platform = "android",
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        osName = "Android",
        osVersion = Build.VERSION.RELEASE,
        locale = Locale.getDefault().toLanguageTag(),
        screenWidth = metrics.widthPixels / metrics.density.toDouble(),
        screenHeight = metrics.heightPixels / metrics.density.toDouble(),
        screenScale = metrics.density.toDouble(),
        isVirtual = isEmulator(),
    )
}

private fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.contains("emulator") ||
        Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish") ||
        Build.PRODUCT.contains("sdk") || Build.MODEL.contains("Emulator")

actual fun platformAccelerometer(): AccelerometerSource = AndroidAccelerometer()

private class AndroidAccelerometer : AccelerometerSource {
    private val sensorManager: SensorManager
        get() = KmpAndroid.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val isAvailable: Boolean
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    // Accelerometer access needs no runtime permission on Android.
    override suspend fun requestPermission(): Boolean = true

    override fun samples(intervalMs: Int): Flow<RawAcceleration> = callbackFlow {
        val manager = sensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw KmpException(KmpErrorCode.UNAVAILABLE, "This device has no accelerometer")
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    RawAcceleration(
                        x = event.values[0].toDouble(),
                        y = event.values[1].toDouble(),
                        z = event.values[2].toDouble(),
                        timestampMs = event.timestamp / 1_000_000.0,
                        unit = AccelerationUnit.METERS_PER_SECOND_SQUARED,
                        convention = AxisConvention.ANDROID_W3C,
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, intervalMs * 1000)
        awaitClose { manager.unregisterListener(listener) }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
