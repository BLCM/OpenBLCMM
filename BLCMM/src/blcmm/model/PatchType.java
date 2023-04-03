/*
 * Copyright (C) 2023 Christopher J. Kucera
 * <cj@apocalyptech.com>
 * <https://apocalyptech.com/contact.php>
 *
 * OpenBLCMM is free software: you can redistribute it and/or modify
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
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the original proprietary BLCMM Launcher, BLCMM
 * Lib Distributor, BLCMM Utilities, or BLCMM Data Interaction Library
 * Jarfiles (or modified versions of those libraries), containing parts
 * covered by the terms of their proprietary license, the licensors of
 * this Program grant you additional permission to convey the resulting
 * work.
 *
 */

package blcmm.model;

import blcmm.utilities.IconManager;
import blcmm.utilities.Options;
import java.awt.image.BufferedImage;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Enum to describe the patch types available for OpenBLCMM.  This class was
 * reimplemented based on the calls BLCMM made into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 *
 * @author apocalyptech
 */
public enum PatchType {

    // Members
    BL2("Borderlands 2",
            new String[] {
                "GD_Soldier_Streaming",
                "GD_Siren_Streaming",
                "GD_Mercenary_Streaming",
                "GD_Assassin_Streaming",
                "GD_Tulip_Mechro_Streaming",
                "GD_Lilac_Psycho_Streaming",
                "GD_Runner_Streaming",
                "GD_BTech_Streaming",
                "GD_Orchid_HarpoonHovercraft",
                "GD_Orchid_RocketHovercraft",
                "GD_Orchid_SawHovercraft",
                "GD_Sage_ShockFanBoat",
                "GD_Sage_CorrosiveFanBoat",
                "GD_Sage_IncendiaryFanBoat",
            }, new BLMap[] {
                new BLMap("Ash_P", "Eridium Blight"),
                new BLMap("BackBurner_P", "The Backburner"),
                new BLMap("BanditSlaughter_P", "Fink's Slaughterhouse"),
                new BLMap("Boss_Cliffs_P", "The Bunker"),
                new BLMap("Boss_Volcano_P", "Vault of the Warrior"),
                new BLMap("CastleExterior_P", "Hatred's Shadow"),
                new BLMap("CastleKeep_P", "Dragon Keep"),
                new BLMap("caverns_p", "Caustic Caverns"),
                new BLMap("Colosseum_P", "Colosseum"),
                new BLMap("Cove_P", "Southern Shelf - Bay"),
                new BLMap("CraterLake_P", "Sawtooth Cauldron"),
                new BLMap("CreatureSlaughter_P", "Natural Selection Annex"),
                new BLMap("dam_p", "Bloodshot Stronghold"),
                new BLMap("damtop_p", "Bloodshot Ramparts"),
                new BLMap("Dark_Forest_P", "The Forest"),
                new BLMap("Dead_Forest_P", "Immortal Woods"),
                new BLMap("Distillery_P", "Rotgut Distillery"),
                new BLMap("Docks_P", "Unassuming Docks"),
                new BLMap("Dungeon_P", "Lair of Infinite Agony"),
                new BLMap("DungeonRaid_P", "The Winged Storm"),
                new BLMap("Easter_P", "Wam Bam Island"),
                new BLMap("FinalBossAscent_P", "Hero's Pass"),
                new BLMap("Fridge_P", "The Fridge"),
                new BLMap("Frost_P", "Three Horns - Valley"),
                new BLMap("Fyrestone_P", "Arid Nexus - Boneyard"),
                new BLMap("GaiusSanctuary_P", "Paradise Sanctum"),
                new BLMap("Glacial_P", "Windshear Waste"),
                new BLMap("Grass_Cliffs_P", "Thousand Cuts"),
                new BLMap("Grass_Lynchwood_P", "Lynchwood"),
                new BLMap("Grass_P", "The Highlands"),
                new BLMap("Helios_P", "Helios Fallen"),
                new BLMap("Hunger_P", "Gluttony Gulch"),
                new BLMap("HyperionCity_P", "Opportunity"),
                new BLMap("HypInterlude_P", "Friendship Gulag"),
                new BLMap("icecanyon_p", "Frostburn Canyon"),
                new BLMap("Ice_P", "Three Horns - Divide"),
                new BLMap("Interlude_P", "The Dust"),
                new BLMap("Iris_DL1_P", "Arena"),
                new BLMap("Iris_DL1_TAS_P", "Arena"),
                new BLMap("Iris_DL2_Interior_P", "Pyro Pete's Bar"),
                new BLMap("Iris_DL2_P", "The Beatdown"),
                new BLMap("Iris_DL3_P", "Forge"),
                new BLMap("Iris_Hub2_P", "Southern Raceway"),
                new BLMap("Iris_Hub_P", "Badass Crater of Badassitude"),
                new BLMap("Iris_Moxxi_P", "Badass Crater Bar"),
                new BLMap("Luckys_P", "The Holy Spirits"),
                new BLMap("Mines_P", "Mines of Avarice"),
                new BLMap("MissionTest_P", "Mission Test"),
                new BLMap("OldDust_P", "Dahl Abandon"),
                new BLMap("Orchid_Caves_P", "Hayter's Folly"),
                new BLMap("Orchid_OasisTown_P", "Oasis"),
                new BLMap("Orchid_Refinery_P", "Washburne Refinery"),
                new BLMap("Orchid_SaltFlats_P", "Wurmwater"),
                new BLMap("Orchid_ShipGraveyard_P", "The Rustyards"),
                new BLMap("Orchid_Spire_P", "Magnys Lighthouse"),
                new BLMap("Orchid_WormBelly_P", "The Leviathan's Lair"),
                new BLMap("Outwash_P", "The Highlands - Outwash"),
                new BLMap("PandoraPark_P", "Wildlife Exploitation Preserve"),
                new BLMap("Pumpkin_Patch_P", "Hallowed Hollow"),
                new BLMap("ResearchCenter_P", "Mt. Scarab Research Center"),
                new BLMap("RobotSlaughter_P", "Ore Chasm"),
                new BLMap("Sage_Cliffs_P", "Candlerakk's Crag"),
                new BLMap("Sage_HyperionShip_P", "H.S.S. Terminus"),
                new BLMap("Sage_PowerStation_P", "Ardorton Station"),
                new BLMap("Sage_RockForest_P", "Scylla's Grove"),
                new BLMap("Sage_Underground_P", "Hunter's Grotto"),
                new BLMap("SanctIntro_P", "Fight for Sanctuary"),
                new BLMap("SanctuaryAir_P", "Sanctuary"),
                new BLMap("Sanctuary_Hole_P", "Sanctuary Hole"),
                new BLMap("Sanctuary_P", "Sanctuary"),
                new BLMap("SandwormLair_P", "Writhing Deep"),
                new BLMap("Sandworm_P", "The Burrows"),
                new BLMap("SouthernShelf_P", "Southern Shelf"),
                new BLMap("SouthpawFactory_P", "Southpaw Steam & Power"),
                new BLMap("Stockade_P", "Arid Nexus - Badlands"),
                new BLMap("TempleSlaughter_P", "Murderlin's Temple"),
                new BLMap("TestingZone_P", "The Raid on Digistruct Peak"),
                new BLMap("ThresherRaid_P", "Terramorphous Peak"),
                new BLMap("tundraexpress_p", "Tundra Express"),
                new BLMap("TundraTrain_P", "End of the Line"),
                new BLMap("Village_P", "Flamerock Refuge"),
                new BLMap("VOGChamber_P", "Control Core Angel"),
                new BLMap("Xmas_P", "Frost Bottom"),
            }),
    TPS("Borderlands: The Pre-Sequel",
            new String[] {
                "GD_Gladiator_Streaming",
                "GD_Enforce_Streaming",
                "GD_Lawbringer_Streaming",
                "GD_Prototype_Streaming",
                "Quince_Doppel_Streaming",
                "Crocus_Baroness_Streaming",
                "GD_MoonBuggy_Streaming",
                "GD_Co_Stingray_Streaming",
            }, new BLMap[] {
                new BLMap("Access_P", "Tycho's Ribs"),
                new BLMap("centralterminal_p", "Hyperion Hub of Heroism"),
                new BLMap("ComFacility_P", "Crisis Scar"),
                new BLMap("DahlFactory_Boss", "Titan Robot Production Plant"),
                new BLMap("DahlFactory_P", "Titan Industrial Facility"),
                new BLMap("Deadsurface_P", "Regolith Range"),
                new BLMap("Digsite_P", "Vorago Solitude"),
                new BLMap("Digsite_Rk5arena_P", "Outfall Pumping Station"),
                new BLMap("Eridian_slaughter_P", "The Holodome"),
                new BLMap("InnerCore_p", "Eleseer"),
                new BLMap("InnerHull_P", "Veins of Helios"),
                new BLMap("JacksOffice_P", "Jack's Office"),
                new BLMap("LaserBoss_P", "Eye of Helios"),
                new BLMap("Laser_P", "Lunar Launching Station"),
                new BLMap("Ma_Deck13_P", "Deck 13 1/2"),
                new BLMap("Ma_FinalBoss_P", "Deck 13.5"),
                new BLMap("Ma_LeftCluster_P", "Cluster 00773 P4ND0R4"),
                new BLMap("Ma_Motherboard_P", "Motherlessboard"),
                new BLMap("Ma_Nexus_P", "The Nexus"),
                new BLMap("Ma_RightCluster_P", "Cluster 99002 0V3RL00K"),
                new BLMap("Ma_SubBoss_P", "The Cortex"),
                new BLMap("Ma_Subconscious_P", "Subconscious"),
                new BLMap("Meriff_P", "The Meriff's Office"),
                new BLMap("Moon_P", "Triton Flats"),
                new BLMap("MoonShotIntro_P", "Helios Station"),
                new BLMap("MoonSlaughter_P", "Abandoned Training Facility"),
                new BLMap("Moonsurface_P", "Serenity's Waste"),
                new BLMap("Outlands_P2", "Outlands Canyon"),
                new BLMap("Outlands_P", "Outlands Spur"),
                new BLMap("RandDFacility_P", "Research and Development"),
                new BLMap("Spaceport_P", "Concordia"),
                new BLMap("StantonsLiver_P", "Stanton's Liver"),
                new BLMap("Sublevel13_P", "Sub-Level 13"),
                new BLMap("Wreck_P", "Pity's Fall"),
            });

