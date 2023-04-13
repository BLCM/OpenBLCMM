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
package blcmm.utilities.options;

import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.model.PatchIO;
import javax.swing.JComponent;

/**
 *
 * @author LightChaosman
 */
public class StringListOption extends Option<String[]> {

    public StringListOption(String name, String[] defaultData) {
        super(name, defaultData);
    }

    @Override
    public String[] stringToData(String stringData) {
        //Remove the square braces *and* the opening quote for the first entry, and the closing quote for the last entry
        if (stringData.equals("[]")) {
            return new String[0];
        }
        String nobrackets = stringData.substring(2, stringData.length() - 2);
        String[] split = nobrackets.split("\",\"");
        for (int i = 0; i < split.length; i++) {
            split[i] = PatchIO.unescape(split[i]);
        }
        return split;
    }

    @Override
    public String dataToString() {
        if (getData().length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\"" + PatchIO.escape(getData()[0]) + "\"");
        for (int i = 1; i < getData().length; i++) {
            sb.append(",\"" + PatchIO.escape(getData()[i]) + "\"");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        return null;
    }

}