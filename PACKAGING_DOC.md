# Overview

A Developer Package project, forked from the Developer Package template repository, comes with a set of features to
ease the setup and development. This document lists and describes all these features.

# Forewords

The packaging logic and features are implemented in the Gradle plugin `com.microej.gradle.plugin.developer-packaging`,
located in `buildSrc`. While the plugin provides the essential functionality needed by most Developer Packages, it can
be adapted to fit the specific needs of your packaging project.

# Developer Package Template Features

## Configure the Package

The `gradle.properties` file at the root of the project defines system properties that allow to configure the package:

- `package.module.name`: the name of the package.
- `package.group.name`: the group name of the package.
- `package.version`: the version of the package.
- `app.developer.mode.enabled`: whether to enable the Application Developer Mode.

## Sources Directory Structure

All sources files located in the `src` folder of the project are automatically packaged in the `src` folder of the
generated package archive.

The typical folder structure for the `src` directory is the following:

```
└── src
    ├── app                     # Applications
        ├── app1
        └── app2
    ├── buildSrc                # Convention plugin and build utilities
    └── gradle
        ├── wrapper
    └── vee
        ├── kernel              # Kernel
        ├── <target1>           # Target VEE Ports (e.g., linux-x86, rt1170)
        ...
        └── <targetN>
    gradlew
    gradlew.bat
    settings.gradle.kts
```

This layout follows the standard Gradle multi-project configuration.
Of course, this may be adapted to suit the specific requirements of your project (i.e., runtime environment, libraries,
etc.).

## Set Up a Kernel

To set up a kernel, place the Kernel sources under `src/vee/kernel/` and the VEE Port sources under
`src/vee/<target>/` (e.g. `src/vee/linux-x86/`).

The project configuration differs depending on whether the Kernel targets a single or multiple VEE Ports. Follow the
appropriate section below.

### Using a Single VEE Port

This is a standard setup:

1. **Register the kernel and VEE Port projects** in `src/settings.gradle.kts`:

    ```kotlin
    include(":kernel")
    project(":kernel").projectDir = file("vee/kernel")
    
    include(":<target>", ":<target>:front-panel", ":<target>:mock")
    project(":<target>").projectDir = file("vee/<target>/vee-port")
    project(":<target>:front-panel").projectDir = file("vee/<target>/vee-port/extensions/front-panel")
    project(":<target>:mock").projectDir = file("vee/<target>/vee-port/mock")
    ```

2. **Declare the VEE Port dependency** directly in the kernel `build.gradle.kts`:

    ```kotlin
    dependencies {
        microejVee(project(":<target>"))
    }
    ```

### Using Multiple VEE Ports

When the same kernel source targets multiple hardware platforms, use the following configuration:

1. **Register the kernel and VEE Port projects** in `src/settings.gradle.kts` using the `kernel-<target>` naming.
   Each `kernel-<target>` project points to the shared `vee/kernel/` source directory, see the example below:

    ```kotlin
    include(":kernel-linux-x86")
    project(":kernel-linux-x86").projectDir = file("vee/kernel")
    include(":kernel-rt1170")
    project(":kernel-rt1170").projectDir = file("vee/kernel")
       
    include(":linux-x86", ":rt1170")
    project(":linux-x86").projectDir = file("vee/linux-x86/vee-port")
    project(":rt1170").projectDir = file("vee/rt1170/vee-port")
    // ...
    ```

2. **Apply the `microej-kernel-conventions` plugin** in the kernel `build.gradle.kts`:

    ```kotlin
    plugins {
        alias(libs.plugins.microej.application)
        id("microej-kernel-conventions")
    }
    ```

   This convention plugin (located at `src/buildSrc/src/main/kotlin/microej-kernel-conventions.gradle.kts`) handles
   automatically:

    - **VEE Port resolution**: the VEE Port dependency is derived from the kernel project name (e.g. `kernel-rt1170` →
      `:rt1170`), no need to add the `microejVee` configuration.

    - **Build directory isolation**: each `kernel-<target>` project gets its own build directory under
      `build/targets/<target>/`, preventing collisions when the kernel source directory is shared across targets.

#### Naming Convention

In multi-VEE-Port mode, each kernel project follows the `kernel-<target>` naming convention, where `<target>`
identifies the hardware platform (e.g. `linux-x86`, `rt1170`). This convention connects the kernel project to its
VEE Port and to the package output directories.

