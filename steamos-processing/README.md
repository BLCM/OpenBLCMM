Linux/SteamOS Native Compilation
================================

This dir contains the support files needed to compile a native binary
version for Linux, specifically targetting the Steam Deck.  The builds
created here will likely work on practically all other Linux systems
as well, though desktop Linux users are encouraged to use the Pure
Java versions instead.

Assuming that this version works all right on the Deck, we may look
into packaging the application up into a Flatpak as well, for even
easier distribution.

* [The Short Version](#the-short-version)
* [Preparing a SteamOS Installation](#preparing-a-steamos-installation)
* [Compilation Process](#compilation-process)
* [Known Bugs](#known-bugs)

The Short Version
-----------------

SteamOS Preparation:

1. Create a SteamOS VM somewhere, or (probably even better) use a real
   Steam Deck.
2. Install these OS packages: `base-devel glibc linux-api-headers`
3. Download [Liberica NIK](https://bell-sw.com/pages/downloads/native-image-kit/#downloads)
   TGZ and unpack in `~deck`

Compilation:

1. Clone the OpenBLCMM git repo somewhere
2. Transfer a built `OpenBLCMM.jar` from your build host's `store` dir
   into your SteamOS git checkout `store` dir.
3. Run `native-agent.sh` and interact with the app for awhile, focusing
   on "new" functionality since the last release.
    1. If there are changes to the JSON in `src/META-INF/native-image/blcmm/blcmm`,
       commit it, rebuild `OpenBLCMM.jar` on your build host, and
       re-transfer back over to `store`.
4. Run `native-compile.sh` to compile into `store/steamos`.  Transfer
   that out to wherever you're going to do packaging (presumably the
   build host).

That's it!  We go into some more detail below:

Preparing a SteamOS Installation
--------------------------------

We can actually probably generally get away with compiling this on any
random Arch desktop.  SteamOS 3 is based on Arch, and during my testing
I was able to get builds from either system working on either.  Still,
it's probably safest to compile the application on a SteamOS install,
in case Arch ends up having ABI-breaking updates which SteamOS doesn't
get for awhile.  The *best* case for compilation is to probably do it
on a real Steam Deck, but I don't have one of those available.

I ended up mostly following the [virtualization/installation method at
Alberto Garcia's blog](https://blogs.igalia.com/berto/2022/07/05/running-the-steam-decks-os-in-a-virtual-machine-using-qemu/)
for installing the official Steam Deck image into a VM and using that.
The Steam Deck image name I used was `steamdeck-recovery-4.img`, which
was from 2022, but it updated to the most recent version without trouble.
Using the community [HoloISO](https://github.com/HoloISO/holoiso) project
might be a better solution, but I haven't actually tried it yet.

For the official Deck image, you'll need to do a couple of things to get
the OS ready to compile.  There are some development packages to install,
and some base packages to *reinstall*, since the SteamOS image intentionally
trims some development-related files from some packages.  This sequence
of commands should do the trick, though it's possible I'm missing a step
relating to the keyring stuff:

    steamos-readonly disable
    pacman-key --init
    pacman -S archlinux-keyring
    pacman -S base-devel glibc linux-api-headers
    steamos-readonly enable

The `prepare-deck.sh` will kick all that off for you, if you like -- be
sure to run it with `sudo`.  Note that this process will have to be
repeated whenever your SteamOS image processes an update.  Only the
contents of `/home` are preserved between updates.

Once those packages are installed, you should be able to continue with
the compilation.

Compilation Process
-------------------

We're actually doing the exact same thing here that we've been doing for
native Windows EXE builds, so the general outline of the build process
[in the Windows-specific documentation](../windows-processing) will go
into more detail than I intend to put here.

Briefly, we're using [GraalVM](https://www.graalvm.org/)'s
[Native Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
technology to compile Java into a native executable, using the
[Liberica NIK](https://bell-sw.com/liberica-native-image-kit/) fork of
GraalVM, since that supports the AWT/Swing graphics library which
OpenBLCMM uses.  Before actually compiling, we need to [run OpenBLCMM
inside a "tracing agent"](https://www.graalvm.org/22.0/reference-manual/native-image/Agent/)
so GraalVM has a map of all the dynamically-loaded stuff.

We've got a couple of helper scripts for that -- one for running the agent,
and another for doing the compile.  Both require that Liberica NIK is
downloaded and unpacked somewhere.  Grab the TGZ version from the
[Liberica NIK download area](https://bell-sw.com/pages/downloads/native-image-kit/#downloads)
and unpack it in the `deck` user's homedir.  If you put it elsewhere
or end up using a different version than the scripts expect, be sure to
edit them to suit.  Since the `/home` partition persists through upgrades,
you shouldn't have to touch that again unless you want to change
Liberica NIK versions in the future.

The agent-running script, `native-agent.sh`, also expects the `jq` and
`unix2dos` utilities to be available on the system.  Both should be
available by default on SteamOS.  Those are used to "normalize" the JSON
output so that it plays nicely with the JSON we're already using from the
Windows compilation -- they end up combining into a single set of
configuration for both OSes.  The script takes no argument, so just:

    ./native-agent.sh

Then interact with any "new" areas of the app and generally just click
around until you get bored.  If there are any changes to the JSON afterwards,
commit them and rebuild `OpenBLCMM.jar` out on your main development host.

Then once you're ready to compile, just run the compilation script:

    ./native-compile.sh

That should leave you with a binary and some `.so` files, inside `store/steamos/`.

Known Bugs
----------

One problem is that the Java fonts look pretty bad inside SteamOS in general, at
least in my test VM.  Even if we can get "pure java" fonts looking good in there,
the GraalVM-compiled version doesn't seem to get any better.  I [opened a bug
about that](https://github.com/bell-sw/Liberica/issues/140) at Liberica's
tracker, so we'll see how that goes.

Another issue is that Java doesn't know how to open URLs on SteamOS by default.
It turns out that doing so requires the `gvfs` package, which can be installed
using the same method used to get all the development stuff installed, but that's
not feasible for average users (and, like the development packages, that
install wouldn't survive an upgrade either).  It's possible we could address
this with Flatpak packaging?

