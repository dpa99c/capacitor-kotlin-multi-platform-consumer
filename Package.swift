// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorKotlinMultiPlatformConsumer",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorKotlinMultiPlatformConsumer",
            targets: ["KmpConsumerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0"),
        // The base plugin – installed next to this package in node_modules (Swift helper `KmpCapacitor`).
        .package(name: "CapacitorKotlinMultiPlatform", path: "../capacitor-kotlin-multi-platform")
    ],
    targets: [
        // Shared Kotlin code (kmp/shared + the exported base `core`), built by `npm run kmp:build`.
        .binaryTarget(
            name: "KmpConsumerShared",
            path: "ios/Frameworks/KmpConsumerShared.xcframework"),
        .target(
            name: "KmpConsumerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "CapacitorKmpSupport", package: "CapacitorKotlinMultiPlatform"),
                "KmpConsumerShared"
            ],
            path: "ios/Sources/KmpConsumerPlugin"),
        .testTarget(
            name: "KmpConsumerPluginTests",
            dependencies: ["KmpConsumerPlugin", "KmpConsumerShared"],
            path: "ios/Tests/KmpConsumerPluginTests")
    ]
)
