/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

pluginManagement {
    repositories {
        /* Local Repository */
        maven {
            name = "localRepository"
            url = uri("${System.getProperty("user.home")}/.microej/repository")
        }
        maven {
            name = "moduleRepositoryMaven"
            url = uri(buildscript.sourceFile!!.parentFile)
        }
        ivy {
            name = "moduleRepositoryIvy"
            url = uri(buildscript.sourceFile!!.parentFile)
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Central repository for Maven/Gradle modules */
        maven {
            name = "microEJCentral"
            url = uri("https://repository.microej.com/modules")
        }
        /* MicroEJ Forge Central repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeCentral"
            url = uri("https://forge.microej.com/artifactory/microej-central-repository-release")
        }
        /* MicroEJ Developer repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeDeveloper"
            url = uri("https://forge.microej.com/artifactory/microej-developer-repository-release")
        }
        /* MicroEJ SDK 6 repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeSDK6"
            url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
        }

        /* MicroEJ Central repository for Ivy modules */
        ivy {
            name = "microEJCentralIvy"
            url = uri("https://repository.microej.com/modules")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Forge Central repository for Ivy modules */
        ivy {
            name = "microEJForgeCentralIvy"
            url = uri("https://forge.microej.com/artifactory/microej-central-repository-release")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Developer repository for Ivy modules */
        ivy {
            name = "microEJForgeDeveloperIvy"
            url = uri("https://forge.microej.com/artifactory/microej-developer-repository-release")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ SDK 6 repository for Ivy modules */
        ivy {
            name = "microEJForgeSDK6Ivy"
            url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        maven {
            name = "localRepository"
            url = uri("${System.getProperty("user.home")}/.microej/repository")
        }
        maven {
            name = "moduleRepositoryMaven"
            url = uri(buildscript.sourceFile!!.parentFile)
        }
        ivy {
            name = "moduleRepositoryIvy"
            url = uri(buildscript.sourceFile!!.parentFile)
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Central repository for Maven/Gradle modules */
        maven {
            name = "microEJCentral"
            url = uri("https://repository.microej.com/modules")
        }
        /* MicroEJ Forge Central repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeCentral"
            url = uri("https://forge.microej.com/artifactory/microej-central-repository-release")
        }
        /* MicroEJ Developer repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeDeveloper"
            url = uri("https://forge.microej.com/artifactory/microej-developer-repository-release")
        }
        /* MicroEJ SDK 6 repository for Maven/Gradle modules */
        maven {
            name = "microEJForgeSDK6"
            url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
        }
        /* MicroEJ Central repository for Ivy modules */
        ivy {
            name = "microEJCentralIvy"
            url = uri("https://repository.microej.com/modules")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Forge Central repository for Ivy modules */
        ivy {
            name = "microEJForgeCentralIvy"
            url = uri("https://forge.microej.com/artifactory/microej-central-repository-release")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ Developer repository for Ivy modules */
        ivy {
            name = "microEJForgeDeveloperIvy"
            url = uri("https://forge.microej.com/artifactory/microej-developer-repository-release")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        /* MicroEJ SDK 6 repository for Ivy modules */
        ivy {
            name = "microEJForgeSDK6Ivy"
            url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
            patternLayout {
                artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                setM2compatible(true)
            }
        }
        mavenCentral()
    }
}