| Item             | Format              | Example           |
|------------------|---------------------|-------------------|
| Kernel project   | `kernel-<target>`   | `kernel-rt1170`   |
| VEE Port project | `<target>`          | `rt1170`          |
| Kernel sources   | `src/vee/kernel/`   | `src/vee/kernel/` |
| VEE Port sources | `src/vee/<target>/` | `src/vee/rt1170/` |

## Set Up Applications

Apply the `microej-app-conventions` plugin in the applications `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.microej.application)
    id("microej-app-conventions")
}
```

This convention plugin (located at `src/buildSrc/src/main/kotlin/microej-app-conventions.gradle.kts`) reads two
system properties:

- `kernel.name` (required): the name of the kernel to use (e.g., `mykernel`, `kernel-rt1170`).
- `app.developer.mode.enabled` (default `true`): when enabled, resolves VEE from the prebuilt binaries in
  `bin/executable/<kernel-name>/` and `bin/virtualDevice/<kernel-name>/`. When disabled, resolves VEE from the kernel
  sources.

The convention plugin automatically adds the correct `microejVee` dependency based on the system properties.
Applications do not need to handle VEE selection themselves.

Set these properties in `src/gradle.properties`, or pass it on the command line:

```shell
./gradlew :app:runOnSimulator -Dkernel.name=<kernel-name>
```

## Obfuscate Kernel and Applications

To obfuscate the JAR of the Kernel or the Applications, add the plugin `com.microej.plugin.obfuscate` in the Kernel or
Application project:

```kotlin
plugins {
    ...
    id("com.microej.plugin.obfuscate")
}
```

This plugin automatically replaces the default JAR file with the obfuscated version (the original JAR is renamed with
the `-non-obfuscated` suffix).
This obfuscated JAR is also used in the generated Virtual Device.

Besides adding the Gradle plugin, the Proguard configuration must be defined by creating a file named `module.pro` at
the root of the project.
This file must contain the class to not obfuscate.
As a basic example, if your project is an Application with a main class `com.mycompany.Main`,
you can obfuscate all classes except this main class by putting this content in the `module.pro` file:

```
-keep public class com.mycompany.Main {
    public static void main(java.lang.String[]);
}
```

In a Kernel project, all classes opened to the Applications developers must be added in this configuration to not
obfuscate them.

