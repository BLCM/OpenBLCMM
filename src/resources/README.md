Provenance of the various resources in here
===========================================

**Note:** Any new resources added here will also need to be added into
the `META-INF/native-image/blcmm/blcmm/resource-config.json` registry,
for the compiled Windows EXE version to Not Crash when the resources
are requested.

* `donate.png` - This was just taken from a screenshot at paypal.com from
  their set-up-a-donation-link page, with the HTML/CSS fiddled a little
  bit to just say "Donate" and to make the button less wide.  (Then edited
  in Gimp to make the background transparent.)

* `Icon.png` - OpenBLCMM's app icon, unless someone is willing to create
  and submit something better.  Constructed by apocalyptech in Gimp in a
  rather crude fashion.  The source Gimp files (and a couple of gradients
  used while playing with it) can be found in the top-level `resources/icon`
  directory.

* `Qmark.png` - Free-to-use icon from [Fathema Khanom](https://www.flaticon.com/authors/fathema-khanom)'s
  [User Interface set](https://www.flaticon.com/packs/user-interface-2899), available
  under [Flaticon's Free License](https://www.flaticon.com/free-icons/ui).
  (Though not with the filename used here.)

* `exec.png` / `padlock.png` / `speaker.png` - Some icons from
  [Dave Gandy](http://www.flaticon.com/authors/dave-gandy)'s
  [Font Awesome set](http://www.flaticon.com/packs/font-awesome), available under
  CC BY 3.0.  (Though not with the filenames used here.)

* `folder.png` - [Free-to-use icon](https://www.flaticon.com/free-icon/folder_148955)
  from [Smashicons](https://www.flaticon.com/authors/smashicons)'
  [Essential Collection set](https://www.flaticon.com/packs/essential-collection),
  available under [Flaticon's Free License](https://www.flaticon.com/free-icons/ui).
  (Though not with the filename used here.)

* `<gamedir>/GBX_hotfixes.blcm` - BLCM-formatted mod file containing the
  vanilla hotfixes still provided by GBX at time of writing.  This is the
  same version as was present in the original BLCMM versions, since it's
  just the raw data.

* `<gamedir>/vanillaLevelLists.blcm` - BLCM-formatted mod file containing
  the vanilla `LevelList` attributes for all `LevelDependencyList` objects.
  Used by BLCMM for handling map merges.  This is the same version as was
  present in the original BLCMM versions, since it's just the raw data.

* `<gamedir>/Icon.png` - For BL2 and TPS, these are the official game icons,
  though the official icons you can currently get from Steam are only 64x64.
  I'm pretty sure that that Steam originally provided 256x256 versions of the
  icons, though, which is where these had come from.  The AoDK icon was created
  by [Julia @ steamgriddb.com](https://www.steamgriddb.com/icon/10266), and is
  used here by their kind permission!
