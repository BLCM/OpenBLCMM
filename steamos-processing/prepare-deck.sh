#!/bin/bash
# vim: set expandtab tabstop=4 shiftwidth=4:

echo
echo "This script will (theoretically) prepare a SteamOS installation to be"
echo "able to compile OpenBLCMM using GraalVM / Liberica NIK, at least from"
echo "an OS level.  It will *not* actually install Liberica NIK itself, but"
echo "it should be good to go otherwise.  There's a couple of steps to this:"
echo
echo "  1) Disabling readonly mode"
echo "  2) Initializing the Arch Linux keyring"
echo "  3) Installing development tools, plus reinstalling a few Arch"
echo "     packages whose contents have been intentionally trimmed on"
echo "     SteamOS"
echo "  4) Re-enabling readonly mode"
echo

if [ ${UID} -ne 0 ]; then
    echo "ERROR:"
    echo
    echo "This script must be run as root!  Obvs. look it over first to make"
    echo "sure you understand what it's doing."
    echo
    exit 1
fi

echo "If you don't want to do that, or have already done it, hit Ctrl-C"
echo "now.  Otherwise, hit enter to proceed!"
echo
read omg

echo "Here we go!"
echo
steamos-readonly disable
pacman-key --init
pacman -S archlinux-keyring
pacman -S base-devel glibc linux-api-headers
steamos-readonly enable
echo

echo "Done!  If you haven't already, download the Liberica NIK tgz from:"
echo
echo "    https://bell-sw.com/pages/downloads/native-image-kit/#downloads"
echo
echo "... and unpack it in ~deck.  You may need to check the paths given in"
echo "native-agent.sh and native-compile.sh to ensure they match."
echo

