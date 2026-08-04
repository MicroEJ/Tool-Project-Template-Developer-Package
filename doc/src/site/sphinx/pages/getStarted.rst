.. _get_started:

Get Started
===========

Run the Virtual Device
----------------------

The Virtual Device is an executable that allows running, on the Simulator, the same Core Engine, libraries, and pre-installed Applications as on the real device.
The Virtual Device provided in this Developer Package contains the Applications and Kernel that demonstrate the <framework_name> Framework.

.. tabs::

   .. tab:: Windows

      Run ``bin/virtualDevice/<target>/launcher-windows.bat``

   .. tab:: Linux

      Run ``bin/virtualDevice/<target>/launcher-linux.sh``

   .. tab:: MacOS

      1. Install a JRE 11 from `Adoptium <https://adoptium.net/temurin/releases/>`_ or `Oracle <https://www.oracle.com/fr/java/technologies/downloads/>`_.
      2. Add execution rights to the launcher script: ``chmod +x bin/virtualDevice/<target>/launcher-macos.sh``
      3. Run ``bin/virtualDevice/<target>/launcher-macos.sh``

Replace ``<target>`` with the name of the kernel target you want to use.

Run on Simulator
----------------

Prerequisites
~~~~~~~~~~~~~

Complete the :ref:`Software Setup <software_setup>` instructions before continuing.

Run on Device
-------------

Prerequisites
~~~~~~~~~~~~~

Complete the :ref:`Hardware Setup <hardware_setup>` instructions before continuing.


.. 
   | Copyright 2025-2026 MicroEJ Corp. All rights reserved.
   | MicroEJ Corp. PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.