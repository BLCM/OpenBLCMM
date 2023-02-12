*(this is gonna be super barebones for awhile...)*

TODO
====

- Rename to BLCMM-Basic or something?
- Remove "invert mod" functionality?
- Remove launch-game functionality (`updateLaunchGameButton`)
- Doublecheck stuff that used to be in `toggleDeveloperMode`:
  - `MainGUI.INSTANCE.setChangePatchTypeEnabled(box.isSelected());`
  - `MainGUI.INSTANCE.updateComponentTreeUI();`
- Doublecheck all `*Action` functions, post-struct-and-dev-mode-removal
- AoDK in OE
- Maybe completely separate BLCMM from OE?
- Rewrite launcher w/ GPL version
- Rewrite other linked libraries w/ GPL versions
- Remove game setup hexedits-n-such
  - Make a markdown file with info on all available hexedits, though
- Three game-detection functions exposed in previous hexedit window
  (this will be pretty much 100% for just file-dialog button shortcuts)
  - Steam autodetect
  - EGS autodetect
  - Manual choice
- Make sure the former hexedit window tells folks to hit up PythonSDK
- Convert BL2/TPS boolean to an Enum
  - Maybe add AoDK support?
- Default to offline; don't even bother with the checkbox

