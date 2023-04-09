/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package blcmm.utilities.hex;

import blcmm.model.PatchType;
import static blcmm.model.PatchType.*;
import blcmm.utilities.OSInfo;
import blcmm.utilities.OSInfo.OS;
import static blcmm.utilities.OSInfo.OS.*;
import static blcmm.utilities.hex.HexDictionary.HexType.*;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * "Dictionary" containing the raw info about the various hex edits that we
 * support.  Note that the vast majority of these are *not* actually being
 * used by the game right now!  We only support the legacy main "set"-enabling
 * edit (which is strongly recommended to be enabled via PythonSDK anyway), and
 * the array limit increase.
 *
 * @author LightChaosman
 */
public class HexDictionary {

    //First we just define all our hexedits, for each platform, for each game, for each game.
    //courtesy of c0dycode

    //Enabling set commands:
    // This one works for both BL2+TPS, but not for AODK.
    private static final HexEdit WINDOWS_BOTH_SET_COMMAND = new WildCardHexEdit(
            "83 C4 0C 85 ?? 75 1A 6A",
            "C0",
            "FF");

    // This one looks like it would work for all three.
    private static final HexEdit WINDOWS_BOTH_SAY_COMMAND = new WildCardHexEdit(
            "61 00 77 00 20 00 5B 00 47 00 54 00 5D 00 00 00 ?? 00 ?? 00 ?? 00 ?? 00 00 00 00 00 6D 73 67 20",
            "73 61 79 20",
            "00 00 00 20");

