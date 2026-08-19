/*
 * Kotlin
 *
 * Copyright 2024-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.guardsquare:proguard-gradle:7.7.0")
    implementation("com.microej.gradle:plugins:${libs.versions.microej.sdk.get()}")
}

gradlePlugin {
    plugins {
        create("feature-deploy-plugin") {
            id = "com.microej.plugin.application-feature-deploy"
            implementationClass = "FeatureDeployPlugin"
        }
        create("obfuscate-plugin") {
            id = "com.microej.plugin.obfuscate"
            implementationClass = "ObfuscatePlugin"
        }
    }
}