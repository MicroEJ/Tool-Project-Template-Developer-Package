/*
 * Kotlin
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

allprojects {
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            if (repositories.none { it is MavenArtifactRepository }) {
                repositories {
                    maven {
                        name = "localRepository"
                        url = uri("${System.getProperty("user.home")}/.microej/repository")
                    }
                }
            }
        }
    }
}
