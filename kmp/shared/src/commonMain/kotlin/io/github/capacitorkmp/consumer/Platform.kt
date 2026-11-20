package io.github.capacitorkmp.consumer

import io.github.capacitorkmp.consumer.device.DeviceInfoSource
import io.github.capacitorkmp.consumer.motion.AccelerometerSource

// The platform-specific half of the demo. Each declaration has an `actual` implementation in
// androidMain (Android SDK), iosMain (UIKit / CoreMotion via Kotlin/Native interop) and jsMain
// (browser APIs via `dynamic`). Everything else in this module is shared.

/** Device data from Android `Build`/`DisplayMetrics`, iOS `UIDevice`/`UIScreen`, or the browser `navigator`. */
expect fun platformDeviceInfoSource(): DeviceInfoSource

/** Accelerometer from Android `SensorManager`, iOS `CMMotionManager`, or the W3C `devicemotion` event. */
expect fun platformAccelerometer(): AccelerometerSource
