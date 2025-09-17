/*
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

class DeveloperPackagingPlugin @Inject constructor(private val softwareComponentFactory: SoftwareComponentFactory) :
    Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply(JavaPlugin::class.java)

        val offlineRepositoryConf = project.configurations.create("offlineRepository")
        val offlineRepositoryDir = project.layout.buildDirectory.dir("offlineRepository")
        val fetchOfflineRepositoryTask = project.tasks.register("fetchOfflineRepository") {
            inputs.files(offlineRepositoryConf)
            outputs.dir(offlineRepositoryDir)

            doLast {
                logger.info("Fetching the Offline Repository declared as dependency")
                val offlineRepository = offlineRepositoryConf.files.firstOrNull { it.name.endsWith(".zip") }
                if (offlineRepository != null) {
                    logger.info("Extracting the Offline Repository to ${offlineRepositoryDir.get().asFile.absolutePath}")
                    project.copy {
                        from(project.zipTree(offlineRepository))
                        into(offlineRepositoryDir)
                    }
                } else {
                    logger.info("No Offline Repository declared")
                }
            }
        }

        val packageTask = project.tasks.register("package", Zip::class.java) {
            val docTask = project.rootProject.project(":doc").tasks.getByName("buildDoc")
            dependsOn(docTask)

            into("repository") {
                from(fetchOfflineRepositoryTask)
            }
            into("src") {
                from(project.rootProject.layout.projectDirectory.dir("src"))
            }
            into("doc") {
                from(docTask)
            }
            into("/") {
                from(project.rootProject.layout.projectDirectory.dir("resources"))
            }
            archiveFileName.set("my-package.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("package"))
        }

        project.tasks.named("assemble") {
            dependsOn(packageTask)
        }

        registerPublication(project, packageTask)
    }

    /**
     * Registers a new publication component named "developerPackage" which can be declared in the project publications.
     * It contains the developer package archive artifact.
     */
    private fun registerPublication(
        project: Project,
        packageTask: TaskProvider<Zip>
    ) {
        val developerPackageConfiguration = project.configurations.create("developerPackage") {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named(LibraryElements::class.java, "developer-package")
                )
            }
        }
        project.artifacts.add("developerPackage", packageTask)

        val packageComponent = softwareComponentFactory.adhoc("developerPackage")
        project.components.add(packageComponent)

        packageComponent.addVariantsFromConfiguration(developerPackageConfiguration, { })
    }

}