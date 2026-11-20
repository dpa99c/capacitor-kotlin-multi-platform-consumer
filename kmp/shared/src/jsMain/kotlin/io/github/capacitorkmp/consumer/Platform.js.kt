package io.github.capacitorkmp.consumer

import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.device.RawDeviceInfo
import io.github.capacitorkmp.consumer.motion.AccelerationUnit
import io.github.capacitorkmp.consumer.motion.AccelerometerSource
import io.github.capacitorkmp.consumer.motion.AxisConvention
import io.github.capacitorkmp.consumer.motion.RawAcceleration
import io.github.capacitorkmp.core.JsKmpBridge
import io.github.capacitorkmp.core.toJsBridge
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.Promise

/** Entry point used by the plugin's `web.ts` (via `KmpWebPlugin`). */
@JsExport
fun createKmpConsumerBridge(): JsKmpBridge = ConsumerModule().toJsBridge()

private val hasWindow: Boolean get() = js("typeof window !== 'undefined'") as Boolean

actual fun platformDeviceInfoSource(): DeviceInfoSource = DeviceInfoSource {
    if (!hasWindow) return@DeviceInfoSource RawDeviceInfo(platform = "web", userAgent = "", locale = "en")
    val navigator: dynamic = js("window.navigator")
    val screen: dynamic = js("window.screen")
    RawDeviceInfo(
        platform = "web",
        // Browsers don't expose the manufacturer; the model is parsed from the user agent in shared code.
        locale = navigator.language as String?,
        screenWidth = (screen.width as Number).toDouble(),
        screenHeight = (screen.height as Number).toDouble(),
        screenScale = ((js("window.devicePixelRatio") as Number?) ?: 1).toDouble(),
        userAgent = navigator.userAgent as String,
        maxTouchPoints = ((navigator.maxTouchPoints as Number?) ?: 0).toInt(),
    )
}

actual fun platformAccelerometer(): AccelerometerSource = WebAccelerometer()

private class WebAccelerometer : AccelerometerSource {
    // Desktop browsers define DeviceMotionEvent but never fire it – availability means "API present".
    override val isAvailable: Boolean
        get() = hasWindow && js("typeof DeviceMotionEvent !== 'undefined'") as Boolean

    override suspend fun requestPermission(): Boolean {
        if (!isAvailable) return false
        val deviceMotionEvent: dynamic = js("DeviceMotionEvent")
        // iOS Safari 13+: must be called from a user gesture.
        if (jsTypeOf(deviceMotionEvent.requestPermission) != "function") return true
        val state = (deviceMotionEvent.requestPermission() as Promise<String>).await()
        return state == "granted"
    }

    override fun samples(intervalMs: Int): Flow<RawAcceleration> = callbackFlow {
        val window: dynamic = js("window")
        val handler: (dynamic) -> Unit = { event ->
            val a = event.accelerationIncludingGravity
            if (a != null && a.x != null) {
                trySend(
                    RawAcceleration(
                        x = (a.x as Number).toDouble(),
                        y = (a.y as Number).toDouble(),
                        z = (a.z as Number).toDouble(),
                        timestampMs = (event.timeStamp as Number).toDouble(),
                        unit = AccelerationUnit.METERS_PER_SECOND_SQUARED,
                        convention = AxisConvention.ANDROID_W3C,
                    ),
                )
            }
        }
        window.addEventListener("devicemotion", handler)
        awaitClose { window.removeEventListener("devicemotion", handler) }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
