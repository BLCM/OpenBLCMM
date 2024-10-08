Developing OpenBLCMM
====================

- [Development Environment](#development-environment)
  - [OE Datapacks](#oe-datapacks)
  - [Git Branch](#git-branch)
- [Unit Tests](#unit-tests)
- [Library Bundling](#library-bundling)
- [Compiling for Windows](#compiling-for-windows)
- [Compiling for Steam Deck](#compiling-for-steam-deck)
- [Packaging / Release Procedures](#packaging--release-procedures)
  - [Pre-Release Checks](#pre-release-checks)
  - [Actual Release](#actual-release)

Development Environment
-----------------------

OpenBLCMM was developed using [NetBeans](https://netbeans.apache.org/),
most recently with [NetBeans IDE 16](https://netbeans.apache.org/download/nb16/index.html).

The currently-checked-in project expects a NetBeans Java platform named
"`JDK_20`" -- you can get to that via NetBeans' `Tools -> Java Platforms`.
If you've already got a JDK 20 installed under a different name, you could
try renaming it, or alter the configured Java environment to suit.
Alternatively, you could create a new environment and just point it at
the same directory as your existing Java Environment.

Once the project's been opened up in Netbeans and the `JDK_20` platform is
available, you should be able to just run it via the GUI -- there'll be a
green "Play" icon in the main toolbar, or various menu options.

At time of writing, the most recent NetBeans IDE version is 17.  That
hasn't been packaged up for Apocalyptech's distro yet, so we haven't moved
to that new version.  NetBeans 16 is still available from its site, though.

### OE Datapacks

Ordinarily you'd install datapacks by putting them next to `OpenBLCMM.jar`
(or whatever compiled/packaged version is appropriate), but Netbeans doesn't
actually run from a built Jar.  Instead, to use the Object Explorer
datapacks while running the Netbeans version, simply store them inside the
main project checkout directory, and the app should find them when you next
startup.

The data used for OpenBLCMM is extracted and processed by [the DataDumper
PythonSDK project](https://github.com/BLCM/DataDumper).  Head over there if
you want to package up your own data, or feel like making some changes to
the datalib component of OpenBLCMM.

### Git Branch

Work on minor bugfix updates are likely to happen out in the main branch,
but development for future [major/minor updates](https://semver.org/) will
more likely happen on the `next` branch.  Flip over there if you want to
see what's being worked on (if anything).

Unit Tests
----------

OpenBLCMM has vanishingly few of these, alas.  The ones that we have probably
also veer closer to being more integration test than unit test, to boot.  At
time of writing, we've got some which cover the detection of overwritten code,
and some which cover the mod-statement parsing from the code edit window (ie:
turning user input into sanitized mod statements).

If you're touching either of those things, it'll be good to make sure to run
the unit tests we have, and even add some new ones if you're fixing bugs in
there or adding new functionality.  If anything *else* that you're working on
happens to be a good candidate for unit testing, too, more tests would certainly
be welcome.

The tests can be run right from Netbeans' "Run" menu.  Note that at the moment
there are two overwrite-check tests which we expect to fail, related to
[bug #20](https://github.com/BLCM/OpenBLCMM/issues/20).  If that bug's ever
fixed up, those two failures should clear themselves out.

Library Bundling
----------------

OpenBLCMM uses a few third-party libraries for various things, and I felt
it'd be nice to not have to distribute extra Jars along with the app jar
itself.  Eclipse apparently has a setting to create a "fat" Jar which
includes both the project classes and all third-party-lib classes in a
single Jar.  That's not "natively" supported by Netbeans' default `build.xml`,
but it's easy enough to hack it in there.

Various places online will recommend creating a brand new target for it,
but I decided to just make use of the `-post-jar` target.  The base Ant
config was taken from [this stackoverflow post](https://stackoverflow.com/questions/70526988/netbeans-how-to-create-an-executable-jar-file-with-all-libraries-and-source-fi)
but it's been modified pretty extensively since.  That composite-jar
build gets triggered automatically whenever the "jar" target is called,
so you shouldn't have to think about it.  The original "skinny" Jar
generated by Netbeans default remains inside the `dist/` directory
(and its third-party libs in `dist/lib/`), whereas the "fat" Jar gets
put inside `store/`.

Then for anyone wanting to use the "pure" Java version, the only file we
need to distribute is `OpenBLCMM.jar` from that `store` dir, though in
practice we'll also include at least a `.sh` and `.bat` for easier
launching.

Note that due to some filename mapping stuff we're doing to avoid
collisions in the combined Jarfile, if we ever add in new third-party
libraries, our `build.xml` will have to be updated to support the new
lib file.

Compiling for Windows
---------------------

See the [`windows-processing` directory](windows-processing/) for
information on compiling the "native" EXE version (and its installer)
for Windows.  The Packaging section below does go through many of those
same steps, too.

Compiling for Steam Deck
------------------------

See the [`steamos-processing` directory](steamos-processing/) for
information on compiling the native binary version, intended for use
on Steam Deck.  (The binary will likely work on most other Linux
systems too, but Linux users are encouraged to use the Pure Java
version.)

Packaging / Release Procedures
------------------------------

These are the steps that I'm currently running to get a new version
packaged and uploaded.  This assumes that the primary development work
is being done on Linux, and the Windows components are done inside
a Windows VM.  It's assumed that you have a full git checkout of the
project on the Windows side as well, though!

Also on the Windows and SteamOS side, make sure you've got the various
bits of software installed as detailed on the [Windows Processing
README](windows-processing/README.md) and [SteamOS Processing
README](steamos-processing/README.md).

The various batch files, shell scripts, and Inno Setup runs mentioned
here *should* be runnable right in-place.

### Pre-Release Checks

These are just some checks to be done before officially cutting a release.
The vast majority of this is just checking out the GraalVM/Liberica native
compilation on Windows, since that process is a bit finnicky.

1. Development is typically done using the most recently-supported Java verison, but it
   wouldn't hurt to make sure the project compiles cleanly on the *oldest*-supported
   Java version as well.  Switch the project properties and do a Clean-and-build to
   verify.  Don't forget to switch back and Clean-and-build again when done.
2. Build the project from Netbeans.  This will produce `store/OpenBLCMM.jar`
   at the end.
3. Double-check running that Jar natively (`java -jar OpenBLCMM.jar`)
4. Depending on what's changed, doublecheck that the GraalVM/Liberica Native Image
   configs are still sufficient:
    1. Transfer `store/OpenBLCMM.jar` to a Windows VM (also in the `store/` dir).
    2. On the Windows VM, run `native-agent-merge.bat` (inside the `windows-processing`
       directory), and interact with the new parts of the app.
    3. Once done, compare the contents of the files in `src/META-INF/native-image/blcmm/blcmm`
       to the current git HEAD.  If there are any new entries, add them in, and
       rebuild the project (to get a new `store/OpenBLCMM.jar`).  Re-transfer the
       updated `store/OpenBLCMM.jar` to Windows so you've got the changes inside
       the Jar.
    4. Compile to EXE using `native-compile.bat` (inside the `windows-processing`
       directory).
    5. Double-click the new `store/OpenBLCMM.exe` and interact with the new parts of the
       app.  So long as there's no crashing, it should theoretically be good.
5. Do the same thing on a SteamOS VM (or even a real Steam Deck):
    1. Transfer `store/OpenBLCMM.jar` to SteamOS, also in the `store/` dir).
    2. On SteamOS, run `native-agent.sh` (inside the `steamos-processing`
       directory) to run the agent.  Interact with it.
    3. Once done, compare `src/META-INF/native-image/blcmm/blcmm` and commit if needed.
       Rebuild the project if there were and re-transfer `store/OpenBLCMM.jar`
    4. Compile to native binary using `native-compile.sh` (inside the
       `steamos-processing` directory).
    5. Run `store/steamos/OpenBLCMM` to doublecheck, though note that the compiled
       version might require more CPU flags than the VM supports, if you're running
       SteamOS in a VM.  In that case, can just try running it on a Linux desktop.

### Actual Release

Once we're sure that the compiled Windows EXE + SteamOS builds work fine, we can
proceed.  Some of these steps are redundant if you've just gone through the full
pre-release check
section.

1. Make sure that `src/blcmm/Meta.java` and `windows-processing/openblcmm.iss`
   have the new version number.
2. "Clean and Build" the project from Netbeans.  This will produce
   `store/OpenBLCMM.jar` at the end.
    1. Do one more quick spot-check that the built Jar works (`java -jar OpenBLCMM.jar`)
3. Tag the release in git.
    1. Push the tag with `git push --tags`
4. Transfer `store/OpenBLCMM.jar` to Windows VM (also in the `store` directory)
    1. On Windows VM, start a Visual Studio "x64 Native Tools Command Prompt" and
       compile with `native-compile.bat` (inside the `windows-processing` directory).
        1. Creates `OpenBLCMM.exe` and a number of required DLLs in the same dir, and
           also copies those files into a `compiled` directory.
        2. Do a quick spot-check that the compiled `OpenBLCMM.exe` launches fine
    2. On Windows VM, open `openblcmm.iss` with Inno Setup and click on "Compile"
        1. Creates `store/OpenBLCMM-<version>-Installer.exe`
        2. Double-check that the installer works properly, and that the installed
           version launches fine.
    3. Transfer back from the Windows VM, into `store`:
        1. The `store/compiled` directory
        2. `store/OpenBLCMM-<version>-Installer.exe`
5. Transfer `store/OpenBLCMM.jar` to SteamOS VM or Steam Deck (also in the `store`
   directory)
    1. On SteamOS, compile with `native-compile.sh` (inside the `steamos-processing`
       directory).
        1. Creates `steamos/OpenBLCMM` and a number of required `.so` libraries in
           the same dir.
        2. Do a quick spot-check that the compiled version launches fine.
    2. Transfer the `store/steamos` directory back to the build host.
6. Back on Linux, run `release-processing/finish-release.py`
    1. This will create `store/OpenBLCMM-<version>-Windows.zip`,
       `store/OpenBLCMM-<version>-SteamDeck.tgz`, and a bunch of
       `store/OpenBLCMM-<version>-Java-<OS>.zip` files
    2. Doublecheck the contents of those.  The "Windows" one should have the
       EXE, ten DLLs, and README+LICENSE+CHANGELOG files.  The "SteamDeck" one
       should have the binary, ten `.so` files, and the README+LICENSE+CHANGELOG.
       The various "Java" ones should have the Jarfile, README+LICENSE+CHANGELOG,
       and whatever OS-specific launch scripts are necessary.
7. Create a new github release and upload all six packaged releases:
    1. `store/OpenBLCMM-<version>-Installer.exe`
    2. `store/OpenBLCMM-<version>-Windows.zip`
    3. `store/OpenBLCMM-<version>-Java-Windows.zip`
    4. `store/OpenBLCMM-<version>-Java-Linux.zip`
    5. `store/OpenBLCMM-<version>-Java-Mac.zip`
    6. `store/OpenBLCMM-<version>-SteamDeck.zip`
8. Update `openblcmm-latest.txt` wherever that ends up living For Real.  This is
    what will make existing OpenBLCMM installations report that a new version is
    available.

