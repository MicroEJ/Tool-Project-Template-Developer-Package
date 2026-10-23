/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

plugins {
    alias(libs.plugins.microej.repository)
}

microej {
    checkersRootDir = rootProject.layout.projectDirectory
}

dependencies {
    microejModule(libs.architecture)
    microejModule(libs.pack.ui.architecture)

    val sdkVersion = libs.versions.microej.sdk.get()
    microejModule("com.microej.gradle:plugins:$sdkVersion")
    microejModule("com.microej.gradle.application:com.microej.gradle.application.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.library:com.microej.gradle.library.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.jse-library:com.microej.gradle.jse-library.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.mock:com.microej.gradle.mock.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.veeport:com.microej.gradle.veeport.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.testsuite:com.microej.gradle.testsuite.gradle.plugin:$sdkVersion")
    microejModule("com.microej.gradle.mock-frontpanel:com.microej.gradle.mock-frontpanel.gradle.plugin:$sdkVersion")
}
