/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

rootProject.name = "developer-packaging-template"

include("packaging")
project(":packaging").name = providers.systemProperty("package.module.name").getOrElse("my-package")
include("doc")
include("module-repository")

includeBuild("src")

// Use the libs.versions.toml in `src` for version management in the packaging
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./src/gradle/libs.versions.toml"))
        }
    }
}