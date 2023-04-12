Windows Native Compilation
==========================

This dir contains the support files needed to compile a "native" EXE
version for Windows, and package it up into an installer.  Apocalyptech
does his development on Linux and then transfers the resulting files
over to Windows for this compilation process, so none of this is actually
integrated with the NetBeans `build.xml`, etc.

If anyone feels like doing the work to get it all integrated so it's
easier for native Windows users, feel free to send in a PR!

* [The Short Version](#the-short-version)
* [Compiling for Windows](#compiling-for-windows)
  * [Installing/Using Those Components](#installingusing-those-components)
  * [Vanilla GraalVM](#vanilla-graalvm)
* [Setting an EXE Icon](#setting-an-exe-icon)
* [Building the Installer](#building-the-installer)
* [Distribution](#distribution)

The Short Version
-----------------

Assuming you've already built the project via NetBeans and have an
`OpenBLCMM.jar` from the `BLCMM/store/` directory, transfer that jar
over to a Windows host and make sure you've got the following software
installed:

1. [Visual Studio](https://visualstudio.microsoft.com/) and its C++ components.
2. [Liberica NIK](https://bell-sw.com/pages/downloads/native-image-kit/#downloads).
3. [WinRun4J](https://winrun4j.sourceforge.net), if you want to set an EXE icon
4. [Inno Setup](https://jrsoftware.org/isinfo.php)

Then:

1. Start a "x64 Native Tools Command Prompt" shell from Visual Studio
2. If you want, run `native-agent-new.bat` or `native-agent-merge.bat` and
   interact with the app for awhile.  Compare the contents of `conf-dir`
   with the `META-INF/native-image` dir in sourcecode to see if anything
   new is required.  If so, add them and rebuild `OpenBLCMM.jar`.
    1. `native-agent-new.bat` creates a totally fresh `conf-dir` each time,
       so make sure to be careful about merging in with the originals, since
       the new one might be missing functionality if you didn't do a totally
       thorough runthrough.
    2. `native-agent-merge.bat` should *update* an existing `conf-dir` with
       new activity, which will probably make updates a little easier to
       deal with.
3. Run `native-compile.bat` (or just `native-image -jar OpenBLCMM.jar`) to
   compile `OpenBLCMM.exe`
    1. The batch file also uses WinRun4J to set an icon on the EXE itself,
       with `RCEDIT64.EXE /I OpenBLCMM.exe openblcmm.ico`.  If you want an
       icon but didn't use the batch file, be sure to do that.  The batch
       file also collects the EXE and all required DLLs into a `compiled`
       subdir.
4. Open `openblcmm.iss` in Inno Setup.  Update the version number if required
   and hit "Compile."  You'll end up with an `OpenBLCMM-<version>-Installer.exe`
   inside an `Output` dir.  That's the installer!

That's it!  Details of those steps follow, divided into the three main sections
(compiling, EXE icon, and then creating the installer).

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

Note that there's an alternate version of the tracing agent call which
will *merge* in an existing `conf-dir` with the newly-seen activity,
instead of starting fresh:

    java -agentlib:native-image-agent=config-merge-dir=conf-dir
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

Note that at the moment, Liberica/GraalVM doesn't seem to support setting a
custom EXE icon on the resulting file.  There are utilities available to
set that, but I didn't see anything that was also opensource, and since
the installer sets up shortcuts which *do* have icons, I'm just leaving it
with the default boring icon for now.

### Vanilla GraalVM

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

Setting an EXE Icon
-------------------

GraalVM/Liberica Native Image doesn't support setting a custom icon on the
compiled EXE, and it'd be nice to have.  When installing from an installer
(see the next section) it's not too important, since the shortcuts all have
icons, and the app sets its own runtime icons so it looks fine while running.
Someone looking at the main EXE directly, though, might appreciate having
the icon in place.

The main util that most folks seem to probably use for this kind of EXE
tweaking is [Resource Hacker](http://www.angusj.com/resourcehacker/), which
seems quite well-established and featureful, but it's not opensource, so I
didn't really want to use it myself.  Instead, I'm using [WinRun4J](https://winrun4j.sourceforge.net),
which includes an `RCEDIT64.EXE` utility for doing simple tweaks like this.

There's no actual installer for WinRun4J, so just unzip it somewhere and
slap the RCEDIT64 binary somewhere in your `%PATH%`.  Then once you have
the compiled EXE available, run:

    RCEDIT64.EXE /I OpenBLCMM.exe openblcmm.ico

That's it!

Building the Installer
----------------------

We're using the well-known [Inno Setup](https://jrsoftware.org/isinfo.php) to
create an installer for Windows.  At time of writing, we're using v6.2.2.
There's not really a lot to say about this step.  It's pretty straightforward
and we've not deviated much from the Inno Setup defaults (as populated by the
initial Wizard).

The config file is `openblcmm.iss`.  To build an installer, just open that
file in Inno Setup (you should be able to double-click on it) and hit Compile
or Build or whatever it is.  Once the process is done, there'll be an `Output`
directory containing your fresh installer.  When building up a new version,
make sure to update the version string in there as well.

Some custom changes inside `openblcmm.iss` that we've made:

* Removed the full paths that the wizard puts on all file paths, so it
  just uses files in the same dir as `openblcmm.iss`.
* Added the app version number to the output filename, plus the text `Installer`.
* Pre-install info text was taken from the "Credits" tab on our About dialog.
* Associated `.blcm` file extension with OpenBLCMM.
* Included an icon in the app install directory.
* Associated that icon with both the start menu and desktop shortcuts.

That's honestly about it!

Distribution
------------

The plan, at the moment, is to distribute *both* the installer EXE, and
also a zipfile of the app EXE+DLLs, though we'll see if that's too confusing
for users.  So we do still need to establish a process to zip up the
installerless components (in addition to zipping up the "raw" Java Jar
for folks who aren't using the Windows EXE version at all).

