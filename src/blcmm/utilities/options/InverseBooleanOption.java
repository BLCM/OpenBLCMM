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
import blcmm.gui.components.FontInfoJCheckBox;
import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.OptionsBase;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * Boolean option whose GUI checkbox should actually store/display the *inverse*
 * of the on-disk value.
 *
 * @author apocalyptech
 */
public class InverseBooleanOption extends BooleanOption {

    /**
     * Constructor for a boolean option which will not be displayed on the
     * settings panel.
     *
     * @param optionsObj The Options that this Option is a part of
     * @param name Key for the option
     * @param fontInfo Font information to use on the option
     * @param defaultData Default value for the option
     */
    public InverseBooleanOption(OptionsBase optionsObj, String name, FontInfo fontInfo, boolean defaultData) {
        super(optionsObj, name, fontInfo, defaultData);
    }

    /**
     * Constructor for a boolean option. If displayDesc is null, the option will
     * not be shown on the settings panel.
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
    public InverseBooleanOption(OptionsBase optionsObj,
            String name,
            FontInfo fontInfo,
            boolean defaultData,
            Option.Shown shownPanel,
            String displayDesc,
            String callback,
            String tooltip) {
        super(optionsObj, name, fontInfo, defaultData, shownPanel, displayDesc, callback, tooltip);
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        BooleanOption option = this;
        FontInfoJCheckBox check = new FontInfoJCheckBox(this.fontInfo);
        check.setSelected(!this.getData());
        check.setHorizontalAlignment(SwingConstants.RIGHT);
        check.addActionListener(ae -> {
            setData(!check.isSelected());
            panel.callBack(option, check);
            optionsObj.save();
        });
        return check;
    }

}