    /**
     * Information about a Bordlerlands map.  This used to come from the
     * data library, but IMO this is the kind of thing that should work even
     * if the user *doesn't* have data installed.  I'd really rather just
     * hardcode it in here.
     */
    public static class BLMap {
        public String code, name;

        public BLMap(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    // Offline strings
    public static String OFFLINE1 = "set Transient.SparkServiceConfiguration_0 ServiceName Micropatch";
    public static String OFFLINE2 = "set Transient.SparkServiceConfiguration_0 ConfigurationGroup Default";
    public static String OFFLINE3 = "set Transient.GearboxAccountData_1 Services (Transient.SparkServiceConfiguration_0)";

    // This is a bit stupid but it's sometimes useful to have it as a set,
    // and others as a list
    private final HashSet<String> ONDEMAND_SET_LOWER = new HashSet<>();
    private final ArrayList<String> ONDEMAND_LIST = new ArrayList<>();
    private final HashSet<String> LEVEL_SET_LOWER = new HashSet<>();
    private final ArrayList<BLMap> LEVEL_LIST = new ArrayList<>();

    // Icon resource location
    private final String iconPath;

    // Game name
    private final String gameName;

    /**
     * Initialize a new PatchType.
     *
     * @param onDemands An array describing the valid OnDemand types for the game
     */
    private PatchType(String gameName, String[] onDemands, BLMap[] levels) {
        // Set the game name and icon path
        this.gameName = gameName;
        this.iconPath = "/resources/" + this.toString() + "/Icon.png";

        // Set OnDemand types
        for (String onDemand : onDemands) {
            this.addOnDemand(onDemand);
        }

        // Set Levels
        for (BLMap level : levels) {
            this.addLevel(level);
        }
    }

    /**
     * Returns the English game name for this type
     *
     * @return The game name
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Adds a new key for OnDemand hotfixes
     * @param onDemand The string to match
     */
    private void addOnDemand(String onDemand) {
        this.ONDEMAND_LIST.add(onDemand);
        this.ONDEMAND_SET_LOWER.add(onDemand.toLowerCase());
    }

    /**
     * Adds a new key for Level hotfixes
     * @param onDemand The string to match
     */
    private void addLevel(BLMap level) {
        this.LEVEL_LIST.add(level);
        this.LEVEL_SET_LOWER.add(level.code.toLowerCase());
    }

    /**
     * Get the full list of OnDemand hotfix keys.
     * @return
     */
    public List<String> getOnDemandPackages() {
        return this.ONDEMAND_LIST;
    }

    /**
     * Get the full list of lvel hotfix keys.
     * @return
     */
    public List<BLMap> getLevels() {
        return this.LEVEL_LIST;
    }

    /**
     * Check to see if the specified OnDemand hotfix key is valid for our PatchType
     * @param onDemand The hotfix key to check
     * @return True if the OnDemand type matches this game
     */
    public boolean isOnDemandInPatchType(String onDemand) {
        return this.ONDEMAND_SET_LOWER.contains(onDemand.toLowerCase());
    }

    /**
     * Check to see if the specified level hotfix key is valid for our PatchType
     * @param level The hotfix key to check
     * @return True if the Level type matches this game
     */
    public boolean isLevelInPatchType(String level) {
        return this.LEVEL_SET_LOWER.contains(level.toLowerCase());
    }

    /**
     * Returns the initial text of the area of the OpenBLCMM file which contains hotfixes.
     * This brings us right up to the opening-paren of the hotfix key statement.
     *
     * @param offline If this is being saved in offline mode or not
     * @param linebreak The linebreak to use on the saved file
     * @return The multiline string
     */
    public String getFunctionalHotfixPrefix(boolean offline, String linebreak) {
        ArrayList<String> lines = new ArrayList<>();
        int serviceNumber;
        if (offline) {
            serviceNumber = 0;
            lines.add(OFFLINE1);
            lines.add("");
            lines.add(OFFLINE2);
            lines.add("");
        } else {
            serviceNumber = Options.INSTANCE.getIntOptionData(Options.OptionNames.onlineServiceNumber);
        }
        lines.add("set Transient.SparkServiceConfiguration_" + serviceNumber + " Keys (");

        return String.join(linebreak, lines);
    }

    /**
     * Returns the "center" text of the area of the OpenBLCMM file which contains hotfixes.
     * This closes the paren on the key statement and brings us up to the open-paren
     * of the values statement.
     *
     * @param offline If this is being saved in offline mode or not
     * @param linebreak The linebreak to use on the saved file
     * @return The multiline string
     */
    public String getFunctionalHotfixCenter(boolean offline, String linebreak) {
        ArrayList<String> lines = new ArrayList<>();
        int serviceNumber;
        if (offline) {
            serviceNumber = 0;
        } else {
            serviceNumber = Options.INSTANCE.getIntOptionData(Options.OptionNames.onlineServiceNumber);
        }
        lines.add(")");
        lines.add("set Transient.SparkServiceConfiguration_" + serviceNumber + " Values (");
        return String.join(linebreak, lines);
    }

    /**
     * Returns the end of the area of the OpenBLCMM file which contains hotfixes. This
     * closes the hotfix value statement and then closes out the file.
     *
     * @param offline If this is being saved in offline mode or not
     * @param linebreak The linebreak to use on the saved file
     * @return The multiline string
     */
    public String getFunctionalHotfixPostfix(boolean offline, String linebreak) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(")");
        lines.add("");
        if (offline) {
            lines.add(OFFLINE3);
            lines.add("");
        }
        lines.add("");
        return String.join(linebreak, lines);
    }

