BLCMM Changelog
===============

**v1.3.0** Unreleased
- Released under GPLv3
- Compatibility through Java 19
- Removed features:
  - Plugin functionality
  - First-startup dialogs (welcome, game setup, question-mark/TINS prompt)
  - Structural Edits setting (this is now always enabled)
  - Developer Mode setting (this is now always enabled)
  - Game launch button
- Improved Steam Library Folder detection using StidOfficial's SteamVDF
- When creating and saving a new file, it now opens on the next boot
- BL2 gets detected properly again on mac
- Mac (and native Linux) "online" patch saving was fixed
   TODO: *Hex editing on mac has been enhanced - you no longer need to go back to the title to execute mods, and big mods work properly now
   TODO: *Thanks to c0dycode and Apple1417 for finding this enhancement!
- Backwards compatibility for engine.upk replacement has been removed
- Create your own plugin button is now publicly available TODO make sure the zip file is up to date
- Opening a .blcm file trough Windows' context menu now actually opens the file in BLCMM

The following are currently in the code but commented out
1.2.1? - c0dycode's Pluginloader is now installed when hexediting your game
1.2.1? - Added install options for c0dycode's AutoExec and CommandInjector / pySDK?
  - Several auto-save features when launching game trough BLCMM / when autoexec is installed
  - Added a management menu for when c0dycode's autoexec is installed

**v1.2.0** May 22, 2020
- Added the ability to drag-and-drop text around plenty of BLCMM's text boxes.
- No more disabled dropdown on non-dev-mode. This was confusing people
- Remove unneeded UI elements from the edit window
- Offer to open last backup of file when booting after a crash
- Offer to remove read-only flag when saving to read-only file
- Fix Syntax Highlighting of some objects.
- Added a way for plugins to open in non-modal windows
- Object dumps can now be bookmarked
- Added way to collapse multiline array fields in Object Explorer
- Overhauled the Object explorer user interface. More compact
- Add Regular expression, wrap around, and replace support for search windows.
- Mouse changes back to normal after a plugin crash
- A message is shown when trying to dump an object that is deemed unmoddable / useless
- Plugins are now uninstallable
- Launch button added for the current game (Windows only)
- Update all the game's assets to incorporate DLC5 and UHD updates for BL2 / TPS
- Fixed "Quicker Startup" INI Tweak to allow for recent patches
- Will detect Wine/Proton usage when running on Linux
- General bug fixes


**v1.1.8** November 9, 2018
- A few changes to the available datasets
- Greatly improved data management library, paving the way to a public plugin API.
- Improved plugin handling
- Some more syntax checks
- Various minor bug fixes
- OE search panel is centered on screen
- OE focuses on the search bar when opening a new tab
- Removed the option to download multitool for mac and linux users to prevent confusion

**v1.1.7** August 24, 2018
- Allow viewing code in read-only mode when categories are locked
- Import dialog titles now say "Import" instead of "Open"
- Allow drag-and-drop of selected items into a mutually-exclusive category;
  will be deselected if the category is deselected, in that case
- Prompt the user for confirmation if a file with no actual mod statements
  is attempted to import
- Accidental whitespace in OnDemand and Level hotfix conditions will be
  automatically trimmed
- Populate hotfix level names from autocomplete dropdown a bit more nicely
- Renamed "Import Single Mod" to "Import Mod File(s)" to note that you can
  import multiple mod files into BLCMM using CTRL + Click
- Added "Find Previous" and "Find Next" into the OE/Code Edit text search, and
  set the search to not prevent usage of the main windows.
- When editing code, Ctrl+Enter can be used to save changes and close the dialog
- Added "Fully Collapse Category" action
- Added a notice that BL2/TPS has to be run at least once before game
  autodetection will work, when the game can't be found
- Improved UI when importing multiple mods (progress bar dialog, errors will
  only pop up a single dialog instead of a dialog per error)
- The notice that one forgot to hexedit their game on initial boot makes the hexedit window re-open
- When exporting a category as mod, the initial filename will be that of the category
- No longer scrolls horizontally in the main tree when hopping between nodes
  This applies for both searching and using overwriting statements to hop
- Prevent illegal codes from corrupting some files
- Changed the behavior errors to warnings for now, since they produce false positives
- Made command truncation in the tree the default setting
- Fixed a bug where overwriting a mod in a specific folder wouldn't import to
  the correct location.
- Can now close Object Explorer using Ctrl/CMD-Q
- Fixed level merge warning for Tina DLC
- Undumpable baseclasses now give a warning

**v1.1.6** July 26, 2018
- Increased scroll speed in the settings menu
- User will be prompted when being too eager on downloading data packages.
- Added a check for mismatching quotes in commands
   - Also fixed some crashes and bugs related to singular quotes
- Added a warning when saving to the backup directory
- Added some checks for set commands that won't stick
- Comes with launcher version 1.1.5
   - Has a way to install BLCMM with offline files

**v1.1.5** July 24, 2018
- Allow for hotfixes without value 
- Fix various crashes on some Java installations
- Various parts of BLCMM are now more quote-aware
- A warning message when people forget to hexedit their game on first launch
- Some markdown for the changelog
- Fix unreachable bottom in edit window of large commands
- Can now select commands/categories along the whole row, not just the text
- Map merge conflicts are no longer marked as overwriting statements.
- Commands affecting an entire class now get a different color + tooltip
- Auto combination of map merging statements is now vanilla level list aware
- Added a button to the BLCM github to the file menu
- Added an uninstall button in the help menu. Using it is not advised tho ;)
- Fix a new crash when creating new set_cmp commands
- Show the number of results when searching
- Better 'my documents' detection on systems where the folder was moved (hopefully)
- Renamed content edits to "Developer mode"
   - Made the warning prompt more elaborate
   - Some of the stuff that previously required developer mode, now only
     requires structural edits, like deletion of categories or cut + paste
- Comes with launcher 1.1.4
   - Fix a repeating prompt for too much ram
   - Fix being unable to create a new installation
   - Show a message when there's no internet connection

**v1.1.4** July 18, 2018
- Creating a new file no longer gives unneeded warning when saving
- Opening RAR files now gives proper feedback
- Opening empty files is checked better
- Fixed a crash when the file contains unexpected newlines.
- Make autoexec properly show reverted status
- Disable autoexec when not yet installed - redirect to c0dy's page
- Opening by dragging file into BLCMM now no longer saves to previous file
- Fix an issue in the data library, concerning data containing quotes.
- Make changelog accessible from within BLCMM
- The initial opening of a file won't terminate BLCMM anymore, so the app should always open.
  Subsequent attempts to open a bad file *will* still crash, however.
- Improved missing data detection
- Fix the propagation bug shown in the release video
- Improve username hiding
- Comes with Launcher version 1.1.1
   - Properly reports socket timeouts, suggesting to turn off one's firewall
   - Creates log files on first-time run too
   - Warns user about allocating too much ram

**v1.1.3** July 15, 2018
- Fix bug in "my documents" detection in 1.1.2

**v1.1.2** July 14, 2018
- Warn user before opening huge classes in the class explorer of the OE
- Fix bug in map-merging handling (should fix problems saving w/ mopioid's
  Loot Midget World)
- Visibly disable the tree when Code Edit dialog is open, as opposed to
  disabling it silently
- Better game detection
- Better error handling when a file can't be loaded
- Fix some crashes
- Don't trigger code warnings with some strangely-named TPS classes

**v1.1.1** July 10, 2018
- Fixed a crash when no games were detected

**v1.1.0** July 10, 2018
- Initial public release
