*(this is gonna be super barebones for awhile...)*

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

TODO (maybe?)
=============

- Get a Netbeansless build system working (just ant from CLI).  I'm (apoc)
  still feeling pretty rusty in Java, so having the IDE is pretty nice,
  but it'd undeniably be kind of nice to be rid of it.

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

