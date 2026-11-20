package io.github.capacitorkmp.consumer

import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.device.RawDeviceInfo
import io.github.capacitorkmp.consumer.motion.AccelerometerSource
import io.github.capacitorkmp.consumer.motion.RawAcceleration
import io.github.capacitorkmp.core.KmpBridge
import io.github.capacitorkmp.core.KmpErrorCode
import io.github.capacitorkmp.core.KmpException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumerModuleTest {
    private val fakeDevice = DeviceInfoSource {
        RawDeviceInfo(platform = "ios", manufacturer = "Apple", model = "iPhone", osName = "iOS", osVersion = "18.2", locale = "en_US", screenWidth = 393.0, screenHeight = 852.0, screenScale = 3.0)
    }
    private val noAccelerometer = object : AccelerometerSource {
        override val isAvailable = false
        override suspend fun requestPermission() = false
        override fun samples(intervalMs: Int): Flow<RawAcceleration> = emptyFlow()
    }

    private fun String.json() = Json.parseToJsonElement(this).jsonObject

    @Test
    fun deviceInfoIsNormalisedSharedLogic() = runTest {
        val bridge = KmpBridge(ConsumerModule(fakeDevice, noAccelerometer), StandardTestDispatcher(testScheduler))
        val info = bridge.execute("getDeviceInfo", null).json()
        assertEquals("en-US", info["locale"]!!.jsonPrimitive.content)
        assertEquals("phone", info["formFactor"]!!.jsonPrimitive.content)
        assertEquals(1179, info["screen"]!!.jsonObject["physicalWidth"]!!.jsonPrimitive.int)
        bridge.close()
    }

    @Test
    fun deviceSourceUnavailableIsReported() = runTest {
        val bridge = KmpBridge(ConsumerModule(fakeDevice, noAccelerometer), StandardTestDispatcher(testScheduler))
        assertEquals(false, bridge.execute("isMotionAvailable", null).json()["device"]!!.jsonPrimitive.boolean)
        val error = assertFailsWith<KmpException> { bridge.execute("startMotionUpdates", """{"source":"device"}""") }
        assertEquals(KmpErrorCode.UNAVAILABLE, error.code)
        bridge.close()
    }

    @Test
    fun simulatedMotionStreamsEvents() = runTest {
        val bridge = KmpBridge(ConsumerModule(fakeDevice, noAccelerometer), StandardTestDispatcher(testScheduler))
        val events = mutableListOf<String>()
        bridge.setEventListener { event, _ -> events += event }

        val session = bridge.execute("startMotionUpdates", """{"source":"simulated","intervalMs":20}""").json()
        assertEquals("simulated", session["source"]!!.jsonPrimitive.content)
        advanceTimeBy(12_000)
        runCurrent()
        val summary = bridge.execute("stopMotionUpdates", null).json()

        assertTrue(events.count { it == "motion" } in 550..610, "motion=${events.count { it == "motion" }}")
        assertEquals(1, events.count { it == "shake" })
        assertTrue(events.count { it == "step" } > 15)
        assertEquals(events.count { it == "step" }, summary["steps"]!!.jsonPrimitive.int)

        // Stopped: no more events.
        val before = events.size
        advanceTimeBy(1_000)
        assertEquals(before, events.size)
        bridge.close()
    }
}
