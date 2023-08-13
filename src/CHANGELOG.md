OpenBLCMM Changelog
===================

**v1.4.0** Unreleased
- Settings changes:
  - Added a new "Input" settings tab to configure mouse-click behavior in code
    edit windows and Object Explorer:
    - Can configure which buttons open object links in new/current OE tabs.
    - Can configure mouse buttons to go back/forward in OE history (defaults to
      Mouse 4 and Mouse 5)
  - Added a new "Confirmations" settings tab to configure which warnings /
    confirmations get shown to the user.  The current available settings are:
    - Confirmation when checking or unchecking a tree which contains a mix of
      checked/unchecked statements.
    - Confirmation when deleting code which only contains comments.
    - Confirmation when deleting code in general.
    - Confirmation when doing a "flatten category contents" action (see below)
  - Added a setting to turn off auto-load of most recently open patch file, when
    starting the app.
  - Added a setting to toggle having a colon inbetween the attribute name and
    its value, when doing a `getall` in Object Explorer.
  - Added a new "Dangerous Settings" option to set the `GearboxAccountData`
    index used in offline-saved files.
  - Added some categorization to the "General" settings tab.
  - "Restore Defaults" button in settings should apply all reset settings
    immediately, and update windows if needed (such as for theme or font size
    resets).
  - Fixed a bug where Object Explorer data category selection wasn't applying
    properly on app startup.
