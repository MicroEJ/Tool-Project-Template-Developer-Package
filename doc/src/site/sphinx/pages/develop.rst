.. _develop:

Develop
=======

API
---

The API of the <framework_name> Framework libraries can be found in the `javadoc directory of this package <../../javadoc/index.html>`_.
The API of MicroEJ libraries and Abstraction Layers can be found `on the MicroEJ website <https://developer.microej.com/microej-apis/>`_.

Sequence Diagrams
-----------------

Below is a sequence diagram illustrating the data flow between the components:

.. mermaid::

   sequenceDiagram

      actor User
      participant Kernel
      participant Library as framework library
      participant App

Application Development
-----------------------

Application Developer Mode
~~~~~~~~~~~~~~~~~~~~~~~~~~

Application developers typically do not need to modify the Kernel or the VEE Port, at least initially.
For this reason, the package allows the Kernel to be loaded directly from the prebuilt binaries bundled with it, namely
the executable ``bin/executable/<kernel>/application.out`` and the Virtual Device located in ``bin/virtualDevice/<kernel>/``.
The system property ``kernel.name`` in ``src/gradle.properties`` selects which target to use.

Using prebuilt Kernel binaries lets you avoid installing a full Kernel development environment and reduces build times,
since rebuilding the Kernel is not required. This enables you to focus on application development without the added
complexity of managing Kernel and VEE Port sources.

You can enable or disable Application Developer Mode by setting the system property ``systemProp.app.developer.mode.enabled``
in ``src/gradle.properties``.

When the mode is enabled (default), the Kernel is loaded from the prebuilt binaries included in the package.
When the mode is disabled, the Kernel is built and loaded from the sources.
If the property is not explicitly set, Application Developer Mode is enabled by default.

This mode also controls which projects are visible in the IDE. When enabled, only application projects are loaded,
keeping the project tree clean and Gradle sync fast. When disabled, the Kernel, VEE Port, and all related source
projects are also loaded, giving full access to the platform sources.

Switching to Source-Based Development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When you are ready to modify the Kernel or the VEE Port, disable Application Developer Mode by setting the following
property in ``src/gradle.properties``:

.. code-block:: properties

   systemProp.app.developer.mode.enabled=false

After changing this property, re-sync the Gradle project in the IDE. The Kernel, VEE Port, and other source projects
will appear in the project tree, and builds will use the local sources instead of the prebuilt binaries.

Kernel Development
------------------

VEE Port Development
--------------------


.. 
   | Copyright 2025-2026 MicroEJ Corp. All rights reserved.
   | MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.