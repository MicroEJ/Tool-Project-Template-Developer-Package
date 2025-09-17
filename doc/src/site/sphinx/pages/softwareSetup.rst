.. _software_setup:

Software Setup
==============

Forewords
---------

Unless stated otherwise, all directories mentioned in this documentation are relative to the directory where you extracted the Developer Package files on your file system. To simplify instructions, this directory will be referred to as ``$FRAMEWORK_DIR`` in the following sections. Please make sure to replace ``$FRAMEWORK_DIR`` in the following sections with the absolute path to the Developer Package on your file system.

On Windows, ``$FRAMEWORK_DIR`` must be as short as possible to avoid path-too-long issues.

Install MicroEJ SDK 6
---------------------

The installation of MicroEJ SDK 6 is required before proceeding with the following steps.

Please refer to the `installation chapter of the SDK 6 User Guide <https://docs.microej.com/en/latest/SDK6UserGuide/install.html>`_ before continuing.

Accept SDK EULA
---------------

The use of MICROEJ SDK 6 requires to accept the SDK EULA `<https://docs.microej.com/en/latest/SDK6UserGuide/licenses.html#sdk6-eula>`_.

Please refer to the official documentation `<https://docs.microej.com/en/latest/SDK6UserGuide/licenses.html#sdk-eula-acceptation>`_ for the SDK EULA acceptation.

Configure the Repositories
--------------------------

During the installation of MicroEJ SDK 6, Gradle has been configured to fetch dependencies from the `MicroEJ online repositories <https://docs.microej.com/en/latest/ApplicationDeveloperGuide/modulesRepositories.html>`__ (Central and Developer).

However, the <framework_name> relies on dependencies that are not available in these online repositories.
Therefore, the Gradle configuration must be modified to also resolve dependencies from the Offline Repository provided with the framework.

Follow the steps below to complete the setup:

1. Open the Gradle configuration file ``$USER_HOME/.gradle/init.d/microej.init.gradle.kts``,
2. Edit the configuration to add the Offline Repository to the list of known repositories:
   
	.. code-block:: kotlin

		allprojects {
			repositories {
				/* Local Repository */
				maven {
					name = "localRepository"
					url = uri("${userHome}/.microej/repository")
				}
				/* <framework_name> Offline Repository */
				ivy {
					name = "<framework_repository>"
					url = uri("$FRAMEWORK_DIR/repository")
					patternLayout {
						artifact("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier])(.[ext])")
						ivy("[organisation]/[module]/[revision]/ivy-[revision].xml")
						setM2compatible(true)
					}
				}

				// Other repositories omitted for brevity
			}
		}

	.. note::

		Make sure to place this Offline Repository in second position in the list, after the ``localRepository``.
		This ensures that it takes precedence over online repositories when resolving dependencies.

For more information about how to use Offline Repositories, please refer to our `official documentation <https://docs.microej.com/en/latest/SDK6UserGuide/tutorials/offlineRepository.html#use-an-offline-modules-repository>`__.

.. 
   | Copyright 2025 MicroEJ Corp. All rights reserved.
   | MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.