**NOTE:** This needs another comparison runthrough to make sure I didn't
screw anything up with the copy+pasting.  Also some of these may not even
be valid?  Anyway, don't trust fully yet...

BLCMM used to include functionality to perform various hexedits on BL2
and TPS.  This is no longer the case, and the recommended way to perform
these edits is to just make use of PythonSDK instead.  Still, it seems
useful to make a note of all the hexedits which BLCMM used to perform, so
here goes:

General Info
============

When you see `??` in the search strings, it means that you should allow
any value in that byte position.

Note that none of these have been verified on the standalone
[Assault on Dragon Keep](https://store.steampowered.com/app/1712840/), released
in 2021, and BL2's `set` hexedit in particular ends up breaking the game.
It's recommended to stick with PythonSDK regardless, but for AoDK it's
basically required.

Enable `set` Command
====================

For Windows, there's two components to this: one is to enable the `set`
command itself, and another to disable the auto-`say` which happens when
you type in console commands.  For the native Linux + Mac versions, there's
an alternate method which takes care of it with a single hexedit, though
keep in mind that with those hexedits, you have to execute mods from the
title screen, not the main menu.  (The Mac version for BL2 apparently has
a separate hexedit which might improve that aspect.)

### Enabling `set` itself - BL2 and TPS (Windows)

Search for:

    83 C4 0C 85 ?? 75 1A 6A

The `??` byte is ordinarily `C0` -- replace it with `FF`

### Removing auto-`say` - BL2 and TPS (Windows)

Search for:

    61 00 77 00 20 00 5B 00 47 00 54 00 5D 00 00 00 ?? 00 ?? 00 ?? 00 ?? 00 00 00 00 00 6D 73 67 20

The four `??` bytes are ordinarily `73 61 79 20`.  Replace them with `00 00 00 20`.

### BL2 (Linux and Mac) and TPS (Linux)

*Note:* This edit requires the user to execute mods from the title screen,
not main menu.  Also, Linux users are encouraged to use the Windows versions
via Proton, instead, in which case you should use the Windows edit, above.

Search for:

    00 00 00 00 43 00 00 00 6F 00 00 00 6E 00 00 00 73 00 00 00 75 00 00 00 6D 00 00 00 65 00 00 00 00 00 00 00 4E 00 00 00

The sixteen bytes *before* that pattern are ordinarily:

    73 00 00 00 61 00 00 00 79 00 00 00 20 00 00 00

... replace them instead with all NUL bytes:

    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

### TPS (Mac)

*Note:* This edit requires the user to execute mods from the title screen,
not main menu.

This is basically the same pattern as the BL2 Linux/Mac version above, except
the sixteen bytes you change occur 24 bytes prior to the pattern, not 16 (so
there's eight bytes inbetween what you're changing and the pattern.

So, search for the same string as the BL2 Linux/Mac version, above:

    00 00 00 00 43 00 00 00 6F 00 00 00 6E 00 00 00 73 00 00 00 75 00 00 00 6D 00 00 00 65 00 00 00 00 00 00 00 4E 00 00 00

Go back 24 bytes from the start of that pattern.  The 16 bytes found there
are ordinarily:

    73 00 00 00 61 00 00 00 79 00 00 00 20 00 00 00

... replace them instead with all NUL bytes:

    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

### Enhancement for BL2 (Mac)

I believe this hex-edit allows the Mac version to execute patches from the
main menu, rather than the title screen, though I'm honestly not sure.

Search for this pattern:

    41 ?? 01 00 00 00 BA 01 00 00 00 E8 ?? ?? ?? ?? 85 ?? 75 19 48 8D

Moving forward 17 bytes from the start of the pattern (so: the final `??`
wildcard in there), that byte is ordinarily `C0`.  Change it to `D8`.

### Enhancement for TPS (Mac) - BROKEN

This theoretically does the same enhancement for TPS that the previous one
does for BL2, but the notes in BLCMM mention that it seems to break mods
entirely, instead of making them better.  So, don't actually use this, but
I'm including it for posterity in case anyone wants to investigate.

Search for this pattern:

    89 3C 24 C7 44 24 08 01 00 00 00 C7 44 24 04 ?? ?? ?? ?? E8 ?? ?? ?? ?? 85 ?? 74 ?? 8B 8D

Moving forward 25 bytes from the start of the pattern (so: the second-to-last
`??` wildcard in there), that byte is ordinarily `C0`.  Change it to `D8`.

Array Limit Disabling
=====================

When using `obj dump` on the console to view variables, arrays will generally
be limited to 100 items on the output.  These hexedits get rid of that
restriction, which is useful to modders.  It's of no interest to folks who
are just *using* mods.

There are technically two parts to this: disabling the limit itself, and
then removing the message which gets printed on the console once you get
to the 100th entry.

### Remove Message - BL2 (Windows)

Search for this pattern:

    8B 40 04 83 F8 64 0F ?? 7B 00 00 00 8B 8D 9C EE FF FF 83 C0 9D 50 68

The `??` byte is ordinarily `C0` -- replace it with `D8`.

### Remove Message - BL2 (Mac)

Search for this pattern:

    80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00

Move ahead `0x35` bytes (53 in decimal) from the start of that pattern.  The
byte at that location is ordinarily `7C` -- change it to `75`.

### Remove Message - BL2 (Linux)

Search for this pattern:

    FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1

Move ahead `0x31` bytes (49 in decimal) from the start of that pattern.  The
byte at that location is ordinarily `7C` -- change it to `75`.

### Remove Message - TPS (Windows)

Search for this pattern:

    8B 40 04 83 F8 64 ?? 7B 8B 8D 94 EE FF FF 83 C0 9D 50 68

The `??` byte is ordinarily `7C` -- replace it with `EB`.

### Remove Message - TPS (Mac)

Seach for this pattern:

    80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00

Move ahead `0x35` bytes (53 in decimal) from the start of that pattern.  The
six bytes at that location are ordinarily `0F 8C 2F 01 00 00` -- change them
to `0F 85 2F 01 00 00`.

### Remove Message - TPS (Linux)

Search for this pattern:

    FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1

Move ahead `0x36` bytes (54 in decimal) from the start of that pattern.  The
byte at that location is ordinarily `7C` -- change it to `75`.

### Remove Limit - BL2 and TPS (Windows)

Search for this pattern:

    ?? 05 B9 64 00 00 00 3B F9 0F 8D

The `??` byte is ordinarily `7E` -- replace it with `75`.

### Remove Limit - BL2 and TPS (Mac)

Search for this pattern:

    80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00 ?? ?? ??

The three `??` bytes are ordinarily `0F 4F CA` -- replace them with `90 90 90`.

### Remove Limit - BL2 (Linux)

Search for this pattern:

    FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1 ?? ?? ??

The three `??` bytes are ordinarily `0F 4F CB` -- replace them with `90 90 90`.

### Remove Limit - TPS (Linux)

Search for this pattern:

    FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1

Move ahead `0x1A` bytes (26 in decimal) from the start of that pattern.  The
three bytes at that location are ordinarily `0F 4F CA` -- replace them with
`90 90 90`.

Disable Sanity Check
====================

BL2 and TPS ordinarily do a "sanity check" on weapons/items to make sure that
they're valid.  This can lead to modded gear getting deleted from savegames.
These hexedits get rid of that check.  Note that the PythonSDK mod
[Sanity Saver](https://bl-sdk.github.io/mods/SanitySaver/) is a much more
convenient way to do this.

The Windows hexedits we have are apparently just for weapons, and on a
per-part basis, whereas the Linux/Mac ones are more generalized to one
for items and another for weapons.

### Weapon Body - BL2 (Windows)

Search for:

    83 7F 10 00 8D 47 10 74

Replace with:

    83 7F 10 FF 8D 47 10 75

### Weapon Grip - BL2 (Windows)

Search for:

    83 7F 14 00 8D 47 14 74

Replace with:

    83 7F 14 FF 8D 47 14 75

### Weapon Barrel - BL2 (Windows)

Search for:

    83 7F 18 00 8D 47 18 74

Replace with:

    83 7F 18 FF 8D 47 18 75

### Weapon Sight - BL2 (Windows)

Search for:

    83 7F 1C 00 8D 47 1C 74

Replace with:

    83 7F 1C FF 8D 47 1C 75

### Weapon Stock - BL2 (Windows)

Search for:

    83 7F 20 00 8D 47 20 74 11

Replace with:

    83 7F 20 FF 8D 47 20 75 11

### Weapon Element - BL2 (Windows)

Search for:

    83 7F 24 00 8D 47 24 74

Replace with:

    83 7F 24 FF 8D 47 24 75

### Weapon Accessory 1 - BL2 (Windows)

Search for:

    83 7F 28 00 8D 47 28 74

Replace with:

    83 7F 28 FF 8D 47 28 75

### Weapon Accessory 2 - BL2 (Windows)

Search for:

    83 7F 2C 00 8D 47 2C 74

Replace with:

    83 7F 2C FF 8D 47 2C 75

### Weapon Material - BL2 (Windows)

Search for:

    83 7F 30 00 8D 47 30 74 11

Replace with:

    83 7F 30 FF 8D 47 30 75 11

### Weapon Body - TPS (Windows)

Search for:

    83 7E 10 00 8D 46 10 74 30 50 8D 4D DC

Replace with:

    83 7E 10 FF 8D 46 10 75 30 50 8D 4D DC

### Weapon Grip - TPS (Windows)

Search for:

    83 7E 14 00 8D 46 14 74 0D 50 8D 4D DC

Replace with:

    83 7E 14 FF 8D 46 14 75 0D 50 8D 4D DC

### Weapon Barrel - TPS (Windows)

Search for:

    83 7E 18 00 8D 46 18 74 11 50 8D 4D DC

Replace with:

    83 7E 18 FF 8D 46 18 75 11 50 8D 4D DC

### Weapon Sight - TPS (Windows)

Search for:

    83 7E 1C 00 8D 46 1C 74 11 50 8D 4D DC

Replace with:

    83 7E 1C FF 8D 46 1C 75 11 50 8D 4D DC

### Weapon Stock - TPS (Windows)

Search for:

    83 7E 20 00 8D 46 20 74 11 50 8D 4D DC

Replace with:

    83 7E 20 FF 8D 46 20 75 11 50 8D 4D DC

### Weapon Element - TPS (Windows)

Search for:

    83 7E 24 00 8D 46 24 74 11 50 8D 4D DC

Replace with:

    83 7E 24 FF 8D 46 24 75 11 50 8D 4D DC

### Weapon Accessory 1 - TPS (Windows)

Search for:

    83 7E 28 00 8D 46 28 74 11 50 8D 4D DC

Replace with:

    83 7E 28 FF 8D 46 28 75 11 50 8D 4D DC

### Weapon Accessory 2 - TPS (Windows)

Search for:

    83 7E 2C 00 8D 46 2C 74 11 50 8D 4D DC

Replace with:

    83 7E 2C FF 8D 46 2C 75 11 50 8D 4D DC

### Weapon Material - TPS (Windows)

Search for:

    83 7E 30 00 8D 46 30 74 11 50 8D 4D DC

Replace with:

    83 7E 30 FF 8D 46 30 75 11 50 8D 4D DC

### Weapons - BL2 (Mac)

At the address `0x74AFD5`, you should find this pattern:

    E8 58 A1 28 00

Replace with:

    90 90 90 90 90

### Items - BL2 (Mac)

At the address `0x74AF61`, you should find this pattern:

    E8 FE A1 28 00

Replace with:

    90 90 90 90 90

### Weapons - TPS (Mac)

At the address `0x9B83BE`, you should find this pattern:

    E8 4F 49 CF FF

Replace with:

    90 90 90 90 90

### Items - TPS (Mac)

At the address `0x9B8346`, you should find this pattern:

    E8 0B 4A CF FF

Replace with:

    90 90 90 90 90

### Weapons - BL2 (Linux)

At the address `0xD26870`, you should find this pattern:

    E8 F7 23 17 00

Replace with:

    90 90 90 90 90

### Items - BL2 (Linux)

At the address `0xD267F0`, you should find this pattern:

    E8 A9 24 17 00

Replace with:

    90 90 90 90 90

### Weapons - TPS (Linux)

At the address `0xCFE1C8`, you should find this pattern:

    E8 0D 94 17 00

Replace with:

    90 90 90 90 90

### Items - TPS (Linux)

At the address `0xCFE148`, you should find this pattern:

    E8 CF 94 17 00

Replace with:

    90 90 90 90 90

Offline-Only Mode
=================

This hexedit currently only exists for BL2 on Linux.

### BL2 (Linux)

At the address `0x00B3B988`, you should find this pattern:

    E8 E9 0C 5A FF

Replace with:

    90 90 90 90 90

