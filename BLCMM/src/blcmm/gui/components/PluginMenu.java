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
package blcmm.gui.components;

import blcmm.Startup;
import blcmm.data.lib.DataManager;
import blcmm.gui.MainGUI;
import blcmm.gui.panels.MasterSettingsPanel;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.Category;
import blcmm.model.ModelConverter;
import blcmm.model.ModelElement;
import blcmm.model.PatchType;
import blcmm.plugins.BLCMMModelPlugin;
import blcmm.plugins.BLCMMPlugin;
import blcmm.plugins.BLCMMUtilityPlugin;
import blcmm.plugins.PluginLoader;
import blcmm.plugins.PluginSecurityManager;
import blcmm.plugins.pseudo_model.PCategory;
import blcmm.utilities.BLCMMUtilities;
import blcmm.utilities.Utilities;
import general.utilities.GlobalLogger;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author LightChaosman
 */
public class PluginMenu extends JMenu {

    static {
        PluginLoader.loadPlugins();
        System.setSecurityManager(new PluginSecurityManager());
    }
    private final Map<BLCMMPlugin, JMenuItem> pluginToButtonMap = new LinkedHashMap<>();
    private final Map<BLCMMUtilityPlugin, ForceClosingJFrame> nonModalMap = new HashMap<>();

    private JMenuItem addExternalPluginButton;
    private JMenuItem makeYourOwnPluginButton;
    private JMenuItem removePluginButton;

    public PluginMenu() {
        initLoadedPlugins();
        initFixedButtons();
    }