    /**
     * Returns an icon appropriate for the PatchType, or an "empty" image if
     * the image can't be loaded.
     *
     * @return The icon for the game
     */
    public BufferedImage getIcon() {
        return IconManager.getIcon(this.iconPath);
    }

    /**
     * Returns an icon appropriate for the PatchType, scaled to the given width/height.
     *
     * @param size The size to scale to -- images are assumed to be square
     * @return The icon for the game
     */
    public BufferedImage getIcon(int size) {
        return IconManager.getIcon(this.iconPath, size);
    }

    /**
     * Returns the timestamp of our extracted SQLite data as of the last time
     * it was successfully verified, as reported in our app options file.  It's
     * possible this belongs more in DataManager than here, but I'd kind of
     * prefer to keep the case-handling internal.  (Though there are doubtless
     * cases where I have *not* done so, throughout the code.
     *
     * @return The timestamp of the database file (in milliseconds) when last
     *         it was successfully verified, or 0 if the verification has never
     *         succeeded yet.
     */
    public long getOEDataSuccessTimestampDb() {
        switch (this) {
            case TPS:
                return Options.INSTANCE.getOEDataSuccessTimestampDbTPS();
            case BL2:
            default:
                return Options.INSTANCE.getOEDataSuccessTimestampDbBL2();
        }
    }

