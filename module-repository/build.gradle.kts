/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
}
