/*
 * Kotlin
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

/*
 * Convention plugin for MicroEJ kernel subprojects (multi-VEE-Port mode).
 *
 * Requires the kernel project to follow the `kernel-<target>` naming
 * convention, where <target> identifies the VEE Port (e.g. `kernel-rt1170`).
 *
 * Factorizes the build setup for kernel projects:
 *   - Isolates the build directory under build/targets/<target>/ to
 *     prevent collisions when the same kernel source directory is shared
 *     across multiple targets.
 *   - Resolves the VEE Port dependency from the kernel project name
 *     (e.g. kernel-rt1170 -> :rt1170).
 *
 * This plugin is intended for multi-kernel support (multiple targets
 * sharing the same kernel source directory). For single-kernel setups,
 * configure the build directory and VEE Port dependency directly in the
 * kernel build.gradle.kts instead.
 *
 * Usage in the kernel build.gradle.kts:
 *
 *   plugins {
 *       alias(libs.plugins.microej.application)
 *       id("microej-kernel-conventions")
 *   }
 */

if (!project.name.startsWith("kernel-")) {
    throw GradleException(
        "The project '${project.name}' does not follow the `kernel-<target>` naming convention.\n\n" +
                "This convention plugin requires the kernel project to be named `kernel-<target>`, " +
                "where <target> identifies the VEE Port (e.g. `kernel-rt1170`).\n" +
                "Rename the project in settings.gradle.kts or remove this plugin if targeting " +
                "a single VEE Port."
    )
}

val target = project.name.removePrefix("kernel-")

// Build directory isolation — each kernel-<target> gets its own build directory.
project.layout.buildDirectory.set(
    project.projectDir.resolve("build/targets/$target")
)

// VEE Port dependency resolution — derived from the project name.
dependencies {
    add("microejVee", project(":$target"))
}
