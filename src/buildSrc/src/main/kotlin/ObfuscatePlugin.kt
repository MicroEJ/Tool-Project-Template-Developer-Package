/*
 * Kotlin
 *
 * Copyright 2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
import com.microej.gradle.tasks.BuildWpkTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.withType
import proguard.gradle.ProGuardTask
import java.io.File

private const val OBFUSCATE_TASK_NAME = "obfuscate"

private const val PROGUARD_CONFIGURATION_FILENAME = "module.pro"

class ObfuscatePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val jarTask = project.tasks.named("jar")
        val obfuscateTask = project.tasks.register(OBFUSCATE_TASK_NAME, ProGuardTask::class.java) {
            dependsOn(jarTask)
            mustRunAfter("test")

            val jarTask = project.tasks.named("jar")
            val libsDir = project.layout.buildDirectory.dir("libs")
            val nonObfuscatedName = "${project.name}-${project.version}-non-obfuscated.jar"
            val nonObfuscatedJar = project.layout.buildDirectory.file("libs/$nonObfuscatedName")

            doFirst {
                project.copy {
                    from(jarTask)
                    into(libsDir)
                    rename { nonObfuscatedName }
                }
            }

            configuration(PROGUARD_CONFIGURATION_FILENAME)
            injars(nonObfuscatedJar)
            libraryjars(project.files(project.configurations.getByName("compileClasspath")))
            outjars(jarTask)
        }
        jarTask.configure {
            finalizedBy(obfuscateTask)
        }

        project.tasks.withType<BuildWpkTask> {
            applicationJar.from(obfuscateTask)
        }

        // Generates Javadoc only for the classes matching "-keep".
        project.tasks.named("javadoc", Javadoc::class.java) {
            dependsOn(OBFUSCATE_TASK_NAME)
            setSource(project.files(getKeptClasses(project)))
        }

        // Packages the sources only with the classes matching "-keep".
        project.tasks.named("sourcesJar", Jar::class.java) {
            dependsOn(OBFUSCATE_TASK_NAME)
            getKeptClasses(project).forEach { c ->
                include(c.relativeTo(project.file("src/main/java")).path)
            }
        }
    }

    // Lists the classes matching "-keep" in the "module.pro".
    fun getKeptClasses(project: Project): List<File> {
        return project.file(PROGUARD_CONFIGURATION_FILENAME).readLines()
            .map { it.trim() }
            .mapNotNull { line ->
                val regex = Regex("""^-keep\s+.*\bclass\s+([\w.$-]+)""")
                regex.find(line)?.groupValues?.get(1)
            }
            .map { fqn -> project.file("src/main/java/${fqn.replace('.', '/')}.java") }
            .filter { it.exists() }
    }
}
