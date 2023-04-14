/*
 * Copyright (C) 2018-2020  LightChaosman
 * Copyright (C) 2023 Christopher J. Kucera
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

import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.OptionsBase;
import blcmm.utilities.Utilities;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

/**
 * A filename option
 *
 * @author apocalyptech
 */
public class FilenameOption extends Option<String> {

    /**
     * Constructor for a string option which will not be displayed on the
     * settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key for the option
     * @param defaultData Default value for the option
     */
    public FilenameOption(OptionsBase optionsObj, String name, String defaultData) {
        super(optionsObj, name, defaultData);
    }

    /**
     * Constructor for a string option. If displayDesc is null, the option will
     * not be shown on the settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key for the option
     * @param defaultData Default value for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     */
    public FilenameOption(OptionsBase optionsObj,
            String name,
            String defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip) {
        super(optionsObj, name, defaultData, shownPanel, displayDesc, callback, tooltip);
    }

    /**
     * Converts the given string (read from a config file) into the correct data
     * type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    @Override
    public String stringToData(String stringData) {
        return stringData;
    }

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    @Override
    public String dataToString() {
        return this.getData();
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        FilenameOption option = this;
        JButton but = new JButton("Select file");
        but.addActionListener((ActionEvent ae) -> {
            JFileChooser fc = new BLCMM_FileChooser(Utilities.getDefaultOpenLocation());
            int returnVal = fc.showOpenDialog(panel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                setData(file.getAbsolutePath());
                panel.callBack(option, but);
                optionsObj.save();
            }
        });
        return but;
    }

}
