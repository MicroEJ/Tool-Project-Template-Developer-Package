/*
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import com.pswidersk.gradle.python.VenvTask

plugins {
    id("com.pswidersk.python-plugin") version "2.9.0"
}

pythonPlugin {
    pythonVersion = "3.11.0"
}

tasks {
    register<VenvTask>("buildDoc") {
        workingDir = projectDir.resolve("src/site/sphinx")
        args = listOf("build.py", layout.buildDirectory.dir("site").get().asFile.absolutePath)
    }
}
