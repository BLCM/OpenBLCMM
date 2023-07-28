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
import blcmm.gui.components.BoldJTabbedPane;
import blcmm.gui.components.InfoLabel;
import blcmm.model.PatchType;
import blcmm.utilities.GameDetection;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.OSInfo;
import blcmm.utilities.Utilities;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Dialog to provide INI tweaks.
 *
 * Note that we're passing in a Font to use as our base font -- I was unable
 * to find a reliable way of propagating a default font after the user has
 * changed the font size in the app, and eventually decided to just use a
 * sledgehammer instead.
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class IniTweaksPanel extends GameTweaksPanel {

    public static IniTweaksPanel INSTANCE;

    private JPanel BL2Panel;
    private JPanel TPSPanel;
    private JPanel AODKPanel;

    private boolean hasTPS;
    private boolean hasBL2;
    private boolean hasAODK;

    private FontInfo fontInfo;

    /**
     * Creates new form FirstTimeActions
     */
    public IniTweaksPanel(FontInfo fontInfo) {
        INSTANCE = this;
        this.fontInfo = fontInfo;
        GlobalLogger.log("Creating IniTweaksPanel");
        hasBL2 = GameDetection.iniFilePathExists(PatchType.BL2);
        hasTPS = GameDetection.iniFilePathExists(PatchType.TPS);
        hasAODK = GameDetection.iniFilePathExists(PatchType.AODK);
        initComponents();
        initPanels();

        // Fix title font
        titleLabel.setFont(fontInfo.getFont());

        if (hasBL2) {
            initUI(PatchType.BL2);
        } else {
            this.initININotFoundUI(BL2Panel, PatchType.BL2);
        }
        if (hasTPS) {
            initUI(PatchType.TPS);
        } else {
            this.initININotFoundUI(TPSPanel, PatchType.TPS);
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
        if (hasAODK) {
            initUI(PatchType.AODK);
        } else {
            this.initININotFoundUI(AODKPanel, PatchType.AODK);
        }
    }

    private void initPanels() {

        BoldJTabbedPane tabbed = new BoldJTabbedPane();
        tabbed.setFont(this.fontInfo.getFont());
        masterPanel.setLayout(new GridBagLayout());
        masterPanel.add(tabbed, new GridBagConstraints(
                // x, y
                0, 0,
                // width, height
                1, 1,
                // weights (x, y)
                1, 1,
                // anchor
                GridBagConstraints.NORTH,
                // fill
                GridBagConstraints.BOTH,
                // insets
                new Insets(10, 10, 10, 10),
                // pad (x, y)
                0, 0));

        BL2Panel = new JPanel();
        BL2Panel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.BL2.getGameName(), BL2Panel);

        TPSPanel = new JPanel();
        TPSPanel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.TPS.getGameName(), TPSPanel);

        AODKPanel = new JPanel();
        AODKPanel.setBorder(BorderFactory.createEtchedBorder());
        tabbed.add(PatchType.AODK.getGameName(), AODKPanel);
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
        titleLabel.setText("Select your desired INI file tweaks");

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

    private static File getConfigFile(String filename, PatchType type) {
        String toQuery;
        if (GameDetection.getVirtualOS(type) == OSInfo.OS.UNIX) {
            toQuery = filename.toLowerCase();
        } else {
            toQuery = filename;
        }
        return new File(GameDetection.getPathToINIFiles(type) + toQuery);
    }

    private static boolean fileIsNoneNullAndExists(File f) {
        return f != null && f.exists();
    }

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
        if (!fileIsNoneNullAndExists(getConfigFile("WillowInput.ini", type))) {
            initPanel(panel, null);
            String gameLabel = type.toString();
            JLabel label = new JLabel("<html><body style='width: 5in;'>"
                    + "<center><b>Note:<b><br/>" + Meta.NAME + " found an executable for " + gameLabel + ", but no configuration files.<br/>"
                    + "Please run " + gameLabel + " once, then restart " + Meta.NAME + ".");
            label.setFont(this.fontInfo.getFont());
            panel.add(label, new GridBagConstraints(
                    0, 1000,
                    3, 1,
                    1d, 50000d,
                    GridBagConstraints.NORTH,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0),
                    0, 0));
        } else {
            initPanel(panel, getActions(type));
        }
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
     * INI files for a game.
     *
     * @param panel The panel to put ourselves into.
     * @param patchType The game we're currently processing
     */
    private void initININotFoundUI(JPanel panel, PatchType patchType) {

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
        JLabel autodetectLabel = new JLabel("<html>"
                + "<b>Note:</b> " + Meta.NAME + " cannot autodetect " + patchType.getGameName() + " INI files"
                + " unless it has been run at least once.  If this is a fresh"
                + " desktop or account, make sure to run " + patchType.getGameName() + " at least once.");
        autodetectLabel.setFont(this.fontInfo.getFont());
        panel.add(autodetectLabel, cs);
        cs.gridy++;

        // Finally, a spacer so that the label stays up near the button
        cs.weightx = cs.weighty = 100;
        panel.add(new JPanel(), cs);

    }

    private List<ComponentProvider> getActions(PatchType type) {
        List<ComponentProvider> actions = new ArrayList<>();
        actions.addAll(getConfigActions(type));
        return actions;
    }

    private List<ComponentProvider> getConfigActions(PatchType type) {
        OSInfo.OS VIRTUAL_OS = GameDetection.getVirtualOS(type);
        List<ComponentProvider> actions = new ArrayList<>();

        //actions.add(new SeperatorComponentProvider("Performance boosting .ini tweaks"));
        File engineINIFile = getConfigFile("WillowEngine.ini", type);
        File gameINIFile = getConfigFile("WillowGame.ini", type);
        File inputINIFile = getConfigFile("WillowInput.ini", type);

        FileEditChoiceSetupAction consoleKey = new FileEditChoiceSetupAction(
                "Console key",
                this,
                this.fontInfo,
                inputINIFile,
                "ConsoleKey",
                "Engine.Console",
                "Undefine",
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "~", "None"},
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "Tilde", "Undefine"}
        );
        consoleKey.setDescription("<html>Select the key you wish to use to open the console<br/>"
                + "<em>(Unnecessary if PythonSDK's default of Tilde/~ is all right with you)</em>"
        );
        actions.add(consoleKey);

        // Our Renderer/PostProcessor option is a bit different because the var may live in two or more
        // locations.  Most files probably only have it in [Engine.Engine], but at least some files may
        // have it in [WillowGame.WillowGameEngine] as well (or instead?) and that location takes
        // precedence.  So check that first, and if we get a known value use that.  Otherwise, fall back
        // to using the more common [Engine.Engine] location regardles of its status.
        File rendererFile = engineINIFile;
        String rendererVarName = "DefaultPostProcessName";
        String rendererDefault = "WillowEngineMaterials.WillowScenePostProcess";
        String[] rendererChoices = {"Default"/*                                */, "No black lines"/*                         */, "No black lines, more colors"};
        String[] rendererValues = {"WillowEngineMaterials.WillowScenePostProcess", "WillowEngineMaterials.RyanScenePostProcess", "WillowEngineMaterials.CinematicScenePostProcess"};
        FileEditChoiceSetupAction rendererSetupAction = new FileEditChoiceSetupAction(
                "Renderer",
                this,
                this.fontInfo,
                rendererFile, rendererVarName, "WillowGame.WillowGameEngine", rendererDefault,
                rendererChoices, rendererValues
        );
        if (!EnumSet.of(SetupStatus.ACTIVE, SetupStatus.INACTIVE).contains(rendererSetupAction.getCurrentStatus())) {
            rendererSetupAction = new FileEditChoiceSetupAction(
                    "Renderer",
                    this,
                    this.fontInfo,
                    rendererFile, rendererVarName, "Engine.Engine", rendererDefault,
                    rendererChoices, rendererValues);
        }
        rendererSetupAction.setDescription("Options to remove black outlines from the game, giving a massive performance boost overall");
        actions.add(rendererSetupAction);

        INIFileEditSetupAction noDistortions = new INIFileEditSetupAction(
                "Disable distortion",
                this,
                this.fontInfo,
                engineINIFile,
                "Distortion",
                "SystemSettings",
                "True", "False"
        );
        noDistortions.setDescription("Disables the distortion effect around explosions in combat, decreasing visual polution whilst increasing performance");
        actions.add(noDistortions);

        INIFileEditSetupAction noGodRays = new INIFileEditSetupAction(
                "No godrays",
                this,
                this.fontInfo,
                engineINIFile,
                "bAllowLightShafts",
                "SystemSettings",
                "True", "False"
        );
        noGodRays.setDescription("No more god rays. Very noticable in Washburne refinery");
        actions.add(noGodRays);

        INIFileEditSetupAction simpleShadows = new INIFileEditSetupAction(
                "Simple shadows",
                this,
                this.fontInfo,
                engineINIFile,
                "DynamicShadows",
                "SystemSettings",
                "True", "False"
        );
        simpleShadows.setDescription("Circular shadows instead of full shadows");
        actions.add(simpleShadows);

        INIFileEditSetupAction ragdollSetup1 = new INIFileEditSetupAction(
                "",
                this,
                this.fontInfo,
                gameINIFile,
                "SecondsBeforeConsideringRagdollRemoval",
                "WillowGame.WillowPawn",
                "600", "30"
        );
        INIFileEditSetupAction ragdollSetup2 = new INIFileEditSetupAction(
                "",
                this,
                this.fontInfo,
                gameINIFile,
                "SecondsBeforeVisibleRagdollRemoval",
                "WillowGame.WillowPawn",
                "600", "30"
        );
        ragdollSetup1.setAlternateChecker(new INIFileEditSetupAction.AlternateValueChecker.NumberValueChecker(0, 30));
        ragdollSetup2.setAlternateChecker(new INIFileEditSetupAction.AlternateValueChecker.NumberValueChecker(0, 30));
        CompoundSetupAction fewerCorpses = new CompoundSetupAction(
                "Fewer corpses",
                this,
                this.fontInfo,
                ragdollSetup1,
                ragdollSetup2
        );
        fewerCorpses.setDescription("Reduces the time it takes for corpses to decay to 30 seconds");
        actions.add(fewerCorpses);

        List<String> movies1 = new ArrayList<>(Arrays.asList(new String[]{"2K_logo", "Gearbox_logo", "Loading"}));
        List<String> movies2 = new ArrayList<>(Arrays.asList(new String[]{";2K_logo", ";Gearbox_logo", ";Loading"}));
        if (VIRTUAL_OS != OSInfo.OS.WINDOWS) {
            movies1.add("Aspyr");
            movies2.add(";Aspyr");
            if (type == PatchType.TPS) {
                // TODO: I'm not sure if these still show up on Mac; they *will* be there on native Linux though.
                movies1.add("2K_Australia_Logo");
                movies2.add(";2K_Australia_Logo");
            }
        }
        INIFileEditSetupAction quickerStartup = new INIFileEditSetupAction(
                "Quicker startup",
                this,
                this.fontInfo,
                engineINIFile,
                "StartupMovies",
                "FullScreenMovie",
                movies1.toArray(new String[0]),
                movies2.toArray(new String[0])
        );
        quickerStartup.setDescription("Removes some of the startup screens, getting you to the main menu faster");
        quickerStartup.setAlternateChecker((String value) -> {
            if (value.isEmpty()) {
                return SetupStatus.ACTIVE;
            }
            boolean b1 = movies1.contains(value);
            boolean b2 = movies2.contains(value);
            if (b1 && b2) {
                return SetupStatus.IGNORE;
            } else if (b1) {
                return SetupStatus.INACTIVE;
            } else if (b2) {
                return SetupStatus.ACTIVE;
            }
            return SetupStatus.UNKNOWN;
        });
        actions.add(quickerStartup);

        INIFileEditSetupAction fewerCutscenes = new INIFileEditSetupAction(
                "Fewer cutscenes",
                this,
                this.fontInfo,
                engineINIFile,
                "bForceNoMovies",
                "FullScreenMovie",
                new String[]{"FALSE"},
                new String[]{"TRUE"}
        );
        StringBuilder cutsceneDesc = new StringBuilder();
        cutsceneDesc.append("<html>Removes some cutscenes from the game. As a side effect, your loading screens turn black.");
        if (type == PatchType.BL2 || type == PatchType.TPS) {
            // I'm actually not sure if FDH had a TPS patch for this or not, but don't quite care enough to check.
            // There's definitely not one for AODK, though.
            cutsceneDesc.append("<br/>There's also a mod that disables more cutscenes, without this side effect, by FromDarkHell");
        }
        fewerCutscenes.setDescription(cutsceneDesc.toString());
        actions.add(fewerCutscenes);
        if (type == PatchType.TPS) {
            // This does seem to work fine in AoDK, at least with the limited testing I'd done.
            fewerCutscenes.disable("This feature soft-locks TPS while enabled, so it's not available through " + Meta.NAME + ".", false);
        }
        return actions;
    }

    public static class INIFileEditSetupAction extends AbstractFileEditSetupAction {

        private final String[] originalValues;
        private final String[] ourValues;
        private AlternateValueChecker alternateChecker;

        public INIFileEditSetupAction(String name,
                IniTweaksPanel panel,
                FontInfo fontInfo,
                File file,
                String field,
                String preHeaderName,
                String originalValues,
                String ourValues) {
            this(name, panel, fontInfo, file, field, preHeaderName, new String[]{originalValues}, new String[]{ourValues});
        }

        public INIFileEditSetupAction(String name,
                IniTweaksPanel panel,
                FontInfo fontInfo,
                File file,
                String field,
                String preHeaderName,
                String[] originalValues,
                String[] ourValues) {
            super(name, panel, fontInfo, file, field, preHeaderName);
            this.originalValues = originalValues;
            this.ourValues = ourValues;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            if (!isAvailable()) {
                return SetupStatus.UNAVAILABLE;
            }
            try {
                int original = 0, changed = 0, same = 0;
                for (String line : getRelevantLines()) {
                    if (line.toLowerCase().startsWith(field.toLowerCase() + "=")) {
                        String value = line.substring(field.length() + 1);
                        if (alternateChecker == null) {
                            for (int i = 0; i < originalValues.length; i++) {
                                if (originalValues[i].equals(ourValues[i]) && value.equalsIgnoreCase(originalValues[i])) {
                                    same++;
                                    break;
                                }
                                if (value.equalsIgnoreCase(originalValues[i])) {
                                    original++;
                                    break;
                                } else if (value.equalsIgnoreCase(ourValues[i])) {
                                    changed++;
                                    break;
                                }
                            }
                        } else {
                            switch (alternateChecker.getStatusOfValue(value)) {
                                case ACTIVE:
                                    changed++;
                                    break;
                                case INACTIVE:
                                    original++;
                                    break;
                                case IGNORE:
                                    same++;
                                    break;
                                case UNKNOWN:
                                    return SetupStatus.UNKNOWN;
                            }
                        }
                    }
                }
                if (original + same == originalValues.length) {
                    return SetupStatus.INACTIVE;
                }
                if (changed + same == originalValues.length) {
                    return SetupStatus.ACTIVE;
                }
                if (changed + same + original == originalValues.length) {
                    return SetupStatus.PARTIAL;
                }
                return SetupStatus.UNKNOWN;
            } catch (Exception e) {
                GlobalLogger.log(e);
                return SetupStatus.ERROR;
            }
        }

        public void setAlternateChecker(AlternateValueChecker alternateChecker) {
            this.alternateChecker = alternateChecker;
        }

        @Override
        public void apply() {
            try {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String line : getRelevantLines()) {
                    if (line.startsWith(field + "=")) {
                        String value = line.substring(field.length() + 1);
                        if (alternateChecker == null) {
                            for (int i = 0; i < originalValues.length; i++) {
                                if (value.equalsIgnoreCase(originalValues[i])) {
                                    line = field + "=" + ourValues[i];
                                    break;
                                }
                            }
                        } else {
                            if (alternateChecker.getStatusOfValue(value) != SetupStatus.UNKNOWN) {
                                line = field + "=" + ourValues[(count++) % ourValues.length];
                            }
                        }
                    }
                    sb.append(line).append("\n");
                }
                if (promptForReadOnlyFile()) {
                    return;
                }
                Utilities.writeStringToFile(fileContent.replace(relevantChunk, sb).replace("\n", "\r\n"), file);
                revertReadOnlyIfChosen();
                setActive();
            } catch (IOException ex) {
                setError(ex);
                GlobalLogger.log(ex);
            }
        }

        @Override
        public boolean revert() {
            try {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String line : getRelevantLines()) {
                    if (line.startsWith(field + "=")) {
                        if (alternateChecker == null) {
                            for (int i = 0; i < originalValues.length; i++) {
                                if (line.substring(field.length() + 1).equalsIgnoreCase(ourValues[i])) {
                                    line = field + "=" + originalValues[i];
                                    break;
                                }
                            }
                        } else {
                            line = field + "=" + originalValues[(count++) % originalValues.length];
                        }
                    }
                    sb.append(line).append("\n");
                }
                if (promptForReadOnlyFile()) {
                    return false;
                }
                Utilities.writeStringToFile(fileContent.replace(relevantChunk, sb.toString()).replace("\n", "\r\n"), file);
                revertReadOnlyIfChosen();
                setInactive();
                return true;
            } catch (IOException ex) {
                setError(ex);
                GlobalLogger.log(ex);
                return false;
            }
        }

        @Override
        public void fix() {
            try {
                StringBuilder sb = new StringBuilder();
                int c = 0;
                for (String line : getRelevantLines()) {
                    if (line.startsWith(field + "=")) {
                        line = field + "=" + originalValues[c++ % originalValues.length];//Just in case someone duplicated lines in there
                    }
                    sb.append(line).append("\n");
                }
                for (; c < originalValues.length; c++) {//If lines are missing
                    sb.append(field).append("=").append(originalValues[c]).append("\n");
                }
                if (promptForReadOnlyFile()) {
                    return;
                }
                Utilities.writeStringToFile(fileContent.replace(relevantChunk, sb.toString()).replace("\n", "\r\n"), file);
                revertReadOnlyIfChosen();
                setInactive();
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
            }
        }

        public static interface AlternateValueChecker {

            public static class NumberValueChecker implements AlternateValueChecker {

                private final double min, max;

                public NumberValueChecker(double min, double max) {
                    this.min = min;
                    this.max = max;
                }

                @Override
                public SetupStatus getStatusOfValue(String value) {
                    try {
                        double d = Double.parseDouble(value);
                        return min <= d && d <= max ? SetupStatus.ACTIVE : SetupStatus.INACTIVE;
                    } catch (NumberFormatException e) {
                        return SetupStatus.UNKNOWN;
                    }
                }
            };

            public SetupStatus getStatusOfValue(String value);
        }

    }

}
