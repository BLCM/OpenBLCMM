/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm.gui.theme;

import blcmm.utilities.options.SelectionOptionData;
import java.awt.Color;
import java.util.HashMap;
import javax.swing.ImageIcon;

/**
 * Small class for containing the available themes
 *
 * @author LightChaosman
 */
public class Theme implements SelectionOptionData {

    static {
        ThemeManager.getTheme();//Force the ThemeManager to initialize
    }

    //The names displayed in the UI
    private final String name;
    private final String displayName;

    //The hashmaps containing the colors and the icons of the theme.
    private final HashMap<ThemeManager.ColorType, Color> colorMap = new HashMap<>();
    private final HashMap<String, ImageIcon> iconMap = new HashMap<>();

    /**
     * The private constructor for this class
     *
     * @param name
     */
    Theme(String name) {
        this(name, null);
    }

    Theme(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName == null ? name + " mode" : displayName;
    }

    @Override
    public String toSaveString() {
        return this.name;
    }

    public Color get(ThemeManager.ColorType colortype) {
        return colorMap.get(colortype);
    }

    void put(ThemeManager.ColorType colortype, Color color) {
        colorMap.put(colortype, color);
    }

    HashMap<ThemeManager.ColorType, Color> getColorMap() {
        return colorMap;
    }

    HashMap<String, ImageIcon> getIconMap() {
        return iconMap;
    }

}
