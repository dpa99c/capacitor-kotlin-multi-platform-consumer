# capacitor-kotlin-multi-platform-consumer

An example Capacitor plugin built on
[`capacitor-kotlin-multi-platform`](https://github.com/dpa99c/capacitor-kotlin-multi-platform). It shows how a real plugin
shares its logic across **Android, iOS and the Web** with Kotlin Multiplatform, and how it talks to
**equivalent platform APIs** through Kotlin `expect`/`actual`.

| Feature | Platform-specific (`expect`/`actual`) | Shared Kotlin (`commonMain`) |
|---|---|---|
| `getDeviceInfo()` | Android `Build` / `DisplayMetrics` / `Locale`, iOS `UIDevice` / `UIScreen` / `NSLocale` / `uname`, browser `navigator` / `screen` | OS version parsing & comparison, user-agent parsing, locale normalisation (BCP-47), form-factor classification, minimum-OS policy, summary text |
| Motion (`startMotionUpdates`, `motion` / `step` / `shake` events) | Android `SensorManager`, iOS `CMMotionManager`, W3C `devicemotion` – each as a `callbackFlow` | Unit and axis normalisation (CoreMotion g → m/s²), gravity low-pass filter, pitch/roll, shake detection, step counting + cadence, event throttling, a pure-Kotlin simulated accelerometer |

The platform-specific code is about 200 lines of Kotlin in total, and the JavaScript-facing glue is one
line per method. Everything else is written and unit tested once (19 tests, run on JVM, iOS simulator and
Node).

## Layout

```
kmp/                                  Gradle build using the base plugin's build-logic, core and versions
  settings.gradle.kts                 ← wires in node_modules/capacitor-kotlin-multi-platform
  shared/src/commonMain/…/consumer/
    ConsumerModule.kt                 the plugin: methods + events (KmpModule)
    Platform.kt                       expect fun platformDeviceInfoSource() / platformAccelerometer()
    device/                           DeviceInfoService, OsVersion, UserAgentParser
    motion/                           MotionNormalizer, MotionProcessor, SimulatedAccelerometer
  shared/src/androidMain/…            actuals: Build, SensorManager
  shared/src/iosMain/…                actuals: UIDevice, UIScreen, CMMotionManager
  shared/src/jsMain/…                 actuals: navigator, devicemotion + @JsExport createKmpConsumerBridge()
  shared/src/commonTest/…             tests for all shared logic and the module (virtual time)
android/…/KmpConsumerPlugin.kt        extends KmpCapacitorPlugin – 5 × forward(call)
ios/Sources/KmpConsumerPlugin/        CAPPlugin – 5 × forward(call) via KmpCapacitor
src/web.ts                            extends KmpWebPlugin – 5 × this.invoke(…)
example/                              Vite + Capacitor demo app (Android, iOS, Web)
```

## Install

Install **both** packages so the Capacitor CLI links the native code of each:

```bash
npm install capacitor-kotlin-multi-platform capacitor-kotlin-multi-platform-consumer
npx cap sync
```

## Usage

```ts
import { KmpConsumer } from 'capacitor-kotlin-multi-platform-consumer';

const info = await KmpConsumer.getDeviceInfo();
console.log(info.summary); // "Google Pixel 9 Pro · Android 16.0 · en-GB"

await KmpConsumer.addListener('step', ({ count, cadence }) => console.log(count, cadence));
await KmpConsumer.addListener('shake', ({ intensity }) => console.log('shake!', intensity));
await KmpConsumer.addListener('motion', (s) => console.log(s.x, s.y, s.z, s.pitch, s.roll));

const { device } = await KmpConsumer.isMotionAvailable();
await KmpConsumer.startMotionUpdates({ source: device ? 'device' : 'simulated', intervalMs: 50 });
// …
const summary = await KmpConsumer.stopMotionUpdates(); // { samples, steps, shakes, running }
```

On the Web, call `requestMotionPermission()` from a click handler first (iOS Safari requires it). The
iOS Simulator and desktop browsers have no accelerometer, so use `source: 'simulated'` there. The Android
emulator's virtual sensors work (Extended controls › Virtual sensors).

## Example app

```bash
# once: build the base plugin (sibling checkout) and this plugin
(cd ../capacitor-kotlin-multi-platform && npm install && npm run build)
npm install && npm run build

cd example
npm install
npm run build && npx cap sync
npx cap run android      # or: npx cap run ios  |  npm start (web, http://localhost:5173)
```

The app shows the normalised device info, plus a motion panel with a device/simulated source toggle, a
live x/y/z/|linear| chart, and step, cadence, shake and tilt readouts.

## Build your own

> This plugin demonstrates the **multi-plugin** approach. For the same features as two plugins backed by
> one Kotlin framework (smaller iOS apps), see
> [`capacitor-kotlin-multi-platform-umbrella`](https://github.com/dpa99c/capacitor-kotlin-multi-platform-umbrella).

This repository is the reference for
[docs/EXTENDING.md](https://github.com/dpa99c/capacitor-kotlin-multi-platform/docs/EXTENDING.md) in the base plugin. It covers
scaffolding a plugin, the KMP build, expect/actual patterns, the Android/iOS/Web glue, events, errors,
testing and publishing.

## API

<docgen-index>

* [`getDeviceInfo()`](#getdeviceinfo)
* [`isMotionAvailable()`](#ismotionavailable)
* [`requestMotionPermission()`](#requestmotionpermission)
* [`startMotionUpdates(...)`](#startmotionupdates)
* [`stopMotionUpdates()`](#stopmotionupdates)
* [`addListener('motion', ...)`](#addlistenermotion-)
* [`addListener('step', ...)`](#addlistenerstep-)
* [`addListener('shake', ...)`](#addlistenershake-)
* [`addListener('motionError', ...)`](#addlistenermotionerror-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Example plugin built on capacitor-kotlin-multi-platform.

All logic is shared Kotlin (`kmp/shared/src/commonMain`); only the raw platform access
(device data and the accelerometer) is implemented per platform with Kotlin `expect`/`actual`.

### getDeviceInfo()

```typescript
getDeviceInfo() => Promise<DeviceInfo>
```

Reads device data with platform APIs (Android `Build`, iOS `UIDevice`, browser `navigator`) and
normalises it with shared Kotlin logic (OS version parsing, user-agent parsing, locale
normalisation, form-factor classification, minimum-OS policy).

**Returns:** <code>Promise&lt;<a href="#deviceinfo">DeviceInfo</a>&gt;</code>

**Since:** 0.1.0

--------------------


### isMotionAvailable()

```typescript
isMotionAvailable() => Promise<MotionAvailability>
```

Whether a hardware accelerometer is available. The simulated source is always available.

**Returns:** <code>Promise&lt;<a href="#motionavailability">MotionAvailability</a>&gt;</code>

**Since:** 0.1.0

--------------------


### requestMotionPermission()

```typescript
requestMotionPermission() => Promise<MotionPermission>
```

Requests access to motion data where required (iOS Safari). Call from a user gesture on the web.

**Returns:** <code>Promise&lt;<a href="#motionpermission">MotionPermission</a>&gt;</code>

**Since:** 0.1.0

--------------------


### startMotionUpdates(...)

```typescript
startMotionUpdates(options?: StartMotionOptions | undefined) => Promise<MotionSession>
```

Starts streaming accelerometer data through the shared Kotlin `MotionProcessor`, which emits
`motion`, `step` and `shake` events. Restarts the stream if already running.

Rejects with code `UNAVAILABLE` when `source` is `'device'` and there is no accelerometer.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#startmotionoptions">StartMotionOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#motionsession">MotionSession</a>&gt;</code>

**Since:** 0.1.0

--------------------


### stopMotionUpdates()

```typescript
stopMotionUpdates() => Promise<MotionSummary>
```

Stops the motion stream and returns a summary of the session.

**Returns:** <code>Promise&lt;<a href="#motionsummary">MotionSummary</a>&gt;</code>

**Since:** 0.1.0

--------------------


### addListener('motion', ...)

```typescript
addListener(eventName: 'motion', listenerFunc: (sample: MotionSample) => void) => Promise<PluginListenerHandle>
```

Processed accelerometer samples, at most every `intervalMs`.

| Param              | Type                                                                       |
| ------------------ | -------------------------------------------------------------------------- |
| **`eventName`**    | <code>'motion'</code>                                                      |
| **`listenerFunc`** | <code>(sample: <a href="#motionsample">MotionSample</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 0.1.0

--------------------


### addListener('step', ...)

```typescript
addListener(eventName: 'step', listenerFunc: (event: StepEvent) => void) => Promise<PluginListenerHandle>
```

A step detected by the shared step counter.

| Param              | Type                                                                |
| ------------------ | ------------------------------------------------------------------- |
| **`eventName`**    | <code>'step'</code>                                                 |
| **`listenerFunc`** | <code>(event: <a href="#stepevent">StepEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 0.1.0

--------------------


### addListener('shake', ...)

```typescript
addListener(eventName: 'shake', listenerFunc: (event: ShakeEvent) => void) => Promise<PluginListenerHandle>
```

A shake gesture detected by the shared shake detector.

| Param              | Type                                                                  |
| ------------------ | --------------------------------------------------------------------- |
| **`eventName`**    | <code>'shake'</code>                                                  |
| **`listenerFunc`** | <code>(event: <a href="#shakeevent">ShakeEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 0.1.0

--------------------


### addListener('motionError', ...)

```typescript
addListener(eventName: 'motionError', listenerFunc: (error: MotionError) => void) => Promise<PluginListenerHandle>
```

The motion stream failed after it was started (e.g. the sensor became unavailable).

| Param              | Type                                                                    |
| ------------------ | ----------------------------------------------------------------------- |
| **`eventName`**    | <code>'motionError'</code>                                              |
| **`listenerFunc`** | <code>(error: <a href="#motionerror">MotionError</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 0.1.0

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes all listeners for this plugin.

**Since:** 0.1.0

--------------------


### Interfaces


#### DeviceInfo

| Prop                   | Type                                              | Description                                                                          |
| ---------------------- | ------------------------------------------------- | ------------------------------------------------------------------------------------ |
| **`platform`**         | <code>'android' \| 'ios' \| 'web'</code>          |                                                                                      |
| **`manufacturer`**     | <code>string</code>                               |                                                                                      |
| **`model`**            | <code>string</code>                               |                                                                                      |
| **`operatingSystem`**  | <code>string</code>                               | `Android`, `iOS`, `iPadOS`, `macOS`, `Windows`, …                                    |
| **`osVersion`**        | <code>string</code>                               |                                                                                      |
| **`locale`**           | <code>string</code>                               | BCP-47 locale tag, e.g. `en-GB`.                                                     |
| **`language`**         | <code>string</code>                               |                                                                                      |
| **`region`**           | <code>string</code>                               |                                                                                      |
| **`formFactor`**       | <code><a href="#formfactor">FormFactor</a></code> |                                                                                      |
| **`screen`**           | <code><a href="#screeninfo">ScreenInfo</a></code> |                                                                                      |
| **`isVirtual`**        | <code>boolean</code>                              | Emulator / simulator.                                                                |
| **`browser`**          | <code>string</code>                               | Browser name and major version (web only).                                           |
| **`minimumOsVersion`** | <code>string</code>                               | Minimum OS version required by the (fictional) product policy implemented in Kotlin. |
| **`isOsSupported`**    | <code>boolean</code>                              |                                                                                      |
| **`summary`**          | <code>string</code>                               | Human readable one-liner, e.g. `Google Pixel 9 Pro · Android 16.0 · en-GB`.          |


#### ScreenInfo

| Prop                 | Type                | Description                                |
| -------------------- | ------------------- | ------------------------------------------ |
| **`width`**          | <code>number</code> | Logical width (dp / points / CSS pixels).  |
| **`height`**         | <code>number</code> | Logical height (dp / points / CSS pixels). |
| **`scale`**          | <code>number</code> | Physical pixels per logical pixel.         |
| **`physicalWidth`**  | <code>number</code> |                                            |
| **`physicalHeight`** | <code>number</code> |                                            |


#### MotionAvailability

| Prop            | Type                 | Description                                                                   |
| --------------- | -------------------- | ----------------------------------------------------------------------------- |
| **`device`**    | <code>boolean</code> | A hardware accelerometer (or, on the web, the DeviceMotion API) is available. |
| **`simulated`** | <code>boolean</code> | The simulated source (pure Kotlin) is always available.                       |


#### MotionPermission

| Prop          | Type                 |
| ------------- | -------------------- |
| **`granted`** | <code>boolean</code> |


#### MotionSession

| Prop             | Type                                                  |
| ---------------- | ----------------------------------------------------- |
| **`source`**     | <code><a href="#motionsource">MotionSource</a></code> |
| **`intervalMs`** | <code>number</code>                                   |


#### StartMotionOptions

| Prop                 | Type                                                  | Description                                                                                                                                                         | Default               |
| -------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| **`intervalMs`**     | <code>number</code>                                   | Requested sampling / event interval in milliseconds (16–1000).                                                                                                      | <code>50</code>       |
| **`source`**         | <code><a href="#motionsource">MotionSource</a></code> | `'device'` uses the platform accelerometer; `'simulated'` uses a pure-Kotlin signal generator (walking + periodic shake), handy on simulators and desktop browsers. | <code>'device'</code> |
| **`shakeThreshold`** | <code>number</code>                                   | Linear acceleration (m/s²) a peak must exceed to count towards a shake.                                                                                             | <code>13</code>       |


#### MotionSummary

| Prop          | Type                 | Description                                |
| ------------- | -------------------- | ------------------------------------------ |
| **`running`** | <code>boolean</code> | Whether a stream was running when stopped. |
| **`samples`** | <code>number</code>  |                                            |
| **`steps`**   | <code>number</code>  |                                            |
| **`shakes`**  | <code>number</code>  |                                            |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### MotionSample

| Prop                  | Type                | Description                                                                  |
| --------------------- | ------------------- | ---------------------------------------------------------------------------- |
| **`x`**               | <code>number</code> | Acceleration including gravity in m/s² (Android/W3C axes on every platform). |
| **`y`**               | <code>number</code> |                                                                              |
| **`z`**               | <code>number</code> |                                                                              |
| **`linearMagnitude`** | <code>number</code> | Magnitude of the gravity-free acceleration in m/s².                          |
| **`pitch`**           | <code>number</code> | Tilt derived from the gravity vector, in degrees.                            |
| **`roll`**            | <code>number</code> |                                                                              |
| **`steps`**           | <code>number</code> |                                                                              |
| **`shakes`**          | <code>number</code> |                                                                              |
| **`timestamp`**       | <code>number</code> | Monotonic timestamp in milliseconds.                                         |


#### StepEvent

| Prop            | Type                        | Description                                                              |
| --------------- | --------------------------- | ------------------------------------------------------------------------ |
| **`count`**     | <code>number</code>         |                                                                          |
| **`cadence`**   | <code>number \| null</code> | Steps per minute over the last 10 s (null until two steps are detected). |
| **`timestamp`** | <code>number</code>         |                                                                          |


#### ShakeEvent

| Prop            | Type                | Description                                    |
| --------------- | ------------------- | ---------------------------------------------- |
| **`count`**     | <code>number</code> |                                                |
| **`intensity`** | <code>number</code> | Peak linear acceleration of the shake in m/s². |
| **`timestamp`** | <code>number</code> |                                                |


#### MotionError

| Prop          | Type                |
| ------------- | ------------------- |
| **`code`**    | <code>string</code> |
| **`message`** | <code>string</code> |


### Type Aliases


#### FormFactor

<code>'phone' | 'tablet' | 'desktop'</code>


#### MotionSource

<code>'device' | 'simulated'</code>

</docgen-api>
