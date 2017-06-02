This is the node.js version modified to be embedded into Folding@Home applications.

The following changes are needed and provided through patch files located in patches/ dir:

1 - Create a hardened mode to block a set of functions from being executed when the process is started with '--harden' argument
2 - Add PIE mode when building for Android
3 - Applies a fix to make network requests possible on Android
4 - Add native support for OpenMM and x509
    4.1 - Copy 'x509/', 'openmm/' and 'nan/' dir into the node's 'deps/' dir
    4.2 - Copy both 'x509/x509.js' and 'openmm/openmm.js' into node's 'lib/' dir
5 - Applies a fix to make possible to build node+openmm on MacOSX

