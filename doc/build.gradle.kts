/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

import com.pswidersk.gradle.python.VenvTask

plugins {
    id("com.pswidersk.python-plugin") version "3.2.12"
}

pythonPlugin {
    pythonVersion = "3.11.0"
}

val buildDoc: TaskProvider<VenvTask> = tasks.register<VenvTask>("buildDoc") {
    val docDir = projectDir.resolve("src/site/sphinx")
    workingDir = docDir
    val siteOutputDir = layout.buildDirectory.dir("site")
    args = listOf("build.py", siteOutputDir.get().asFile.absolutePath)
    inputs.dir(docDir)
    outputs.dir(siteOutputDir)
}

val docElements: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.USER_MANUAL))
    }
}

artifacts {
    add(docElements.name, buildDoc.map { task -> task.outputs.files.singleFile })
}