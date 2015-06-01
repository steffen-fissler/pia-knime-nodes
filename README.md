pia-knime-nodes
===============

This repository holds the declaration for KNIME nodes of PIA using 
[GenericKnimeNodes](https://github.com/genericworkflownodes/GenericKnimeNodes).

For more information about PIA please go to
https://github.com/mpc-bioinformatics/pia.


Download and Install
===

The recommended way to use the KNIME plugins is downloading them via the
"Install New Software" (in the Help-Menu). For this, you need to add the KNIME
trunk repository (http://update.knime.org/community-contributions/trunk) to the 
available software sites. For more information, please check the
[PIA wiki](https://github.com/mpc-bioinformatics/pia/wiki/Running-PIA-via-KNIME).

If for any reason you do not want to use the automatic installation, downloads
of the plugin are available with the [PIA releases](https://github.com/mpc-bioinformatics/pia/releases/latest).

Unzip the download and put the single jar file (de.mpc.pia_VERSION.jar) into the dropins folder of your KNIME installation. Do the same for your platform specific folder, e.g. on a Windows 64bit installation copy the folder "de.mpc.pia.win32.x86_64_VERSION" folder into the KNIME dropins folder. Now restart KNIME.

On the KNIME splash screen, you should now see the PIA logo and the nodes are in the "Community Nodes" section.

If not, some dependencies are missing. If so, the easiest way ist to also install the OpenMS-nodes, using KNIME's "Install new software..." under the "Help" section.
