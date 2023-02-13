/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
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
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */
package blcmm.gui.panels;

import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.components.InfoLabel;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.PatchType;
import blcmm.utilities.GameDetection;
import blcmm.utilities.Utilities;
import general.utilities.GlobalLogger;
import general.utilities.OSInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class FirstTimeActionsPanel extends javax.swing.JPanel {

    public static FirstTimeActionsPanel INSTANCE;

    private JPanel BL2Panel, TPSPanel;

    private boolean hasTPS, hasBL2, advanced;

    private static final String SHA256_BL2_PATCHED = "2b781b3abfa3caf91adf7d30bac4b5de27c8323e84099d8dfd86f48a56a8f110";
    private static final String SHA256_TPS_PATCHED = "4153dd1f15ffc41ff1a1f86cbe5d21c491cc4a9738109acc3894753a5b54fafa";
    private static final String SHA256_BL2_STEAM_API_DLL_ORIGINAL = "b55054a9d9287c704b8e0ad3acfef1ea5c3fa6982b20b7e36466dc76a6ad8925";
    private static final String SHA256_TPS_STEAM_API_DLL_ORIGINAL = "b55054a9d9287c704b8e0ad3acfef1ea5c3fa6982b20b7e36466dc76a6ad8925";
    private static final String SHA256_WINDOWS_BL2_PHYSX_DLL_ORIGINAL = "b04c6adac65712eb9a7be470ccbfb5ed96e7f6a1369e5dfcbe79e9825e627ee5";
    private static final String SHA256_WINDOWS_TPS_PHYSX_DLL_ORIGINAL = "1a0340ce2fec52612107a2943525bfaf53efa19269492641484432b964a552ed";

    private final List<SetupAction> patchActions = new ArrayList<>(), consoleActions = new ArrayList<>();

    /**
     * Creates new form FirstTimeActions
     *
     * @param showAdvanced To show, or not to show more stuff to confuse people
     * with
     */
    public FirstTimeActionsPanel(boolean showAdvanced) {
        INSTANCE = this;
        GlobalLogger.log("Creating FirstTimeActionsPanel - advanced=" + showAdvanced);
        hasBL2 = GameDetection.getBL2Path() != null && GameDetection.getBL2Exe() != null;
        hasTPS = GameDetection.getTPSPath() != null && GameDetection.getTPSExe() != null;
        advanced = showAdvanced;
        initComponents();
        initPanels();

        if (hasBL2) {
            initUIBL2();
        } else {
            this.initNotFoundUI(BL2Panel, "BL2",
                    new ManualSelectionListener(GameDetection.getBL2Exe(), "Borderlands2", "Borderlands 2", "Borderlands 2") {
                @Override
                public void callback(File f) {
                    GameDetection.setBL2PathManually(f.getAbsolutePath());
                    hasBL2 = true;
                    initUIBL2();
                }
            });
        }
        if (hasTPS) {
            initUITPS();
        } else {
            this.initNotFoundUI(TPSPanel, "TPS",
                    new ManualSelectionListener(GameDetection.getTPSExe(), "BorderlandsPreSequel", "Borderlands TPS", "BorderlandsPreSequel") {
                @Override
                public void callback(File f) {
                    GameDetection.setTPSPathManually(f.getAbsolutePath());
                    hasTPS = true;
                    initUITPS();
                }
            });
        }
    }

    private void initPanels() {
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.LINE_AXIS));

        BL2Panel = new JPanel();
        TitledBorder bl2Border = new TitledBorder(new EtchedBorder(), PatchType.BL2.toString(), TitledBorder.LEFT, TitledBorder.CENTER);
        BL2Panel.setBorder(bl2Border);
        masterPanel.add(BL2Panel);

        TPSPanel = new JPanel();
        TitledBorder tpsBorder = new TitledBorder(new EtchedBorder(), PatchType.TPS.toString(), TitledBorder.LEFT, TitledBorder.CENTER);
        TPSPanel.setBorder(tpsBorder);
        masterPanel.add(TPSPanel);

        try {
            //This will not work in Java 10, appearantly, but since we're using 8, we good
            Field f = TitledBorder.class.getDeclaredField("label");
            f.setAccessible(true);
            ((JLabel) f.get(bl2Border)).setIcon(new ImageIcon(PatchType.BL2.getIcon()));
            ((JLabel) f.get(tpsBorder)).setIcon(new ImageIcon(PatchType.TPS.getIcon()));
            f.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("Select your desired game file tweaks");

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

    private static File getConfigFile(String filename, boolean BL2) {
        String toQuery;
        if (GameDetection.getVirtualOS(BL2) == OSInfo.OS.UNIX) {
            toQuery = filename.toLowerCase();
        } else {
            toQuery = filename;
        }
        return new File(GameDetection.getPathToINIFiles(BL2) + toQuery);
    }

    private static boolean fileIsNoneNullAndExists(File f) {
        return f != null && f.exists();
    }

    private void initUIBL2() {
        initUI(true);
    }

    private void initUITPS() {
        initUI(false);
    }

    private void initUI(boolean BL2) {
        JPanel panel = BL2 ? BL2Panel : TPSPanel;
        if (!fileIsNoneNullAndExists(getConfigFile("WillowInput.ini", BL2))) {
            initPanel(panel, null);
            String gameLabel = BL2 ? "BL2" : "TPS";
            JLabel label = new JLabel("<html><body style='width: 5in;'>"
                    + "<center><b>Note:<b><br/>BLCMM found an executable for " + gameLabel + ", but no configuration files.<br/>"
                    + "Please run " + gameLabel + " once, then restart BLCMM.");
            panel.add(label, new GridBagConstraints(0, 1000, 3, 1, 1d, 50000d, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        } else {
            initPanel(panel, getActions(BL2));
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
     * game.
     *
     * @param panel The panel to put ourselves into.
     * @param gameLabel The game text which will be put on the button.
     * @param buttonListener The listener which will be attached to the button
     */
    private void initNotFoundUI(JPanel panel, String gameLabel,
            ManualSelectionListener buttonListener) {

        // Set up the panel layout
        panel.setLayout(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.anchor = GridBagConstraints.NORTH;
        cs.insets = new Insets(5, 0, 5, 0);
        cs.weightx = 1;
        cs.weighty = 1;
        cs.gridx = 0;
        cs.gridy = 0;

        // First up: a button for the user to hit.
        JButton button = new JButton("No " + gameLabel + " installation detected. Click here to select manually");
        button.addActionListener(buttonListener);
        panel.add(button, cs);

        // Would like to use "em" for the width here, but Java doesn't seem
        // to support that.  "in" should be better than "px", at least.
        cs.gridy = 1;
        panel.add(new JLabel("<html><body style='width: 4.5in;'>"
                + "<b>Note:</b> BLCMM cannot autodetect " + gameLabel
                + " unless it has been run at least once.  If this is a fresh"
                + " desktop or account, make sure to run " + gameLabel + " at least once."),
                cs);

        // Finally, a spacer so that the label stays up near the button
        cs.gridx = 2;
        cs.weightx = cs.weighty = 100;
        panel.add(new JPanel(), cs);

    }

    private List<ComponentProvider> getActions(boolean BL2) {
        List<ComponentProvider> actions = new ArrayList<>();
        actions.addAll(getConfigActions(BL2));
        return actions;
    }

    private List<ComponentProvider> getConfigActions(boolean BL2) {
        OSInfo.OS VIRTUAL_OS = GameDetection.getVirtualOS(BL2);
        List<ComponentProvider> actions = new ArrayList<>();
        File inputINIFile = getConfigFile("WillowInput.ini", BL2);
        FileEditChoiceSetupAction consoleKey = new FileEditChoiceSetupAction("Console key", inputINIFile, "ConsoleKey", "Engine.Console", "Undefine",
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "~"/**/, "None"},
                new String[]{"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "Tilde", "Undefine"});
        consoleKey.setDescription("Select the key you wish to use to open the console");
        actions.add(consoleKey);
        consoleActions.add(consoleKey);

        if (advanced) {
            actions.add(new SeperatorComponentProvider("Performance boosting .ini tweaks"));
            File engineINIFile = getConfigFile("WillowEngine.ini", BL2);
            File gameINIFile = getConfigFile("WillowGame.ini", BL2);

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
            FileEditChoiceSetupAction rendererSetupAction = new FileEditChoiceSetupAction("Renderer",
                    rendererFile, rendererVarName, "WillowGame.WillowGameEngine", rendererDefault,
                    rendererChoices, rendererValues);
            if (!EnumSet.of(SetupStatus.ACTIVE, SetupStatus.INACTIVE).contains(rendererSetupAction.getCurrentStatus())) {
                rendererSetupAction = new FileEditChoiceSetupAction("Renderer",
                        rendererFile, rendererVarName, "Engine.Engine", rendererDefault,
                        rendererChoices, rendererValues);
            }
            rendererSetupAction.setDescription("Options to remove black outlines from the game, giving a massive performance boost overall");
            actions.add(rendererSetupAction);

            INIFileEditSetupAction noDistortions = new INIFileEditSetupAction("Disable distortion", engineINIFile, "Distortion", "SystemSettings", "True", "False");
            noDistortions.setDescription("Disables the distortion effect around explosions in combat, decreasing visual polution whilst increasing performance");
            actions.add(noDistortions);

            INIFileEditSetupAction noGodRays = new INIFileEditSetupAction("No godrays", engineINIFile, "bAllowLightShafts", "SystemSettings", "True", "False");
            noGodRays.setDescription("No more god rays. Very noticable in Washburne refinery");
            actions.add(noGodRays);

            INIFileEditSetupAction simpleShadows = new INIFileEditSetupAction("Simple shadows", engineINIFile, "DynamicShadows", "SystemSettings", "True", "False");
            simpleShadows.setDescription("Circular shadows instead of full shadows");
            actions.add(simpleShadows);

            INIFileEditSetupAction ragdollSetup1 = new INIFileEditSetupAction("", gameINIFile, "SecondsBeforeConsideringRagdollRemoval", "WillowGame.WillowPawn", "600", "30");
            INIFileEditSetupAction ragdollSetup2 = new INIFileEditSetupAction("", gameINIFile, "SecondsBeforeVisibleRagdollRemoval", "WillowGame.WillowPawn", "600", "30");
            ragdollSetup1.setAlternateChecker(new INIFileEditSetupAction.AlternateValueChecker.NumberValueChecker(0, 30));
            ragdollSetup2.setAlternateChecker(new INIFileEditSetupAction.AlternateValueChecker.NumberValueChecker(0, 30));
            CompoundSetupAction fewerCorpses = new CompoundSetupAction("Fewer corpses", ragdollSetup1, ragdollSetup2);
            fewerCorpses.setDescription("Reduces the time it takes for corpses to decay to 30 seconds");
            actions.add(fewerCorpses);

            List<String> movies1 = new ArrayList<>(Arrays.asList(new String[]{"2K_logo", "Gearbox_logo", "Loading"}));
            List<String> movies2 = new ArrayList<>(Arrays.asList(new String[]{";2K_logo", ";Gearbox_logo", ";Loading"}));
            if (VIRTUAL_OS != OSInfo.OS.WINDOWS) {
                movies1.add("Aspyr");
                movies2.add(";Aspyr");
                if (!BL2) {
                    // TODO: I'm not sure if these still show up on Mac; they *will* be there on native Linux though.
                    movies1.add("2K_Australia_Logo");
                    movies2.add(";2K_Australia_Logo");
                }
            }
            INIFileEditSetupAction quickerStartup = new INIFileEditSetupAction("Quicker startup", engineINIFile, "StartupMovies", "FullScreenMovie", movies1.toArray(new String[0]), movies2.toArray(new String[0]));
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

            INIFileEditSetupAction fewerCutscenes = new INIFileEditSetupAction("Fewer cutscenes", engineINIFile, "bForceNoMovies", "FullScreenMovie", new String[]{"FALSE"}, new String[]{"TRUE"});
            fewerCutscenes.setDescription("<html>Removes some cutscenes from the game. As a side effect, your loading screens turn black.<br/>There's also a mod that disables more cutscenes, without this side effect, by FromDarkHell");
            actions.add(fewerCutscenes);
            if (!BL2) {
                fewerCutscenes.disable("This feature soft-locks TPS while enabled, so it's not available through BLCMM.", false);
            }
        }
        return actions;
    }

    /**
     * Shows the resulting dialog, if applicable. Let {@code} option be true, if
     * the resulting dialog should display an option to re-open the
     * FirstTimeActionPanel window, false otherwise. Technically, {@code options ==
     * advanced} in all cases, but let's keep it seperate for now.
     *
     * @param option
     * @return
     */
    public final boolean showResultString(boolean option) {
        if (advanced) {
            return false;
        }

        int patchCount = patchActions.size();
        int patchDone = 0;
        StringBuilder sb = new StringBuilder();
        for (SetupAction action : patchActions) {
            sb.append("Hex Action: " + action.getCurrentStatus() + ", ");
            patchDone += action.getCurrentStatus() == SetupStatus.ACTIVE ? 1 : 0;
        }
        int consoleCount = consoleActions.size();
        int consoleDone = 0;
        for (SetupAction action : consoleActions) {
            sb.append("Console Action: " + action.getCurrentStatus() + ", ");
            consoleDone += (action.getCurrentStatus() == SetupStatus.ACTIVE || action.getCurrentStatus() == SetupStatus.UNKNOWN) ? 1 : 0;
        }
        if (sb.length() > 0) {
            GlobalLogger.log(sb.substring(0, sb.length() - 2));
        }
        String forgot = null;

        if (patchDone == 0 && patchCount > 0) {
            if (consoleDone == 0 && consoleCount > 0) {
                forgot = "hexedit your games <b><u>and</u></b> activate your console";
            } else {
                forgot = "hexedit your games";
            }
        } else if (consoleDone == 0 && consoleCount > 0) {
            forgot = "activate your console";
        }

        if (forgot != null) {
            String base = "<html>It seems like you were in such a hurry to get into modding, that you forgot to %s.<br/>"
                    + "You won't be contacting the developers to help you if your patch isn't working because of that, right?";
            if (option) {
                base += "<font size = \"2\"><br/><br/>"
                        + "(Press yes to see the dialog again!)";
                GlobalLogger.log(String.format("Showing dialog to re-show first time actions.(%s,%s,%s,%s)", patchCount, patchDone, consoleCount, consoleDone));
                int res = JOptionPane.showConfirmDialog(this, String.format(base, forgot), "You forgot something...", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                return res == JOptionPane.YES_OPTION;
            }
            JOptionPane.showMessageDialog(this, String.format(base, forgot), "You forgot something...", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (consoleCount < patchCount) {
            JOptionPane.showMessageDialog(this, "<html>For one or both games no configuration files could be found.<br/>"
                    + "Run the games once, and revisit the setup screen to enable console.<br/>"
                    + "This screen can be found in 'Tools'->'Setup game files for mods'", "Game detection incomplete", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return false;
    }

    private abstract class ManualSelectionListener implements ActionListener {

        private final File defaultFile;
        private final String executableBaseName;
        private final String name;
        private final String dirName;

        ManualSelectionListener(File defaultFile, String executableBaseName, String name, String dirName) {
            this.defaultFile = defaultFile;
            this.executableBaseName = executableBaseName;
            this.name = name;
            this.dirName = dirName;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {

            String pirateWarningMessage
                    = "<html><b>WARNING:</b> If you are using a pirated version of " + name + ", note that we can provide<br/>"
                    + "<i>NO SUPPORT</i> for BLCMM in the Discord or otherwise.<br/>"
                    + "<br/>"
                    + "There is no guarantee that hex-editing will work, or that mods will work even if the hex-edits do.<br/>"
                    + "A pirated TPS may fail where a pirated BL2 may succeed, etc.  Basically: if you use a pirated version,<br/>"
                    + "you're on your own for support.  We recommend just waiting for a Steam sale and picking it up<br/>"
                    + "legitimately for pennies.<br/>"
                    + "<br/>"
                    + "<i>(Apologies for nagging you, if you've got a legitimate version which we just couldn't figure<br/>"
                    + "out how to autodetect!)</i>";
            JOptionPane.showMessageDialog(FirstTimeActionsPanel.this, pirateWarningMessage, "Don't expect support for pirated versions!", JOptionPane.WARNING_MESSAGE);

            JFileChooser fc = new BLCMM_FileChooser(defaultFile);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String executable;
                    switch (OSInfo.CURRENT_OS) {
                        case UNIX:
                            executable = executableBaseName;
                            break;
                        case MAC:
                            executable = executableBaseName + ".app";
                            break;
                        default:
                            executable = executableBaseName + ".exe";
                            break;
                    }
                    return file.isDirectory() || file.getName().equalsIgnoreCase(executable);
                }

                @Override
                public String getDescription() {
                    return name + " executable";
                }
            });
            int returnVal = fc.showOpenDialog(FirstTimeActionsPanel.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                while (f != null
                        && (!f.isDirectory()
                        || !f.getName().equalsIgnoreCase(dirName))) {
                    f = f.getParentFile();
                }
                if (f == null) {
                    JOptionPane.showMessageDialog(FirstTimeActionsPanel.this, "Invalid file selected, please try again");
                    actionPerformed(evt);
                    return;
                }
                callback(f);
            }
        }

        //called on succesful selection
        public abstract void callback(File f);
    }

    public static interface ComponentProvider {

        public Component[] updateComponents();
    }

    private static class SeperatorComponentProvider implements ComponentProvider {

        private final Component seperator;

        SeperatorComponentProvider(String title) {
            JLabel label = new JLabel(title);
            label.setBorder(new EmptyBorder(5, 0, 2, 0));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            seperator = label;
        }

        @Override
        public Component[] updateComponents() {
            return new Component[]{seperator};
        }

    }

    public static abstract class SetupAction implements ComponentProvider {

        private static boolean shownErrorDialog = false;

        protected final JLabel namelabel, statuslabel;
        protected final JButton button;
        protected final String name;
        protected boolean available = true;
        protected boolean revertOnly = false;
        private String disableReason;
        private boolean disableClickable = false;
        protected JComponent[] components;
        protected SetupStatus overrideStatus = null;
        private String revertFailMessage = null;

        SetupAction(String name) {
            this.name = name;
            statuslabel = new JLabel();
            button = new JButton();
            namelabel = new JLabel(name);
            components = new JComponent[]{namelabel, statuslabel, button};
        }

        void setRevertOnly(String reason) {
            revertOnly = true;
            this.disableReason = reason;
        }

        void setRevertFailMessage(String message) {
            this.revertFailMessage = message;
        }

        public void disable(String reason, boolean clickable) {
            this.available = false;
            this.disableReason = reason;
            this.disableClickable = clickable;
        }

        protected abstract SetupStatus getRealCurrentStatus();

        public SetupStatus getCurrentStatus() {
            if (overrideStatus != null) {
                return overrideStatus;
            }
            if (revertOnly && (name == null || name.isEmpty())) {
                return SetupStatus.IGNORE;
            }
            if (!available) {
                return SetupStatus.UNAVAILABLE;
            }
            return getRealCurrentStatus();
        }

        public boolean isAvailable() {
            return available;
        }

        public void setDescription(String s) {
            if (s != null && !s.isEmpty()) {
                namelabel.setToolTipText(s);
            }
        }

        public abstract void apply();

        public abstract boolean revert();

        public abstract void fix();

        public void overrideStatus(SetupStatus newStatus) {
            overrideStatus = newStatus;
        }

        @Override
        public Component[] updateComponents() {
            switch (getCurrentStatus()) {
                case ACTIVE:
                    setActive();
                    break;
                case PARTIAL:
                    setPartial();
                    break;
                case UNKNOWN:
                    setUnknown();
                    break;
                case UNAVAILABLE:
                    setUnavailable();
                    break;
                case REFUSEFIX:
                    setRefusefix();
                    break;
                case INACTIVE:
                    setInactive();
                    break;
                case ERROR:
                    setError("");
                    break;
                default:
                    throw new IllegalStateException();
            }
            return components;
        }

        protected void setError(String text) {
            namelabel.setEnabled(false);
            components[2].setEnabled(false);
            statuslabel.setText("Error");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Error");

            if (text != null && !text.isEmpty()) {
                statuslabel.setToolTipText(Utilities.hideUserName(text));
                if (!shownErrorDialog) {
                    JOptionPane.showMessageDialog(null, "One of your selected operations caused an error.\n"
                            + "No changes were made to the file.\n"
                            + "Hover over the red `Error` text for more information.", "Error during setup",
                            JOptionPane.ERROR_MESSAGE);
                    shownErrorDialog = true;//could make this an Option
                }
            }

        }

        protected void setError(Exception e) {
            String s;
            if (e instanceof java.nio.file.AccessDeniedException) {
                s = "Can not save to " + new File(((java.nio.file.AccessDeniedException) e).getFile()).getName() + ", check if it's not set to read-only";
            } else {
                s = e.toString();
            }
            setError(s);
        }

        protected void setUnavailable() {
            statuslabel.setText("Not available");
            statuslabel.setToolTipText(disableReason);
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusDisabledText));
            if (disableClickable) {
                button.setEnabled(true);
                button.setText("More info");
                for (ActionListener l : button.getActionListeners()) {
                    button.removeActionListener(l);
                }
                button.addActionListener(e -> {
                    // html content
                    JEditorPane ep = new JEditorPane("text/html", "<html><body>" //
                            + disableReason //
                            + "</body></html>");

                    //styling
                    JLabel label = new JLabel();
                    Font font = label.getFont();
                    StyleSheet styleSheet = ((HTMLDocument) ep.getDocument()).getStyleSheet();
                    styleSheet.addRule("body{font-size:" + font.getSize() + "pt;}");
                    Color c = ThemeManager.getColor(ThemeManager.ColorType.CodeSingleQuote);
                    styleSheet.addRule("a{color: rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ");}");

                    // handle link events
                    ep.addHyperlinkListener(e1 -> {
                        if (e1.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                            try {
                                Desktop.getDesktop().browse(e1.getURL().toURI()); // roll your own link launcher or use Desktop if J6+
                            } catch (URISyntaxException | IOException ex) {
                                Logger.getLogger(FirstTimeActionsPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    Color bgColor = ThemeManager.getColor(ThemeManager.ColorType.UINimbusBase);
                    UIDefaults defaults = new UIDefaults();
                    defaults.put("EditorPane[Enabled].backgroundPainter", bgColor);
                    ep.putClientProperty("Nimbus.Overrides", defaults);
                    ep.setEditable(false);

                    // show
                    JOptionPane.showMessageDialog(null, ep);
                });
            } else {
                button.setText("-");
                button.setEnabled(false);
            }
            button.setToolTipText(disableReason);
        }

        protected void setUnknown() {
            statuslabel.setText("Unknown");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Fix");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                fix();
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        protected void setRefusefix() {
            statuslabel.setText("Error");
            statuslabel.setToolTipText(disableReason);
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Cannot Fix");
            button.setEnabled(false);
        }

        protected void setPartial() {
            statuslabel.setText("Partially installed");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusOrange));
            button.setText("Complete");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                apply();
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        protected void setInactive() {
            statuslabel.setText("Inactive");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
            button.setText("Apply");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                apply();
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
            if (revertOnly) {
                components[2].setEnabled(false);
                components[2].setToolTipText(disableReason);
            }
        }

        protected void setActive() {
            statuslabel.setText("Installed");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusGreen));
            button.setText("Revert");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (!revert()) {
                    button.setText("Can't Revert");
                    button.setEnabled(false);
                    if (this.revertFailMessage != null) {
                        button.setToolTipText(this.revertFailMessage);
                    } else {
                        button.setToolTipText("There was a problem reverting to the stock Borderlands value");
                    }
                }
                FirstTimeActionsPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        private void RemoveOldActionListeners() {
            for (ActionListener al : button.getActionListeners()) {
                button.removeActionListener(al);
            }
        }

    }

    public static class CompoundSetupAction extends SetupAction {

        private final SetupAction[] actions;

        public CompoundSetupAction(String name, SetupAction... actions) {
            super(name);
            this.actions = actions;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            HashMap<SetupStatus, Integer> ress = new HashMap<>();
            for (SetupStatus s : SetupStatus.values()) {
                ress.put(s, 0);
            }
            for (SetupAction action : actions) {
                SetupStatus s = action.getCurrentStatus();
                ress.put(s, ress.getOrDefault(s, 0) + 1);
            }
            int target = actions.length - ress.getOrDefault(SetupStatus.IGNORE, 0);
            if (ress.get(SetupStatus.ERROR) > 0) {
                return SetupStatus.ERROR;
            } else if (ress.get(SetupStatus.UNKNOWN) > 0) {
                return SetupStatus.UNKNOWN;
            } else if (ress.get(SetupStatus.UNAVAILABLE) > 0) {
                return SetupStatus.UNAVAILABLE;
            } else if (ress.get(SetupStatus.ACTIVE) == target) {
                return SetupStatus.ACTIVE;
            } else if (ress.get(SetupStatus.INACTIVE) == target) {
                return SetupStatus.INACTIVE;
            } else if (ress.get(SetupStatus.INACTIVE) + ress.get(SetupStatus.ACTIVE) + ress.get(SetupStatus.PARTIAL) == target) {
                return SetupStatus.PARTIAL;
            }
            return SetupStatus.UNKNOWN;
        }

        @Override
        public void apply() {
            for (SetupAction action : actions) {
                if (action.revertOnly) {
                    action.revert();
                } else {
                    action.apply();
                }
            }
            updateComponents();
        }

        @Override
        public boolean revert() {
            boolean succeeded = true;
            for (int i = actions.length - 1; i >= 0; i--) {
                succeeded = succeeded && actions[i].revert();
            }
            updateComponents();
            return succeeded;
        }

        @Override
        public void fix() {
            for (SetupAction action : actions) {
                action.fix();
                if (action.revertOnly) {
                    action.revert();
                }
            }
            updateComponents();
        }

    }

    public static class SingleFileSetupAction extends SetupAction {

        private final File fileToModify;
        private final File backupOfOriginal;
        private final StreamProvider streamProvider;
        private String shaOfOriginal;
        private String shaOfReplacement;
        private String lastCurrentSha;
        private boolean justExistence = false;
        private boolean allowWeakerCheck = false;
        private StreamProvider sourceForOriginal;
        private boolean neverRevert;

        SingleFileSetupAction(String name, File fileToModify, File backupOfOriginal, StreamProvider stream) {
            super(name);
            this.fileToModify = fileToModify;
            this.backupOfOriginal = backupOfOriginal;
            this.streamProvider = stream;
        }

        @Override
        public void apply() {
            if (backupOfOriginal != null) {
                if (backupOfOriginal.exists()) {
                    fileToModify.delete();
                } else {
                    fileToModify.renameTo(backupOfOriginal);
                }
            }
            if (streamProvider != null) {
                try (InputStream strm = streamProvider.provideStream();) {
                    fileToModify.getParentFile().mkdirs();
                    Files.copy(strm, fileToModify.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    strm.close();
                    setActive();
                } catch (IOException ex) {
                    setError(ex);
                    GlobalLogger.log(ex);
                }
            }
        }

        @Override
        public boolean revert() {
            if (neverRevert) {
                // This is a bit fuzzy, but we'd already be handling user
                // notification of failed reversions elsewhere if this is the
                // case.
                return true;
            }
            if (sourceForOriginal != null) {
                try (InputStream strm = sourceForOriginal.provideStream();) {
                    System.out.println(sourceForOriginal + " " + strm);
                    Files.copy(strm, fileToModify.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    strm.close();
                    backupOfOriginal.delete();
                    setInactive();
                    return true;
                } catch (IOException ex) {
                    setError(ex);
                    return false;
                }
            }
            if (backupOfOriginal == null) {
                fileToModify.delete();
                setInactive();
                return true;
            } else if (backupOfOriginal.exists()) {
                fileToModify.delete();
                backupOfOriginal.renameTo(fileToModify);
                setInactive();
                return true;
            } else if (shaOfOriginal != null) {
                File alternateBackup = findAlternateBackup();
                if (alternateBackup != null && alternateBackup.exists()) {
                    fileToModify.delete();
                    alternateBackup.renameTo(fileToModify);
                    setInactive();
                    return true;
                }
            }
            setUnknown();
            return false;
        }

        @Override
        public void fix() {
            revert();
            apply();
            updateComponents();
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            if (neverRevert) {
                return SetupStatus.IGNORE;
            } else if (justExistence) {
                return fileToModify.exists() ? SetupStatus.ACTIVE : SetupStatus.INACTIVE;
            } else if (streamProvider == null) {
                return fileToModify.exists() ? SetupStatus.INACTIVE : SetupStatus.ACTIVE;

            }
            try {
                String shaCurrent = Utilities.sha256(fileToModify);
                InputStream strm = streamProvider.provideStream();
                lastCurrentSha = Utilities.sha256(strm);
                strm.close();
                if (lastCurrentSha.equals(shaCurrent)) {
                    return SetupStatus.ACTIVE;
                }
                if (shaCurrent.equals(shaOfOriginal)) {
                    return SetupStatus.INACTIVE;
                }
                if (shaOfReplacement != null && !shaOfReplacement.equals(lastCurrentSha)) {
                    return SetupStatus.UNKNOWN;
                }
                if (allowWeakerCheck && backupOfOriginal != null) {
                    return backupOfOriginal.exists() ? SetupStatus.ACTIVE : SetupStatus.INACTIVE;
                }
                return SetupStatus.UNKNOWN;
            } catch (IOException | NoSuchAlgorithmException ex) {
                GlobalLogger.log(ex);
                return SetupStatus.ERROR;
            }
        }

        private void setJustCheckExistence() {
            justExistence = true;
        }

        private void setAllowWeakCheck() {
            this.allowWeakerCheck = true;
        }

        public void setShaOfOriginal(String shaOfOriginal) {
            this.shaOfOriginal = shaOfOriginal;
            if (sourceForOriginal != null) {
                try {
                    String sha = Utilities.sha256(sourceForOriginal.provideStream());
                    if (!sha.equals(shaOfOriginal)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        public void setShaOfReplacement(String shaOfReplacement) {
            this.shaOfReplacement = shaOfReplacement;
            if (streamProvider != null) {
                try {
                    String sha = Utilities.sha256(streamProvider.provideStream());
                    if (!sha.equals(shaOfReplacement)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        private void setSourceForOriginal(StreamProvider sourceForOriginal) {
            this.sourceForOriginal = sourceForOriginal;
            if (shaOfOriginal != null) {
                try {
                    String sha = Utilities.sha256(sourceForOriginal.provideStream());
                    if (!sha.equals(shaOfOriginal)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        private void neverRevert() {
            this.neverRevert = true;
        }

        private File findAlternateBackup() {
            assert shaOfOriginal != null;
            String filenamefilter = fileToModify.getName();
            if (filenamefilter.contains(".")) {
                filenamefilter = filenamefilter.substring(filenamefilter.indexOf("."));
            }
            final String filenamefilter1 = filenamefilter.toLowerCase();
            for (File f : fileToModify.getAbsoluteFile().getParentFile().listFiles()) {
                if (f.getName().toLowerCase().contains(filenamefilter1)) {
                    try {
                        if (Utilities.sha256(f).equals(shaOfOriginal)) {
                            return f;
                        }
                    } catch (IOException | NoSuchAlgorithmException ex) {

                    }
                }
            }
            return null;
        }

    }

    private static interface StreamProvider {

        public InputStream provideStream() throws IOException;
    }

    public static class FileStreamProvider implements StreamProvider {

        private final File file;

        public FileStreamProvider(File f) {
            this.file = f;
        }

        @Override
        public InputStream provideStream() throws IOException {
            return new FileInputStream(file);
        }

    }

    public static class ClassStreamProvider implements StreamProvider {

        private final String pathInClassPath;

        public ClassStreamProvider(String pathInClassPath) {
            this.pathInClassPath = pathInClassPath;
        }

        @Override
        public InputStream provideStream() throws IOException {
            return ClassLoader.getSystemResourceAsStream(pathInClassPath);
        }

        @Override
        public String toString() {
            return getClass() + " - path: " + pathInClassPath;
        }

    }

    public static abstract class AbstractFileEditSetupAction extends SetupAction {

        protected final File file;
        protected final String preHeaderName;
        protected final String field;
        protected transient String fileContent, relevantChunk;
        private boolean revertReadOnly = false;

        public AbstractFileEditSetupAction(String name, File file, String field, String preHeaderName) {
            super(name);
            this.file = file;
            this.preHeaderName = preHeaderName;
            this.field = field;
        }

        protected final void obtainContents() throws IOException, StringIndexOutOfBoundsException {
            fileContent = Utilities.readFileToString(file);
            int idx = fileContent.indexOf("\n[" + preHeaderName + "]\n");
            if (idx == -1) {
                throw new StringIndexOutOfBoundsException();
            }
            idx += ("\n[" + preHeaderName + "]\n").length();
            int idx2 = fileContent.indexOf("\n[", idx);
            if (idx2 == -1) {
                throw new StringIndexOutOfBoundsException();
            }
            relevantChunk = fileContent.substring(idx, idx2);
        }

        protected final String[] getRelevantLines() throws IOException, StringIndexOutOfBoundsException {
            obtainContents();
            return relevantChunk.split("\n");
        }

        protected final boolean promptForReadOnlyFile() {
            if (!file.canWrite()) {
                final String[] options = {"Yes", "No", "Yes, and revert back to readonly when done"};
                int option = JOptionPane.showOptionDialog(null, "Your " + file.getName() + " is in read-only mode.\n"
                        + "Disregard read-only mode and apply change anyway?",
                        "Read only mode detected", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (option == 1) {
                    return true;
                }
                file.setWritable(true, false);
                System.out.println(option);
                revertReadOnly = option == 2;

            }
            return false;
        }

        protected final void revertReadOnlyIfChosen() {
            if (revertReadOnly) {
                file.setReadOnly();
            }
        }
    }

    public static class INIFileEditSetupAction extends AbstractFileEditSetupAction {

        private final String[] originalValues;
        private final String[] ourValues;
        private AlternateValueChecker alternateChecker;

        public INIFileEditSetupAction(String name, File file, String field, String preHeaderName, String originalValues, String ourValues) {
            this(name, file, field, preHeaderName, new String[]{originalValues}, new String[]{ourValues});
        }

        public INIFileEditSetupAction(String name, File file, String field, String preHeaderName, String[] originalValues, String[] ourValues) {
            super(name, file, field, preHeaderName);
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

    public static class FileEditChoiceSetupAction extends AbstractFileEditSetupAction {

        private final String defaultValue;
        private final Map<String, String> choicesToValuesMap;
        private final JComboBox combobox;
        private String current;
        boolean init = false;
        private Object selchoice;

        public FileEditChoiceSetupAction(String name, File file, String field, String preHeaderName, String defaultValue, String[] choices, String[] values) {
            super(name, file, field, preHeaderName);
            this.defaultValue = defaultValue;
            this.choicesToValuesMap = new TreeMap<>();
            for (int i = 0; i < choices.length; i++) {
                choicesToValuesMap.put(choices[i], values[i]);
            }
            combobox = new JComboBox(choices);

            ((JLabel) combobox.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            combobox.addItemListener(new ItemListener() {
                private Object prevItem = null;

                @Override
                public void itemStateChanged(ItemEvent ie) {
                    if (!init) {
                        return;
                    }
                    if (ie.getStateChange() == ItemEvent.DESELECTED) {
                        prevItem = ie.getItem();
                        return;
                    }
                    try {
                        String choice = (String) combobox.getSelectedItem();
                        String val = choicesToValuesMap.get(choice);
                        StringBuilder sb = new StringBuilder();
                        boolean found = false;
                        for (String line : getRelevantLines()) {
                            if (line.startsWith(field + "=")) {
                                line = field + "=" + val;
                                found = true;
                            }
                            sb.append(line).append("\n");
                        }
                        if (!found) {
                            sb.append(field).append("=").append(val).append("\n");
                        }
                        if (promptForReadOnlyFile()) {
                            init = false;//just abuse the already existent init variable for this
                            combobox.setSelectedItem(prevItem);
                            init = true;
                            return;
                        }
                        Utilities.writeStringToFile(fileContent.replace(relevantChunk, sb).replace("\n", "\r\n"), file);
                        revertReadOnlyIfChosen();
                        current = val;
                        if (val.equals(defaultValue)) {
                            setInactive();
                        } else {
                            setActive();
                        }
                    } catch (IOException ex) {
                        setError(ex);
                        GlobalLogger.log(ex);
                    }
                }
            });
            components[2] = combobox;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            try {
                current = null;
                String lowerField = field.toLowerCase();
                for (String line : getRelevantLines()) {
                    if (line.toLowerCase().startsWith(lowerField + "=")) {
                        int idx = line.indexOf("=");
                        current = line.substring(idx + 1, line.length());
                        break;
                    }
                }
                if (current == null) {
                    current = "Error";
                    selchoice = null;
                    return SetupStatus.UNKNOWN;
                }

                selchoice = null;
                for (String choice : choicesToValuesMap.keySet()) {
                    if (choicesToValuesMap.get(choice).equals(current)) {
                        selchoice = choice;
                        break;
                    }
                }
                if (selchoice != null) {
                    combobox.setSelectedItem(selchoice);
                    if (current.equals(defaultValue)) {
                        return SetupStatus.INACTIVE;
                    } else {
                        return SetupStatus.ACTIVE;
                    }
                } else {
                    return SetupStatus.UNKNOWN;
                }
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
                return SetupStatus.ERROR;
            } catch (StringIndexOutOfBoundsException ex) {
                setError(file.getName() + "'s content does not match expectations");
                return SetupStatus.ERROR;
            }
        }

        @Override
        public Component[] updateComponents() {
            Component[] components = super.updateComponents();
            components[2] = combobox;
            switch (getCurrentStatus()) {
                case ACTIVE:
                case INACTIVE:
                    combobox.setSelectedItem(selchoice);
                    break;
                case UNKNOWN:
                    choicesToValuesMap.put(current, current);
                    combobox.setModel(new DefaultComboBoxModel(choicesToValuesMap.keySet().toArray()));
                    combobox.setSelectedItem(current);
            }
            init = true;
            return components;
        }

        @Override
        public void apply() {
        }

        @Override
        public boolean revert() {
            return true;
        }

        @Override
        public void fix() {
        }

    }

    public static enum SetupStatus {
        ACTIVE, PARTIAL, INACTIVE, UNKNOWN, UNAVAILABLE, ERROR, IGNORE, REFUSEFIX;
    }

}
