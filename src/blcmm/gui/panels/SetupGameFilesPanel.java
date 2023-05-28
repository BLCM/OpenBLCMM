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
package blcmm.gui.panels;

import blcmm.Meta;
import blcmm.gui.theme.ThemeManager;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class SetupGameFilesPanel extends JPanel {

    public SetupGameFilesPanel() {
        this.setLayout(new GridBagLayout());

        JLabel topLabel = new JLabel(
                "<html>"
                + Meta.NAME + " is no longer needed to do hex-edits for Borderlands 2 or The Pre-Sequel.  <b>Installing PythonSDK "
                + "will enable console + modding, and provide other benefits as well!</b>  PythonSDK can be installed on both "
                + "Steam and EGS versions."
        );

        JButton sdkButton = SetupGameFilesPanel.getBLModdingDotComRedirectButton();

        JLabel bottomLabel = new JLabel(
                "<html>"
                + "<p>"
                + "Even though PythonSDK modding is a completely separate style of modding from BLCMM text-based mods, "
                + "you can use the two together with no problems at all.  So, install PythonSDK to get your Borderlands "
                + "install ready for modding, and continue using " + Meta.NAME + " for managing the content of your text-based mods. "
                + "</p>"
                + "<br/>"
                + "<p>"
                + "An additional benefit of using PythonSDK to enable mods is that you can also benefit from an ever-growing "
                + "collection of SDK-based mods, which allow modders to do things impossible in the kinds of mods that " + Meta.NAME + " "
                + "manages.<br/>"
                + "</p>"
                + "<br/>"
                + "<p>"
                + "The INI-file tweaks previously available on this screen have been moved to their own \"INI Tweaks\" dialog, "
                + "and if you <b>do</b> need to use one of the legacy hex edits, those have been moved to their own \"Legacy "
                + "Hex Edits\" dialog, too."
                + "</p>"
        );

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> SwingUtilities.getWindowAncestor(SetupGameFilesPanel.this).dispose());

        add(topLabel, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(20, 20 / 2, 5, 20), 0, 0));
        add(sdkButton, new GridBagConstraints(0, 1, 3, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        add(bottomLabel, new GridBagConstraints(0, 2, 3, 1, 1, 500, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(5, 20 / 2, 5, 20), 0, 0));
        add(okButton, new GridBagConstraints(0, 3, 1, 1, 500, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 5, 20, 20), 0, 0));
    }

    public static JButton getBLModdingDotComRedirectButton() {
        String sdkURL = "https://borderlandsmodding.com/running-mods/";
        JButton sdkButton = new JButton();
        Color linkColor = ThemeManager.getColor(ThemeManager.ColorType.UITextLink);
        sdkButton.setText("<html><font color=\"" + Integer.toHexString(linkColor.getRGB()).substring(2) + "\"><u><nobr>Click here for PythonSDK Install Instructions</nobr></u></font></html>");
        sdkButton.setOpaque(true);
        sdkButton.setToolTipText(sdkURL);
        sdkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    URL sdkToBrowse = new URI(sdkURL).toURL();
                    Desktop.getDesktop().browse(sdkToBrowse.toURI());
                } catch (URISyntaxException | IOException ex) {
                }
            }
        });
        return sdkButton;
    }

}
