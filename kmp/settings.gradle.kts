// KMP build of capacitor-kotlin-multi-platform-consumer.
//
// Everything that makes this a "Capacitor KMP" build comes from the base plugin package
// (capacitor-kotlin-multi-platform), resolved from node_modules:
//   - kmp/build-logic   → the `io.github.capacitorkmp.plugin` convention plugin
//   - kmp/gradle/libs.versions.toml → shared Kotlin / kotlinx / AGP versions
//   - kmp-repo          → the prebuilt `core` library (all targets) as a local Maven repository

pluginManagement {
    includeBuild("../node_modules/capacitor-kotlin-multi-platform/kmp/build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "capacitorKotlinMultiPlatform"
            url = uri("../node_modules/capacitor-kotlin-multi-platform/kmp-repo")
            content { includeGroup("io.github.capacitorkmp") }
        }
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../node_modules/capacitor-kotlin-multi-platform/kmp/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "capacitor-kotlin-multi-platform-consumer-kmp"

include(":shared")
