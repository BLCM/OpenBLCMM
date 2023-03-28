*(this is gonna be super barebones for awhile...)*

**NOTE:** This branch is *not* compatible with the original BLCMM game data
packages.  Prepackaged data files should be available at [this Google
Drive link](https://drive.google.com/drive/folders/1ssqbAIGTm2xZvhQPizqnrlWsez9ba9Bw?usp=share_link).
At the moment, the fork is only really tested while running via Netbeans --
download those data packages and save them inside the `BLCMM` directory.
The app should see them on startup and extract the sqlite database to
an `extracted-data` directory.  At the moment there's no real indication that
that's happening apart from log entries -- once we have a launcher in place,
that should get that sorted out a bit better.

The generation scripts for the new data can be found in the [DataDumper
PythonSDK mod](https://github.com/BLCM/DataDumper).  Note that the data
format may change without warning for awhile yet, as the new version
approaches release!

A Changelog can be found at [BLCMM/src/CHANGELOG.md](BLCMM/src/CHANGELOG.md).

OpenBLCMM's license (GPLv3) can be found at [BLCMM/src/LICENSE.txt](BLCMM/src/LICENSE.txt).

TODO (immediate)
================

- AoDK support (both in OpenBLCMM itself and OE)
- Rewrite launcher w/ GPL version
  - New-version notification in-app
- Rewrite other linked libraries w/ GPL versions
- Don't include hotfix "name" in hotfix keys (or at least strip to alphanumeric)?
- Memory monitor in main window?
- About dialog improvements:
  - Link/button to github
  - Report Java version
  - Report data versions
  - Report memory usage + config?  Though if we have a memory counter on
    the main window then maybe that's beside the point
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

TODO (maybe?)
=============

- Add some caching to IconManager -- probably had it originally.
- Convert StringTable to use a CSV library
  - Will need some one-time conversion stuff in there, and the ability to
    discern between a converted version.
- Get a Netbeansless build system working (just ant from CLI, or I guess
  Maven might be the currently-recommended thing?).  I'm (apoc) still
  feeling pretty rusty in Java, so having the IDE is pretty nice, but
  it'd undeniably be kind of nice to be rid of it.
- Allow multiple OE windows?

TODO (long-term)
================
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

