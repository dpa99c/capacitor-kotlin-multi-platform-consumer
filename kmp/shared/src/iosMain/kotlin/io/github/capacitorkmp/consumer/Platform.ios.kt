package io.github.capacitorkmp.consumer

import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.device.RawDeviceInfo
import io.github.capacitorkmp.consumer.motion.AccelerationUnit
import io.github.capacitorkmp.consumer.motion.AccelerometerSource
import io.github.capacitorkmp.consumer.motion.AxisConvention
import io.github.capacitorkmp.consumer.motion.RawAcceleration
import io.github.capacitorkmp.core.KmpErrorCode
import io.github.capacitorkmp.core.KmpException
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSLocale
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSProcessInfo
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.posix.uname
import platform.posix.utsname

actual fun platformDeviceInfoSource(): DeviceInfoSource = DeviceInfoSource {
    // UIKit must be used on the main thread; KmpBridge runs handlers on a background dispatcher.
    withContext(Dispatchers.Main) {
        val device = UIDevice.currentDevice
        val (width, height) = UIScreen.mainScreen.bounds.useContents { size.width to size.height }
        val environment = NSProcessInfo.processInfo.environment
        val simulatorModel = environment["SIMULATOR_MODEL_IDENTIFIER"] as String?
        RawDeviceInfo(
            platform = "ios",
            manufacturer = "Apple",
            // e.g. "iPhone" + machine identifier "iPhone17,1" (the simulator reports the simulated model)
            model = "${device.model} (${simulatorModel ?: machineIdentifier()})",
            osName = device.systemName,
            osVersion = device.systemVersion,
            locale = NSLocale.currentLocale.localeIdentifier,
            screenWidth = width,
            screenHeight = height,
            screenScale = UIScreen.mainScreen.scale,
            isVirtual = simulatorModel != null,
        )
    }
}

private fun machineIdentifier(): String = memScoped {
    val info = alloc<utsname>()
    uname(info.ptr)
    info.machine.toKString()
}


actual fun platformAccelerometer(): AccelerometerSource = IosAccelerometer()

private class IosAccelerometer : AccelerometerSource {
    private val manager = CMMotionManager()

    override val isAvailable: Boolean get() = manager.accelerometerAvailable

    // Raw accelerometer data needs no permission prompt (unlike CMPedometer / motion activity).
    override suspend fun requestPermission(): Boolean = isAvailable

    override fun samples(intervalMs: Int): Flow<RawAcceleration> = callbackFlow {
        if (!manager.accelerometerAvailable) {
            throw KmpException(KmpErrorCode.UNAVAILABLE, "This device has no accelerometer (the iOS Simulator has none)")
        }
        manager.accelerometerUpdateInterval = intervalMs / 1000.0
        manager.startAccelerometerUpdatesToQueue(NSOperationQueue()) { data, _ ->
            if (data != null) {
                data.acceleration.useContents {
                    trySend(
                        RawAcceleration(
                            x = x,
                            y = y,
                            z = z,
                            timestampMs = data.timestamp * 1000.0,
                            unit = AccelerationUnit.STANDARD_GRAVITY,
                            convention = AxisConvention.CORE_MOTION,
                        ),
                    )
                }
            }
        }
        awaitClose { manager.stopAccelerometerUpdates() }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