- Code formatting improvements:
  - Code formatting backend got a bit of an overhaul in general, which looks
    to have improved performance when editing large `set` statements.
    Let us know if there's any strange behavior with code processing now!
  - Allow the word `set` to appear inside statement values.  (This was always
    a problem for Ezeith's Red Text Explainer mod.)
  - Prevent app from aggressively trimming whitespace in `set` commands, which
    could have affected UI labels which included parentheses (such as in
    `SkillDescription` attributes, for instance).
- Fixed various font scaling issues with non-default font sizes, and when
  changing the font size in the app.  Object Explorer will dynamically update
  its font size just like the main window.  A few tooltips still may not update
  their font size until the app is restarted, and some very-early-startup error
  dialogs may not use the user's font size preference.
- Fixed search box placement when maximizing and un-maximizing the main window
  (thanks, ernimril from Libera's #java!)
- Object Explorer's "Auto format" button is now a toggle (like the "Collapse
  Arrays" button).  OE will also remember the state of both those buttons
  between runs, in addition to the selected "Deformat arrays" number.
- New actions available on the main mod tree:
  - "Replace Category With Contents" - Replaces a category/folder with its
    contents.  Cannot be done on mutually-exclusive categories, or on locked
    categories.
  - "Flatten Category Contents" - Inside the selected category, flattens out
    the entire structure to result in a list of all the comments/statements
    which used to be sorted into categories.  All the prior categories will be
    removed.  This cannot be done if there are any mutually-exclusive or locked
    categories down the tree from the category to be flattened, or if the
    category itself is mutually exclusive or locked.
- Updated Assault on Dragon Keep icon with a custom icon designed by Julia at
  steamgriddb.com (used with permission).
- Crashes Fixed:
  - Fixed a crash which could occur when unexpected files/directories were found
    in OpenBLCMM's auto-backup dir.
  - Fixed a crash which could occur when going back/forward through Object
    Explorer search history.
  - Fixed a crash which could occur when navigating Object Explorer bookmarks.
  - Fixed a crash when triggering a search while no datapack is loaded.
  - Fixed a crash when updating the OE Data category selections while an OE
    search is running.
- Dialogs should be closeable with the OS "window close" button when possible.
- Game INI file detection on Linux will try case-insensitive file matching if
  the canonical case version isn't found (to support Wineroot
  case-insensitivity)
- Tabs in comments will render properly in the mod tree (useful for mods which
  use Command Extensions and include `pyb` statements), and normalized tab stops
  to four characters in code edit dialogs and Object Explorer.
- Double-clicking to select text in Object Explorer or code edit dialogs should
  always select the entire object name, if appropriate.
- Window focus should always return to the original window once a dialog is
  closed.
- INI Tweaks dialog tooltips added to the buttons/dropdowns as well, instead
  of just the text labels.
- Object Explorer will display the list of active data categories when a search
  produces no results.
- Better handling of URL links, when Java doesn't know how to open a browser
  on the running system.  Will present a dialog with the URL in a copyable
  form, so it can at least be opened manually.
- If the application tries to write to a readonly file and the user opts to
  override permissions to write anyway, the write permission will only be
  granted to the file's owner, if the OS supports that distinction.
- Tweaked the application icon slightly and introduced a generation script
  for it.
- Improved performance on an internal substring-matching function, and fixed
  an edge case in its behavior.
- Tightened up About dialog display a bit.
- Third-party software version updates:
  - Updated sqlite-jdbc version to 3.42.0.0
  - Updated Apache Commons Lang to 3.13.0
  - Windows EXE building with Liberica NIK 23 JDK 17

**v1.3.3** May 26, 2023
 - Fixed a startup crash issue for Windows users whose system username has a
   @domain suffix which isn't present in their homedir.

**v1.3.2** May 24, 2023
 - Fixed TPS Game Detection on Windows
 - Importing mods via drag-and-drop will correctly update OpenBLCMM's "last
   imported" internal variable.

**v1.3.1** May 14, 2023
- Fixed Wilhelm string (`GD_Enforcer_Streaming`) in hotfix editing dialog

**v1.3.0** May 12, 2023
- Renamed to OpenBLCMM
- Added Tiny Tina's Assault on Dragon Keep support
- Released under GPLv3
  - Various components rewritten from scratch as part of the FOSS process
- Compatibility through Java 20
- Windows EXE packaging updated, with optional full GUI installer.  No need
  to install Java!
- Removed features:
  - GUI Launcher / Autoupdates
  - GUI to set system memory.  The Windows EXE version doesn't really need it
    due to how the new system handles RAM.  Linux/Mac users can instead update
    the memory parameter directly in the launcher shell script.
  - Plugin functionality
  - First-startup dialogs (welcome, game setup, question-mark/TINS prompt)
  - Structural Edits setting (this is now always enabled)
  - Game launch button
  - Hex Multitool launch
  - "Invert Mod" functionality
  - Saving to FilterTool-formatted and structureless files
- Object Explorer tweaks:
  - Combined all data into a single data package, and included dumps of all
    classes historically omitted from the OE data set (with the exceptions of
    `AnimSequence`, `GBXNavMesh`, `GFxRawData`, `SwfMovie`, and `Terrain`
    objects, which are effectively un-dumpable with our current methods).
  - OE has its own game selection dropdown, which is completely separate from
    the main window's dropdown.  This replaces the former game-notification
    icon in the main panel area in OE.
  - Data category selection now only affects fulltext/"refs" searching in OE.
    An `Others` category has been added for the newly-added classes.
  - Categories to use for fulltext/refs searching can be toggled at any point
    via the Settings menu, without the need to restart the app.
  - Startup speed improved
  - "Class Explorer" panel renamed to "Class Browser"
  - "Package Explorer" panel renamed to "Object Browser"
  - All classes can be browsed via Class/Object Browser without number-of-object
    warnings
  - Class Browser tree is sorted case-insensitively
  - Class Browser tree does not sort "leaf" entries separately from folders
  - Added setting to normalize the search bar with the "full" object name when
    viewing dumps (ie: including the class type: `ClassName'GD_Obj.Foo'`).  The
    setting is off by default.
  - Dumps and searches will report which game they came from in the first line
    of the textbox.
  - Fixed some inconsistent behavior with the bookmark icon (and hopefully
    didn't introduce new inconsistent behavior...)
  - "getall" queries will correctly report the top-level class of the returned
    objects
  - Increased tab width to help cut down on accidental tab closures.
- Autocomplete changes:
  - Object autocomplete will correctly handle `:` separators in addition to
    periods.
  - Fixed some problems with full-object-name substitution in the main code
    edit area.
  - Attribute name autocompletes will attempt to use class-specific names when
    possible, or pull from the entire pool of top-level attribute names
    otherwise.
  - Attribute value autocompletes will pull from the entire pool of Enum
    values
- "Setup game files for mods" dialog redirects to PythonSDK
- INI file tweaks moved to new "INI Tweaks" dialog
- Hex Edits moved to new "Legacy Hex Edits" dialog -- note that hex editing
  is unnecessary nowadays.  PythonSDK is the preferred method for enabling mods.
- Improved array-limit message removal hexedit for BL2, courtesy apple1417
- Added new-version notification to main application.  This can be toggled off
  in the settings if you don't want to be bothered by it!
- Will refuse to import Python scripts, since they are likely PythonSDK mods.
- Added "allow toggling individual statements" option to settings, and improved
  dynamically setting the option from the main OpenBLCMM window.
- Added a "log-latest.log" logfile which never gets removed, and updated the
  filenames for the date-stamped logfiles to use human-readable dates.
- Improved Steam Library Folder detection using StidOfficial's SteamVDF
- Moved "offline" checkbox to new "Dangerous settings" dialog.  Mods will be
  saved in offline mode by default, and it's recommended to keep it that way.
- Added ability to change SparkService index in Dangerous Settings dialog, for
  anyone who insists on *not* saving in Offline mode.
- "Restore defaults" button on settings screen will only restore the settings
  shown on the active screen (this prevents clearing the recently-viewed file
  list, for instance)
- Selecting multiple commands to edit will now always show them in the order
  they appear in the file, instead of the order they were selected.
- Revamped "About" dialog; now shows various bits of system + version info.
- Added "Working Dir" file dialog button, for when that differs from the
  OpenBLCMM install location.
- Added a folder icon to the file dialog directory presets for Last-Imported
  and Working directories (instead of the empty space they had previously)
- Crash-handling window includes a link to Github Issues for OpenBLCMM, and
  only the "OK" button will actually close that dialog.
- "Get More Mods" menu item redirects to the ModCabinet wiki instead of the
  main Github page.
- When creating and saving a new file, it now opens on the next boot
- Opening a .blcm file through Windows' context menu now actually opens the
  file in OpenBLCMM, when installed via the installer.
- When run for the first time, OpenBLCMM will attempt to import most of your
  settings from an original BLCMM application install, if possible.
- "Copy" action is available regardless of developer mode ("Cut" had already
  been accessible)
- Make sure that "Insert" option appears in right-click menu regardless of
  if developer mode is on (will be greyed out if dev mode is *not* on)
- Fixed up some bugs when attempting to import very old mod files.
- Username hiding in logfiles only matches at the beginning of the pathnames,
  instead of at arbitrary points, and has been expanded to include more
  instances of pathnames being logged.
- Improved Changelog rendering
- Some tightening-up of mod-name detection when the mod category and filename
  don't exactly match.
- Changed behavior when encountering invalid strings in map/level merge
  statements -- should possibly be more accepting now.
- Replaced icon resources with known-free-to-use versions, and updated the
  OpenBLCMM icon to a new one to distinguish it from the original BLCMM.
- Added "-creator" CLI argument to launch OpenBLCMM in Creator Mode.
- Added "-userdir" and "-installdir" CLI arguments to support some peculiarities
  with the Mac `.app` bundling.
- Standardized internal hotfix naming in saved files.

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
