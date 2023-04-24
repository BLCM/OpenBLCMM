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
import blcmm.gui.MainGUI;
import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.IconManager;
import blcmm.utilities.Utilities;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.commons.text.WordUtils;

/**
 *
 * @author LightChaosman
 */
public final class AboutPanel extends JPanel {

    public AboutPanel(boolean showDonate) {
        this.setLayout(new GridBagLayout());

        int cur_y = 0;

        // Main header
        JLabel headerLabel = new JLabel("<html><b><nobr><font size=\"+2\">" + Meta.NAME + " v" + Meta.VERSION + "</font></nobr></b></html>",
                new ImageIcon(IconManager.getBLCMMIcon(64)),
                SwingConstants.CENTER
        );
        headerLabel.setIconTextGap(20);
        this.add(headerLabel, new GridBagConstraints(
                // x, y
                0, cur_y++,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(10, 20, 5, 20),
                // pad (x, y)
                0, 0));

        // Gather System Info
        //GlobalLogger.log(System.getProperties().toString());
        Runtime curRuntime = Runtime.getRuntime();
        LinkedHashMap<String, String> sysInfo = new LinkedHashMap<> ();
        sysInfo.put(Meta.NAME + " Version", Meta.VERSION);
        if (Utilities.isCreatorMode()) {
            sysInfo.put("Creator Mode", "Yes");
        }
        sysInfo.put("Java Version", System.getProperty("java.version"));
        sysInfo.put("Java VM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
        sysInfo.put("Memory Used", Utilities.humanReadableByteCount(curRuntime.totalMemory() - curRuntime.freeMemory()));
        if (curRuntime.maxMemory() == Long.MAX_VALUE) {
            sysInfo.put("Memory Max", "Unlimited");
        } else {
            sysInfo.put("Memory Max", Utilities.humanReadableByteCount(curRuntime.maxMemory()));
        }
        for (PatchType type : PatchType.values()) {
            sysInfo.put(type.name() + " Data", MainGUI.INSTANCE.getDMM().getStatus(type));
        }

        // Create system info table
        JPanel sysInfoPanel = new JPanel();
        sysInfoPanel.setLayout(new GridBagLayout());
        //sysInfoPanel.setBorder(BorderFactory.createEtchedBorder());
        int sysinfo_cur_y = 0;
        for (Entry entry : sysInfo.entrySet()) {
            sysInfoPanel.add(new JLabel("<html><b>" + entry.getKey() + ":</b>"), new GridBagConstraints(
                    // x, y
                    0, sysinfo_cur_y,
                    // width, height
                    1, 1,
                    // weights (x, y)
                    1, 1,
                    // anchor
                    GridBagConstraints.NORTHEAST,
                    // fill
                    GridBagConstraints.NONE,
                    // insets
                    new Insets(1, 3, 1, 3),
                    // pad (x, y)
                    0, 0));
            sysInfoPanel.add(new JLabel("<html>" + WordUtils.wrap((String)entry.getValue(), 65, "<br>", true)), new GridBagConstraints(
                    // x, y
                    1, sysinfo_cur_y,
                    // width, height
                    1, 1,
                    // weights (x, y)
                    1, 1,
                    // anchor
                    GridBagConstraints.WEST,
                    // fill
                    GridBagConstraints.NONE,
                    // insets
                    new Insets(1, 3, 1, 3),
                    // pad (x, y)
                    0, 0));
            sysinfo_cur_y++;
        }

        // And add the panel in
        this.add(sysInfoPanel, new GridBagConstraints(
                // x, y
                0, cur_y++,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.HORIZONTAL,
                // insets
                new Insets(5, 10, 5, 10),
                // pad (x, y)
                0, 0));

        // A list of buttons
        ArrayList<JButton> buttons = new ArrayList<> ();
        String newVersion = MainGUI.INSTANCE.getNewVersion();
        if (newVersion == null) {
            buttons.add(this.createURLButton(Meta.NAME + " Github", Meta.CODE_URL));
        } else {
            buttons.add(this.createURLButton("Download " + Meta.NAME + " v" + newVersion, Meta.RELEASES_URL));
        }
        JButton clipButton = new JButton();
        clipButton.setText("<html><b><nobr>Copy Info to Clipboard</nobr></b></html>");
        clipButton.setToolTipText("Copy Info to Clipboard");
        clipButton.setOpaque(true);
        clipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                StringBuilder sb = new StringBuilder();
                for (Entry entry : sysInfo.entrySet()) {
                    sb.append(entry.getKey());
                    sb.append(": ");
                    sb.append(entry.getValue());
                    sb.append("\n");
                }
                StringSelection stringSelection = new StringSelection(sb.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
        buttons.add(clipButton);

        JButton logButton = new JButton();
        logButton.setText("<html><b><nobr>Open Log Dir</nobr></b></html>");
        logButton.setToolTipText("Open Log Dir");
        logButton.setOpaque(true);
        logButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    Desktop.getDesktop().open(GlobalLogger.getLOG_FOLDER());
                } catch (IOException ex) {
                    GlobalLogger.log(ex);
                }
            }
        });
        buttons.add(logButton);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        int button_cur_x = 0;
        for (JButton button : buttons) {
            buttonPanel.add(button, new GridBagConstraints(
                    // x, y
                    button_cur_x, 0,
                    // width, height
                    1, 1,
                    // weights (x, y)
                    1, 1,
                    // anchor
                    GridBagConstraints.CENTER,
                    // fill
                    GridBagConstraints.NONE,
                    // insets
                    new Insets(5, 5, 5, 5),
                    // pad (x, y)
                    0, 0));
            button_cur_x++;
        }

        // And add the button panel in
        this.add(buttonPanel, new GridBagConstraints(
                // x, y
                0, cur_y++,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(5, 10, 5, 10),
                // pad (x, y)
                0, 0));

        // And now a JTabbedPane for other various info
        JTabbedPane tabs = new JTabbedPane();
        this.add(tabs, new GridBagConstraints(
                // x, y
                0, cur_y++,
                // width, height
                1, 1,
                // weights (x, y)
                1, 500,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.BOTH,
                // insets
                new Insets(5, 20, 5, 20),
                // pad (x, y)
                0, 0));

        // General About Tab
        JPanel aboutTabPanel = new JPanel();
        aboutTabPanel.setLayout(new GridBagLayout());
        aboutTabPanel.setBorder(BorderFactory.createEtchedBorder());
        // Intentionally *not* using Meta.NAME here since this is more a
        // historical record than a current-state.
        JLabel aboutTabLabel = new JLabel(
                "<html>The Borderlands Community Mod Manager was developed by LightChaosman, with<br/>"
                + "assistance from apocalyptech, Bugworm, c0dycode, and FromDarkHell.<br/>"
                + "<br/>"
                + "OpenBLCMM is a fully-opensourced version of BLCMM maintained by the BLCMods community (with<br/>"
                + "apocalyptech as the current lead).  Thanks to LightChaosman for opensourcing the BLCMM core!<br/>"
                + "Other OpenBLCMM contributions have come from apple1417 and ZetaDæmon.<br/>"
                + "<br/>"
                + "Thanks, too, to the countless members of the community who have contributed by testing,<br/>"
                + "providing support, and spreading the word.  Apologies to anyone we've missed!<br/>"
        );
        aboutTabPanel.add(aboutTabLabel, new GridBagConstraints(
                // x, y
                0, 0,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.NORTHWEST,
                // fill
                GridBagConstraints.HORIZONTAL,
                // insets
                new Insets(5, 5, 5, 5),
                // pad (x, y)
                0, 0));
        tabs.add("About/Credits", aboutTabPanel);

        // Third-party resources tab
        JPanel thirdTabPanel = new JPanel();
        thirdTabPanel.setLayout(new GridBagLayout());
        thirdTabPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel thirdTabLabel = new JLabel(
                "<html>" + Meta.NAME + " makes use of the following third-party libraries/resources:<br/>"
                + "<br/>"
                + "<b>Java Libraries</b><br/>"
                + "<ul>"
                + "<li>StidOfficial's 'SteamVDF' library for some Steam data parsing, available under the GPLv3</li>"
                + "<li>Apache Commons Text and Apache Commons Lang, available under the Apache License v2.0</li>"
                + "<li>Xerial's sqlite-jdbc, available under the Apache License v2.0</li>"
                + "<li>Vincent Durmont's semver4j, available under the MIT License</li>"
                + "<li>CommonMark's commonmark-java, available under the 2-clause BSD License</li>"
                + "</ul>"
                + "<b>Resources</b><br/>"
                + "<ul>"
                + "<li>Some icons from Dave Gandy's Font Awesome set, available under CC BY 3.0</li>"
                + "<li>An icon from Fathema Khanom's User Interface set, available under Flaticon's Free License</li>"
                + "<li>An icon from Smashicons' Essential Collection set, available under Flaticon's Free License</li>"
                + "</ul>"
                + "<b>Toolset</b><br/>"
                + "<ul>"
                + "<li>Apache Netbeans is the development environment</li>"
                + "<li>GraalVM Native Image / Liberica NIK provides Windows EXE compilation</li>"
                + "<li>Visual Studio provides the C++ compiler for GraalVM/Liberica</li>"
                + "<li>Winrun4j provides a utility to set icons on Windows EXEs</li>"
                + "<li>Inno Setup is used to create the Windows installer</li>"
                + "</ul>"
        );
        thirdTabPanel.add(thirdTabLabel, new GridBagConstraints(
                // x, y
                0, 0,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.NORTHWEST,
                // fill
                GridBagConstraints.HORIZONTAL,
                // insets
                new Insets(5, 5, 5, 5),
                // pad (x, y)
                0, 0));
        JScrollPane thirdScroller = new JScrollPane();
        thirdScroller.setViewportView(thirdTabPanel);
        tabs.add("Third-Party Resources", thirdScroller);

        // Donate Tab
        JPanel donateTabPanel = new JPanel();
        donateTabPanel.setLayout(new GridBagLayout());
        donateTabPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel donateLabel = new JLabel("Donations will go to LightChaosman, the original author of BLCMM.");
        donateTabPanel.add(donateLabel, new GridBagConstraints(
                // x, y
                0, 0,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(20, 5, 5, 5),
                // pad (x, y)
                0, 0));
        // Not using IconManager here since that only returns square images
        JLabel donateButton = new JLabel(new ImageIcon(AboutPanel.class.getClassLoader().getResource("resources/donate.png")));
        donateButton.setToolTipText("Donate with PayPal");
        donateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    URL faq = new URL("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YVAJKH5S7WSG4&lc=US");
                    Desktop.getDesktop().browse(faq.toURI());
                } catch (URISyntaxException | IOException ex) {
                    GlobalLogger.log(ex);
                }
            }
        });
        donateTabPanel.add(donateButton, new GridBagConstraints(
                // x, y
                0, 1,
                // width, height
                1, 1,
                // weights (x, y)
                1, 500,
                // anchor
                GridBagConstraints.NORTH,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(5, 5, 5, 5),
                // pad (x, y)
                0, 0));
        tabs.add("Donate", donateTabPanel);

        // "OK" Button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> SwingUtilities.getWindowAncestor(AboutPanel.this).dispose());
        this.add(okButton, new GridBagConstraints(
                // x, y
                0, cur_y++,
                // width, height
                1, 1,
                // weights (x, y)
                500, 1,
                // anchor
                GridBagConstraints.EAST,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(10, 10, 20, 20),
                // pad (x, y)
                0, 0));

    }

    /**
     * Creates a simple button which will link to the specified URL when clicked.
     *
     * @param linkText The text to show on the button
     * @param remoteURL The URL to send the user to
     * @return The new JButton
     */
    private JButton createURLButton(String linkText, String remoteURL) {
        JButton button = new JButton();
        //Color linkColor = ThemeManager.getColor(ThemeManager.ColorType.UITextLink);
        //button.setText("<html><font color=\"" + Integer.toHexString(linkColor.getRGB()).substring(2) + "\"><u>" + linkText + "</u></font></html>");
        button.setText("<html><b><nobr>" + linkText + "</nobr></b></html>");
        button.setOpaque(true);
        button.setToolTipText(remoteURL);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    URL sdkToBrowse = new URL(remoteURL);
                    Desktop.getDesktop().browse(sdkToBrowse.toURI());
                } catch (URISyntaxException | IOException ex) {
                    GlobalLogger.log(ex);
                }
            }
        });
        return button;
    }

}