    private static final HexEdit LINUX_BOTH_MAC_BL2_SAY_COMMAND = new PatternHexEdit(//
            "00 00 00 00 43 00 00 00 6F 00 00 00 6E 00 00 00 73 00 00 00 75 00 00 00 6D 00 00 00 65 00 00 00 00 00 00 00 4E 00 00 00",
            -0x10,
            "73 00 00 00 61 00 00 00 79 00 00 00 20 00 00 00",
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");

    private static final HexEdit MAC_TPS_SAY_COMMAND = new PatternHexEdit(//
            "00 00 00 00 43 00 00 00 6F 00 00 00 6E 00 00 00 73 00 00 00 75 00 00 00 6D 00 00 00 65 00 00 00 00 00 00 00 4E 00 00 00",
            -0x18,
            "73 00 00 00 61 00 00 00 79 00 00 00 20 00 00 00",
            "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");

    private static final HexEdit MAC_BL2_ENHANCE = new PatternHexEdit(//
            "41 ?? 01 00 00 00 BA 01 00 00 00 E8 ?? ?? ?? ?? 85 ?? 75 19 48 8D",
            17,
            "C0",
            "D8");
    private static final HexEdit MAC_TPS_ENHANCE = new PatternHexEdit(//
            "89 3C 24 C7 44 24 08 01 00 00 00 C7 44 24 04 ?? ?? ?? ?? E8 ?? ?? ?? ?? 85 ?? 74 ?? 8B 8D",
            25,
            "C0",
            "D8");

    //Array limit removal
    private static final HexEdit WINDOWS_BL2_ARRAYLIMIT_MESSAGE = new WildCardHexEdit(//
            "8B 40 04 83 F8 64 0F ?? ?? ?? ?? ?? 8B 8D ?? ?? ?? ?? 83 C0 9D 50 68",
            "8C 7B 00 00 00 9C EE FF FF",
            "85 7B 00 00 00 9C EE FF FF");

    private static final HexEdit WINDOWS_TPS_ARRAYLIMIT_MESSAGE = new WildCardHexEdit(//
            "8B 40 04 83 F8 64 ?? ?? 8B 8D ?? ?? ?? ?? 83 C0 9D 50 68",
            "7C 7B 94 EE FF FF",
            "EB 7B 94 EE FF FF");

    private static final HexEdit WINDOWS_BOTH_ARRAYLIMIT = new PatternHexEdit(//
            "05 B9 64 00 00 00 3B F9 0F 8D",
            -1,
            "7E",
            "75");

    private static final HexEdit MAC_TPS_ARRAYLIMIT_MESSAGE = new PatternHexEdit(//
            "80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00",
            0x35,
            "0F 8C 2F 01 00 00",
            "0F 85 2F 01 00 00");

    private static final HexEdit MAC_BL2_ARRAYLIMIT_MESSAGE = new PatternHexEdit(//
            "80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00",
            0x35,
            "7C",
            "75");

    private static final HexEdit MAC_BOTH_ARRAYLIMIT = new PatternHexEdit(//
            "80 FF FF 8B 44 08 04 83 F8 64 89 C1 BA 64 00 00 00",
            0x11,
            "0F 4F CA ",
            "90 90 90");

    private static final HexEdit LINUX_BL2_ARRAYLIMIT_MESSAGE = new PatternHexEdit(//
            "FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1",
            0x31,
            "7C",
            "75");
    private static final HexEdit LINUX_TPS_ARRAYLIMIT_MESSAGE = new PatternHexEdit(//
            "FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1",
            0x36,
            "7C",
            "75");
    private static final HexEdit LINUX_BL2_ARRAYLIMIT = new PatternHexEdit(//
            "FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1",
            0x15,
            "0F 4F CB",
            "90 90 90");
    private static final HexEdit LINUX_TPS_ARRAYLIMIT = new PatternHexEdit(//
            "FF 44 24 5C 8B 44 24 6C 8B 4C 24 68 8B 44 08 04 83 F8 64 89 C1",
            0x1A,
            "0F 4F CA",
            "90 90 90");

    //Sanity check disabling
    private static final HexEdit LINUX_BL2_SANITYCHECK_ITEM = new AddressHexEdit(//
            0xD267F0,
            "E8 A9 24 17 00",
            "90 90 90 90 90"
    );
    private static final HexEdit LINUX_BL2_SANITYCHECK_WEAPON = new AddressHexEdit(//
            0xD26870,
            "E8 F7 23 17 00",
            "90 90 90 90 90"
    );
    private static final HexEdit LINUX_TPS_SANITYCHECK_ITEM = new AddressHexEdit(//
            0xCFE148,
            "E8 CF 94 17 00",
            "90 90 90 90 90"
    );
    private static final HexEdit LINUX_TPS_SANITYCHECK_WEAPON = new AddressHexEdit(//
            0xCFE1C8,
            "E8 0D 94 17 00",
            "90 90 90 90 90"
    );

    private static final HexEdit MAC_BL2_SANITYCHECK_ITEM = new AddressHexEdit(//
            0x74AF61,
            "E8 FE A1 28 00",
            "90 90 90 90 90"
    );
    private static final HexEdit MAC_BL2_SANITYCHECK_WEAPON = new AddressHexEdit(//
            0x74AFD5,
            "E8 58 A1 28 00",
            "90 90 90 90 90"
    );
    private static final HexEdit MAC_TPS_SANITYCHECK_ITEM = new AddressHexEdit(//
            0x9B8346,
            "E8 0B 4A CF FF",
            "90 90 90 90 90"
    );
    private static final HexEdit MAC_TPS_SANITYCHECK_WEAPON = new AddressHexEdit(//
            0x9B83BE,
            "E8 4F 49 CF FF",
            "90 90 90 90 90"
    );
    //Windows...

    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_BODY = new PatternHexEdit(//
            "83 7F 10 00 8D 47 10 74",
            "83 7F 10 FF 8D 47 10 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_GRIP = new PatternHexEdit(//
            "83 7F 14 00 8D 47 14 74",
            "83 7F 14 FF 8D 47 14 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_BARREL = new PatternHexEdit(//
            "83 7F 18 00 8D 47 18 74",
            "83 7F 18 FF 8D 47 18 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_SIGHT = new PatternHexEdit(//
            "83 7F 1C 00 8D 47 1C 74",
            "83 7F 1C FF 8D 47 1C 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_STOCK = new PatternHexEdit(//
            "83 7F 20 00 8D 47 20 74 11",
            "83 7F 20 FF 8D 47 20 75 11");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_ELEMENT = new PatternHexEdit(//
            "83 7F 24 00 8D 47 24 74",
            "83 7F 24 FF 8D 47 24 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_ACC1 = new PatternHexEdit(//
            "83 7F 28 00 8D 47 28 74",
            "83 7F 28 FF 8D 47 28 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_ACC2 = new PatternHexEdit(//
            "83 7F 2C 00 8D 47 2C 74",
            "83 7F 2C FF 8D 47 2C 75");
    private static final HexEdit WINDOWS_BL2_SANITYCHECK_WEAPON_MAT = new PatternHexEdit(//
            "83 7F 30 00 8D 47 30 74 11",
            "83 7F 30 FF 8D 47 30 75 11");

    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_BODY = new PatternHexEdit(//
            "83 7E 10 00 8D 46 10 74 30 50 8D 4D DC",
            "83 7E 10 FF 8D 46 10 75 30 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_GRIP = new PatternHexEdit(//
            "83 7E 14 00 8D 46 14 74 0D 50 8D 4D DC",
            "83 7E 14 FF 8D 46 14 75 0D 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_BARREL = new PatternHexEdit(//
            "83 7E 18 00 8D 46 18 74 11 50 8D 4D DC",
            "83 7E 18 FF 8D 46 18 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_SIGHT = new PatternHexEdit(//
            "83 7E 1C 00 8D 46 1C 74 11 50 8D 4D DC",
            "83 7E 1C FF 8D 46 1C 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_STOCK = new PatternHexEdit(//
            "83 7E 20 00 8D 46 20 74 11 50 8D 4D DC",
            "83 7E 20 FF 8D 46 20 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_ELEMENT = new PatternHexEdit(//
            "83 7E 24 00 8D 46 24 74 11 50 8D 4D DC",
            "83 7E 24 FF 8D 46 24 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_ACC1 = new PatternHexEdit(//
            "83 7E 28 00 8D 46 28 74 11 50 8D 4D DC",
            "83 7E 28 FF 8D 46 28 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_ACC2 = new PatternHexEdit(//
            "83 7E 2C 00 8D 46 2C 74 11 50 8D 4D DC",
            "83 7E 2C FF 8D 46 2C 75 11 50 8D 4D DC");
    private static final HexEdit WINDOWS_TPS_SANITYCHECK_WEAPON_MAT = new PatternHexEdit(//
            "83 7E 30 00 8D 46 30 74 11 50 8D 4D DC",
            "83 7E 30 FF 8D 46 30 75 11 50 8D 4D DC");

    //Force offline mode
    private static final HexEdit LINUX_BL2_OFFLINE = new AddressHexEdit(//
            0x00B3B988,
            "E8 E9 0C 5A FF",
            "90 90 90 90 90"
    );

    //Old patterns
    //Set command enablers:
    private static final HexEdit DEPRECATED_ENABLE_SET_COMMANDS_1 = new PatternHexEdit(//
            "83 C4 0C 85 C0 75 1A 6A",
            "83 C4 0C 85 FF 75 1A 6A");

    private static final HexEdit DEPRECATED_ENABLE_SET_COMMANDS_2 = new PatternHexEdit(//
            "73 00 61 00 79 00 20 00",
            "00 00 00 00 00 00 20 00");

    private static final HexEdit DEPRECATED_WINDOWS_BL2_SET_COMMAND = new PatternHexEdit(//
            "FF 68 98 E8 F2 01 51 E8 14 5B D3 FF",
            12,
            "83 C4 0C 85 C0 75 1A 6A",
            "83 C4 0C 85 FF 75 1A 6A");

    private static final HexEdit DEPRECATED_WINDOWS_TPS_SET_COMMAND = new PatternHexEdit(//
            "FF 68 90 17 DB 01 51 E8 D5 55 58 FF 83 C4 0C 85",
            12,
            "83 C4 0C 85 C0 75 1A 6A",
            "83 C4 0C 85 FF 75 1A 6A");

    private static final HexEdit DEPRECATED_WINDOWS_BL2_SAY_COMMAND = new PatternHexEdit(//
            "69 6F 6E 54 69 6D 65 00",
            8,
            "73 00 61 00 79",
            "00 00 00 00 00");

    private static final HexEdit DEPRECATED_WINDOWS_TPS_SAY_COMMAND = new PatternHexEdit(//
            "00 20 00 00 00 00 00 50 6F 6C 79",
            -5,
            "73 00 61 00 79",
            "00 00 00 00 00");

    //Array limits replacements:
    private static final HexEdit DEPRECATED_DISABLE_ARRAY_LIMIT_1 = new PatternHexEdit(//
            "7E 05 B9 64 00 00 00 3B F9 0F 8D",
            "75 05 B9 64 00 00 00 3B F9 0F 8D");

    private static final HexEdit DEPRECATED_DISABLE_ARRAY_LIMIT_2_BL2 = new PatternHexEdit(//
            "0F 8C 7B 00 00 00 8B 8D 9C EE FF FF 83 C0 9D 50",
            "0F 85 7B 00 00 00 8B 8D 9C EE FF FF 83 C0 9D 50");

    private static final HexEdit DEPRECATED_DISABLE_ARRAY_LIMIT_2_TPS = new PatternHexEdit(//
            "7C 7B 8B 8D 94 EE FF FF",
            "75 7B 8B 8D 94 EE FF FF");

    private static final Map<PatchType, Map<HexType, Map<OSInfo.OS, HashSet<HexEdit>>>> DICTIONARY = new HashMap<>();

    static {
        for (PatchType game : PatchType.values()) {
            DICTIONARY.put(game, new EnumMap<>(HexType.class));
            for (HexType type : HexType.values()) {
                DICTIONARY.get(game).put(type, new EnumMap<>(OS.class));
                for (OS os : OS.values()) {
                    DICTIONARY.get(game).get(type).put(os, new HashSet<>());
                }
            }
        }
        //console enabling
        //windows
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS).get(WINDOWS).add(WINDOWS_BOTH_SET_COMMAND);
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS).get(WINDOWS).add(WINDOWS_BOTH_SAY_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS).get(WINDOWS).add(WINDOWS_BOTH_SET_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS).get(WINDOWS).add(WINDOWS_BOTH_SAY_COMMAND);
        //mac
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS).get(MAC).add(LINUX_BOTH_MAC_BL2_SAY_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS).get(MAC).add(MAC_TPS_SAY_COMMAND);
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS).get(MAC).add(MAC_BL2_ENHANCE);
        // DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS).get(MAC).add(MAC_TPS_ENHANCE);//The current one above seems to disable mods completely
        //linux
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS).get(UNIX).add(LINUX_BOTH_MAC_BL2_SAY_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS).get(UNIX).add(LINUX_BOTH_MAC_BL2_SAY_COMMAND);

        //array limit disabling
        //windows
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(WINDOWS).add(WINDOWS_BL2_ARRAYLIMIT_MESSAGE);
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(WINDOWS).add(WINDOWS_BOTH_ARRAYLIMIT);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(WINDOWS).add(WINDOWS_TPS_ARRAYLIMIT_MESSAGE);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(WINDOWS).add(WINDOWS_BOTH_ARRAYLIMIT);
        //mac
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(MAC).add(MAC_BOTH_ARRAYLIMIT);
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(MAC).add(MAC_BL2_ARRAYLIMIT_MESSAGE);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(MAC).add(MAC_BOTH_ARRAYLIMIT);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(MAC).add(MAC_TPS_ARRAYLIMIT_MESSAGE);
        //linux
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(UNIX).add(LINUX_BL2_ARRAYLIMIT);
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT).get(UNIX).add(LINUX_BL2_ARRAYLIMIT_MESSAGE);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(UNIX).add(LINUX_TPS_ARRAYLIMIT);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT).get(UNIX).add(LINUX_TPS_ARRAYLIMIT_MESSAGE);

        //sanity check disabling
        //mac
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(MAC).add(MAC_BL2_SANITYCHECK_ITEM);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(MAC).add(MAC_BL2_SANITYCHECK_WEAPON);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(MAC).add(MAC_TPS_SANITYCHECK_ITEM);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(MAC).add(MAC_TPS_SANITYCHECK_WEAPON);
        //linux
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(UNIX).add(LINUX_BL2_SANITYCHECK_ITEM);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(UNIX).add(LINUX_BL2_SANITYCHECK_WEAPON);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(UNIX).add(LINUX_TPS_SANITYCHECK_ITEM);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(UNIX).add(LINUX_TPS_SANITYCHECK_WEAPON);
        //windows
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_ACC1);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_ACC2);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_BARREL);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_BODY);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_ELEMENT);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_GRIP);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_MAT);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_SIGHT);
        DICTIONARY.get(BL2).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_BL2_SANITYCHECK_WEAPON_STOCK);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_ACC1);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_ACC2);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_BARREL);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_BODY);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_ELEMENT);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_GRIP);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_MAT);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_SIGHT);
        DICTIONARY.get(TPS).get(DISABLE_SANITYCHECK).get(WINDOWS).add(WINDOWS_TPS_SANITYCHECK_WEAPON_STOCK);

        //offline-only mode
        //linux
        DICTIONARY.get(BL2).get(FORCE_OFFLINE).get(UNIX).add(LINUX_BL2_OFFLINE);

        //deprecated entries
        //console enabling
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_ENABLE_SET_COMMANDS_1);
        DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_ENABLE_SET_COMMANDS_2);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_ENABLE_SET_COMMANDS_1);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_ENABLE_SET_COMMANDS_2);
        //DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_WINDOWS_BL2_SET_COMMAND);
        //DICTIONARY.get(BL2).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_WINDOWS_BL2_SAY_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_WINDOWS_TPS_SET_COMMAND);
        DICTIONARY.get(TPS).get(ENABLE_SET_COMMANDS_OLD).get(WINDOWS).add(DEPRECATED_WINDOWS_TPS_SAY_COMMAND);
        //array limit disabling
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT_OLD).get(WINDOWS).add(DEPRECATED_DISABLE_ARRAY_LIMIT_1);
        DICTIONARY.get(BL2).get(DISABLE_ARRAY_LIMIT_OLD).get(WINDOWS).add(DEPRECATED_DISABLE_ARRAY_LIMIT_2_BL2);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT_OLD).get(WINDOWS).add(DEPRECATED_DISABLE_ARRAY_LIMIT_1);
        DICTIONARY.get(TPS).get(DISABLE_ARRAY_LIMIT_OLD).get(WINDOWS).add(DEPRECATED_DISABLE_ARRAY_LIMIT_2_TPS);

    }

    public static HexEdit[] getHexEdits(PatchType type, OS os, HexType... types) {
        return getHexEdits(new HexQuery(os, type, types));
    }

    public static HexEdit[] getHexEdits(PatchType type, OS os, Collection<HexType> types) {
        return getHexEdits(new HexQuery(os, type, types));
    }

    public static HexEdit[] getHexEdits(HexQuery query) {
        if (query.types.length == 0) {
            throw new IllegalArgumentException();
        }
        HashSet<HashSet<HexEdit>> foundEdits = new HashSet<>();
        for (HexType type : query.types) {
            foundEdits.add(DICTIONARY.get(query.game).get(type).get(query.OS));
        }
        HashSet<HexEdit> res = new HashSet<>();
        for (HashSet<HexEdit> set : foundEdits) {
            if (set == null) {
                throw new NullPointerException("Non-existant hex edits requested");
            }
            if (set.isEmpty()) {
                //throw new IllegalStateException("Non-existant hex edits requested");
            }
            res.addAll(set);
        }
        return res.toArray(new HexEdit[0]);
    }

    static Collection<HexEdit> getEveryHexEditForThisGameAndOS(PatchType game, OSInfo.OS os) {
        HashSet<HexEdit> hexEdits = new HashSet<>();
        HashSet<HashSet<HexEdit>> foundEdits = new HashSet<>();

        for (HexType type : HexType.values()) {
            // We don't want any old hex-edits, only the ones we can actually apply right now
            if (type.toString().contains("_OLD")) {
                continue;
            }
            foundEdits.add(DICTIONARY.get(game).get(type).get(os));
        }
        // Filter all null results & add all results to hexEdits
        foundEdits.stream().filter(p -> p != null).forEachOrdered(hexEdits::addAll);
        return hexEdits;
    }

    public static class HexQuery {

        public final OSInfo.OS OS;
        public final PatchType game;
        public final HexType[] types;

        public HexQuery(OSInfo.OS OS, PatchType type, HexType... types) {
            this.OS = OS;
            this.game = type;
            this.types = types;
        }

        public HexQuery(OSInfo.OS OS, PatchType type, Collection<HexType> types) {
            this.OS = OS;
            this.game = type;
            this.types = types.toArray(new HexType[0]);
        }

    }

    public static enum HexType {
        ENABLE_SET_COMMANDS, DISABLE_ARRAY_LIMIT,
        DISABLE_SANITYCHECK, FORCE_OFFLINE,
        ENABLE_SET_COMMANDS_OLD, DISABLE_ARRAY_LIMIT_OLD
    }

}
