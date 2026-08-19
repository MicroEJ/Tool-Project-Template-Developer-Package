# Overview

This repository contains a template project for creating a Customer Developer Package.

A Developer Package is an archive that provides all the components needed for end users—primarily developers—to explore
and get started with development with MicroEJ.

Users will typically find sources, documentation, a module repository, and more, as shown in the following directory
structure:

```
└── my-package-name
    ├── bin
    │   ├── executable/
    │   │   └── <target>/         # One per kernel target (e.g. linux-x86, rt1170)
    │   └── virtualDevice/
    │       └── <target>/         # One per kernel target
    ├── doc             # Documentation
    ├── repository      # Module repository
    ├── src             # Sources
    ├── CHANGELOG.md
    └── README.md
```

The goals of this template are to:

- Help developers bootstrap a new package,
- Provide the tools for building and validating a package,
- Showcase the recommended file structure for packages,
- Provide a documentation template.

# Usage

# Requirements

- [SDK 6](https://docs.microej.com/en/latest/SDK6UserGuide/install.html)
- Python 3.3 or later

## Setup

To create a new Developer Package project, start by cloning this repository. Then:

- Set the module name, group name and version in the root `gradle.properties`.
- Add your source projects under `src/` and configure `src/settings.gradle.kts` accordingly.
- Adapt the `README.md` file content, at least to replace the placeholder `<PRODUCT_NAME>`.

You can now refer to the [PACKAGING_DOC.md](./PACKAGING_DOC.md) file to learn how to structure the sources, add a custom
Module Repository, write tests for the package and configure the build.

---
_Copyright 2025-2026 MicroEJ Corp. All rights reserved._
_MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms._
_Build: 7E4D1F7C_