    private void initLoadedPlugins() {
        for (BLCMMPlugin plugin : PluginLoader.PLUGINS.keySet()) {
            JMenuItem item = new JMenuItem(plugin.getName());
            item.addActionListener(new ActionListener() {

                private Category getPluginRoot() {
                    final String name = "Plugin outputs";
                    for (ModelElement el : MainGUI.INSTANCE.getCurrentPatch().getRoot().getElements()) {
                        if (el instanceof Category && ((Category) el).getName().equals(name)) {
                            return (Category) el;
                        }
                    }
                    Category r = new Category(name);
                    MainGUI.INSTANCE.getCurrentPatch().insertElementInto(r, MainGUI.INSTANCE.getCurrentPatch().getRoot());
                    return r;
                }

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (plugin instanceof BLCMMUtilityPlugin && nonModalMap.containsKey((BLCMMUtilityPlugin) plugin)) {
                        nonModalMap.get((BLCMMUtilityPlugin) plugin).requestFocus();
                        return;
                    }
                    try {
                        MainGUI.INSTANCE.cursorWait();
                        PluginLoader.setModelSupplier((f1, f2) -> ModelConverter.convertToPseudo(MainGUI.INSTANCE.getCurrentPatch(), f1, f2));
                        JPanel panel = plugin.getGUI();//We use a modal option pane, so the rest of BLCMM is inaccesable while the plugin GUI is displayed
                        Window main;
                        Container contentPane;
                        if (plugin instanceof BLCMMUtilityPlugin) {
                            main = new ForceClosingJFrame(plugin.getName());
                            contentPane = ((ForceClosingJFrame) main).getContentPane();
                        } else {
                            main = new JDialog(MainGUI.INSTANCE, plugin.getName());
                            contentPane = ((JDialog) main).getContentPane();
                        }
                        contentPane.setLayout(new GridBagLayout());
                        contentPane.add(panel, new GridBagConstraints(1, 1, 3, 1, 100d, 100d, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
                        Utilities.makeWindowOfComponentResizable(panel);
                        JButton okButton = new JButton("OK");
                        JButton cancelButton = new JButton("Cancel");
                        boolean[] ok = {false};
                        okButton.addActionListener(e -> {
                            ok[0] = true;
                            main.dispose();
                        });
                        cancelButton.addActionListener(e -> {
                            ok[0] = false;
                            main.dispose();
                        });

                        StringBuilder info = new StringBuilder();
                        if (plugin.getAuthor() != null) {
                            info.append("Plugin made by: " + plugin.getAuthor() + " - ");
                        }
                        if (plugin.getVersion() != null) {
                            info.append("Version: " + plugin.getVersion() + " - ");
                        }
                        info.append("Compiled on: " + SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(new Date(PluginLoader.PLUGINS.get(plugin).getCompileTime().toMillis())));
                        JLabel label = new JLabel(info.toString());
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                        int bottomSize = label.getPreferredSize().width + okButton.getPreferredSize().width + cancelButton.getPreferredSize().width + 3 * 10;
                        if ((float) bottomSize / panel.getPreferredSize().width > 1.2f) {
                            float alpha = 96f / 255f;
                            float[] backC = panel.getBackground().getColorComponents(null);
                            float[] foreC = label.getForeground().getColorComponents(null);
                            for (int i = 0; i < backC.length; i++) {
                                backC[i] = alpha * foreC[i] + (1 - alpha) * backC[i];
                            }
                            int r = (int) (backC[0] * 255);
                            int g = (int) (backC[1] * 255);
                            int b = (int) (backC[2] * 255);
                            label.setText("<html><table style=\"color: " + String.format("rgb(%s,%s,%s);border-spacing: 0;", r, g, b) + "\"><tr><td>" + info.toString().replace(" - ", "</td></tr><tr><td>").replace(":", ":</td><td>") + "</td></tr></table>");

                            contentPane.add(label, new GridBagConstraints(1, 2, 1, 2, 100d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
                            contentPane.add(okButton, new GridBagConstraints(2, 3, 1, 1, 1d, 1d, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
                            contentPane.add(cancelButton, new GridBagConstraints(2, 2, 1, 1, 1d, 1d, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
                        } else {
                            label.setForeground(new Color(label.getForeground().getRed(), label.getForeground().getGreen(), label.getForeground().getBlue(), 96));

                            contentPane.add(label, new GridBagConstraints(1, 2, 1, 1, 100d, 1d, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
                            contentPane.add(okButton, new GridBagConstraints(2, 2, 1, 1, 1d, 1d, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
                            contentPane.add(cancelButton, new GridBagConstraints(3, 2, 1, 1, 1d, 1d, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
                        }

                        main.pack();

                        if (plugin instanceof BLCMMUtilityPlugin) {
                            nonModalMap.put((BLCMMUtilityPlugin) plugin, (ForceClosingJFrame) main);
                            main.addWindowListener(new WindowAdapter() {
                                @Override
                                public void windowClosed(WindowEvent e) {
                                    nonModalMap.remove((BLCMMUtilityPlugin) plugin);
                                }
                            });
                        } else {
                            ((JDialog) main).setModal(true);
                        }
                        main.setLocationRelativeTo(MainGUI.INSTANCE);
                        MainGUI.INSTANCE.cursorNormal();
                        main.setVisible(true);

                        if (plugin instanceof BLCMMModelPlugin && ok[0]) {
                            final PCategory pseudo;
                            JProgressBar pbar = ((BLCMMModelPlugin) plugin).getProgressBar();
                            if (pbar == null) {
                                MainGUI.INSTANCE.cursorWait();
                                pseudo = ((BLCMMModelPlugin) plugin).getOutputModel();
                                MainGUI.INSTANCE.cursorNormal();
                            } else {
                                JDialog progressBarDialog = new JDialog(MainGUI.INSTANCE);
                                progressBarDialog.setLayout(new BoxLayout(progressBarDialog.getContentPane(), BoxLayout.PAGE_AXIS));
                                progressBarDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
                                JLabel myLabel = new JLabel("Please wait while the plugin is working", SwingConstants.CENTER);
                                myLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                                progressBarDialog.add(myLabel);
                                progressBarDialog.add(pbar);
                                progressBarDialog.setUndecorated(true);
                                SwingWorker<PCategory, PCategory> worker = new SwingWorker<PCategory, PCategory>() {

                                    @Override
                                    protected PCategory doInBackground() throws Exception {
                                        try {
                                            return ((BLCMMModelPlugin) plugin).getOutputModel();
                                        } catch (Throwable t2) {
                                            throw new RuntimeException(t2);
                                        }
                                    }

                                    @Override
                                    protected void done() {
                                        progressBarDialog.dispose();
                                    }
                                };
                                worker.execute();
                                progressBarDialog.pack();
                                progressBarDialog.setLocationRelativeTo(MainGUI.INSTANCE);
                                progressBarDialog.setVisible(true);
                                pseudo = worker.get();
                            }
                            if (pseudo != null) {
                                Category r = ModelConverter.convertFromPseudo(pseudo, MainGUI.INSTANCE.getCurrentPatch());
                                if (r.size() == 0) {
                                    JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                                            "The plugin did not produce any output!",
                                            "No output from plugin",
                                            JOptionPane.WARNING_MESSAGE
                                    );
                                } else {
                                    Category root = getPluginRoot();
                                    Optional<Category> findFirst = root.getElements().stream().
                                            filter(e -> e instanceof Category).
                                            map(e -> (Category) e).
                                            filter(e -> e.getName().equalsIgnoreCase(r.getName())).
                                            findFirst();
                                    if (findFirst.isPresent()) {
                                        int showInputDialog = JOptionPane.showOptionDialog(MainGUI.INSTANCE, "There was already an output with name '" + r.getName() + "'.\n"
                                                + "Overwrite the old category, or keep both?", "Existing output found", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                                new String[]{"Overwrite", "Keep both"}, "Overwrite");
                                        if (showInputDialog == 0) {
                                            MainGUI.INSTANCE.getCurrentPatch().removeElementFromParentCategory(findFirst.get());
                                        }
                                    }
                                    MainGUI.INSTANCE.getCurrentPatch().insertElementInto(r, root);
                                    MainGUI.INSTANCE.profileChanged();
                                }
                            }
                        }
                    } catch (Throwable t) {
                        GlobalLogger.log("plugin '" + plugin.getName() + "' crashed with the following exception:");
                        GlobalLogger.log(t);
                        MainGUI.INSTANCE.cursorNormal();
                        if (t instanceof OutOfMemoryError) {
                            JOptionPane.showMessageDialog(MainGUI.INSTANCE, "The plugin ran out of memory. Try allocating more RAM using the launcher.\n"
                                    + "If this does not resolve the issue, contact the developer of the plugin and provide the log.",
                                    "Plugin ran out of memory. Allocate more RAM", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(MainGUI.INSTANCE, "The plugin crashed. The error has been logged.\n"
                                    + "Please contact the developer of the plugin and provide the log.",
                                    "Crash in plugin", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    MainGUI.INSTANCE.requestFocus();
                }
            });
            this.add(item);
            pluginToButtonMap.put(plugin, item);
        }
        updatePluginMenuEnabledness();
        if (!PluginLoader.FAILED_TO_LOAD.isEmpty()) {
            JSeparator brokenSeperator = new JSeparator(SwingConstants.HORIZONTAL);
            this.add(brokenSeperator);
        }
        // This loads our broken, or otherwise improper plugins but without the ability to load them.
        for (Map.Entry<String, String> entry : PluginLoader.FAILED_TO_LOAD.entrySet()) {
            String pluginName = entry.getKey();
            String pluginError = entry.getValue();
            String errorReport = String.format("<html>"
                    + "The plugin %s, crashed while loading into BLCMM!<br>"
                    + "The plugin was probably written for an older version of BLCMM, check if there are updates for the plugin<br>"
                    + "Error: %s.<br>"
                    + "Check your log file at: %s for more details!</html>",
                    pluginName, pluginError, Utilities.hideUserName(GlobalLogger.getLOG().getAbsolutePath()));
            JMenuItem item = new JMenuItem(pluginName);
            item.addActionListener(e -> {
                JDialog dialog = new JDialog(MainGUI.INSTANCE, "Plugin Error!", Dialog.ModalityType.APPLICATION_MODAL);
                Icon icon = javax.swing.UIManager.getIcon("OptionPane.errorIcon");
                JLabel label = new JLabel(errorReport);
                label.setIconTextGap(10);
                label.setIcon(icon);
                JButton ok = new JButton("OK");

                JPanel panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = c.gridy = 0;
                c.gridwidth = 5;
                c.gridheight = 1;
                c.insets = new Insets(10, 10, 10, 10);
                panel.add(label, c);
                c.gridy = c.gridwidth = 1;
                c.weightx = 100;
                panel.add(new JPanel(), c);
                c.weightx = 1;
                c.gridx++;
                c.insets.left = 0;
                c.insets.right = 5;

                JButton openFile = new JButton("Open log");
                JButton openFolder = new JButton("Open log folder");

                panel.add(openFolder, c);
                c.gridx++;
                panel.add(openFile, c);
                c.gridx++;
                c.insets.right = 10;

                openFile.addActionListener((ActionEvent ae) -> {
                    try {
                        Desktop.getDesktop().open(GlobalLogger.getLOG());
                    } catch (IOException ex) {
                        Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dialog.dispose();
                });

                openFolder.addActionListener((ActionEvent ae) -> {
                    try {
                        Desktop.getDesktop().open(GlobalLogger.getLOG_FOLDER());
                    } catch (IOException ex) {
                        Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dialog.dispose();
                });

                panel.add(ok, c);
                ok.addActionListener((ActionEvent ae) -> {
                    dialog.dispose();
                });
                dialog.add(panel);
                dialog.setIconImages(MainGUI.INSTANCE.getIconImages());
                dialog.pack();
                dialog.setLocationRelativeTo(MainGUI.INSTANCE);
                ok.requestFocus(); //so just mashing enter causes OK to be pressed, and not a folder to be opened as well.
                dialog.setVisible(true);
            });
            item.setToolTipText(errorReport);
            item.setForeground(ThemeManager.getColor(ThemeManager.ColorType.TreeCommentChecker));
            this.add(item);
        }

    }

    private void initFixedButtons() {
        JSeparator seperator = new JSeparator(SwingConstants.HORIZONTAL);
        addExternalPluginButton = new JMenuItem("Add external plugin");
        addExternalPluginButton.addActionListener(ae -> {
            int yesno = JOptionPane.showConfirmDialog(null,
                    "You are about to add an external plugin.\n"
                    + "The code of such a plugin may be unsafe, and cause harm to your system.\n"
                    + "Only proceed if you trust the author.\n"
                    + "Do you wish to proceed?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (yesno != JOptionPane.YES_OPTION) {
                MainGUI.INSTANCE.requestFocus();
                return;

            }
            JFileChooser choose = new BLCMM_FileChooser(BLCMMUtilities.getLauncher().getParentFile());
            choose.setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
            int openres = choose.showOpenDialog(null);
            if (openres != JFileChooser.APPROVE_OPTION) {
                MainGUI.INSTANCE.requestFocus();
                return;
            }
            File plugin = choose.getSelectedFile();
            File dest = new File(PluginLoader.PLUGINS_DIR_TO_UPDATE + File.separator + plugin.getName());
            try {
                PluginLoader.PLUGINS_DIR_TO_UPDATE.mkdirs();
                Files.copy(plugin.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                        "Unable to copy plugin: " + ex.getMessage(),
                        "Error copying plugin", JOptionPane.ERROR_MESSAGE);
            }
            Startup.promptRestart(false);
            MainGUI.INSTANCE.requestFocus();
        });
        makeYourOwnPluginButton = new JMenuItem("Make your own plugin");
        makeYourOwnPluginButton.addActionListener(e -> {
            int showConfirmDialog = JOptionPane.showConfirmDialog(null, "This will generate a Netbeans project set up to develop a plugin for BLCMM on your system.\n"
                    + "The generated project contains the required libraries, but no data.\n"
                    + "To supply your development enviroment with data, copy the data folder of BLCMM to the project directory.\n"
                    + "Do you wish to proceed?", "Confirm project generation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (showConfirmDialog != JOptionPane.YES_OPTION) {
                MainGUI.INSTANCE.requestFocus();
                return;
            }
            JFileChooser fc = new BLCMM_FileChooser(BLCMMUtilities.getLauncher().getParentFile());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int showSaveDialog = fc.showSaveDialog(null);
            if (showSaveDialog != JFileChooser.APPROVE_OPTION) {
                MainGUI.INSTANCE.requestFocus();
                return;
            }
            //sources
            InputStream stream = ClassLoader.getSystemResourceAsStream("resources/BLCMM_Plugin.zip");
            File whereToFindLibs = new File(System.getProperty("user.dir") + "/lib/");
            String[] libs = new String[]{//
                "BLCMM_utilities-javadoc.jar",
                "BLCMM_Utilities.jar",
                "BLCMM_Data_Interaction_Library-javadoc.jar",
                "BLCMM_Data_Interaction_Library.jar"};
            //destinations
            File destdir = fc.getSelectedFile();
            String whereToPutLibs = destdir.getAbsolutePath() + "/BLCMM Plugin/lib/";
            File dataFolder1ToMake = new File(destdir.getAbsolutePath() + "/BLCMM Plugin/data/BL2/");
            File dataFolder2ToMake = new File(destdir.getAbsolutePath() + "/BLCMM Plugin/data/TPS/");

            String pluginPackageName = "plugin_" + Long.toString(System.nanoTime(), 36); //because... why not? :^)
            try {
                Utilities.unzip(stream, destdir);
                dataFolder1ToMake.mkdirs();
                dataFolder2ToMake.mkdirs();
                new File(whereToPutLibs).mkdirs();
                for (File f : whereToFindLibs.listFiles()) {
                    for (String lib : libs) {
                        if (f.getName().equalsIgnoreCase(lib)) {
                            Files.copy(f.toPath(), new File(whereToPutLibs + f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                File src = new File(destdir.getAbsolutePath() + "/BLCMM Plugin/src/blcmm/plugins/myplugin");
                for (File f : src.listFiles()) {
                    //They're tiny files, we can afford to be inefficient.
                    Utilities.writeStringToFile(Utilities.readFileToString(f).replace("package blcmm.plugins.myplugin;", "package blcmm.plugins." + pluginPackageName + ";"), f);
                }
                src.renameTo(new File(destdir.getAbsolutePath() + "/BLCMM Plugin/src/blcmm/plugins/" + pluginPackageName));
            } catch (IOException ex) {
                Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                        "Unable to setup Netbeans project: " + ex.getMessage(),
                        "Error setting up Netbeans project", JOptionPane.ERROR_MESSAGE);
            }
            MainGUI.INSTANCE.requestFocus();
        });
        this.add(seperator);
        this.add(addExternalPluginButton);
        if (PluginLoader.PLUGINS.size() + PluginLoader.FAILED_TO_LOAD.size() > 0) {
            removePluginButton = new JMenuItem("Remove plugin");
            removePluginButton.addActionListener(e -> {
                String jarName = (String) JOptionPane.showInputDialog(MainGUI.INSTANCE, "Select the plugin to delete", "Delete plugin", JOptionPane.QUESTION_MESSAGE, null, PluginLoader.PLUGINS_DIR.list((File dir, String name1) -> name1.endsWith(".jar")), null);
                // No point in marking null for deletion
                // Happens when the user just opens the menu
                if (jarName != null) {
                    PluginLoader.markForDeletion(jarName);
                    Startup.promptRestart(false);
                }
                MainGUI.INSTANCE.requestFocus();

            });
            this.add(removePluginButton);
        }
        this.add(makeYourOwnPluginButton);
    }

    public void updatePluginMenuEnabledness() {
        for (BLCMMPlugin plugin : pluginToButtonMap.keySet()) {
            JMenuItem item = pluginToButtonMap.get(plugin);
            item.setEnabled(true);//innocent until proven otherwise
            if (MainGUI.INSTANCE.getCurrentPatch() == null) {
                item.setEnabled(false);
                item.setToolTipText("You must open a file for plugins to work");
                continue;
            }
            item.setToolTipText(null);
            item.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
            if (false
                    || (MainGUI.INSTANCE.getCurrentPatch().getType() == PatchType.BL2 && !plugin.supportsBorderlands2())
                    || (MainGUI.INSTANCE.getCurrentPatch().getType() == PatchType.TPS && !plugin.supportsBorderlandsTPS())) {
                item.setToolTipText("This plugin is not available for " + MainGUI.INSTANCE.getCurrentPatch().getType());
                item.setEnabled(false);
            } else {
                String[] neededClasses = plugin.getRequiredDataClasses();
                if (neededClasses != null && neededClasses.length > 0) {
                    List<String> missing = Arrays.stream(neededClasses).filter(c -> !DataManager.isDataForClassPresent(c)).collect(Collectors.toList());
                    if (!missing.isEmpty()) {
                        item.setEnabled(false);

                        List<String> packages = missing.stream().map(c -> DataManager.getDataPackageForClass(c)).distinct().collect(Collectors.toList());
                        String postfix;
                        if (packages.isEmpty() || (packages.size() == 1 && packages.iterator().next() == null)) {
                            postfix = "classes: " + Arrays.toString(missing.toArray());
                        } else {
                            postfix = "packages: " + Arrays.toString(packages.toArray());
                        }
                        item.setToolTipText("<html>Some of the required data packages are missing.<br>"
                                + "No access to the following required: " + postfix
                                + "<br>Clicking on the plugin will download the packages for you!</html>");
                        if (item.getMouseListeners().length <= 2) {
                            item.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseClicked(MouseEvent e) {
                                    if (e.getClickCount() > 1) {
                                        return;
                                    }
                                    MasterSettingsPanel panel = new MasterSettingsPanel();
                                    panel.focusToUpdatePanel();
                                    panel.getUpdatePanel().checkAllNeededPackages(packages,
                                            new boolean[]{
                                                plugin.supportsBorderlands2(),
                                                plugin.supportsBorderlandsTPS()
                                            });
                                    Startup.promptRestart(true);
                                    // Just in case the user decides not to restart, GC'll pick this up later.
                                    panel = null;
                                    MainGUI.INSTANCE.requestFocus();
                                }
                            });
                        }
                    } else {
                        FileTime c_plugin = PluginLoader.PLUGINS.get(plugin).getCompileTime();
                        FileTime c_data = Startup.getDataLibraryCompiletime();
                        FileTime c_util = Startup.getUtilitiesCompiletime();
                        if (c_plugin.compareTo(c_util) < 0 || c_plugin.compareTo(c_data) < 0) {
                            item.setForeground(ThemeManager.getColor(ThemeManager.ColorType.TreeContentError));
                            item.setToolTipText("<html>BLCMM has been updated since this plugin was released.<br/>This may cause a crash. If so, please update your plugin.");
                        }
                    }
                }
            }
        }
    }

    public void closeAllUtilities() {
        nonModalMap.forEach((p, w) -> w.dispose());
        nonModalMap.clear();
    }

}
