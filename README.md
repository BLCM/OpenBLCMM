*(this is gonna be super barebones for awhile...)*

**NOTE:** This branch is *not* compatible with the original BLCMM game data
packages.  There's currently not precompiled data available (though there
probably will be Soon™), though the generation scripts are available
via the [DataDumper PythonSDK mod](https://github.com/BLCM/DataDumper).  Also
note that the data format may change without warning for awhile yet, as
the new version approaches release.

TODO (immediate)
================

- Rename to BLCMM-Basic or something?
- AoDK support (both in BLCMM itself and OE)
- Rewrite launcher w/ GPL version
  - New-version notification in-app
- Rewrite other linked libraries w/ GPL versions
- Don't include hotfix "name" in hotfix keys (or at least strip to alphanumeric)?
- Add link/button to github in the about dialog
- Add Java version in about dialog
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
- Figure out opening SQLite DBs in read-only mode
- May as well cache *all* our PreparedStatements in DataManager...
- Apparently multi-selections screw with statement order in the Edit window?
- Improve "basic" attr-name autocomplete to restrict results to appropriate
  field names
- Options for changing object-opening behavior (ctrl/alt/shift-click, single/
  double/triple/whatever).  Also an option to disable links altogether?
- Make sure we handle launching from a readonly filesystem properly
  (original BLCMM launcher crashes when that happens; main app might too)

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
- Maybe completely separate BLCMM from OE?
- Maybe completely separate INI tweaks into their own little app?
- Pass around references to stuff like our Options instance, rather than
  referring to global "instance" vars?

