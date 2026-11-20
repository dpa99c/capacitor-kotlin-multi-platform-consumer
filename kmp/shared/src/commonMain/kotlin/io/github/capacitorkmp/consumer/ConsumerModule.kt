package io.github.capacitorkmp.consumer

import io.github.capacitorkmp.consumer.device.DeviceInfoService
import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.motion.AccelerometerSource
import io.github.capacitorkmp.consumer.motion.MotionConfig
import io.github.capacitorkmp.consumer.motion.MotionNormalizer
import io.github.capacitorkmp.consumer.motion.MotionProcessor
import io.github.capacitorkmp.consumer.motion.SimulatedAccelerometer
import io.github.capacitorkmp.core.KmpErrorCode
import io.github.capacitorkmp.core.KmpException
import io.github.capacitorkmp.core.KmpModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared implementation of the `KmpConsumer` Capacitor plugin.
 *
 * Methods (see `src/definitions.ts`): `getDeviceInfo`, `isMotionAvailable`, `requestMotionPermission`,
 * `startMotionUpdates`, `stopMotionUpdates`. Events: `motion`, `step`, `shake`, `motionError`.
 *
 * Platform access is injected through [DeviceInfoSource] and [AccelerometerSource]; the no-argument
 * constructor uses the `expect`/`actual` platform implementations, tests pass fakes.
 */
class ConsumerModule(
    deviceInfoSource: DeviceInfoSource,
    private val accelerometer: AccelerometerSource,
    private val simulatedAccelerometer: AccelerometerSource = SimulatedAccelerometer(),
) : KmpModule() {

    /** Uses the platform implementations. (Also the constructor visible from Swift.) */
    constructor() : this(platformDeviceInfoSource(), platformAccelerometer())

    private val deviceInfo = DeviceInfoService(deviceInfoSource)
    private var motionJob: Job? = null
    private var session: MotionSession? = null
    private var processor: MotionProcessor? = null

    init {
        method("getDeviceInfo") { deviceInfo.getDeviceInfo() }

        method("isMotionAvailable") {
            MotionAvailability(device = accelerometer.isAvailable, simulated = true)
        }

        method("requestMotionPermission") {
            MotionPermission(granted = accelerometer.isAvailable && accelerometer.requestPermission())
        }

        method("startMotionUpdates") {
            val options = decode<StartMotionOptions>()
            val source = when (options.source) {
                MotionSourceType.DEVICE -> accelerometer.also {
                    if (!it.isAvailable) {
                        throw KmpException(KmpErrorCode.UNAVAILABLE, "No accelerometer available – try { source: 'simulated' }")
                    }
                }
                MotionSourceType.SIMULATED -> simulatedAccelerometer
            }
            stopMotion()
            startMotion(source, options)
        }

        method("stopMotionUpdates") {
            val summary = MotionSummary(
                running = motionJob?.isActive == true,
                samples = processor?.sampleCount ?: 0,
                steps = processor?.stepCount ?: 0,
                shakes = processor?.shakeCount ?: 0,
            )
            stopMotion()
            summary
        }
    }

    private fun startMotion(source: AccelerometerSource, options: StartMotionOptions): MotionSession {
        val intervalMs = options.intervalMs.coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
        val config = MotionConfig(shakeThreshold = options.shakeThreshold ?: MotionConfig().shakeThreshold)
        val processor = MotionProcessor(config).also { processor = it }
        val session = MotionSession(source = options.source, intervalMs = intervalMs).also { session = it }

        motionJob = scope.launch {
            var lastEmitted = Double.NEGATIVE_INFINITY
            try {
                source.samples(intervalMs).collect { raw ->
                    val update = processor.process(MotionNormalizer.normalize(raw))
                    update.step?.let { emit("step", it) }
                    update.shake?.let { emit("shake", it) }
                    // Platforms may deliver faster than requested; throttle the high-frequency event.
                    if (update.sample.timestamp - lastEmitted >= intervalMs * 0.9) {
                        lastEmitted = update.sample.timestamp
                        emit("motion", update.sample)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val code = (e as? KmpException)?.code ?: KmpErrorCode.INTERNAL
                emit("motionError", MotionError(code, e.message ?: "Motion updates failed"))
            }
        }
        return session
    }

    private suspend fun stopMotion() {
        motionJob?.cancelAndJoin()
        motionJob = null
        session = null
    }

    override fun onClose() {
        motionJob?.cancel()
    }

    companion object {
        const val MIN_INTERVAL_MS: Int = 16
        const val MAX_INTERVAL_MS: Int = 1000
    }
}

@Serializable
enum class MotionSourceType {
    @SerialName("device") DEVICE,
    @SerialName("simulated") SIMULATED,
}

@Serializable
data class StartMotionOptions(
    val intervalMs: Int = 50,
    val source: MotionSourceType = MotionSourceType.DEVICE,
    val shakeThreshold: Double? = null,
)

@Serializable
data class MotionSession(val source: MotionSourceType, val intervalMs: Int)

@Serializable
data class MotionAvailability(val device: Boolean, val simulated: Boolean)

@Serializable
data class MotionPermission(val granted: Boolean)

@Serializable
data class MotionSummary(val running: Boolean, val samples: Int, val steps: Int, val shakes: Int)

@Serializable
data class MotionError(val code: String, val message: String)
