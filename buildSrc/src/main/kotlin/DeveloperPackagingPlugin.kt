/*
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named
import org.gradle.process.ExecOperations
import javax.inject.Inject

private const val ARTIFACT_ELEMENT = "com.microej.artifact.element"

class DeveloperPackagingPlugin @Inject constructor(
    private val softwareComponentFactory: SoftwareComponentFactory,
    private val fsOps: FileSystemOperations,
    private val execOps: ExecOperations
) : Plugin<Project> {

    override fun apply(project: Project) {

        project.plugins.apply(JavaPlugin::class.java)
        val extension = project.extensions.create("developerPackage", DeveloperPackageExtension::class.java)

        val projectName = project.providers.systemProperty("package.module.name").getOrElse("my-package")
        val fetchModuleRepositoryTask = registerFetchModuleRepositoryTask(project)
        val fetchJavadocTask = registerJavadocTask(project)
        val docTask = registerFetchDocumentationTask(project)
        val fetchVirtualDeviceTask = registerFetchVirtualDevicesTask(project)
        val fetchExecutablesTask = registerFetchExecutablesTask(project)
        val cleanBspTask = registerCleanBspTask(project)
        cleanBspTask.configure {
            dependsOn(fetchExecutablesTask)
        }

        val packageTask = project.tasks.register("package", Zip::class.java) {
            dependsOn(cleanBspTask)
            into("repository") {
                from(fetchModuleRepositoryTask)
            }
            into("src") {
                from(project.rootProject.layout.projectDirectory.dir("src")) {
                    exclude("**/.gradle")
                    exclude("**/.idea")
                    exclude("**/.kotlin")
                    exclude("**/.pre-commit-config.yaml")
                    exclude("**/build/")
                    exclude("**/.gh*")
                    exclude("**/Jenkinsfile*")
                    exclude("**/CLAUDE.md")
                    for (action in extension.getSrcContentActions()) {
                        action.execute(this)
                    }
                }
            }
            into("javadoc") {
                from(fetchJavadocTask)
            }
            into("doc") {
                from(docTask)
            }
            into("bin") {
                from(fetchVirtualDeviceTask) {
                    into("virtualDevice")
                }
                from(fetchExecutablesTask) {
                    into("executable")
                }
            }
            into("/") {
                from(project.rootProject.layout.projectDirectory.dir("resources"))
            }
            for (action in extension.getPackageContentActions()) {
                action.execute(this)
            }

            archiveFileName.set("$projectName.zip")
            destinationDirectory.set(project.layout.buildDirectory.dir("package"))
            // Enable building zips with more than 65535 files or bigger than 4GB
            isZip64 = true
        }

        project.tasks.named("assemble") {
            dependsOn(packageTask)
        }

        registerPublication(project, packageTask)
    }

    private fun registerFetchExecutablesTask(project: Project): TaskProvider<Task> {
        val executableConfiguration = project.configurations.create("executable") {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
            attributes {
                attribute(Attribute.of(ARTIFACT_ELEMENT, String::class.java), "executable")
            }
        }
        val outputDir = project.layout.buildDirectory.dir("executable")
        return project.tasks.register("fetchExecutables") {
            inputs.files(executableConfiguration)
            outputs.dir(outputDir)
            doLast {
                logger.info("Fetching Executables...")
                executableConfiguration.incoming.artifacts.resolvedArtifacts.get().forEach { result ->
                    val moduleName = getModuleName(result.id.componentIdentifier)
                    val destinationPath = outputDir.get().asFile.resolve(moduleName)
                    logger.info("Copying executable into '${destinationPath.canonicalPath}'")
                    fsOps.copy {
                        from(result.file)
                        into(destinationPath)
                    }
                }
            }
        }
    }

    private fun getModuleName(componentId: org.gradle.api.artifacts.component.ComponentIdentifier): String {
        return when (componentId) {
            is ProjectComponentIdentifier -> componentId.projectName
            is ModuleComponentIdentifier -> componentId.module
            else -> componentId.displayName
        }
    }

    private fun registerCleanBspTask(project: Project): TaskProvider<Task> {
        val srcVeeDir = project.rootProject.layout.projectDirectory.dir("src/vee")
        return project.tasks.register("cleanBsp") {
            doFirst {
                logger.info("Cleaning BSP build files...")
            }
            doLast {
                val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")
                val scriptName = if (isWindows) "clean.bat" else "clean.sh"
                val veeDir = srcVeeDir.asFile
                if (!veeDir.isDirectory) return@doLast

                veeDir.walkTopDown()
                    .filter {
                        it.name == scriptName && it.parentFile.name == "scripts"
                                && it.parentFile.parentFile.name == "vee"
                    }
                    .forEach { script ->
                        logger.info("Cleaning BSP: ${script.canonicalPath}")
                        execOps.exec {
                            workingDir = script.parentFile
                            if (isWindows) {
                                commandLine("cmd", "/c", script.canonicalPath)
                            } else {
                                commandLine("sh", script.canonicalPath)
                            }
                            isIgnoreExitValue = true
                        }
                    }
            }
        }
    }

    private fun registerFetchModuleRepositoryTask(project: Project): TaskProvider<Task?> {
        val configuration = project.configurations.create("moduleRepository") {
            attributes {
                attribute(Attribute.of(ARTIFACT_ELEMENT, String::class.java), "module-repository")
            }
        }

        val outputDir = project.layout.buildDirectory.dir("moduleRepository")
        val task = project.tasks.register("fetchModuleRepository") {
            inputs.files(configuration)
            outputs.dir(outputDir)

            doLast {
                logger.info("Fetching module repository...")
                val zipTrees = configuration.elements.map { artifacts ->
                    artifacts
                        .filter { it.asFile.extension == "zip" }
                        .map { project.zipTree(it.asFile) }
                }
                fsOps.copy {
                    from(zipTrees)
                    into(outputDir)
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }
        }
        return task
    }

    private fun registerJavadocTask(project: Project): TaskProvider<Task?> {
        val javadocConfiguration = project.configurations.create("javadoc") {
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.JAVADOC))
            }
        }
        val outputDir = project.layout.buildDirectory.dir("javadoc")
        val task = project.tasks.register("fetchJavadoc") {
            inputs.files(javadocConfiguration)
            outputs.dir(outputDir)

            doLast {
                logger.info("Fetching javadoc...")
                val javadoc = javadocConfiguration.files.firstOrNull()
                if (javadoc != null) {
                    logger.info("Extracting javadoc to ${outputDir.get().asFile.canonicalPath}")
                    fsOps.copy {
                        from(project.zipTree(javadoc))
                        into(outputDir)
                    }
                } else {
                    logger.info("No javadoc dependency declared")
                }
            }
        }
        return task
    }

    private fun registerFetchVirtualDevicesTask(project: Project): TaskProvider<Task> {
        val configuration = project.configurations.create("virtualDevice") {
            isCanBeResolved = true
            isCanBeConsumed = false
            isTransitive = false
            attributes {
                attribute(Attribute.of(ARTIFACT_ELEMENT, String::class.java), "virtual-device")
            }
        }
        val outputDir = project.layout.buildDirectory.dir("virtualDevice")
        return project.tasks.register("fetchVirtualDevices") {
            inputs.files(configuration)
            outputs.dir(outputDir)
            doLast {
                logger.info("Fetching Virtual Devices...")
                configuration.incoming.artifacts.resolvedArtifacts.get().forEach { result ->
                    val moduleName = getModuleName(result.id.componentIdentifier)
                    val destinationPath = outputDir.get().asFile.resolve(moduleName)
                    logger.info("Extracting Virtual Device into '${destinationPath.canonicalPath}'")
                    fsOps.copy {
                        from(project.zipTree(result.file))
                        into(destinationPath)
                    }
                }
            }
        }
    }

    private fun registerFetchDocumentationTask(project: Project): TaskProvider<Task?> {
        val configuration = project.configurations.create("documentation") {
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.DOCUMENTATION))
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, project.objects.named(DocsType.USER_MANUAL))
            }
        }
        val outputDir = project.layout.buildDirectory.dir("doc")
        val task = project.tasks.register("fetchDocumentation") {
            inputs.files(configuration)
            outputs.dir(outputDir)

            doLast {
                logger.info("Fetching documentation")
                val documentation = configuration.files.firstOrNull()
                if (documentation != null) {
                    logger.info("Copying documentation to ${outputDir.get().asFile.canonicalPath}")
                    fsOps.copy {
                        from(documentation)
                        into(outputDir)
                    }
                } else {
                    logger.info("No documentation dependency declared")
                }
            }
        }
        return task
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
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named("developer-package"))
            }
        }
        project.artifacts.add("developerPackage", packageTask)

        val packageComponent = softwareComponentFactory.adhoc("developerPackage")
        project.components.add(packageComponent)

        packageComponent.addVariantsFromConfiguration(developerPackageConfiguration, { })
    }

}
