Developing OpenBLCMM
--------------------

*(this is gonna be real barebones for awhile)*

OpenBLCMM was developed using NetBeans, most recently with NetBeans IDE 16.

The currently-checked-in project expects a NetBeans Java platform named
"`JDK_19`" -- you can get to that via NetBeans' `Tools -> Java Platforms`.
If you've already got a JDK 19 installed under a different name, you could
try renaming it, or alter the configured Java environment to suit.
Alternatively, you could create a new environment and just point it at
the same directory as your existing Java Environment.

Once the project's been opened up in Netbeans and the `JDK_19` platform is
available, you should be able to just run it via the GUI -- there'll be a
green "Play" icon in the main toolbar, or various menu options.

Compiling for Windows
---------------------

[GraalVM](https://www.graalvm.org/) is a high-performance JDK which has
the additional feature of being able to compile Java code to run as
native binaries, via its [Native Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
functionality.

At time of writing (April 2023), GraalVM's Native Image [doesn't yet
support Swing/AWT applications for Windows](https://github.com/oracle/graal/issues/3084),
which is the GUI toolkit that BLCMM uses.  Fortunately, there's a
sort-of fork called [Liberica](https://bell-sw.com/libericajdk/) which
also has their own [Liberica Native Image Kit](https://bell-sw.com/liberica-native-image-kit/),
and *that* fork seems to work just fine with BLCMM's code.  So, that's
what we're using.

There's one slight wrinkle to the compilation process, which is that
neither GraalVM nor Liberica will be able to properly compile the
project *just* by looking at sourcecode -- they need to be able to
*watch* the app running, as a user interacts with it, in order to
detect and handle all the dynamic loading that Java does behind the
scenes.  So, you first need to launch the app inside a "tracing
agent", click around on everything you can think of, and the agent
generates some config files with its collected data.  *Then* you run
the compilation process using those config files, and you get an EXE
with some associated DLLs, and you should be good to go.

[GraalVM's docs on the Tracing Agent](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/)
are a good place to look, as is [this Liberica blog post for the
22.2.0 and 21.3.3 release](https://bell-sw.com/announcements/2022/08/05/liberica-native-image-kit-22-2-0-and-21-3-3-builds-are-out/).
There's also some [good docs on the build configuration files themselves](https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/)
on GraalVM's site.

At its most straightforward, what you do is grab a built `OpenBLCMM.jar`
and then start the tracing agent with:

    java -agentlib:native-image-agent=config-output-dir=conf-dir
        -jar OpenBLCMM.jar

... and then after you've clicked around the app as much as possible
*(I should probably make a checklist)*, quit the app and then kick off
the compilation with:

    native-image -Djava.awt.headless=false
        -H:ReflectionConfigurationFiles=conf-dir/reflect-config.json
        -H:ResourceConfigurationFiles=conf-dir/resource-config.json
        -H:JNIConfigurationFiles=conf-dir/jni-config.json
        -jar OpenBLCMM.jar

Having to specify all those arguments to the `native-image` call isn't
super ideal (though it's easy enough to wrap up in a batch script),
but there's an easier way to do it: we can include those files in
the `OpenBLCMM.jar` file itself, under `META-INF/native-image`, along
with a `native-image.properties` file to describe the options in
there.  (Those "build configuration" docs linked above go into all
the detail on that.)  So, that's what we're doing -- you'll find that
structure, and all its config, in `BLCMM/src`.  Since we're building
the Jarfile with those contents, the commandline to compile gets
simplified to:

    native-image -jar OpenBLCMM.jar

As OpenBLCMM gets updated and changed over time, it'll be a good
idea to re-run that tracing agent to ensure that the necessary config
hasn't changed in the meantime.

Once the compilation process is done, you should have an `OpenBLCMM.exe`
and a collection of DLL files (ten of them, currently).  These can be
zipped up and distributed!

### Installing/Using Those Components

This is pretty straightforward.  Note that GraalVM/Liberica Native Image
doesn't support cross-compiling, so to create an EXE you'll have to be
on Windows.

1. Install [Visual Studio](https://visualstudio.microsoft.com/) -- at time
   of writing, the latest version was Community 2022, which is what was
   used to build the initial OpenBLCMM versions.
    1. Make sure you've installed the C++ component as well.  At time of
       writing, I'd installed "Desktop development with C++", available
       via `Tools -> Get Tools and Features` if you don't already have it.
2. Install [Liberica NIK](https://bell-sw.com/pages/downloads/native-image-kit/#downloads).
   We're using "NIK 22 (JDK 17)" at time of writing.  (You don't actually
   need the "regular" Liberica JDK package once you have that NIK package
   installed.)
3. Launch the Visual Studio `cmd.exe` shortcut "x64 Native Tools Command Prompt"
   in the start menu.
    1. If you do an `echo %PATH%` from that prompt, you should see a
       bunch of Visual Studio stuff, and also a Liberica NIK path as well.
    2. Make sure that running `cl` reports something like `Microsoft (R) C/C++
       Optimizing Compiler`
    3. Make sure that running `java -version` reports something like
       `OpenJDK Runtime Environment GraalVM`
    4. Make sure that running `native-image --version` reports something
       like `GraalVM 22.3.1 Java 17 CE`

At that point you're good to go - make sure to run the various commands
from inside that Visual Studio-wrapped prompt.

Once "vanilla" GraalVM supports compiling Swing/AWT apps properly on
Windows, it's possible we may move over to that instead.  The install
procedure for GraalVM is a bit different -- you [grab the relevant
zipfile from GraalVM's github releases page](https://github.com/graalvm/graalvm-ce-builds/releases)
and unzip it wherever you like.  Then you manually set the `PATH` and
`JAVA_HOME` env variables to point into the unzipped location.  At that
point you should be able to use their [GrallVM
Updater/`gu` utility](https://www.graalvm.org/22.3/reference-manual/graalvm-updater/)
to install the `native-image` component:

    > gu available
    > gu install native-image

Visual Studio would still be required, though, and you'd need to kick
off the processes via that "x64 Native Tools Command Prompt."

