import Foundation
import Capacitor
import KmpConsumerShared
#if canImport(CapacitorKmpSupport)
import CapacitorKmpSupport // Swift Package Manager
#else
import CapacitorKotlinMultiPlatform // CocoaPods: the helper is part of the base plugin's pod
#endif

/// iOS glue of the `KmpConsumer` plugin.
///
/// All behaviour lives in the shared Kotlin `ConsumerModule` (kmp/shared), compiled into
/// `KmpConsumerShared.xcframework`. This class only declares the plugin methods, forwards them to the
/// Kotlin `KmpBridge` and relays the module's events (`motion`, `step`, `shake`) to JavaScript.
@objc(KmpConsumerPlugin)
public class KmpConsumerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "KmpConsumerPlugin"
    public let jsName = "KmpConsumer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getDeviceInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isMotionAvailable", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestMotionPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startMotionUpdates", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopMotionUpdates", returnType: CAPPluginReturnPromise)
    ]

    private lazy var kmp = KmpBridge(module: ConsumerModule())

    override public func load() {
        KmpCapacitor.connectEvents(of: self) { listener in kmp.setEventListener(listener: listener) }
    }

    deinit {
        kmp.close()
    }

    @objc func getDeviceInfo(_ call: CAPPluginCall) { forward(call) }
    @objc func isMotionAvailable(_ call: CAPPluginCall) { forward(call) }
    @objc func requestMotionPermission(_ call: CAPPluginCall) { forward(call) }
    @objc func startMotionUpdates(_ call: CAPPluginCall) { forward(call) }
    @objc func stopMotionUpdates(_ call: CAPPluginCall) { forward(call) }

    private func forward(_ call: CAPPluginCall) {
        KmpCapacitor.forward(call) { method, args, onSuccess, onError in
            kmp.call(method: method, argsJson: args, onSuccess: onSuccess, onError: onError)
        }
    }
}
