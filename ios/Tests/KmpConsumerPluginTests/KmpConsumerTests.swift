import XCTest
import KmpConsumerShared
@testable import KmpConsumerPlugin

class KmpConsumerTests: XCTestCase {
    private func call(_ bridge: KmpBridge, _ method: String, _ args: String? = nil) -> [String: Any] {
        let done = expectation(description: method)
        var result: [String: Any] = [:]
        bridge.call(method: method, argsJson: args, onSuccess: { json in
            result = (try? JSONSerialization.jsonObject(with: Data(json.utf8)) as? [String: Any]) ?? [:]
            done.fulfill()
        }, onError: { code, message in
            XCTFail("\(code): \(message)")
            done.fulfill()
        })
        wait(for: [done], timeout: 5)
        return result
    }

    func testDeviceInfoFromKotlinOnIOS() {
        let bridge = KmpBridge(module: ConsumerModule())
        defer { bridge.close() }
        let info = call(bridge, "getDeviceInfo")
        XCTAssertEqual(info["platform"] as? String, "ios")
        XCTAssertEqual(info["manufacturer"] as? String, "Apple")
        XCTAssertNotNil(info["summary"] as? String)
    }

    func testSimulatedMotionEmitsEvents() {
        let bridge = KmpBridge(module: ConsumerModule())
        defer { bridge.close() }
        let motion = expectation(description: "motion event")
        motion.assertForOverFulfill = false
        bridge.setEventListener { event, _ in
            if event == "motion" { motion.fulfill() }
        }
        _ = call(bridge, "startMotionUpdates", "{\"source\":\"simulated\",\"intervalMs\":20}")
        wait(for: [motion], timeout: 5)
        let summary = call(bridge, "stopMotionUpdates")
        XCTAssertGreaterThan(summary["samples"] as? Int ?? 0, 0)
    }
}
