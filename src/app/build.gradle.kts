/*
 * Kotlin
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

plugins {
    id("com.microej.gradle.application")
    id("microej-app-conventions")
    id("com.microej.plugin.application-feature-deploy")
}

group = "com.microej.example"
version = "1.0.0"

microej {
    applicationEntryPoint = "com.microej.example.app.Main"
    architectureUsage = System.getProperty("com.microej.architecture.usage") ?: "eval" // or "prod"
    produceVirtualDeviceDuringBuild()
}

dependencies {
    implementation(libs.ej.edc)
    implementation(libs.ej.bon)
    
    // VEE dependency (microejVee) is managed by the microej-app-conventions plugin.
    // See src/buildSrc/src/main/kotlin/microej-app-conventions.gradle.kts.
}

appConnect {
    setHost(providers.gradleProperty("appconnect.host"))
    setPort(providers.gradleProperty("appconnect.port"))
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            microej.useMicroejTestEngine(this)

            dependencies {
                implementation(project())
                implementation(libs.ej.edc)
                implementation(libs.ej.bon)
                implementation(libs.ej.junit)
            }
        }
    }
}
