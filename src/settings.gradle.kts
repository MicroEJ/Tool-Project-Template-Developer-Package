/*
 * Kotlin
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

// Applies the repository configuration from the Module Repository bundled in the package
val moduleRepositoryScript = file("../repository/module-repository.gradle.kts")
if (moduleRepositoryScript.exists()) {
    apply(moduleRepositoryScript)
}

rootProject.name = "src"

val isAppDeveloperMode: Boolean = providers.systemProperty("app.developer.mode.enabled")
    .map(String::toBoolean)
    .getOrElse(true)

// Application projects are always included.
include(":app")
project(":app").projectDir = file("app")

// Some projects are included only when the Application Developer Mode is disabled (e.g., kernel, vee-port, runtime-api).
// When the Application Developer Mode is enabled, the applications use prebuilt executables and published modules.
if (!isAppDeveloperMode) {
//    include(
//        ":kernel-<target>",
//        ":<target>",
//        ":<target>:front-panel",
//        ":<target>:image-generator",
//        ":<target>:mock",
//        ":<target>:validation:core",
//        ":my-runtime-api",
//    )
//
//    project(":kernel-<target>").projectDir = file("vee/kernel")
//    project(":<target>").projectDir = file("vee/<target>/vee-port")
//    project(":<target>:front-panel").projectDir = file("vee/<target>/vee-port/extensions/front-panel")
//    project(":<target>:mock").projectDir = file("vee/<target>/vee-port/mock")
//    project(":<target>:image-generator").projectDir = file("vee/<target>/vee-port/extensions/image-generator")
//    project(":my-runtime-api").projectDir = file("vee/my-runtime-api")
//
//    // Optional: Substitute dependencies available in a module repository with local source projects.
//    // Without this, "com.mycompany:my-runtime-api:..." would resolve from the module repository,
//    // even though project ':my-runtime-api' is included in the build.
//    gradle.beforeProject {
//        configurations.all {
//            resolutionStrategy.dependencySubstitution {
//                substitute(module("com.mycompany:my-runtime-api"))
//                    .using(project(":my-runtime-api"))
//            }
//        }
//    }
}
