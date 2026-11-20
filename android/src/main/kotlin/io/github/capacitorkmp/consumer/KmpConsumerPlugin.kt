package io.github.capacitorkmp.consumer

import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import io.github.capacitorkmp.core.KmpCapacitorPlugin
import io.github.capacitorkmp.core.KmpModule

/**
 * Android glue of the `KmpConsumer` plugin. All behaviour lives in the shared Kotlin
 * [ConsumerModule] (kmp/shared); [KmpCapacitorPlugin] from capacitor-kotlin-multi-platform hosts it,
 * initialises the Android Context for the `actual` implementations and forwards module events to
 * JavaScript listeners.
 */
@CapacitorPlugin(name = "KmpConsumer")
class KmpConsumerPlugin : KmpCapacitorPlugin() {

    override fun createModule(): KmpModule = ConsumerModule()

    @PluginMethod
    fun getDeviceInfo(call: PluginCall) = forward(call)

    @PluginMethod
    fun isMotionAvailable(call: PluginCall) = forward(call)

    @PluginMethod
    fun requestMotionPermission(call: PluginCall) = forward(call)

    @PluginMethod
    fun startMotionUpdates(call: PluginCall) = forward(call)

    @PluginMethod
    fun stopMotionUpdates(call: PluginCall) = forward(call)
}
