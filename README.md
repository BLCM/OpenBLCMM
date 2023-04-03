OpenBLCMM
---------

OpenBLCMM is a fully-opensource fork of the
[Borderlands Community Mod Manager](https://borderlandsmodding.com/running-mods/#managing-text-based-mods-starting-blcmm),
which is the tool for managing text-based mods for Borderlands 2 and
The Pre-Sequel.  BLCMM was written by LightChaosman with some contributions
from other community members and officially released in 2018.  In 2022,
LightChaosman opensourced the ["core" BLCMM code on github](https://github.com/LightChaosman/blcmm).
In 2023 this project was started to fix up some longstanding issues
with BLCMM, and provide a fully-opensource version of the entire BLCMM
application.

This fork is still in development but is proceeding along nicely.  It
intentionally omits a few features from the original BLCMM app -- perhaps
most notably, it no longer provides any hex-editing functionality, since
[PythonSDK](https://borderlandsmodding.com/sdk-mods/) is the current
recommended method for doing so.  As it stands, OpenBLCMM has not really
been tested outside of its development environment, and isn't really ready
for public use.  Check out our [short notes on its development environment](README-developing.md)
if you'd like to help out with development.

**NOTE:** This branch is *not* compatible with the original BLCMM game data
packages.  Prepackaged data files should be available for now at [this Google
Drive link](https://drive.google.com/drive/folders/1ssqbAIGTm2xZvhQPizqnrlWsez9ba9Bw?usp=share_link)
(that is not the final location of the datafiles -- once we're closer to
a public release, they'll be put somewhere more official).  To use those data
packages while running inside Netbeans, download them and save them inside the
checked-out `BLCMM` directory.  The app should see them on startup and extract
the sqlite database to an `extracted-data` directory.  At the moment there's no
real indication that that's happening apart from log entries -- once we have a
launcher in place, that should get that sorted out a bit better.

The generation scripts for the new data can be found in the [DataDumper
PythonSDK mod](https://github.com/BLCM/DataDumper).  Note that the data
format may change without warning for awhile yet, as the new version
approaches release!

Changelog
=========

A Changelog can be found at [BLCMM/src/CHANGELOG.md](BLCMM/src/CHANGELOG.md).

Hex Edits
=========

As mentioned above, OpenBLCMM no longer provides hex-editing functionality,
since [PythonSDK](https://borderlandsmodding.com/sdk-mods/) is the
community-recommended method for doing so.  We do have a document here for
all the hex edits we were aware of:

* [README-hexedits.md](README-hexedits.md)

Not all the edits in that doc were accessible via BLCMM.  That info might be
moved to a separate wiki or documentation area in the future!

TODO
====

### Immediate (would like to get done before public release)

- GUI notification of sqlite extraction/verification, when appropriate
- Figure out a Windows installer (associate `.blcm` extension?)
- AoDK support (both in OpenBLCMM itself and OE)
- New-version notification in-app
- Don't include hotfix "name" in hotfix keys (or at least strip to alphanumeric)?
- Memory monitor in main window, when in Creator Mode?
- About dialog improvements:
  - Link/button to github
  - Report Java version
  - Report data versions
  - Report memory usage + config?  Though if we have a memory counter on
    the main window then maybe that's beside the point
- Figure out opening SQLite DBs in read-only mode
- May as well cache *all* our PreparedStatements in DataManager...
- Apparently multi-selections screw with statement order in the Edit window?
- Improve "basic" attr-name autocomplete to restrict results to appropriate
  field names
- Options for changing object-opening behavior (ctrl/alt/shift-click, single/
  double/triple/whatever).  Also an option to disable links altogether?
- Make sure we handle launching from a readonly filesystem properly
  (original BLCMM launcher crashes when that happens; main app might too)
- Add in proper version checking in `.blcm` loading.
- Make sure that data jars can be found when launching from some other dir
- Feedback to user when extracting/verifying sqlite (that whole Thing might
  just get moved over into the launcher anyway, but we'll see)
- Check diskspace prior to sqlite extraction
- "No results found" when refs doesn't return anything, in OE
- Get a real Markdown renderer for the Changelog window?
- Figure out what the proper default-file-open location should be for the
  various cases, and make sure we're handling those properly.
- I don't think any settings actually require app restarts; maybe at least
  hide that stuff, for now?
- Try to trigger out-of-memory and make sure that the reporting for that
  works well enough (and that the messages there make sense).  Also maybe
  provide a clickable button/link to the Github Issues page.
  - Maybe add that into the crash report window in general, yeah?  Check
    `MyExceptionHandler` in `Startup.java`
- Get rid of those annoying registry execeptions when running on Windows
  (on a host which doesn't have the game installed, presumably)
- Settings migration from vanilla BLCMM
- Doublecheck username suppression in logs:
  - INI files
  - "installation can be found"
  - elsewhere...
- Needed testing:
  - Doublecheck all `*Action` functions, post-struct-and-dev-mode-removal
  - Doublecheck file saving -- there was a lot of autoexec stuff in there
    which got ripped out.
  - BPD index/length.  Check Fragile Minecraft Blocks for an easy test.
  - BPD number converter applet thing (surprisingly, the BPD stuff looks
    like it might actually be correct right out the gate.  Do some more
    thorough testing before getting rid of this line, though)
  - Wouldn't hurt to test out some more failure conditions in the map-merging
    code.  Incomplete/cut-off statements, etc.
  - Test out various scenarios relating to data availability; make sure that
    we can't NPE in OE, etc.
  - Test out various failure situations on the new datalib packing (new
    versions, version mismatches, min/max dbver restrictions, checksum failures,
    mtime updates, etc)
  - Do at least a bit of testing in all currently-supported Java versions
  - We decode dump data using `ISO_8859_1` -- search through the data to find
    out if there's edge cases where that'll fail.
  - Uninstalls?  I actually think that the uninstall function does nothing
    right now; looks like it's probably the launcher which used to do that.

### Can probably wait until after the first public release

- Convert StringTable to use a CSV library
  - Will need some one-time conversion stuff in there, and the ability to
    discern between a converted version.
- Get a Netbeansless build system working (just ant from CLI, or I guess
  Maven might be the currently-recommended thing?).  I'm (apoc) still
  feeling pretty rusty in Java, so having the IDE is pretty nice, but
  it'd undeniably be kind of nice to be rid of it.
- Allow multiple OE windows?

### Longer-term ideas, or stuff that I'm not super sure about

- Improve game-detection routines to be able to enumerate *all* detected
  game installs. in addition to user-selected install(s) via a Settings
  screen.  With current functionality, this would basically only ever be
  used for the File dialogs, which have shortcut buttons on the right-hand
  side.  But that way we could have, for instance, separate "BL2 Steam"
  and "BL2 EGS" buttons, etc.  At the moment, the detection routines just
  return the first thing they find, though.
- Maybe completely separate OpenBLCMM from OE?
- Maybe completely separate INI tweaks into their own little app?
- Pass around references to stuff like our Options instance, rather than
  referring to global "instance" vars?

Third-Party Content
===================

OpenBLCMM makes use of the following third party libraries/resources:

- [StidOfficial](https://github.com/StidOfficial)'s [SteamVDF library](https://github.com/StidOfficial/SteamVDF)
  for some Steam data parsing, available under the GPLv3.
- [Apache Commons Text](https://commons.apache.org/proper/commons-text/) and
  [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/), available
  under the Apache License v2.0.
- [Xerial](https://github.com/Xerial)'s [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc),
  available under the Apache License v2.0.
- Some icons from [Dave Gandy](http://www.flaticon.com/authors/dave-gandy)'s
  [Font Awesome set](http://www.flaticon.com/packs/font-awesome), available under
  CC BY 3.0.
- An icon from [Fathema Khanom](https://www.flaticon.com/authors/fathema-khanom)'s
  [User Interface set](https://www.flaticon.com/packs/user-interface-2899), available
  under [Flaticon's Free License](https://www.flaticon.com/free-icons/ui).

License
=======

OpenBLCMM is licensed under the [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.en.html).
A copy can be found at [LICENSE.txt](LICENSE.txt).

