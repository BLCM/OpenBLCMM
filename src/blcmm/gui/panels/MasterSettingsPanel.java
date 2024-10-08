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
 */
package blcmm.gui.panels;

import blcmm.Meta;
import blcmm.gui.FontInfo;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Utilities;
import blcmm.utilities.options.Option;
import javax.swing.BoxLayout;

/**
 * Master Panel for OpenBLCMM Settings.
 *
 * Note that we're passing in a Font to use as our base font -- I was unable
 * to find a reliable way of propagating a default font after the user has
 * changed the font size in the app, and eventually decided to just use a
 * sledgehammer instead.
 *
 * Note that this panel does *not*, itself, update its font size as the user
 * changes the font in the main settings tab, even though the rest of the
 * app will do a "live" resize.  The GUI elements just get too weirdly
 * scrunched up unless the window is also resized, and I basically just don't
 * *want* to resize the window -- I think that'd end up feeling weird to the
 * user, and has too much possibility of behaving unpredictably.
 *
 * @author LightChaosman, apocalyptech
 */
public class MasterSettingsPanel extends javax.swing.JPanel {

    private final ToolSettingsPanel toolSettingsPanel;
    private final ToolSettingsPanel confirmationSettingsPanel;
    private final ToolSettingsPanel inputSettingsPanel;
    private final ToolSettingsPanel oeSettingsPanel;
    private final ToolSettingsPanel dangerousSettingsPanel;

    private final FontInfo fontInfo;

