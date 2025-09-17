settingsEvaluated {

    val packageRootDir = System.getProperty("package.root.dir")

    allprojects {
        repositories {
            /* Local Repository for Maven/Gradle modules */
            maven {
                name = "localRepository"
                url = uri("${packageRootDir}/repository")
            }
            /* Local Repository for Ivy modules */
            ivy {
                name = "localRepository"
                url = uri("${packageRootDir}/repository")
                patternLayout {
                    artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                    ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                    setM2compatible(true)
                }
            }
            /* MicroEJ SDK 6 repository for Maven/Gradle modules */
            maven {
                name = "microEJForgeSDK6"
                url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
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
            maven {
                name = "microEJCentral"
                url = uri("https://repository.microej.com/modules")
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
            /* MicroEJ Developer repository for Maven/Gradle modules */
            maven {
                name = "microEJForgeDeveloper"
                url = uri("https://forge.microej.com/artifactory/microej-developer-repository-release")
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
        }
    }

    pluginManagement {
        repositories {
            /* Local Repository for Maven/Gradle modules */
            maven {
                name = "localRepository"
                url = uri("${packageRootDir}/repository")
            }
            /* Local Repository for Ivy modules */
            ivy {
                name = "localRepository"
                url = uri("${packageRootDir}/repository")
                patternLayout {
                    artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
                    ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
                    setM2compatible(true)
                }
            }
            /* MicroEJ SDK 6 repository for Maven/Gradle modules */
            maven {
                name = "microEJForgeSDK6"
                url = uri("https://forge.microej.com/artifactory/microej-sdk6-repository-release/")
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
            maven {
                name = "microEJCentral"
                url = uri("https://repository.microej.com/modules")
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
        }
    }
}