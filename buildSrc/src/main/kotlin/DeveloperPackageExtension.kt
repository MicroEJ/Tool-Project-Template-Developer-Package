/*
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

import org.gradle.api.Action
import org.gradle.api.file.CopySpec

/**
 * Extension DSL for customizing the developer package.
 */
abstract class DeveloperPackageExtension {

    private val srcContentActions = mutableListOf<Action<CopySpec>>()

    // Customizes the src/ CopySpec (rename files, filter content, etc.).
    // The action receives the CopySpec used to copy src/ into the package zip.
    fun srcContent(action: Action<CopySpec>) {
        srcContentActions.add(action)
    }

    // Returns the accumulated src content actions.
    // Called by DeveloperPackagingPlugin during task configuration.
    internal fun getSrcContentActions(): List<Action<CopySpec>> = srcContentActions

    private val packageContentActions = mutableListOf<Action<CopySpec>>()

    // Add arbitrary content to the package zip.
    // The action receives the Zip task's top-level CopySpec, allowing
    // into("path") { from(...) } additions.
    fun packageContent(action: Action<CopySpec>) {
        packageContentActions.add(action)
    }

    // Returns the accumulated package content actions.
    // Called by DeveloperPackagingPlugin during task configuration.
    internal fun getPackageContentActions(): List<Action<CopySpec>> = packageContentActions
}
