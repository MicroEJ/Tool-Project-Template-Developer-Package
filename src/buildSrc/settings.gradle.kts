/*
 * Kotlin
 *
 * Copyright 2024-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// Applies the repository configuration from the Module Repository bundled in the package
val moduleRepositoryScript = file("../../repository/module-repository.gradle.kts")
if (moduleRepositoryScript.exists()) {
    apply(moduleRepositoryScript)
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}