    /**
     * Creates new form MasterSettingsPanel
     *
     * @param fontInfo Info about the font that this panel will use.
     */
    public MasterSettingsPanel(FontInfo fontInfo) {
        GlobalLogger.log("Opened Master settings Panel");
        // We're making a *copy* of our fontInfo object because we intentionally
        // want to *not* resize fonts while changing the app font size in here,
        // and otherwise the reset-defaults button would use the new font size.
        this.fontInfo = fontInfo.copy();
        initComponents();

        // Reset fonts.  updateUI() is required because otherwise, the first
        // tab often inherits the previous font size, until it gets
        // mouseover'd.
        this.masterSettingsTabbedPane.setFont(this.fontInfo.getFont());
        this.masterSettingsTabbedPane.updateUI();

        // General Settings
        toolSettingsPanel = new ToolSettingsPanel(Option.Shown.SETTINGS, this.fontInfo, masterSettingsTabbedPane);
        toolSettingsPanel.setSize(generalSettingsGuiPanel.getSize());
        generalSettingsGuiPanel.setLayout(new BoxLayout(generalSettingsGuiPanel, BoxLayout.PAGE_AXIS));
        generalSettingsGuiPanel.add(toolSettingsPanel);

        // Confirmations Settings
        confirmationSettingsPanel = new ToolSettingsPanel(Option.Shown.CONFIRMATIONS, this.fontInfo, masterSettingsTabbedPane,
                "These settings control the behavior of various confirmations<br/>"
                + "which are shown when using " + Meta.NAME + ".  Click on any<br/>"
                + "option to disable the associated confirmation dialog."
        );
        confirmationSettingsPanel.setSize(confirmationSettingsGuiPanel.getSize());
        confirmationSettingsGuiPanel.setLayout(new BoxLayout(confirmationSettingsGuiPanel, BoxLayout.PAGE_AXIS));
        confirmationSettingsGuiPanel.add(confirmationSettingsPanel);

        // Input Settings
        inputSettingsPanel = new ToolSettingsPanel(Option.Shown.INPUT, this.fontInfo, masterSettingsTabbedPane,
                "Choose the behavior of mouse clicks while viewing/editing<br/>"
                + "code.  Current/New actions only apply to object links, and<br/>"
                + "Back/Forward actions only apply in Object Explorer.  Using<br/>"
                + "the same mouse button for more than one section can result<br/>"
                + "in inconsistent behavior."
        );
        inputSettingsPanel.setSize(inputSettingsGuiPanel.getSize());
        inputSettingsGuiPanel.setLayout(new BoxLayout(inputSettingsGuiPanel, BoxLayout.PAGE_AXIS));
        inputSettingsGuiPanel.add(inputSettingsPanel);

        // Object Explorer Data
        oeSettingsPanel = new ToolSettingsPanel(Option.Shown.OE, this.fontInfo, masterSettingsTabbedPane,
                "Choose which package categories will be included in the<br/>"
                + "fulltext and 'refs' searches.  More packages will make<br/>"
                + "the search take longer."
        );
        oeSettingsPanel.setSize(oeSettingsGuiPanel.getSize());
        oeSettingsGuiPanel.setLayout(new BoxLayout(oeSettingsGuiPanel, BoxLayout.PAGE_AXIS));
        oeSettingsGuiPanel.add(oeSettingsPanel);

        // Dangerous Settings
        dangerousSettingsPanel = new ToolSettingsPanel(Option.Shown.DANGEROUS, this.fontInfo, masterSettingsTabbedPane,
                "The settings on this screen should be left alone unless you know<br/>"
                + "exactly what they do, and have a strong need to do so."
        );
        dangerousSettingsPanel.setSize(dangerousSettingsGuiPanel.getSize());
        dangerousSettingsGuiPanel.setLayout(new BoxLayout(dangerousSettingsGuiPanel, BoxLayout.PAGE_AXIS));
        dangerousSettingsGuiPanel.add(dangerousSettingsPanel);

        generalSettingsGuiScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        confirmationSettingsGuiScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        inputSettingsGuiScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        autoupdateSettingsGuiScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dangerousSettingsGuiScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        Utilities.makeWindowOfComponentResizable(this);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        masterSettingsTabbedPane = new blcmm.gui.components.BoldJTabbedPane();
        generalSettingsGuiScrollPane = new javax.swing.JScrollPane();
        generalSettingsGuiPanel = new javax.swing.JPanel();
        confirmationSettingsGuiScrollPane = new javax.swing.JScrollPane();
        confirmationSettingsGuiPanel = new javax.swing.JPanel();
        inputSettingsGuiScrollPane = new javax.swing.JScrollPane();
        inputSettingsGuiPanel = new javax.swing.JPanel();
        autoupdateSettingsGuiScrollPane = new javax.swing.JScrollPane();
        oeSettingsGuiPanel = new javax.swing.JPanel();
        dangerousSettingsGuiScrollPane = new javax.swing.JScrollPane();
        dangerousSettingsGuiPanel = new javax.swing.JPanel();

        setPreferredSize(new java.awt.Dimension(460, 440));

        masterSettingsTabbedPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout generalSettingsGuiPanelLayout = new javax.swing.GroupLayout(generalSettingsGuiPanel);
        generalSettingsGuiPanel.setLayout(generalSettingsGuiPanelLayout);
        generalSettingsGuiPanelLayout.setHorizontalGroup(
            generalSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 523, Short.MAX_VALUE)
        );
        generalSettingsGuiPanelLayout.setVerticalGroup(
            generalSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 403, Short.MAX_VALUE)
        );

        generalSettingsGuiScrollPane.setViewportView(generalSettingsGuiPanel);

        masterSettingsTabbedPane.addTab("General", generalSettingsGuiScrollPane);

        javax.swing.GroupLayout confirmationSettingsGuiPanelLayout = new javax.swing.GroupLayout(confirmationSettingsGuiPanel);
        confirmationSettingsGuiPanel.setLayout(confirmationSettingsGuiPanelLayout);
        confirmationSettingsGuiPanelLayout.setHorizontalGroup(
            confirmationSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 454, Short.MAX_VALUE)
        );
        confirmationSettingsGuiPanelLayout.setVerticalGroup(
            confirmationSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 373, Short.MAX_VALUE)
        );

        confirmationSettingsGuiScrollPane.setViewportView(confirmationSettingsGuiPanel);

        masterSettingsTabbedPane.addTab("Confirmations", confirmationSettingsGuiScrollPane);

        javax.swing.GroupLayout inputSettingsGuiPanelLayout = new javax.swing.GroupLayout(inputSettingsGuiPanel);
        inputSettingsGuiPanel.setLayout(inputSettingsGuiPanelLayout);
        inputSettingsGuiPanelLayout.setHorizontalGroup(
            inputSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 523, Short.MAX_VALUE)
        );
        inputSettingsGuiPanelLayout.setVerticalGroup(
            inputSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 403, Short.MAX_VALUE)
        );

        inputSettingsGuiScrollPane.setViewportView(inputSettingsGuiPanel);

        masterSettingsTabbedPane.addTab("Input", inputSettingsGuiScrollPane);

        javax.swing.GroupLayout oeSettingsGuiPanelLayout = new javax.swing.GroupLayout(oeSettingsGuiPanel);
        oeSettingsGuiPanel.setLayout(oeSettingsGuiPanelLayout);
        oeSettingsGuiPanelLayout.setHorizontalGroup(
            oeSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 523, Short.MAX_VALUE)
        );
        oeSettingsGuiPanelLayout.setVerticalGroup(
            oeSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 403, Short.MAX_VALUE)
        );

        autoupdateSettingsGuiScrollPane.setViewportView(oeSettingsGuiPanel);

        masterSettingsTabbedPane.addTab("Object Explorer Data", autoupdateSettingsGuiScrollPane);

        javax.swing.GroupLayout dangerousSettingsGuiPanelLayout = new javax.swing.GroupLayout(dangerousSettingsGuiPanel);
        dangerousSettingsGuiPanel.setLayout(dangerousSettingsGuiPanelLayout);
        dangerousSettingsGuiPanelLayout.setHorizontalGroup(
            dangerousSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 523, Short.MAX_VALUE)
        );
        dangerousSettingsGuiPanelLayout.setVerticalGroup(
            dangerousSettingsGuiPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 403, Short.MAX_VALUE)
        );

        dangerousSettingsGuiScrollPane.setViewportView(dangerousSettingsGuiPanel);

        masterSettingsTabbedPane.addTab("Dangerous Settings", dangerousSettingsGuiScrollPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(masterSettingsTabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(masterSettingsTabbedPane)
        );

        masterSettingsTabbedPane.getAccessibleContext().setAccessibleName("Settings");
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane autoupdateSettingsGuiScrollPane;
    private javax.swing.JPanel confirmationSettingsGuiPanel;
    private javax.swing.JScrollPane confirmationSettingsGuiScrollPane;
    private javax.swing.JPanel dangerousSettingsGuiPanel;
    private javax.swing.JScrollPane dangerousSettingsGuiScrollPane;
    private javax.swing.JPanel generalSettingsGuiPanel;
    private javax.swing.JScrollPane generalSettingsGuiScrollPane;
    private javax.swing.JPanel inputSettingsGuiPanel;
    private javax.swing.JScrollPane inputSettingsGuiScrollPane;
    private javax.swing.JTabbedPane masterSettingsTabbedPane;
    private javax.swing.JPanel oeSettingsGuiPanel;
    // End of variables declaration//GEN-END:variables

    public boolean needsToolReset() {
        return toolSettingsPanel.needsToolReset()
                || inputSettingsPanel.needsToolReset()
                || oeSettingsPanel.needsToolReset()
                || dangerousSettingsPanel.needsToolReset();
    }

    public boolean needsTreeResize() {
        return toolSettingsPanel.needsTreeResize()
                || inputSettingsPanel.needsTreeResize()
                || oeSettingsPanel.needsTreeResize()
                || dangerousSettingsPanel.needsTreeResize();
    }

    public void focusToUpdatePanel() {
        masterSettingsTabbedPane.setSelectedIndex(1);
    }
}
