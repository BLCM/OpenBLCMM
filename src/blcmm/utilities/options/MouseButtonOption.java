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
 */
package blcmm.utilities.options;

import blcmm.gui.FontInfo;
import blcmm.gui.components.FontInfoJButton;
import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.OptionsBase;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * An integer-based option to store the selected mouse button based on the
 * user clicking on a JButton.  The MouseEvent generated by the JButton click
 * will be used as the new value of the option.
 *
 * @author apocalyptech
 */
public class MouseButtonOption extends Option<Integer> {

    /**
     * Constructor for a new Mouse-Button option.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key for the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default value for the option
     * @param shownPanel The panel on which to show this option
     * @param displayDesc Display description on the settings panel
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     */
    public MouseButtonOption(OptionsBase optionsObj,
            String name,
            FontInfo fontInfo,
            int defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip) {
        super(optionsObj, name, fontInfo, defaultData, shownPanel, displayDesc, callback, tooltip);
    }

    /**
     * Converts the given string (read from a config file) into the correct data
     * type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    @Override
    public Integer stringToData(String stringData) {
        return Integer.parseInt(stringData);
    }

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    @Override
    public String dataToString() {
        return Integer.toString(this.getData());
    }

    /**
     * Update our button label with our current data
     */
    private void updateLabel(JButton button) {
        button.setText("<html><b>" + this.dataToString() + "</b> <i>(Click to Change)</i>");
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        FontInfoJButton button = new FontInfoJButton(this.fontInfo);
        this.updateLabel(button);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setData(e.getButton());
                updateLabel(button);
            }
        });
        return button;
    }

}
