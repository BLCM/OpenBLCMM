/*
 * Copyright (C) 2023 CJ Kucera
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
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */

package blcmm.model;

import blcmm.utilities.Options;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Enum to describe the patch types available for BLCMM.  This class was
 * reimplemented based on the calls BLCMM makes into BLCMM_Utilities.jar,
 * without reference to the original sourcecode.
 * 
 * @author apocalyptech
 */
public enum PatchType {
    
    // Members
    BL2("/resources/BL2/Icon.png", new String[] {
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
    }),
    TPS("/resources/TPS/Icon.png", new String[] {
        "GD_Gladiator_Streaming",
        "GD_Enforce_Streaming",
        "GD_Lawbringer_Streaming",
        "GD_Prototype_Streaming",
        "Quince_Doppel_Streaming",
        "Crocus_Baroness_Streaming",
        "GD_MoonBuggy_Streaming",
        "GD_Co_Stingray_Streaming",
    });
    
    // Offline strings
    public static String OFFLINE1 = "set Transient.SparkServiceConfiguration_0 ServiceName Micropatch";
    public static String OFFLINE2 = "set Transient.SparkServiceConfiguration_0 ConfigurationGroup Default";
    public static String OFFLINE3 = "set Transient.GearboxAccountData_1 Services (Transient.SparkServiceConfiguration_0)";
    
    // This is a bit stupid but it's sometimes useful to have it as a set,
    // and others as a list
    private final HashSet<String> ONDEMAND_SET_LOWER = new HashSet<>();
    private final ArrayList<String> ONDEMAND_LIST = new ArrayList<>();
    
    // Icon resource location
    private final String iconPath;
    
    /**
     * Initialize a new PatchType.
     * 
     * @param iconPath The resource path to the game's icon
     * @param onDemands An array describing the valid OnDemand types for the game
     */
    private PatchType(String iconPath, String[] onDemands) {
        // Set the icon path
        this.iconPath = iconPath;
        
        // Set OnDemand types
        for (String onDemand : onDemands) {
            this.addOnDemand(onDemand);
        }
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
     * Get the full list of OnDemand hotfix keys.
     * @return 
     */
    public List<String> getOnDemandPackages() {
        return this.ONDEMAND_LIST;
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
     * Returns the initial text of the area of the BLCMM file which contains hotfixes.
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
     * Returns the "center" text of the area of the BLCMM file which contains hotfixes.
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
     * Returns the end of the area of the BLCMM file which contains hotfixes. This
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
    public Image getIcon() {
        try {
            return ImageIO.read(getClass().getResourceAsStream(this.iconPath));
        } catch (IOException e) {
            return new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        }
    }
    
    /**
     * Returns an icon appropriate for the PatchType, scaled to the given width/height.
     * 
     * @param size The size to scale to -- images are assumed to be square
     * @return The icon for the game
     */
    public Image getIcon(int size) {
        Image im = this.getIcon();
        return im.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }

}
