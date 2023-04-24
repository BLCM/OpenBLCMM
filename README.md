OpenBLCMM
---------

OpenBLCMM is a fully-opensource fork of the
[Borderlands Community Mod Manager](https://borderlandsmodding.com/running-mods/#managing-text-based-mods-starting-blcmm),
which is the tool for managing text-based mods for Borderlands 2 and
The Pre-Sequel.  BLCMM was written by LightChaosman with some contributions
from other community members and officially released in 2018.  In 2022,
LightChaosman opensourced the ["core" BLCMM code on github](https://github.com/LightChaosman/blcmm).
In 2023, OpenBLCMM development was started to fix up some longstanding
issues with BLCMM, and to provide a fully-opensource version of the entire
BLCMM application.

Download / Install
==================

OpenBLCMM hasn't been officially publicly released yet, but once it is,
downloads can be found here:

- **OpenBLCMM Releases:** https://github.com/BLCM/OpenBLCMM/releases
- **Object Explorer Datapacks:** *tbd - Temporarily at [this Github
  link](https://github.com/apocalyptech/OpenBLCMM-Data-TestBed/releases)*

**NOTE:** OpenBLCMM is *not* compatible with the original BLCMM game data
packages.  New prepackaged data files are available at the link above.  To
use those datapacks, download and save them inside the same directory as
`OpenBLCMM.exe`/`OpenBLCMM.jar`/`OpenBLCMM.app`.  OpenBLCMM should see the
data on the next startup and have them available for use.

Changelog
=========

A Changelog can be found at [src/CHANGELOG.md](src/CHANGELOG.md).

Commandline Arguments
=====================

There are a few commandline arguments which can be used with OpenBLCMM.  Most
users can safely ignore these.  These args can be specified from `cmd.exe`
or your usual shell, or added to GUI shortcuts.

### Creator Mode

Creator Mode is mostly just intended for people actively developing OpenBLCMM,
and is enabled by default when run via the Netbeans IDE.  It can be toggled on
with the `-creator` argument, such as:

    java -jar OpenBLCMM.jar -creator

The effects of Creator Mode are:

- Will use the current directory to store preferences, logfiles, backup files,
  etc (instead of using `%LOCALAPPDATA%` or `~/.local/share`, for instance)
- Does not do new-version checks while starting
- Will not attempt to import original BLCMM settings on first startup

### Setting User/Install Directory

These settings are similar in nature, and mostly just put into the app to
support some peculiarities of the Mac `.app` bundle.  They're technically
available for anyone else as well, though.  the `-userdir=<foo>` argument
will set the current "user directory," which is ordinarily only really used to
provide shortcut buttons in the File dialogs.  If you have Creator Mode active,
this will also determine where the app's prefs/logfiles/backups are stored.
It can be enabled like so:

    java -jar OpenBLCMM.jar -userdir=dirname

The `-installdir=<foo>` argument overrides the app's understanding of where
its Jarfile is, and is mostly just used to figure out where to look for Object
Explorer Datapacks.  It can be enabled like so:

    java -jar OpenBLCMM.jar -installdir=dirname

The Mac `.app` bundle makes use of both of these arguments, since the Jarfile
lives inside a directory which is ordinarily hidden from Mac users.  So, the
Mac bundle ends up launching OpenBLCMM like so:

    java -jar OpenBLCMM.jar -userdir=.. -installdir=..

### Opening Mod Files

You can also specify a filename at the end of the argument list to have
OpenBLCMM open up that file initially.  This *should* allow you to drag mod
files on top of the app icon, too, when interacting via a GUI.

    java -jar OpenBLCMM.jar patch.txt

If you want to open a mod file whose filename starts with a dash, you'll have
to do the usual UNIX/Linux convention of first putting a double-dash in front
of the filename.  Any argument after that double-dash will be interpreted as
a filename, not another argument:

    java -jar OpenBLCMM.jar -- -a_mod_file_with_a_dash.blcm

Development
===========

We've got a [document with notes about developing OpenBLCMM](README-developing.md)
if you'd like to help out, want to build it yourself, or are just curious.
Enjoy!

TODO
====

- [TODO items which I hope to sort out in the near-term](https://github.com/BLCM/OpenBLCMM/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement+-label%3A%22needs+investigation%22+)
- [TODO items which are a bit more long-term, and might not happen](https://github.com/BLCM/OpenBLCMM/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement+label%3A%22needs+investigation%22+)
- [Current bugs](https://github.com/BLCM/OpenBLCMM/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

Contributions
=============

The Borderlands Community Mod Manager was developed by LightChaosman, and
the majority of the code in this project remains his!  Many thanks to him
for opensourcing the core BLCMM code in 2022.  Apocalyptech's taken the recent
lead in 2023 of building up the OpenBLCMM fork.  Other contributors to both
BLCMM and OpenBLCMM throughout the years have included: apocalyptech, apple1417,
Bugworm, c0dycode, FromDarkHell, and ZetaDæmon.  Thanks, too, to the countless
members of the community who have contributed by testing, providing support,
and spreading the word.  Apologies to anyone we've missed!

Third-Party Content
===================

OpenBLCMM makes use of the following third party libraries/resources:

- **Java Libraries:**
  - [StidOfficial](https://github.com/StidOfficial)'s [SteamVDF library](https://github.com/StidOfficial/SteamVDF)
    for some Steam data parsing, available under the GPLv3.
  - [Apache Commons Text](https://commons.apache.org/proper/commons-text/) and
    [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/), available
    under the Apache License v2.0.
  - [Xerial](https://github.com/Xerial)'s [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc),
    available under the Apache License v2.0.
  - [Vincent Durmont](https://github.com/vdurmont)'s [semver4j](https://github.com/vdurmont/semver4j),
    available under the MIT License.
  - [CommonMark](https://github.com/commonmark)'s [commonmark-java](https://github.com/commonmark/commonmark-java),
    available under the 2-clause BSD License.
- **Resources**:
  - Some icons from [Dave Gandy](http://www.flaticon.com/authors/dave-gandy)'s
    [Font Awesome set](http://www.flaticon.com/packs/font-awesome), available under
    CC BY 3.0.
  - An icon from [Fathema Khanom](https://www.flaticon.com/authors/fathema-khanom)'s
    [User Interface set](https://www.flaticon.com/packs/user-interface-2899), available
    under [Flaticon's Free License](https://www.flaticon.com/free-icons/ui).
  - An icon from [Smashicons](https://www.flaticon.com/authors/smashicons)'
    [Essential Collection set](https://www.flaticon.com/packs/essential-collection),
    available under [Flaticon's Free License](https://www.flaticon.com/free-icons/ui).
- **Toolset**:
  - [Apache Netbeans](https://netbeans.apache.org/) is the development environment
  - [GraalVM Native Image](https://www.graalvm.org/22.0/reference-manual/native-image/) /
    [Liberica NIK](https://bell-sw.com/liberica-native-image-kit/) provides
    Windows EXE compilation
  - [Visual Studio](https://visualstudio.microsoft.com/) provides the C++
    compiler for GraalVM/Liberica
  - [Winrun4j](https://github.com/poidasmith/winrun4j) provides a utility to
    set icons on Windows EXEs
  - [Inno Setup](https://jrsoftware.org/isinfo.php) is used to create the
    Windows installer

License
=======

OpenBLCMM is licensed under the [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.en.html).
A copy can be found at [LICENSE.txt](LICENSE.txt).

