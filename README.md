# Overview

This repository contains the project to build the <PRODUCT_NAME> Developer Package.

A Developer Package is an archive that provides all the components needed for end users (primarily developers) to
explore and get started with development with <PRODUCT_NAME>. It is structured as follows:

```
└── my-product-name
    ├── bin
    │   ├── executable/
    │   │   └── <target>/     # One per kernel target
    │   └── virtualDevice/
    │       └── <target>/     # One per kernel target
    ├── doc             # Documentation
    ├── javadoc         # Javadoc
    ├── repository      # Module repository
    ├── src             # Sources
    ├── CHANGELOG.md
    └── README.md
```

# Usage

## Build

To build the package, run the following command: `./gradlew package`.

When the build is successful, the built package can be found in `packaging/build/package/`.

Go through the following sections depending on what the package should contain.

## Sources

All sources files located in the `src` folder of the project are automatically packaged in the `src` folder of the
package.

The folder structure for the `src` directory is the following:

```
└── src
    ├── app
    ├── buildSrc                # Convention plugins and build utilities
    └── gradle
        ├── wrapper
    └── vee
        ├── kernel              # Kernel sources (shared across targets)
        └── <target>            # VEE Port sources (one per target)
    gradlew
    gradlew.bat
    settings.gradle.kts
```

## Tests

A testsuite is set up in `packaging/src/test`. The main purpose of this testsuite is to verify that the package is
valid, more specifically:

- sources bundled in the package operate as expected (code compiles in the target environment, application builds and
  runs on the target device, etc.).
- package content is correct (file structure, sanity checks, etc.)

The testsuite checks the following conditions:

- the application runs on the Simulator (runs the `runOnSimulator` task on the project `src/app`).
- the Kernel executable is built correctly (runs the `buildExecutable` task on the project `src/vee/kernel`).
- the Feature of the application is built correctly (runs the `buildFeature` task on the project `src/app`).
- the Kernel executable can run on the target device (runs `runOnDevice` task on the project `src/vee/kernel`).
- the Feature can be deployed on the target device (runs the `featureDeploy` task on the project `src/app`).
- the package file structure is correct.
- the files of the package do not contain any unwanted terms (client names, wrong licenses, illicit terms, etc.).

You can launch the testsuite by running the `test` Gradle task: `./gradlew test`.

For running a specific test, filter the test with `--tests`, for example:

    .\gradlew test --tests BuildRunDeployTest.testKernelBuildExecutable

## Developer Package Gradle Plugin

This packaging project uses the Gradle plugin `com.microej.gradle.plugin.developer-packaging` (defined in `buildSrc`)
which provides a set of features to ease its creation and development.
Refer to the [PACKAGING_DOC.md](./PACKAGING_DOC.md) to learn more on all these features.

# Requirements

- JDK 17 or later
- [SDK 6](https://docs.microej.com/en/latest/SDK6UserGuide/install.html)
- Python 3.3 or later

# Dependencies

_All dependencies are retrieved transitively by Gradle_.

# Source

N/A.

# Restrictions

None.

---
_Copyright 2025-2026 MicroEJ Corp. All rights reserved._
_MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms._
_Build: 7E4D1F7C_
