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
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "KmpConsumerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/KmpConsumerPlugin"),
        .testTarget(
            name: "KmpConsumerPluginTests",
            dependencies: ["KmpConsumerPlugin"],
            path: "ios/Tests/KmpConsumerPluginTests")
    ]
)
