# Overview

Project Template for a Customer Developer Package.
This template comes with the following capabilities:
- fetch and package an offline repository.
- copy and package projects sources.

# Usage

To create a Developer Package Project, clone this project with::
```shell
git clone git@gitlab.cross:M0090_IDE/M0090_Customer-Packaging-Template.git
```

Then go through the following sections depending on what the package should contain.

## Offline repository

To add an Offline Repository in the Developer Package, declare a dependency in the `packaging/build.gradle.kts` file 
with the `offlineRepository` configuration:
```kotlin
"offlineRepository"("com.mycompany:my-repository:1.0.0@zip")
```

The Offline Repository must be available in a repository.

## Sources

All sources files located in the `src` folder are automatically packaged in the `src` folder.

### Example1: “modules at the root of /src folder” (sources are not linked together)

```
└── src
    ├── framework
    ├── kernel
    ├── vee-port
        ├── mimxrt1170-evkb-vee-port
        ├── imx93-evk-vee-port
        └── …
    └── app
```

### Example2 (preferred): “multi modules projects at the root of /src folder” (sources are preconfigured to work together)

```

└── src
    ├── app
    └── vee
        ├── kernel
        ├── vee-port
        ├── runtime-api
        └── framework
```

## Binaries

Generated binaries are located in the `bin` folder.
Kernel binaries must be in `.elf` format (not `.out`) to ensure compatibility with flash tools.

## Documentation

All sources files located in the `doc` folder are automatically packaged in the `doc` folder in HTML format.

This documentation is a base for any developer package. Replace the following words:

- `<framework_name>`: the framework name (e.g. VEE Wear)
- `<target_market>`: the targeted market (e.g. smart watches)
- `<target_hardware>`: the supported hardware(s) (e.g. Actions ATS3085S)
- `<framework_repository>`: the name of the repository (e.g. wearRepository)

To build only the documentation, run ``buildDoc`` task.

### Diagrams with mermaid

You can embed diagrams with the ``::mermaid`` sphinx instruction.

If you want to get a PNG or SVG output instead of embedded HTML, change ``mermaid_output_format`` in ``conf.py`` file.
If you have an error about playwright during diagrams rendering in SVG or PNG, run this command in a terminal: ``playwright install``. This currently works locally but not on CI.

### Introduction

In this section, add the following diagrams:

- High-level architecture diagram (system): Describe the software stacks. Add details and links in the explanation below the scheme.
- High-level functional diagram: Describe the typical data flow. Make several schemes if necessary for the different libraries.

### Software Setup

In this section, add the software setup instructions that are specific to the framework.

### Hardware Setup

In this section, list the supported hardware and the setup instructions.

Use [This tool](https://gitlab.cross/M0127_CustomerCare/M0127_Documentation-Tools) to generate board images with annotations.

### Get Started

In this section, explain how to use the virtual device, build and run the demo applications on the simulator and on the device.

### Develop

In this section, explain how to use the framework in a custom application.

## Resources

All files located in the `resources` folder are automatically packaged in the root folder.
The template contains the `README.md` and `CHANGELOG.md` files as example.

## Tests

A testsuite is declared in the `packaging` project. This testsuite executes the following tests:

- execute the `buildExecutable` task on the project `src/kernel` (adapt the path to your project).
- execute the `buildFeature` task on the project `src/app` (adapt the path to your project).

The tests are disabled by default. To enable them, remove the `@Disabled` annotation from the test class.

# Requirements

N/A.

# Dependencies

_All dependencies are retrieved transitively by Gradle_.

# Source

N/A.

# Restrictions

None.

---
_Copyright 2025 MicroEJ Corp. All rights reserved._
_MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms._
