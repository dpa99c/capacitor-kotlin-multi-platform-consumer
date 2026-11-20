plugins {
    // From capacitor-kotlin-multi-platform: KMP targets (Android, iOS, JS), `api(core)`, and the
    // `capacitorExport` task that publishes the AAR, XCFramework and JS module into this plugin.
    id("io.github.capacitorkmp.plugin")
}

// Maven coordinates: io.github.capacitorkmp.consumer:shared:<package.json version>
group = "io.github.capacitorkmp.consumer"

capacitorKmp {
    android(namespace = "io.github.capacitorkmp.consumer.shared")
    iosFramework("KmpConsumerShared")
    jsModule("kmp-consumer-shared")
}
