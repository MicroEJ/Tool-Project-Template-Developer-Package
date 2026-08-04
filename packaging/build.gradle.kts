/*
 * Java
 *
 * Copyright 2025-2026 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Build: 7E4D1F7C
 */

plugins {
    alias(libs.plugins.microej.base)
    id("com.microej.gradle.plugin.developer-packaging")
    id("maven-publish")
}

microej {
    checkersRootDir = rootProject.layout.projectDirectory
}

dependencies {
    // Fetch and package the documentation
    "documentation"(project(":doc"))

    // Use the module repository declared in the package
    "moduleRepository"(project(":module-repository"))

    // Alternatively, use a published module repository
    //"moduleRepository"("my-company:repository:1.0.0")

    // Fetch and package the Javadoc for a given module
    "javadoc"("com.microej.kernel:kernel-linux-x86:2.+")

    // Fetch and package executables for each kernel target
    "executable"("com.microej.kernel:kernel-linux-x86:2.+")
    //"executable"("com.microej.kernel:kernel-my-target:2.+")

    // Fetch and package the Virtual Devices for each kernel target
    "virtualDevice"("com.microej.kernel:kernel-linux-x86:2.+")
    //"virtualDevice"("com.microej.kernel:kernel-my-target:2.+")
}

testing {
    suites {
        // Default suite (targets linux-x86)
        val test by getting(JvmTestSuite::class) {
            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.assemble)
                        doFirst {
                            systemProperties = targetTestProperties(
                                kernelProjectName = "kernel-linux-x86",
                                veeportProjectName = "linux-x86",
                                veeportSrcDir = "vee/linux-x86",
                            )
                        }
                    }
                }
            }
        }

        // Example: second VEE Port target (uncomment and adapt)
        // val testRT1170 by registering(JvmTestSuite::class) {
        //     targets {
        //         all {
        //             testTask.configure {
        //                 dependsOn(tasks.assemble)
        //                 useJUnitPlatform {
        //                     excludeTags("linuxX86")  // To exclude tests tagged for other targets
        //                 }
        //                 doFirst {
        //                     systemProperties = targetTestProperties(
        //                         kernelProjectName = "kernel-rt1170",
        //                         veeportProjectName = "rt1170",
        //                         veeportSrcDir = "vee/rt1170",
        //                         jenkinsJobName = "MicroEJ_device_deploy_RT1170",
        //                     )
        //                 }
        //             }
        //         }
        //     }
        // }

        // Shared configuration for all test suites
        withType<JvmTestSuite> {
            useJUnitJupiter()

            // All suites share the same test sources (src/test/java, src/test/resources)
            sources {
                java { setSrcDirs(listOf("src/test/java")) }
                resources { setSrcDirs(listOf("src/test/resources")) }
            }

            dependencies {
                implementation(project())
                implementation("commons-io:commons-io:2.5")
                implementation(gradleTestKit())
                implementation("com.fazecast:jSerialComm:2.11.2")
            }
        }
    }
}

// Wire all test suites into the check task
tasks.check {
    dependsOn(testing.suites)
}

/**
 * Returns a complete map of properties for a test suite.
 * This includes both common properties (timeouts, serial, Jenkins) and target-specific properties
 * (kernel/VEE Port names and paths).
 *
 * @param kernelProjectName Gradle project name of the kernel (e.g. `"kernel-linux-x86"`, `"kernel-rt1170"`).
 * @param veeportProjectName Gradle project name of the VEE Port (e.g. `"linux-x86"`, `"rt1170"`).
 * @param kernelSrcDir relative path to the kernel sources inside `src/` (default: `"vee/kernel"`).
 * @param veeportSrcDir relative path to the VEE Port sources inside `src/` (e.g. `"vee/linux-x86"`).
 * @param executableOutputDir relative path to the executable output directory. Defaults to
 *   `"<kernelSrcDir>/build/application/executable/"`.
 * @param jenkinsJobName name of the Jenkins Deploy Job to use. Defaults to the `jenkins.job.name` system property.
 *   Override this when each VEE Port target has its own deploy job.
 * @param serialPort serial port for device communication (default: `"COM4"`).
 * @param serialBaudrate serial baud rate (default: `"115200"`).
 * @param serialDatabits serial data bits (default: `"8"`).
 * @param serialStopbits serial stop bits (default: `"1"`).
 * @param serialParity serial parity: `"none"`, `"even"`, `"odd"` (default: `"none"`).
 * @param serialRetry number of serial connection retries (default: `"3"`).
 * @param outputWaitTimeout timeout in ms for output buffer wait (default: `"400000"`).
 */
fun targetTestProperties(
    kernelProjectName: String,
    veeportProjectName: String,
    kernelSrcDir: String = "vee/kernel",
    veeportSrcDir: String,
    executableOutputDir: String? = null,
    jenkinsJobName: String? = System.getProperty("jenkins.job.name"),
    serialPort: String = "COM4",
    serialBaudrate: String = "115200",
    serialDatabits: String = "8",
    serialStopbits: String = "1",
    serialParity: String = "none",
    serialRetry: String = "3",
    outputWaitTimeout: String = "400000",
): Map<String, Any?> = mapOf(
    // Serial Connection properties
    "string.output.wait.timeout" to outputWaitTimeout,
    "serial.connection.port" to serialPort,
    "serial.connection.baudrate" to serialBaudrate,
    "serial.connection.databits" to serialDatabits,
    "serial.connection.stopbits" to serialStopbits,
    "serial.connection.parity" to serialParity,
    "serial.connection.retry" to serialRetry,
    // Jenkins properties
    "microejtool.deploy.name" to System.getProperty("microejtool.deploy.name"),
    "launch.test.trace.file" to System.getProperty("launch.test.trace.file"),
    "jenkins.url" to System.getProperty("jenkins.url"),
    "jenkins.job.prefix" to System.getProperty("jenkins.job.prefix"),
    "jenkins.job.name" to jenkinsJobName,
    "jenkins.user.name" to System.getProperty("jenkins.user.name"),
    "jenkins.user.token" to System.getProperty("jenkins.user.token"),
    // VEE-Port-specific properties
    "test.kernel.project.name" to kernelProjectName,
    "test.veeport.project.name" to veeportProjectName,
    "test.kernel.src.dir" to kernelSrcDir,
    "test.veeport.src.dir" to veeportSrcDir,
    "test.executable.output.dir" to (executableOutputDir
        ?: "$kernelSrcDir/build/application/executable/"),
)

publishing {
    publications {
        getByName<MavenPublication>("microej") {
            from(components["developerPackage"])
        }
    }
}

