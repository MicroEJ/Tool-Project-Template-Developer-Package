/*
 * Kotlin
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

/*
 * Convention plugin for MicroEJ application subprojects.
 *
 * Factorizes the VEE dependency resolution that every app under src/ would
 * otherwise duplicate:
 *   - Resolves the VEE dependency depending on Application Developer Mode
 *     (system property `app.developer.mode.enabled`, default true) and the
 *     target VEE Port (system property `kernel.name`).
 *
 * This plugin is recommended for handling Application Developer Mode and
 * multi-kernel support (where the target VEE Port may vary). For simple
 * single-kernel setups without Application Developer Mode it is optional —
 * the VEE dependency can be declared directly in the app build.gradle.kts
 * if preferred.
 *
 * Usage in any app build.gradle.kts:
 *
 *   plugins {
 *       alias(libs.plugins.microej.application)
 *       id("microej-app-conventions")
 *   }
 */

val isAppDeveloperMode: Boolean = providers.systemProperty("app.developer.mode.enabled")
    .map(String::toBoolean)
    .getOrElse(true)
val targetKernel: String = providers.systemProperty("kernel.name").orNull
    ?: throw GradleException(
        "Cannot resolve Kernel for project '${project.name}'.\n\n" +
                "The system property `kernel.name` has not been set.\n" +
                "Set it in gradle.properties (e.g. systemProp.kernel.name=<kernel>) " +
                "or pass it on the command line (e.g. -Dkernel.name=<kernel>)."
    )

dependencies {
    // VEE dependency resolution (app developer mode vs building from sources)
    if (isAppDeveloperMode) {
        val binDir = rootProject.projectDir.parentFile.resolve("bin")
        val executableFile = File(binDir, "executable/$targetKernel/application.out")
        val virtualDeviceDir = File(binDir, "virtualDevice/$targetKernel/vee")
        if (executableFile.exists() && virtualDeviceDir.exists()) {
            add("microejVee", files(executableFile, virtualDeviceDir))
        } else {
            throw GradleException(
                "Prebuilt Kernel files were not found.\n\n" +
                        "Application Developer Mode is enabled (system property `app.developer.mode.enabled=true`).\n" +
                        "In this mode, the build expects the prebuilt Kernel Executable and Virtual Device " +
                        "to be available in the following directory:\n" +
                        "${binDir.canonicalPath}.\n" +
                        "Please verify that the package file structure is correct, or disable Application " +
                        "Developer Mode to build the Kernel from sources instead."
            )
        }
    } else {
        add("microejVee", project(":$targetKernel"))
    }
}
