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
* [Post-Compile Processing](#post-compile-processing)
  * [Setting an EXE Icon](#setting-an-exe-icon)
  * [Hiding cmd Window](#hiding-cmd-window)
  * [High-DPI Displays](#high-dpi-displays)
* [Building the Installer](#building-the-installer)

The Short Version
-----------------

Assuming you've already built the project via NetBeans and have an
`OpenBLCMM.jar` from the `store/` directory, transfer that jar
over to a Windows host and make sure you've got the following software
installed:

1. [Visual Studio](https://visualstudio.microsoft.com/) and its C++ components.
2. [Liberica NIK](https://bell-sw.com/pages/downloads/native-image-kit/#downloads).
3. [WinRun4J](https://winrun4j.sourceforge.net), if you want to set an EXE icon
    1. Note that you'll have to get `RCEDIT64.exe` into your `%PATH%` manually.
4. [Inno Setup](https://jrsoftware.org/isinfo.php)

Then:

1. Start a "x64 Native Tools Command Prompt" shell from Visual Studio
2. If you want, run `native-agent-new.bat` or `native-agent-merge.bat` and
   interact with the app for awhile.  Compare the contents of
   `src/META-INF/native-image/blcmm/blcmm` with the latest git HEAD,
   to see if anything new is required.  If so, add them and rebuild
   `OpenBLCMM.jar`.
    1. `native-agent-new.bat` creates a totally fresh config dir each time,
       so make sure to be careful about merging in with the originals, since
       the new one might be missing functionality if you didn't do a totally
       thorough runthrough.
    2. `native-agent-merge.bat` will *update* an existing config dir with
       new activity, which will probably make updates a little easier to
       deal with.
3. Run `native-compile.bat` (or just `native-image -jar OpenBLCMM.jar`) to
   compile `OpenBLCMM.exe`
    1. The batch file also uses WinRun4J to set an icon on the EXE itself,
       using `RCEDIT64.EXE /I OpenBLCMM.exe openblcmm.ico`.  If you want an
       icon but didn't use the batch file, be sure to do that.  The batch
       file also collects the EXE and all required DLLs into a `compiled`
       subdir.
4. Open `openblcmm.iss` in Inno Setup.  Update the version number if required
   and hit "Compile."  You'll end up with an `OpenBLCMM-<version>-Installer.exe`
   inside the `store/` dir.  That's the installer!

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
structure, and all its config, in `src/META-INF/native-image/blcmm/blcmm`.
The bundled batch files all refer to that inner directory when
specifying the config dir location.

Since we're building the Jarfile with those contents, the commandline
to compile gets simplified to:

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

Post-Compile Processing
-----------------------

There's a few things which we do to the application after it's been
compiled, to fix up a few oddities of the plain EXE.

### Setting an EXE Icon

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

### Hiding cmd Window

By default, AWT/Swing apps compiled with GraalVM launch a `cmd.exe`-like
terminal window when you launch the app, in addition to the main window
itself.  This is arguably useful because it gives the user a console output,
so you can see log entries as they get printed.  It's a bit messy from a
UI perspective, though, and most users would prefer without.  Fortunately,
the Windows command `EDITBIN` can be used to switch the EXE to the "Windows"
subsystem, which ends up preventing that window from opening.

[This Issue at GraalVM's github page](https://github.com/oracle/graal/issues/2256)
implies that someone had a problem where the app remained resident in memory
after closing, after having set that subsystem, but I haven't found that to
be the case with OpenBLCMM, at least.  So, we're going ahead and doing it,
which can be done like so:

    EDITBIN /SUBSYSTEM:WINDOWS OpenBLCMM.exe

### High-DPI Displays

If you run OpenBLCMM (or, presumably, any other Liberica NIK compiled AWT/Swing
app) on a display which has been scaled up (something common with 4k+ monitors,
for instance), you'll soon notice that the text is rather blurry.  That's because
the application hasn't been set as advertising itself as High-DPI capable, and
so Windows is doing some pretty basic scaling instead.  Java itself *is* quite
capable of high-DPI displays, though, so we just need to tell the EXE that.

That's done via [application manifests](https://learn.microsoft.com/en-us/windows/win32/sbscs/application-manifests),
and Microsoft even [has a page specifically about high-DPI settings](https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process).
It *is* possible to just distribute the compiled EXE along with an appropriate
`.manifest` file, but it's better to just bake the manifest into the EXE itself.
That can be done with Visual Studio's `MT.EXE`.  See, for instance, [this page
about doing so](https://www.ni.com/docs/en-US/bundle/labview/page/lvhowto/editing_app_manifests.html).

I put together an appropriate manifest for OpenBLCMM based on [a handy
StackOverflow thread](https://stackoverflow.com/questions/23551112/how-can-i-set-the-dpiaware-property-in-a-windows-application-manifest-to-per-mo)
and am using that.  It seems to work great!  Check out `OpenBLCMM.exe.manifest`
for the details, but it can be merged into the EXE with the following:

    MT.exe -manifest OpenBLCMM.exe.manifest -outputresource:OpenBLCMM.exe;#1

Note the extra `#1` after the semicolon at the end of the EXE name.  An EXE can
have more than one manifest -- that's telling `MT.exe` that this should be the
*first* manifest in the EXE.

Building the Installer
----------------------

We're using the well-known [Inno Setup](https://jrsoftware.org/isinfo.php) to
create an installer for Windows.  At time of writing, we're using v6.2.2.
There's not really a lot to say about this step.

The `openblcmm.iss` config file has been getting tweaked over time from the
defaults.  It's currently set up to be run "in place" from a git checkout of
the project -- the various file paths in the file are all relative links which
point to a few places in the repo.  It will output the installer into the main
`store` directory, which is also where it expects to find the compiled
`OpenBLCMM.exe`.

To build an installer, just open `openblcmm.iss` in Inno Setup (you should be
able to double-click on it), make sure that the version number stored in there
is appropriate, and hit the "compile" button.  Once the process is done, there
should be a new installer inside the `store` directory, alongside the original
`OpenBLCMM.exe`.