    /**
     * Returns the timestamp of our data Jar file, as of the last time the
     * extracted SQLite database was verified against the checksum in the
     * Jar file, as reported in our app options file.  It's possible this
     * belongs more in DataManager than here, but I'd kind of prefer to keep the
     * case-handling internal.  (Though there are doubtless cases where I have
     * *not* done so, throughout the code.
     *
     * @return The timestamp of the Jar file (in milliseconds) when the DB was
     *         successfully verified, or 0 if the verification has never
     *         succeeded yet.
     */
    public long getOEDataSuccessTimestampJar() {
        switch (this) {
            case TPS:
                return Options.INSTANCE.getOEDataSuccessTimestampJarTPS();
            case BL2:
            default:
                return Options.INSTANCE.getOEDataSuccessTimestampJarBL2();
        }
    }

    /**
     * Given a BasicFileAttributes object, update our last-successfully-verified
     * timestamp for the extracted SQLite database in the options file.  As with
     * the getters above, arguably this should be in DataManager rather than
     * here...
     *
     * @param attrs The attributes containing the file's timestamp to store.
     */
    public void setOEDataSuccessTimestampDb(BasicFileAttributes attrs) {
        switch (this) {
            case TPS:
                Options.INSTANCE.setOEDataSuccessTimestampDbTPS(attrs.lastModifiedTime().toMillis());
                break;
            case BL2:
            default:
                Options.INSTANCE.setOEDataSuccessTimestampDbBL2(attrs.lastModifiedTime().toMillis());
                break;
        }
    }

    /**
     * Given a BasicFileAttributes object, update our last-successfully-verified
     * timestamp for the data jarfile in the options file.  As with the getters
     * above, arguably this should be in DataManager rather than here...
     *
     * @param attrs The attributes containing the file's timestamp to store.
     */
    public void setOEDataSuccessTimestampJar(BasicFileAttributes attrs) {
        switch (this) {
            case TPS:
                Options.INSTANCE.setOEDataSuccessTimestampJarTPS(attrs.lastModifiedTime().toMillis());
                break;
            case BL2:
            default:
                Options.INSTANCE.setOEDataSuccessTimestampJarBL2(attrs.lastModifiedTime().toMillis());
                break;
        }
    }


}
