/*
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("developer-packaging") {
            id = "com.microej.gradle.plugin.developer-packaging"
            implementationClass = "DeveloperPackagingPlugin"
        }
    }
}