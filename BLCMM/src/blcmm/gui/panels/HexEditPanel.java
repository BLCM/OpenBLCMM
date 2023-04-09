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
import blcmm.gui.components.BoldJTabbedPane;
import blcmm.gui.components.InfoLabel;
import blcmm.model.PatchType;
import blcmm.utilities.GameDetection;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.OSInfo;
import blcmm.utilities.hex.HexDictionary;
import blcmm.utilities.hex.HexDictionary.HexQuery;
import blcmm.utilities.hex.HexEdit;
import blcmm.utilities.hex.HexEditor;
import blcmm.utilities.hex.HexInspectResult;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class HexEditPanel extends GameTweaksPanel {

    public static HexEditPanel INSTANCE;

    private JPanel BL2Panel;
    private JPanel TPSPanel;
    private JPanel AODKPanel;

    private final boolean hasTPS;
    private final boolean hasBL2;
    private final boolean hasAODK;

    /**
     * Creates new form FirstTimeActions
     */
    public HexEditPanel() {
        INSTANCE = this;
        GlobalLogger.log("Creating HexEditPanel");
        hasBL2 = GameDetection.getBL2Path() != null && GameDetection.getBL2Exe() != null;
        hasTPS = GameDetection.getTPSPath() != null && GameDetection.getTPSExe() != null;
        // We don't have any hexedits for AODK at the moment, so don't bother looking
        //hasAODK = GameDetection.getAODKPath() != null && GameDetection.getAODKExe() != null;
        hasAODK = false;
        initComponents();
        initPanels();

        if (hasBL2) {
            initUI(PatchType.BL2);
        } else {
            this.initEXENotFoundUI(BL2Panel, PatchType.BL2);
        }
        if (hasTPS) {
            initUI(PatchType.TPS);
        } else {
            this.initEXENotFoundUI(TPSPanel, PatchType.TPS);
            /** Example of our previous version here, for future reference in case I want to do something like this in the Settings menu
            this.initGameInstallNotFoundUI(TPSPanel, "TPS",
                    new ManualSelectionListener(GameDetection.getTPSExe(), "BorderlandsPreSequel", "Borderlands TPS", "BorderlandsPreSequel") {
                @Override
                public void callback(File f) {
                    GameDetection.setTPSPathManually(f.getAbsolutePath());
                    hasTPS = true;
                    initUI(PatchType.TPS);
                }
            });
            */
        }
        /*
        if (hasAODK) {
            initUI(PatchType.AODK);
        } else {
            this.initEXENotFoundUI(AODKPanel, PatchType.AODK);
        }
        */
    }

    private void initPanels() {

        String sdkURL = "https://borderlandsmodding.com/running-mods/";
        masterPanel.setLayout(new GridBagLayout());

        JLabel topLabel = new JLabel(
                "<html>"
                + "<b>Note:</b> The hex edits on this dialog are no longer needed to enable modding!  "
                + "<b>Installing PythonSDK will enable console + modding, and provide other benefits "
                + "as well!</b>  PythonSDK can be installed on both Steam and EGS versions."
        );
        masterPanel.add(topLabel, new GridBagConstraints(
                // x, y
                0, 0,
                // width, height
                1, 1,
                // weight (x, y)
                1, 1,
                // anchor
                GridBagConstraints.NORTH,
                // fill
                GridBagConstraints.HORIZONTAL,
                // insets
                new Insets(10, 20, 5, 20),
                // pad (x, y)
                0, 0));

        JButton sdkButton = SetupGameFilesPanel.getBLModdingDotComRedirectButton();
        masterPanel.add(sdkButton, new GridBagConstraints(
                // x, y
                0, 1,
                // width, height
                1, 1,
                // weight (x, y)
                1, 1,
                // anchor
                GridBagConstraints.CENTER,
                // fill
                GridBagConstraints.NONE,
                // insets
                new Insets(5, 5, 5, 5),
                // pad (x, y)
                0, 0));

        BoldJTabbedPane tabbed = new BoldJTabbedPane();
        masterPanel.add(tabbed, new GridBagConstraints(
                // x, y
                0, 2,
                // width, height
                1, 1,
                // weights (x, y)
                1, 500,
                // anchor
                GridBagConstraints.NORTH,
                // fill
                GridBagConstraints.BOTH,
                // insets
                new Insets(5, 10, 10, 10),
                // pad (x, y)
                0, 0));

        BL2Panel = new JPanel();
        BL2Panel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.BL2.getGameName(), BL2Panel);

        TPSPanel = new JPanel();
        TPSPanel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.TPS.getGameName(), TPSPanel);

        /*
        AODKPanel = new JPanel();
        AODKPanel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.AODK.getGameName(), AODKPanel);
        */
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        masterPanel = new javax.swing.JPanel();
        infoLabel = new InfoLabel("Mouse over the various options to get more details on what each option does.");

        setPreferredSize(new java.awt.Dimension(500, 350));

        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("Select your desired hex edits");

        javax.swing.GroupLayout masterPanelLayout = new javax.swing.GroupLayout(masterPanel);
        masterPanel.setLayout(masterPanelLayout);
        masterPanelLayout.setHorizontalGroup(
            masterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        masterPanelLayout.setVerticalGroup(
            masterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 204, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(masterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(infoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
                    .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(masterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel infoLabel;
    private javax.swing.JPanel masterPanel;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables

    private void initUI(PatchType type) {
        JPanel panel = null;
        switch (type) {
            case BL2:
                panel = BL2Panel;
                break;
            case TPS:
                panel = TPSPanel;
                break;
            case AODK:
                panel = AODKPanel;
                break;
        }
        initPanel(panel, getActions(type));
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.pack();
        }
    }

    private void initPanel(JPanel panel, Iterable<ComponentProvider> actions) {
        panel.removeAll();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.weightx = 1;
        cs.weighty = 1;
        cs.gridy = 0;
        cs.insets = new Insets(0, 10, 0, 10);
        if (actions != null) {
            for (ComponentProvider action : actions) {
                Component[] cps = action.updateComponents();
                cs.gridx = 0;
                cs.gridwidth = 3 / cps.length;
                for (Component comp : cps) {
                    cs.fill = cs.gridx == 2 || cs.gridwidth == 4 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
                    cs.weightx = cs.gridx == 2 ? 0.5 : 5;
                    panel.add(comp, cs);
                    cs.gridx++;
                }
                cs.gridy++;
            }
        }
        cs.gridx = 2;
        cs.weightx = cs.weighty = 100;
        panel.add(new JPanel(), cs);

    }

    /**
     * Initializes a game's UI panel in the event that we couldn't find the
     * EXE for a game.
     *
     * @param panel The panel to put ourselves into.
     * @param patchType The game we're currently processing
     */
    private void initEXENotFoundUI(JPanel panel, PatchType patchType) {

        // Set up the panel layout
        panel.setLayout(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.anchor = GridBagConstraints.NORTH;
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(10, 10, 10, 10);
        cs.weightx = 1;
        cs.weighty = 1;
        cs.gridx = 0;
        cs.gridy = 0;

        // Would like to use "em" for the width here, but Java doesn't seem
        // to support that.  "in" should be better than "px", at least.
        panel.add(new JLabel("<html>"
                + "<b>Note:</b> " + Meta.NAME + " could not autodetect " + patchType.getGameName() + ".  "
                + "Autodetection might not work until the game has been run at least once, and might not "
                + "work for installs from all game platforms.  Note that hex editing isn't actually "
                + "required to run mods -- the recommended method of enabling text-based mods is by "
                + "installing PythonSDK.  Click the button above for more information on installing that!"
                ),
                cs);
        cs.gridy++;

        // Finally, a spacer so that the label stays up near the button
        cs.weightx = cs.weighty = 100;
        panel.add(new JPanel(), cs);

    }

    private List<ComponentProvider> getActions(PatchType type) {
        List<ComponentProvider> actions = new ArrayList<>();
        actions.addAll(getBinariesActions(type));
        return actions;
    }


    private List<ComponentProvider> getBinariesActions(PatchType patchType) {
        OSInfo.OS VIRTUAL_OS = GameDetection.getVirtualOS(patchType);
        final String gameName = patchType.name();
        List<ComponentProvider> actions = new ArrayList<>();
        actions.add(new SeparatorComponentProvider("Mod support"));
        File executable = GameDetection.getExe(patchType);
        String win32 = executable.getParent() + "/";

        HexEditSetupAction hexEditSetup = new HexEditSetupAction("Unlock console (legacy)", executable, new HexQuery(VIRTUAL_OS, patchType, HexDictionary.HexType.ENABLE_SET_COMMANDS));
        hexEditSetup.setDescription("Enables the 'set' command in the game console.  Unnecessary if you use PythonSDK!");
        actions.add(hexEditSetup);

        HexEditSetupAction arraySetup = new HexEditSetupAction("Remove array limit", executable, new HexQuery(VIRTUAL_OS, patchType, HexDictionary.HexType.DISABLE_ARRAY_LIMIT));
        arraySetup.setDescription("Removes the array-size limit of 100 from object dumps - only useful for mod makers");
        actions.add(arraySetup);

        return actions;
    }

    public static class HexEditSetupAction extends SetupAction {

        private final File file;
        private final HexQuery query;
        private HexInspectResult[] inspectResults;
        private SetupStatus status = null;

        protected static final String[] convertToString(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString().replaceAll("0x", "").split("[\\s,]+");
        }

        public HexEditSetupAction(String name, File file, HexQuery query) {
            super(name);
            this.file = file;
            this.query = query;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            if (status == null) {
                if (isAvailable()) {
                    inspectResults = HexEditor.inspectFile(file, query);
                    int done = 0;
                    int original = 0;
                    int unknown = 0;
                    for (HexInspectResult res : inspectResults) {
                        if (res.matchesOriginal()) {
                            original++;
                        } else if (res.matchesEdited()) {
                            done++;
                        } else {
                            unknown++;
                        }
                    }
                    if (unknown > 0) {
                        status = SetupStatus.UNKNOWN;
                    } else if (done == inspectResults.length) {
                        status = SetupStatus.ACTIVE;
                    } else if (original == inspectResults.length) {
                        status = SetupStatus.INACTIVE;
                    } else {
                        status = SetupStatus.PARTIAL;
                    }
                } else {
                    status = SetupStatus.UNAVAILABLE;
                }
            }
            return status;
        }

        @Override
        public boolean isAvailable() {
            return HexDictionary.getHexEdits(query).length > 0;
        }

        @Override
        public void apply() {
            status = null;
            try {
                HexEditor.HexResult res = HexEditor.performHexEdits(file, query);
                switch (res.result) {
                    case HEXEDIT_ALREADY_DONE:
                    case HEXEDIT_SUCCESFUL:
                        setActive();
                        break;
                    default:
                        GlobalLogger.log(file + " " + res);
                        setError(res.toString());
                }
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
            }
        }

        @Override
        public boolean revert() {
            status = null;
            try {
                HexEdit[] origs = HexDictionary.getHexEdits(query);
                HexEdit[] inverses = new HexEdit[origs.length];
                for (int i = 0; i < origs.length; i++) {
                    inverses[i] = origs[i].getInvertedCopy();
                }
                HexEditor.HexResult res = HexEditor.performHexEdits(file, inverses);
                switch (res.result) {
                    case HEXEDIT_ALREADY_DONE:
                    case HEXEDIT_SUCCESFUL:
                        setInactive();
                        return true;
                    default:
                        setError(res.toString());
                        GlobalLogger.log(file + " " + res);
                        return false;
                }
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
            }
            return false;
        }

        @Override
        public void fix() {
            status = null;
            HexEditor.HexResult res;
            try {
                res = HexEditor.performHexEdits(file, true, query);
                switch (res.result) {
                    case HEXEDIT_ALREADY_DONE:
                    case HEXEDIT_SUCCESFUL:
                        setActive();
                        break;
                    default:
                        GlobalLogger.log(file + " " + res);
                        setError(res.toString());
                }
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
            }

        }
    }

}
