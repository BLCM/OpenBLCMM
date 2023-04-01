This directory contains the [GraalVM native image build configuration](https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/)
that is used for BLCMM.  This is what turns the Java code into a fully-compiled
EXE for use on Windows systems.

At time of writing (April 2023), we're actually using [Liberica Native Image Kit](https://bell-sw.com/liberica-native-image-kit/)
instead of GraalVM itself to do the build, because vanilla GraalVM doesn't
actually support Swing/AWT apps yet, which is what BLCMM is.  Liberica, however,
does.

See README-developing.md out in the repository root for more information on
this process.
