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
import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.components.FontInfoJButton;
import blcmm.gui.components.FontInfoJLabel;
import blcmm.gui.components.ScrollablePanel;
import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.IconManager;
import blcmm.utilities.Utilities;
import java.awt.Desktop;
import java.awt.Font;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.apache.commons.text.WordUtils;

/**
 * An "About" panel for OpenBLCMM.
 *
 * Note that we're passing in a Font to use as our base font -- I was unable
 * to find a reliable way of propagating a default font after the user has
 * changed the font size in the app, and eventually decided to just use a
 * sledgehammer instead.
 *
 * @author LightChaosman, apocalyptech
 */
public final class AboutPanel extends JPanel {

    private final FontInfo fontInfo;
    private final Font currentFont;

    public AboutPanel(FontInfo fontInfo) {
        this.setLayout(new GridBagLayout());
        this.fontInfo = fontInfo;
        this.currentFont = fontInfo.getFont();

        int cur_y = 0;

        // Main header
        JLabel headerLabel = new JLabel("<html><b><nobr>" + Meta.NAME + " v" + Meta.VERSION + "</nobr></b></html>",
                new ImageIcon(IconManager.getBLCMMIcon(64)),
                SwingConstants.CENTER
        );
        // I have *no* idea why the hell I need to increase the font size so much here?
        // I was previously just doing <font size="+2"> in the label HTML, and it looked
        // great, but I seemingly need +20 when doing it this way?  Weird.
        headerLabel.setFont(currentFont.deriveFont(currentFont.getSize2D()+20f));
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
        JLabel keyLabel;
        JLabel valueLabel;
        int sysinfo_cur_y = 0;
        for (Entry entry : sysInfo.entrySet()) {
            keyLabel = new JLabel("<html><b>" + entry.getKey() + ":</b></html>");
            keyLabel.setFont(currentFont);
            sysInfoPanel.add(keyLabel, new GridBagConstraints(
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
            valueLabel = new JLabel("<html>" + WordUtils.wrap((String)entry.getValue(), 65, "<br>", true) + "</html>");
            valueLabel.setFont(currentFont);
            sysInfoPanel.add(valueLabel, new GridBagConstraints(
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
        JButton clipButton = new FontInfoJButton(fontInfo);
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

        JButton logButton = new FontInfoJButton(fontInfo);
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
        tabs.setFont(currentFont);
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
        ScrollablePanel aboutTabPanel = new ScrollablePanel();
        aboutTabPanel.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
        aboutTabPanel.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
        aboutTabPanel.setLayout(new GridBagLayout());
        aboutTabPanel.setBorder(BorderFactory.createEtchedBorder());
        // Intentionally *not* using Meta.NAME here since this is more a
        // historical record than a current-state.
        JLabel aboutTabLabel = new JLabel(
                "<html>The Borderlands Community Mod Manager was developed by LightChaosman, with"
                + " assistance from apocalyptech, Bugworm, c0dycode, and FromDarkHell.<br/>"
                + "<br/>"
                + "OpenBLCMM is a fully-opensourced version of BLCMM maintained by the BLCMods community (with"
                + " apocalyptech as the current lead).  Thanks to LightChaosman for opensourcing the BLCMM core!"
                + " Other OpenBLCMM contributions have come from apple1417 and ZetaDæmon.<br/>"
                + "<br/>"
                + "Thanks, too, to the countless members of the community who have contributed by testing,"
                + " providing support, and spreading the word.  Apologies to anyone we've missed!<br/>"
        );
        aboutTabLabel.setFont(currentFont);
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
        JScrollPane aboutScroller = new JScrollPane();
        aboutScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        aboutScroller.setViewportView(aboutTabPanel);
        aboutScroller.getVerticalScrollBar().setUnitIncrement(16);
        tabs.add("About/Credits", aboutScroller);

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
                + "<li>Rob Camick's ScrollablePanel.java, available without restriction</li>"
                + "</ul>"
                + "<b>Resources</b><br/>"
                + "<ul>"
                + "<li>Custom Assault on Dragon Keep icon from Julia @ steamgriddb.com, used with permission</li>"
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
        thirdTabLabel.setFont(currentFont);
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
        thirdScroller.getVerticalScrollBar().setUnitIncrement(16);
        tabs.add("Third-Party Resources", thirdScroller);

        // Donate Tab
        JPanel donateTabPanel = new JPanel();
        donateTabPanel.setLayout(new GridBagLayout());
        donateTabPanel.setBorder(BorderFactory.createEtchedBorder());
        JLabel donateLabel = new JLabel("Donations will go to LightChaosman, the original author of BLCMM.");
        donateLabel.setFont(currentFont);
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
        JLabel donateButton = new FontInfoJLabel(new ImageIcon(AboutPanel.class.getClassLoader().getResource("resources/donate.png")), fontInfo);
        donateButton.setToolTipText("Donate with PayPal");
        AboutPanel buttonRef = this;
        donateButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Utilities.launchBrowser("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=YVAJKH5S7WSG4&lc=US", buttonRef, fontInfo);
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
        JScrollPane donateScroller = new JScrollPane();
        donateScroller.setViewportView(donateTabPanel);
        donateScroller.getVerticalScrollBar().setUnitIncrement(16);
        tabs.add("Donate", donateScroller);

        // "OK" Button
        JButton okButton = new JButton("OK");
        okButton.setFont(currentFont);
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
        JButton button = new FontInfoJButton(this.fontInfo);
        //Color linkColor = ThemeManager.getColor(ThemeManager.ColorType.UITextLink);
        //button.setText("<html><font color=\"" + Integer.toHexString(linkColor.getRGB()).substring(2) + "\"><u>" + linkText + "</u></font></html>");
        button.setText("<html><b><nobr>" + linkText + "</nobr></b></html>");
        button.setOpaque(true);
        button.setToolTipText(remoteURL);
        AboutPanel buttonRef = this;
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Utilities.launchBrowser(remoteURL, buttonRef, fontInfo);
            }
        });
        return button;
    }

}