See the [ProGuard manual](https://www.guardsquare.com/manual/configuration/usage) for all available options.

## Application Developer Mode

> **Important**
>
> This section describes a feature that is not available in the SDK, it is specific to the Developer Package.

The "Application Developer Mode" is a mode where the end-user of the package is considered an Application Developer.
An Application Developer will typically not modify the Kernel or the VEE Port, at least in the first place.
As a result, the Kernel can be loaded directly from the prebuilt binaries bundled in the package (executable
`bin/executable/<kernel-name>/application.out`, and Virtual Device `bin/virtualDevice/<kernel-name>/vee`).
With this option, the developer can save the installation of a full development environment and have shorter
build/configuration times. The developer can also focus on application development and avoid
the overhead complexity of dealing with Kernel and VEE Port sources.

The end-user of the package can enable or disable the Application Developer Mode with the system property
`systemProp.app.developer.mode.enabled` in `src/gradle.properties`.
When the mode is enabled (default), the Kernel is loaded from the prebuilt binaries included in the package.
When the mode is disabled, the Kernel is built and loaded from the sources.
If the property is not explicitly set, Application Developer Mode is enabled by default.

This mode is not desired when working in the packaging project, so the value is set to `false` in the root
`gradle.properties`.

### Conditional Project Loading

The Application Developer Mode can also control which Gradle projects are loaded in `src/settings.gradle.kts`.
Application projects (e.g. `:app`) are always included, while Kernel, VEE Port, and other source projects are only
included when the mode is disabled:

```kotlin
// Application projects are always included.
include(":app")
project(":app").projectDir = file("app")

// Source projects are only included when Application Developer Mode is disabled.
if (!isAppDeveloperMode) {
    include(":kernel-<target>", ":<target>")
    project(":kernel-<target>").projectDir = file("vee/kernel")
    project(":<target>").projectDir = file("vee/<target>/vee-port")
    // ...
}
```

This provides two benefits:

- **Faster IDE experience**: when Application Developer Mode is enabled, the IDE only loads application projects,
  resulting in faster Gradle sync and a cleaner project tree. This is especially valuable for newcomers who should
  not need to deal with kernel or VEE Port complexity.
- **Reduced configuration errors**: projects that are not needed are not loaded, so misconfigured VEE Port or kernel
  build files do not cause sync failures for application developers.

### Dependency Substitution

When an application depends on a library by its module coordinates (e.g.`implementation("com.mycompany:my-lib:1.0.0")`),
Gradle resolves it from repositories — even if the matching project is included in the build. To use the local sources
instead, add dependency substitution rules in `src/settings.gradle.kts`:

```kotlin
if (!isAppDeveloperMode) {
    include(":my-lib")

    // Substitute module dependencies with local source projects.
    gradle.beforeProject {
        configurations.all {
            resolutionStrategy.dependencySubstitution {
                substitute(module("com.mycompany:my-lib"))
                    .using(project(":my-lib"))
            }
        }
    }
}
```

## Add a Module Repository

A Module Repository bundles dependencies required by the projects in `src/`, so that end-users can resolve them
from a local repository shipped with the package. Depending on the configuration, this repository can complement
public repositories (Partial Repository) or replace them entirely (Full Repository) for offline use.

### Fetch the Module Repository

To add a Module Repository in the Developer Package, declare a dependency in the `packaging/build.gradle.kts`
file with the `moduleRepository` configuration.
The specified Module Repository is automatically bundled into the `repository` folder of the final package.

If the Module Repository project is a subproject of the Developer Packaging multi-project:

```
dependencies {
    "moduleRepository"(project(":repository"))
}
```

If the Module Repository is a published module:

```
dependencies {
    "moduleRepository"("my-company:repository:1.0.0")
}
```

You can specify multiple Module Repositories if needed, they will all be merged in the same Module Repository in the
`repository` folder.

### Use the Module Repository in the Projects

To enable your projects to resolve modules from this Module Repository, add the following to your
`src/settings.gradle.kts`:

```
apply("../repository/module-repository.gradle.kts")
```

This line must be inserted at the start of the file, before including projects, and before using any plugins that depend
on repositories.
This applies the repository configuration to all subprojects, ensuring that dependencies can be resolved from the
packaged Module Repository.

### Define a Module Repository within the Packaging Project

This template provides a Module Repository project in the `module-repository` folder where you can specify all the
required dependencies of the package.

To declare a dependency, use the `microejModule` configuration in `module-repository/build.gradle.kts`:

```
dependencies {
    microejModule("com.microej.architecture.I386.GNUvX_X86Linux:atsauce6:8.4.0")
}
```

This Module Repository project is configured to build a **Partial Repository** by default. A Partial Repository contains
only the modules that are not available in public repositories (MicroEJ Central, Developer, etc.). End-users still need
internet access to resolve the remaining dependencies. The file `module-repository/module-repository.gradle.kts`
configures the projects in `src/` to resolve dependencies from both the bundled repository and the public repositories.

To build a **Full Repository** (offline mode), remove the file `module-repository/module-repository.gradle.kts` and list
all required modules in `module-repository/build.gradle.kts`. A Full Repository is self-contained: end-users can build
the projects in `src/` without any internet access, since all dependencies are bundled. This is useful for restricted
network environments
or when you need to guarantee that the package works independently of external repository availability.

Refer to
the [public documentation on Module Repository](https://docs.microej.com/en/latest/SDK6UserGuide/moduleRepository.html)
to learn more on this topic.

## Add a Virtual Device

To add a Virtual Device in the Developer Package, declare a dependency in the `packaging/build.gradle.kts` file with the
`virtualDevice` configuration. Multiple `virtualDevice` dependencies are supported.

Example for building the Virtual Device of an Application:

```kotlin
"virtualDevice"("com.mycompany:myapp:1.0.0")
```

Example for building the Virtual Device of a Kernel for multiple VEE Ports:

```kotlin
"virtualDevice"("com.microej.kernel:kernel-linux-x86:2.+")
"virtualDevice"("com.microej.kernel:kernel-rt1170:2.+")
```

Each Virtual Device is automatically bundled into the `bin/virtualDevice/` directory of the package.

> **Note**
> The Virtual Device of an Application is not built and published by default. As a result, the selected Application
> must specify `produceVirtualDeviceDuringBuild()` in the `microej` configuration block of the Gradle build file of the
> project.

## Add an Executable

To add an executable in the Developer Package, declare a dependency in the `packaging/build.gradle.kts` file with the
`executable` configuration. Multiple `executable` dependencies are supported.

Example for building the Executable of a Kernel:

```kotlin
"executable"("com.mycompany:mykernel:1.0.0")
```

Example for building the Executable of a Kernel for multiple VEE Ports:

```kotlin
"executable"("com.microej.kernel:kernel-linux-x86:2.+")
"executable"("com.microej.kernel:kernel-rt1170:2.+")
```

Each Executable is automatically bundled into the `bin/executable/` directory of the package.

> **Important**
> Building the executables creates intermediate files during the BSP build which are not excluded by default when
> packaging the sources.
> To prevent BSP build files from being bundled in the final package, the packaging plugin runs the BSP
> clean scripts, if they exist, before packaging the sources. Place clean scripts (`clean.bat` / `clean.sh`) in the
> `bsp/vee/scripts/` directory of each VEE Port under `src/vee/`.

> **Note**
> The Executable of an Application is not built and published by default. As a result, the selected Application must
> specify `produceExecutableDuringBuild()` in the `microej` configuration block of the Gradle build file of the
> project.

## Add Javadoc

To add the Javadoc of a module in the Developer Package, declare a dependency in the `packaging/build.gradle.kts` file
with the `javadoc` configuration:

```kotlin
"javadoc"("com.mycompany:my-runtime-environment:1.0.0")
```

The Javadoc is extracted by default in the `javadoc` folder of the package archive.
The specified module can be in the current project or in a remote repository, and must publish a Javadoc artifact
(for example an Application or a Java Library).

## Add Documentation

The output generated from building the `doc` subproject is automatically packaged in the `doc` folder of the package
archive, in HTML format.

The documentation skeleton provided by the template is a base for any developer package. Replace the following words:

- `<framework_name>`: the framework name (e.g. VEE Wear)
- `<target_market>`: the targeted market (e.g. smartwatches)
- `<target_hardware>`: the supported hardware(s) (e.g. Actions ATS3085S)
- `<framework_repository>`: the name of the repository (e.g. wearRepository)

Then enrich it as needed. The following sections serve as guidelines for the different documentation pages.

To build only the documentation, run the `buildDoc` Gradle task.

### Introduction

In this section, add the following diagrams:

- High-level architecture diagram (system): Describe the software stacks. Add details and links in the explanation below
  the scheme.
- High-level functional diagram: Describe the typical data flow. Make several schemes if necessary for the different
  libraries.

### Software Setup

In this section, add the software setup instructions that are specific to the framework.

### Hardware Setup

In this section, list the supported hardware and the setup instructions.

Use [This tool](https://gitlab.cross/M0127_CustomerCare/M0127_Documentation-Tools) to generate board images with
annotations.

### Get Started

In this section, explain how to use the virtual device, build and run the demo applications on the simulator and on the
device.

### Develop

In this section, explain how to use the framework in a custom application.

### Diagrams with mermaid

You can embed diagrams with the `::mermaid` sphinx instruction.

If you want to get a PNG or SVG output instead of embedded HTML, change `mermaid_output_format` in `conf.py` file.
If you have an error about playwright during diagrams rendering in SVG or PNG, run this command in a terminal:
`playwright install`. This currently works locally but not on CI.

### Troubleshooting: building the documentation on Windows

When building the documentation, the task `:doc:buildDoc` can fail on `:doc:envSetup` with the following error:

```
A problem occurred starting process '…\condabin\conda.bat'
```

This happens when the project is located under a long path: this causes the Python install under
`<rootProjectDir>/.gradle/python/` to exceed the Windows 260-character limit and fail (silently), leading to
missing files in the installation directory.

To work around this issue, you have two options:

- clone and build the project from a short path (for example `C:\git\<project>`).
- customize the conda installation directory using the `installDir` property of the `pythonPlugin { }` extension. See
  [the plugin documentation](https://github.com/PrzemyslawSwiderski/python-gradle-plugin#python-plugin-properties) for
  more information.

## Add Resources

All files located in the `resources` folder are automatically packaged in the root folder.
The template contains the `README.md` and `CHANGELOG.md` files as example.

## Test the Package

A testsuite is set up in `packaging/src/test`. The main purpose of this testsuite is to verify that the produced package
is valid, more specifically:

- sources bundled in the archive operate as expected (code compiles, application builds and runs on the target device,
  etc.).
- package content is correct (file structure, sanity checks, etc.)

The tests are executed on the unzipped package archive to validate the build output and ensure that the generated ZIP
meets the quality standards expected for customer deliveries.

By default, the testsuite provides a small set of tests that serve as examples. The testsuite should be expanded to
include more test cases, to automate the testing of the delivery process.

The current testsuite checks the following conditions:

- the application runs on the Simulator (runs the `runOnSimulator` task on the project `src/app`).
- the Kernel executable is built correctly (runs the `buildExecutable` task on the project `src/vee/kernel`).
- the Feature of the application is built correctly (runs the `buildFeature` task on the project `src/app`).
- the Kernel executable can run on the target device (runs `runOnDevice` task on the project `src/vee/kernel`).
- the Feature can be deployed on the target device (runs the `featureDeploy` task on the project `src/app`).
- the package file structure is correct.
- the files of the package do not contain any unwanted terms (client names, wrong licenses, illicit terms, etc.).

The testsuite of the template can run locally and on Jenkins without having to set up any additional hardware,
thanks to the Linux x86 VEE Port.
Ensure that all requirements are satisfied before running the testsuite:

- the sources of the template are initialized properly (refer to section `Setup` of this README),
- the environment is set up to compile the BSP (refer to section `BSP Setup`
  of [the Linux VEE Port documentation](src/vee/linux-x86/README.md) for more details).

You can launch the testsuite by running `./gradlew test` (runs the default suite) or `./gradlew check` (runs all
registered test suites).

To run a specific test, filter the test with `--tests`. Use `test` for the default suite or the suite name for
a specific suite:

    ./gradlew test --tests BuildRunDeployTest.testKernelBuildExecutable
    ./gradlew testRT1170 --tests BuildRunDeployTest.testKernelBuildExecutable

> **Note**
> the testsuite uses a dedicated temporary Gradle User Home directory instead of the default `~/.gradle`.
> This ensures that the tests run in a clean, isolated environment and do not depend on the host machine's configuration
> or state (such as Gradle caches or global Gradle configuration like `gradle.properties` and init scripts).

### Testing Multiple VEE Port Targets

When a package targets multiple VEE Ports, the same test classes can be executed against each target without code
duplication. Target-specific values (kernel project name, VEE Port project name, source paths, package paths) are
injected via system properties and read by the helper class `TestProperties` (in
`packaging/src/test/java/com/microej/test/util/`).

#### Registering an additional test suite

To add a test suite for a second VEE Port target, register a new `JvmTestSuite` in the `testing` block of
`packaging/build.gradle.kts`. The new suite shares the same sources and dependencies as the default suite:

```kotlin
val testRT1170 by registering(JvmTestSuite::class) {
    targets {
        all {
            testTask.configure {
                dependsOn(tasks.assemble)
                useJUnitPlatform {
                    excludeTags("linuxX86")     // Optionally, exclude tests that would be specific to other VEE Ports
                }
                doFirst {
                    systemProperties = targetTestProperties(
                        kernelProjectName = "kernel-rt1170",
                        veeportProjectName = "rt1170",
                        veeportSrcDir = "vee/rt1170",
                        jenkinsJobName = "MicroEJ_device_deploy_RT1170",
                    )
                }
            }
        }
    }
}
```

Refer to the KDoc of the `targetTestProperties` function to have the complete list of available properties. You can also
add new properties.

To mark a test class or method as target-specific, use the JUnit 5 `@Tag` annotation:

```java

@Tag("linuxX86")
class LinuxX86SpecificTest {
    // ...
}
```

Tests without tags are shared and run by all suites. Suites can use `excludeTags` to skip tests that are specific to
other targets.

Key points:

- All suites share the same test sources (`src/test/java`) and dependencies via the `withType<JvmTestSuite>` block —
  no duplication needed.
- `./gradlew check` or `./gradlew build` run all test suites thanks to `tasks.check { dependsOn(testing.suites) }`.
  Run a single suite with `./gradlew <suite-name>` (e.g., `./gradlew testRT1170`).
- Target-agnostic tests (e.g. `StructureContentTest`) run in every suite. This is intentional — they are fast and
  idempotent. If this becomes an issue with many suites, tag them with `@Tag("packageStructure")` and add
  `excludeTags("packageStructure")` to the additional suites.

### How to Test the Build and Run Cases

A typical test case has the following structure:

    private final GradleBuildRunner buildRunner = new GradleBuildRunner();

	@Test
	public void testAppBuildFeature() {
		// When
		int exitCode = buildRunner.startBuild(srcDir, ":app:buildFeature");

		// Then
		assertEquals(BUILD_SUCCESS, exitCode);
		assertTrue(new File(srcDir, "app/build/application/feature/application.fo").exists());
	}

This snippet tests that the Feature file of the Application `app` can be built correctly.

If you have multiple VEE Ports, you can select the target with property `kernel.name`, for example:

    // When
    int exitCode = buildRunner.startBuild(srcDir, ":app:buildFeature", "-Dkernel.name=kernel-rt1170");

When the test needs to do assertions on the build output, use a `StringOutputBuffer` like in this example:

	@Test
	public void testKernelBuildExecutable() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			// When
			int exitCode = buildRunner.startBuild(srcDir, output.getWriter(), ":kernel-linux-x86:buildExecutable", "-Dapp.developer.mode.enabled=false", "--info");

			// Then
			assertEquals(BUILD_SUCCESS, exitCode);
			assertTrue(output.getContent().contains("[100%] Built target application.out"));
			assertTrue(new File(srcDir, "vee/kernel/build/targets/linux-x86/application/executable/application.out").exists());
		}
	}

When the build is finished, the content of the output can be retrieved using the method
`StringOutputBuffer#getContent()`.

When a build does not stop by itself (like when running on simulator or device for example), it is required to start the
build asynchronously to allow proceeding with assertions:

    @Test
	public void testKernelRunOnDevice() throws Exception {
		try (StringOutputBuffer output = newOutputBuffer()) {
			try {
				// When
				buildRunner.startAsynchronousBuild(srcDir, output.getWriter(), ":kernel-linux-x86:runOnDevice", "-Dapp.developer.mode.enabled=false", "--info");

				// Then
				assertTrue(output.waitUntilContains("MicroEJ START"));
				assertTrue(output.waitUntilContains("INFO: Starting Kernel"));
			} finally {
				stopRunOnDevice(buildRunner, srcDir, output.getWriter());
			}
		}
	}

The `startAsynchronousBuild` method starts a build, redirecting the build output to the `StringOutputBuffer`. The output
can then be tested using the method `StringOutputBuffer#waitUntilContains(...)`.
The method `waitUntilContains` can specify the maximum time to wait before returning. When not specified, the
timeout time is set using the system property `string.output.wait.timeout` in the `targetTestProperties()` function of
`build.gradle.kts`.
The `stopRunOnDevice` call in the `finally` block ensures the application process is stopped even if assertions fail,
preventing orphan processes from hanging the pipeline in CI.

When several async builds are started in a row within the same test (e.g., looping over a list of applications and
running each one on the simulator), each build's `Process` must be destroyed before starting the next iteration.
Otherwise, the launcher and its forked JVMs (Gradle wrapper, daemon, simulator) accumulate across iterations, which can
eventually cause system failures.
The `startAsynchronousBuild` method returns the started `Process` so it can be passed to
`buildRunner.destroyProcess(process)` in a `finally` block:

    @Test
    public void testRunAllAppsOnSimulator() throws Exception {
        for (String app : List.of(":app1:runOnSimulator", ":app2:runOnSimulator")) {
            try (StringOutputBuffer output = newOutputBuffer()) {
                // When
                Process process = buildRunner.startAsynchronousBuild(srcDir, output.getWriter(), app);
                try {
                    // Then
                    assertTrue(output.waitUntilContains("MicroEJ START"));
                } finally {
                    // Ensure the process is destroyed before the next iteration
                    buildRunner.destroyProcess(process);
                }
            }
        }
    }

The `destroyAllProcesses()` call in `@AfterEach` is a safety net that destroys any tracked process still alive at the
end of the test, but it must not be the primary cleanup mechanism: it only runs once after the test completes and
cannot prevent JVMs from piling up across loop iterations.

The ``start*Build`` methods support passing an arbitrary number of Gradle command-line arguments. All provided arguments
are forwarded to the Gradle invocation.
To ensure arguments are parsed correctly by Gradle, the following rules must be respected:

- Each Gradle argument must be passed as a separate argument (do not concatenate multiple options into a single string).
- For properties (``-P`` or ``-D``), avoid using quotes in the argument value. Instead, pass the value as a single
  argument.

For example:

    // Builds the Feature of `app`, passing properties and excluding task `someTask`
    String someValue = "foo";
    buildRunner.startBuild(srcDir, ":app:buildFeature", "-Dkernel.name=kernel-rt1170", "-PsomeProp=" + someValue, "--info", "-x", "someTask");

**Notes:**

- The tests in the class `BuildRunDeployTest` are annotated with `@Order`. Ordering the test execution allows to mimic
  how the end-user will get started with the Developer Package.
- The test `BuildRunDeployTest#testAppFeatureDeploy` is disabled by default because it makes the template build fail on
  Jenkins (no network in the Docker container). To enable this test locally, comment the `@Disabled` annotation on the
  test method.
- The test `BuildRunDeployTest#testAppFeatureDeploy` relies on the fact that a task `featureDeploy` is defined for
  deploying Features. The SDK does not yet provide a generic task for deploying Features. In this example, the task
  is defined by the plugin `com.microej.plugin.application-feature-deploy` in `src/buildSrc`. This implementation uses
  AppConnect to deploy Features over HTTP. You can adapt this task to your specific hardware.
- Build execution is handled by `GradleBuildRunner`, which starts Gradle using the `gradlew` command in a
  `Process`. The Gradle `GradleRunner` is not used here because some builds never stop (like `runOnDevice` on the VEE
  Linux x86), which forbids testing the build output. Asynchronous processes are tracked internally by the
  `GradleBuildRunner` instance. Each test is responsible for destroying the processes it starts (via
  `destroyProcess(Process)` or `stopRunOnDevice` for `runOnDevice` builds); `destroyAllProcesses()` in `@AfterEach`
  acts as a safety net that catches anything still alive at the end of the test. Async builds are launched with
  `--no-daemon` so the build and its forked JVMs stay descendants of the launcher and can be reliably destroyed by
  killing the launcher process.
- For async builds that run applications (e.g., `runOnDevice`), call `TestUtil.stopRunOnDevice()` to stop the
  application before closing the output buffer. This is especially important in CI where orphan processes can hang the
  pipeline. `stopRunOnDevice` handles both local (via `pkill`) and Jenkins (via the Jenkins API) environments.
  An overload `stopRunOnDevice(buildRunner, projectDir, writer, jobName)` allows specifying a custom Jenkins Deploy Job
  name, which is useful when multiple deploy jobs exist (e.g., one per VEE Port target). The default overload reads the
  job name from the `jenkins.job.name` system property.

### Using another VEE Port

The Linux x86 VEE Port is convenient for the package template, as it doesn't require any extra hardware, however it does
not reflect accurately how most VEE Ports redirect the execution logs.

Since the testsuite relies on these logs for the assertions, it is important to configure how logs are retrieved
beforehand.

#### Configure tests for local execution

In general, the execution logs are collected over a serial interface.
When executing the testsuite on the developer machine with a board connected via a serial port,
the serial port parameters must be configured accordingly:

- Set the serial connection configuration using the `serial.connection.*` system properties in the `testing` block of
  `packaging/build.gradle.kts`:

    ```
    "serial.connection.port" to "COM4",
    "serial.connection.baudrate" to "115200",
    "serial.connection.databits" to "8",
    "serial.connection.stopbits" to "1",
    "serial.connection.parity" to "none",
    "serial.connection.retry" to "3",
    ```
- Use the method `TestUtil.newOutputBufferWithSerialRedirection()` to create a `StringOutputBuffer` that reads from
  the specified serial port. The serial logs will be redirected to the build output and can be tested in the same
  fashion.

Example:

    @Test
	public void testKernelRunOnDevice() throws Exception {
		try (StringOutputBuffer output = newOutputBufferWithSerialRedirection()) {
			try {
				// When
				buildRunner.startAsynchronousBuild(srcDir, output.getWriter(), ":kernel-linux-x86:runOnDevice", "-Dapp.developer.mode.enabled=false", "--info");

				// Then
				assertTrue(output.waitUntilContains("MicroEJ START"));
				assertTrue(output.waitUntilContains("INFO: Starting Kernel"));
			} finally {
				stopRunOnDevice(buildRunner, srcDir, output.getWriter());
			}
		}
	}

#### Configure tests for CI

To configure the testsuite to run on CI, follow these steps:

- If not already done, ask DevOps team to setup the board in CI as described in
  this [documentation](https://docs.cross/howto/flash_board.html#board-testsuites-integration-in-ci).
- Uncomment the `ARGS` property in the `Jenkinsfile` and change the value of the `jenkins.job.name`.
  It must be set to the name of the Jenkins Deploy Job for your device.

## Appendix

### Excluding Files

When creating the package archive, the packaging plugin automatically excludes common unwanted files from ``src``, such
as Gradle `build` directories. This ensures that the generated ZIP archive does not contain temporary or working files
produced during the build.
However, environment-specific or non-generic leftovers are not excluded by default (e.g., files generated by a BSP build
or other external tools).

You can handle such environment-specific files in one of the following ways:

- Use the `srcContent` extension point (see [Customizing the Package](#customizing-the-package) below).
- Provide a custom clean strategy: define VEE Port clean scripts (`clean.bat` and `clean.sh`) in the `<bsp>/vee/scripts`
  directory of the VEE Port. The packaging plugin automatically discovers and executes these scripts before the
  `package` task runs.

### Customizing the Package

When forking this template, you may need to adapt the package contents to your project: exclude BSP build artifacts,
rename files, or bundle extra tools. The packaging plugin provides a `developerPackage` extension
in `packaging/build.gradle.kts` so that you can make these changes **without modifying `DeveloperPackagingPlugin.kt`**.
This avoids merge conflicts when pulling template updates.

#### Customize the `src/` Copy

Use `srcContent` to apply custom file operations on the `src/` content before it is packaged (e.g., exclude, rename,
filter, etc.).
The specified `CopySpec` actions operate on the `src/` content only. They cannot affect other sections of the package
(repository, javadoc, doc, bin) or add files outside `src/`.

**Common operations:**

- Exclude files or directories: `exclude("**/pattern")`
- Rename files: `rename("old.txt", "new.txt")` or `filesMatching("**/*.tpl") { ... }`
- Filter file content: `filter(...)`
- Add files into `src/`: `from("extra-sources")` — the added files land inside `src/` in the zip

Any operation supported by `CopySpec` can be used, refer to
the [official Gradle documentation](https://docs.gradle.org/current/userguide/working_with_files.html#copying_files).

**Examples:**

Exclude BSP build artifacts or SDK caches:

```kotlin
developerPackage {
    srcContent {
        exclude("vee/my-port/bsp/sdk/*", "vee/my-port/bsp/vee/scripts/binaries")
    }
}
```

Rename template files (e.g., `settings.gradle.kts.tpl` → `settings.gradle.kts`):

```kotlin
developerPackage {
    srcContent {
        filesMatching("**/*.kts.tpl") {
            relativePath = relativePath.replaceLastName(
                relativePath.lastName.removeSuffix(".tpl")
            )
        }
    }
}
```

#### Add Extra Content to the Package

To bundle additional files or task outputs into the package ZIP, use `packageContent`.
The specified `CopySpec` actions operate on the **entire package zip** and can add files to any directory in the
archive, including existing sections like `src/` or `doc/`.

**Common operations:**

- **Add files** at a specific path: `into("bin/myTool") { from(fetchMyToolTask) }`
- **Add files** at the zip root: `from(rootProject.file("CHANGELOG.rst"))`

**Example:**

```kotlin
developerPackage {
    packageContent {
        into("bin/myTool") { from(fetchMyToolTask) }
        into("/") { from(rootProject.file("CHANGELOG.rst")) }
    }
}
```

#### Handling Duplicate Entries

When both `srcContent` and `packageContent` add files to the same path, the `package` task fails with:

```
Entry <src/some/file is a duplicate but no duplicate handling strategy has been set.
```

By default, Gradle's `Zip` task uses the `FAIL` strategy, which rejects any duplicate entry. To resolve this, set a
`duplicatesStrategy` in the `packageContent` block:

```kotlin
developerPackage {
    packageContent {
        duplicatesStrategy = DuplicatesStrategy.WARN // or EXCLUDE, INCLUDE
    }
}
```

Available strategies:

- `FAIL` (default): rejects duplicates — the build fails.
- `WARN`: keeps the first entry and logs a warning.
- `EXCLUDE`: keeps the first entry silently.
- `INCLUDE`: keeps both entries (last one wins in the zip).

#### Wire Custom Task Dependencies

If you register tasks that must run before packaging, you can wire to the `package` task:

```kotlin
tasks.named<Zip>("package") {
    dependsOn("myTask")
}
```

---
_Copyright 2025-2026 MicroEJ Corp. All rights reserved._
_MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms._
_Build: 7E4D1F7C_
