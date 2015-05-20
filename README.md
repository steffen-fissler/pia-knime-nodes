pia-knime-nodes
===============

This repository holds the declaration for KNIME nodes of PIA using 
[GenericKnimeNodes](https://github.com/genericworkflownodes/GenericKnimeNodes).

For more information about PIA please go to
https://github.com/mpc-bioinformatics/pia.


Download and Install
===

Downloads of the plugin are available with the [PIA releases](https://github.com/mpc-bioinformatics/pia/releases/latest).

Unzip the download and put the single jar file (de.mpc.pia_VERSION.jar) into the dropins folder of your KNIME installation. Do the same for your platform specific folder, e.g. on a Windows 64bit installation copy the folder "de.mpc.pia.win32.x86_64_VERSION" folder into the KNIME dorpins folder.
On the KNIME splash scree, you should see the PIA logo and the nodes are in the "Community Nodes" section.

If not, some dependencies are missing. If so, the easiest way ist to also install the OpenMS-nodes, using KNIME's "Install new software..." under the "Help" section.
