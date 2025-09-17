/*
 * Copyright 2025 MicroEJ Corp. All rights reserved.
 * MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

plugins {
    id("com.microej.gradle.plugin.developer-packaging")
    id("maven-publish")
}

group = "my.company"
version = "1.0.0-RC"

dependencies {
    // Uncomment and adapt the following line to fetch and package an offline repository
    //"offlineRepository"("com.mycompany:my-repository:1.0.0@zip")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation("commons-io:commons-io:2.5")
                implementation(gradleTestKit())
                implementation("org.junit.platform:junit-platform-launcher:1.9.0")
            }

            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.assemble)
                    }
                }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("my-developer-package") {
            from(components["developerPackage"])
        }
    }
}

tasks.register("checkModule") {
    // Do nothing
}