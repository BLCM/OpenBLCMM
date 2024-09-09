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
import blcmm.gui.components.FontInfoJLabel;
import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.OptionsBase;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A fake "Option" to display a section header on our Options screen, which
 * isn't *really* an option.
 *
 * @author apocalyptech
 */
public class SectionHeaderOption extends Option<Integer> {

    private static int num_headers = 0;
    private String headerText;

    /**
     * Creates a new section header.
     *
     * @param optionsObj The Options object we're contained within
     * @param shownPanel The panel in which we appear
     * @param headerText The header text to display
     * @param fontInfo Font information to use on the option
     */
    public SectionHeaderOption(OptionsBase optionsObj, Option.Shown shownPanel, String headerText, FontInfo fontInfo) {
        super(optionsObj, "_sectionheaderoption_header_" + Integer.toString(SectionHeaderOption.num_headers), fontInfo, shownPanel);
        SectionHeaderOption.num_headers++;
        this.headerText = headerText;
    }

    @Override
    public void setData(Integer newData) {
    }

    @Override
    public Integer stringToData(String stringData) {
        return 1;
    }

    @Override
    public String dataToString() {
        return "";
    }

    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        FontInfoJLabel label = new FontInfoJLabel("<html><b>" + this.headerText + "</b><hr/>", this.fontInfo);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

}
