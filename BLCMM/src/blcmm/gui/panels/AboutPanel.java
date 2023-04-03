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
import blcmm.utilities.IconManager;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author LightChaosman
 */
public final class AboutPanel extends JPanel {

    public AboutPanel(boolean showDonate) {
        this.setLayout(new GridBagLayout());

        // Intentionally used Meta.NAME as part of the third-party-lib stanza
        // but hardcoded "OpenBLCMM" earlier, since the earlier statement should
        // remain historically accurate even if someone else forks this in the
        // future.
        JLabel mainLabel = new JLabel("<html>The Borderlands Community Mod Manager was developed by LightChaosman, with<br/>"
                + "assistance from apocalyptech, Bugworm, c0dycode, and FromDarkHell.<br/>"
                + "<br/>"
                + "OpenBLCMM is a fully-opensourced version of BLCMM maintained by the BLCMods community (with<br/>"
                + "apocalyptech as the current lead).  Thanks to LightChaosman for opensourcing the BLCMM core!<br/>"
                + "<br/>"
                + "Special thanks to everyone who's contributed to the development process, and thanks to the entire<br/>"
                + "Borderlands modding community for feedback.<br/>"
                + "<br/>"
                + Meta.NAME + " makes use of the following third-party libraries/resources:<br/>"
                + "  - StidOfficial's 'SteamVDF' library for some Steam data parsing, available under the GPLv3.<br/>"
                + "  - Apache Commons Text and Apache Commons Lang, available under the Apache License v2.0<br/>"
                + "  - Xerial's sqlite-jdbc, available under the Apache License v2.0<br/>"
                + "  - Some icons from Dave Gandy's Font Awesome set, available under CC BY 3.0<br/>"
                + "  - An icon from Fathema Khanom's User Interface set, available under Flaticon's Free License<br/>"
                + "  - An icon from Smashicons' Essential Collection set, available under Flaticon's Free License<br/>"
        );
        int wLog = 64, wPay = 100;
        mainLabel.setIcon(new ImageIcon(IconManager.getBLCMMIcon(wLog)));
        mainLabel.setIconTextGap((wPay - wLog) / 2 + 10);
        JLabel donateButton = new JLabel(new ImageIcon(new ImageIcon(AboutPanel.class.getClassLoader().getResource("resources/donate.png")).getImage().getScaledInstance(wPay, wPay / 4, Image.SCALE_SMOOTH)));
        donateButton.setToolTipText("Donate with PayPal");
        donateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    URL faq = new URL("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YVAJKH5S7WSG4&lc=US");
                    Desktop.getDesktop().browse(faq.toURI());
                } catch (URISyntaxException | IOException ex) {
                }
            }
        });
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> SwingUtilities.getWindowAncestor(AboutPanel.this).dispose());
        JLabel donateMeme = new JLabel("Donations will go to LightChaosman, the original author of BLCMM.");

        add(mainLabel, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(20, 20 + (wPay - wLog) / 2, 5, 20), 0, 0));
        if (showDonate) {
            add(donateButton, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 20, 20, 5), 0, 0));
            add(donateMeme, new GridBagConstraints(1, 1, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 5, 20, 5), 0, 0));
        }
        add(okButton, new GridBagConstraints(2, 1, 1, 1, 500, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 5, 20, 20), 0, 0));
    }

}
