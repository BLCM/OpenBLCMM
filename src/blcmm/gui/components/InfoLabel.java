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
 */
package blcmm.gui.components;

import blcmm.gui.FontInfo;
import blcmm.utilities.OSInfo;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

/**
 *
 * @author Lightchaosman
 */
@SuppressWarnings("serial")
public class InfoLabel extends JLabel {

    private final static String CTRL_KEY = (OSInfo.CURRENT_OS == OSInfo.OS.MAC ? "CMD" : "CTRL");
    public final static String BASIC_1 = "The following hotkeys work in the text area below:<ul>"
            + "<li>" + CTRL_KEY + "+Space -> Auto Complete</li>"
            + "<li>" + CTRL_KEY + "+F -> Search</li>"
            + "<li>" + CTRL_KEY + "+H -> Replace</li>"
            + "<li>" + CTRL_KEY + "+Z -> Undo</li>"
            + "<li>" + CTRL_KEY + "+Y -> Redo</li>";
    public final static String BASIC_2_EDIT_ONLY = "<li>" + CTRL_KEY + "+Enter -> Save and Close Dialog</li>";
    public final static String BASIC_3 = "</ul>Autocomplete is limited to the installed data packages.";

    public final static String OE_SPECIFIC = "Double click on an underlined object to dump it.<br/>"
            + "Use the middle mouse button or ctrl+click to dump in a new tab.<br/><br/>"
            + "When searching, add a dash (-) in front of a word to only display results NOT containing that word.<br/>"
            + "Prepend a word by 'inclass:' to only return objects in that class.<br/>"
            + "Searching supports regular expressions.<br/>"
            + "To perform a getall, search for `getall yourClass`.<br/>"
            + "To perform a getall with a specific field, search for `getall yourClass yourField`.<br/>"
            + "You can hide the class and package explorer by double clicking on the divider<br/>"
            + "If you want to search for an object and not dump it, hold CTRL while you press enter.<br/>"
            + "Need to dump an object often? Bookmark it using the star next to the search bar!<br/>"
            + "To manage your bookmarks, double click the star.";

    private final FontInfo fontInfo;

    public InfoLabel(String tooltip, FontInfo fontInfo, Component parentComponent) {
        this(tooltip, fontInfo, parentComponent, true);
    }

    public InfoLabel(String tooltip, FontInfo fontInfo, Component parentComponent, boolean clickable) {
        this.fontInfo = fontInfo;
        final String tooltip2 = tooltip.toLowerCase().startsWith("<html>") ? tooltip : "<HTML>" + tooltip + "</HTML>";
        super.setToolTipText(tooltip2);
        super.setIcon(new ImageIcon(getClass().getClassLoader().getResource("resources/Qmark.png")));
        if (clickable) {
            super.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    AdHocDialog.run(parentComponent,
                            fontInfo,
                            AdHocDialog.IconType.INFORMATION,
                            "Information",
                            tooltip2,
                            AdHocDialog.ButtonSet.OK);
                }
            });
        }
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        tip.setFont(this.fontInfo.getFont());
        return tip;
    }

}
