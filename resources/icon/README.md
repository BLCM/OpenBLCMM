OpenBLCMM app icon, created by Apocalyptech.

This was originally created in [Gimp](https://www.gimp.org/), which really
wasn't the right tool to use for this kind of vectorish-looking art.  So,
shortly prior to official release, I ended up rebuilding it in
[Inkscape](https://inkscape.org/) as an SVG.  Several orders of magnitude
smaller!  There were still various imperfections in the paths which were
noticeable at high resolutions, so I eventually rebuilt it again using a
generation script (`gen_icon.py`), so the current SVG icon should be
perfectly symmetrical and have identically-built corners and all that.

`arch_reference.jpg` is the screenshot I used to construct the path,
originally.  That's from BL3, actually -- the arch in BL2 has a pretty
different-looking shape, as it turns out, but I like the look of BL3's
better.

Rendered versions in the github tree:
 - `src/resources/Icon.png` - 256x256, used inside OpenBLCMM itself
 - `windows-processing/openblcmm.ico` - 64x64, used for Windows installer
   and EXE.

