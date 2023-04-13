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
package blcmm.gui;

import blcmm.Meta;
import blcmm.Startup;
import blcmm.data.lib.DataManager;
import blcmm.data.lib.DataManagerManager;
import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.components.DefaultTextTextField;
import blcmm.gui.components.ForceClosingJFrame;
import blcmm.gui.components.GUIDataStatusNotifier;
import blcmm.gui.components.GameSelectionPanel;
import blcmm.gui.components.InfoLabel;
import blcmm.gui.components.TimedLabel;
import blcmm.gui.panels.AboutPanel;
import blcmm.gui.panels.HexEditPanel;
import blcmm.gui.panels.IniTweaksPanel;
import blcmm.gui.panels.IntegerConverter;
import blcmm.gui.panels.MasterSettingsPanel;
import blcmm.gui.panels.SetupGameFilesPanel;
import blcmm.gui.theme.Theme;
import blcmm.gui.theme.ThemeManager;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.gui.tree.rightmouse.RightMouseButtonAction;
import blcmm.model.Category;
import blcmm.model.CompletePatch;
import blcmm.model.ModelElement;
import blcmm.model.PatchIO;
import blcmm.model.PatchType;
import blcmm.model.Profile;
import blcmm.utilities.*;
import com.vdurmont.semver4j.Semver;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public final class MainGUI extends ForceClosingJFrame {

    public static final String DEFAULT_FONT_NAME = "Dialog", CODE_FONT_NAME = "Monospaced";
    public static MainGUI INSTANCE;

    private final String titlePostfix;
    private File currentFile;
    private File exportPath = null;
    private CompletePatch patch;
    private SwingWorker backupThread;
    private SwingWorker versionCheckThread;
    private boolean disposed = false;
    private boolean startedMaximized = false;
    private PatchType openedType;
    private JMenu fileHistoryMenu;
    private JTextField searchField;
    private String newVersion = null;

    private boolean editWindowOpen = false;
    private boolean objectExplorerWindowOpen = false;
    private boolean themeSwitchAllowed = true;

    private boolean currentlyLoadingPatch = false;

    private DataManagerManager dmm;

    private File argToOpen;

    /**
     * Creates new form MainGUI
     *
     * @param toOpen The file to open
     * @param titlePostfix A string to add onto the title of this window
     */
    public MainGUI(final File toOpen, final String titlePostfix) {
        INSTANCE = this;
        GUI_IO_Handler.MASTER_UI = INSTANCE;
        this.argToOpen = toOpen;
        this.titlePostfix = titlePostfix;

        // Load our data in the background.  It's possible (probable?) we could
        // do this later on instead, but I don't want to worry about whether
        // or not components have to care about having access to the DMM
        // object or not, so this is now basically the very first thing that
        // happens.
        //
        // The whole background-worker thing is necessary because our
        // GUIDataStatus object needs to be able to update the GUI as it reports
        // on progress, and a SwingWorker is kind of the way to do that.
        SwingWorker dataLoadWorker = new SwingWorker() {

            @Override
            protected Object doInBackground() {
                dmm = new DataManagerManager(PatchType.BL2, new GUIDataStatusNotifier());
                return null;
            }

            @Override
            protected void done() {
                finishInit();
            }

        };
        dataLoadWorker.execute();
    }

    /**
     * Finishes up the initialization process.  This was originally inline as
     * part of the constructor, but since the DataManagerManager init is
     * happening in a SpringWorker, this needed to be able to be called-out-to
     * once that worker's done.
     */
    private void finishInit() {

        initComponents();
        addSearchLayer();
        getGameSelectionPanel().addItemListenerToComboBox(this::gameSelectionAction);

        themeComboBox.setSelectedItem(Options.INSTANCE.getTheme());
        jSpinner1.setValue(Options.INSTANCE.getFontsize());

        setIconImages(Arrays.asList(
                new Image[]{
                    IconManager.getBLCMMIcon(16),
                    IconManager.getBLCMMIcon(32),
                    IconManager.getBLCMMIcon(48),
                    IconManager.getBLCMMIcon(64)}));

        timedLabel.setFont(timedLabel.getFont().deriveFont(Font.BOLD));
        timedLabel.setText("");

        // Re-apply our theme, to hopefully get our tree icons sorted
        setTheme(ThemeManager.getTheme());
        initializeWindowSize();
        updateFontSizes();
        Utilities.changeCTRLMasks(this.getRootPane());
        this.setVisible(true);
        Runnable runnable = () -> {
            initializeTree(this.argToOpen);
            setChangePatchTypeEnabled(patch != null);
            backupThread = startupBackupThread();
            versionCheckThread = null;
            if (Options.INSTANCE.getCheckForNewVersions() && !Utilities.isCreatorMode()) {
                versionCheckThread = startupVersionCheckThread();
            }
        };
        boolean showGUIPriorToLoading = true;
        if (showGUIPriorToLoading) {
            EventQueue.invokeLater(runnable);
            new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    MainGUI.this.setEnabled(false);
                    runnable.run();
                    MainGUI.this.setEnabled(true);
                    return null;
                }
            };
        } else {
            runnable.run();
        }

    }

    private void addSearchLayer() {
        final JPanel layer2 = new JPanel();
        jLayeredPane1.add(layer2);
        jLayeredPane1.setLayer(layer2, 100, 0);
        jLayeredPane1.setLayer(jScrollPane1, 0, 0);
        jLayeredPane1.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layer2.setBounds(jLayeredPane1.getBounds());
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                layer2.setBounds(jLayeredPane1.getBounds());
            }
        });
        jScrollPane1.getViewport().addChangeListener((ChangeEvent e) -> {
            layer2.repaint();
        });
        jTree1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                layer2.repaint();
            }
        });

        layer2.setOpaque(false);
        layer2.setLayout(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.insets.top = 5;
        cs.insets.right = 20;
        cs.weightx = cs.weighty = 1;
        cs.gridx = 4;
        cs.gridy = 1;
        cs.anchor = GridBagConstraints.NORTHEAST;
        JLabel iLabel = new InfoLabel("Hover over the colored elements to see what the colors mean<br/>"
                + "Use the search box to quickly find relevant pieces of your file<br/>"
                + "Check the options menu to enable additional features");
        layer2.add(iLabel, cs);

        searchField = new DefaultTextTextField("Search");
        searchField.setColumns(10);
        searchField.setBorder(new BevelBorder(BevelBorder.RAISED));
        searchField.setToolTipText("Hold shift/ctrl while pressing enter to also search code, not just categories");
        cs.insets.right = 5;
        cs.gridx--;

        layer2.add(searchField, cs);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String st = searchField.getText();
                    if (st.length() > 2) {
                        CheckBoxTree tree = getTree();
                        int[] res = tree.search(st, e.getModifiers() > 0);// idx 0 = current, idx 1 = total
                        getTimedLabel().showTemporary("result " + res[0] + " of " + res[1]);
                    }
                }
            }
        });

        //padding
        cs.gridx = 0;
        cs.weightx = 10000;
        JPanel padding = new JPanel();
        padding.setOpaque(false);
        layer2.add(padding, cs);

        EventQueue.invokeLater(jTree1::requestFocus);
    }

    private void initializeWindowSize() {
        // Resize the main window from our preferences
        if (Options.INSTANCE.getMainWindowMaximized()) {
            // If we start maximized, the user's window manager won't have any
            // idea what size to make the window if it un-maximizes, which on
            // some systems would leave it the same size as the desktop.  So
            // we'll keep track of how we started and manually resize if
            // necessary.
            startedMaximized = true;
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setSize(Options.INSTANCE.getMainWindowWidth(),
                    Options.INSTANCE.getMainWindowHeight());
        }
        setLocationRelativeTo(null);

        // Window state listener, to resize from maximized properly
        this.addWindowStateListener(e -> {
            if (e.getNewState() == JFrame.NORMAL && e.getOldState() == JFrame.MAXIMIZED_BOTH) {
                if (startedMaximized) {
                    setSize((int) Options.INSTANCE.getOption(Options.OptionNames.mainWindowWidth).getDefaultData(),
                            (int) Options.INSTANCE.getOption(Options.OptionNames.mainWindowHeight).getDefaultData());
                    setLocationRelativeTo(null);

                    // Only do this once, since the user's window manager
                    // will remember from now on.
                    startedMaximized = false;
                }
            }
        });
    }

    private SwingWorker startupBackupThread() {
        AutoBackupper.cleanOldBackups();
        SwingWorker thread = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                while (!MainGUI.INSTANCE.disposed) {
                    for (int i = 0; i < AutoBackupper.BACKUP_INTERVAL / 1000; i++) {
                        Thread.sleep(1000);
                        if (MainGUI.INSTANCE.disposed) {
                            return null;
                        }
                    }
                    final File f = currentFile == null ? new File("New File") : currentFile;
                    AutoBackupper.backup(f, new AutoBackupper.Backupable() {
                        @Override
                        public void write(BufferedWriter writer) throws IOException {
                            PatchIO.writeToFile(patch, writer, false);
                        }

                        @Override
                        public boolean inNeedOfBackup() {
                            return getTree().isChanged();
                        }
                    });
                    ((TimedLabel) timedLabel).showTemporary("Made backup of mod");
                }
                return null;
            }
        };
        thread.execute();
        return thread;
    }

    /**
     * Starts up a new thread to check for a new version in the background
     *
     * @return null
     */
    private SwingWorker startupVersionCheckThread() {
        SwingWorker thread = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {

                try {
                    Thread.sleep(1000);
                    if (MainGUI.INSTANCE.disposed) {
                        return null;
                    }

                    // Now do the check
                    URL url = new URL(Meta.UPDATE_VERSION_URL);
                    URLConnection conn = url.openConnection();
                    InputStream stream = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                    String remoteVersionStr = br.readLine().trim();
                    br.close();
                    stream.close();
                    if (MainGUI.INSTANCE.disposed) {
                        return null;
                    }

                    // Check the versions
                    Semver thisVersion = new Semver(Meta.VERSION);
                    Semver remoteVersion = new Semver(remoteVersionStr);
                    if (remoteVersion.isGreaterThan(thisVersion)) {
                        GlobalLogger.log("New version detected: " + remoteVersionStr);
                        ((TimedLabel) timedLabel).putString("version",
                                Meta.NAME + " v" + remoteVersionStr + " is available!  Check Help > About",
                                3);
                        newVersion = remoteVersionStr;
                    } else {
                        GlobalLogger.log("No new " + Meta.NAME + " version detected");
                    }

                } catch (Exception e) {
                    GlobalLogger.log("Error checking for new software versions:");
                    GlobalLogger.log(e);
                }

                return null;
            }
        };
        thread.execute();
        return thread;
    }

    private void initializeTree(final File toOpen) {
        this.updateTitle();
        // Figure out which "recent" file to open.
        Utilities.cleanFileHistory(this.dmm.getCurrentPatchType());
        String[] files = Options.INSTANCE.getFileHistory();
        //first try to open the provided file
        boolean opened = false;
        try {
            if (toOpen != null && toOpen.exists() && toOpen.isFile()) {
                currentFile = toOpen;
                opened = openPatch(currentFile);
            }
            if (Startup.isFirstBootAfterCrash()) {
                File backupFile = AutoBackupper.getMostRecentBackupFile();
                if (backupFile != null) {
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "It appears " + Meta.NAME + " crashed last time.\nWould you like to open the most recent backup file?",
                            "Open backup file?", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        opened = openPatch(backupFile);
                        currentFile = backupFile;
                    }
                }
            }
            //If that fails - use the file history (i.e. app was opened without a given file)
            if (!opened) {
                String file = files.length > 0 ? files[0] : "";
                currentFile = new File(file);
                opened = openPatch(currentFile);
            }
        } catch (Exception e) {
            GlobalLogger.log(e);
            GlobalLogger.markAsPermanentLog();//should be redundant
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, String.format(""
                    + "<html>A fatal error occured while opening " + Meta.NAME + ". It was caused by opening the following file:<br/>"
                    + "<b>%s</b><br/>"
                    + "Please send the log file and the file you were trying to open to the devolopers."
                    + "Try opening a different file, or create a new file.",
                    Utilities.hideUserName(currentFile.getAbsolutePath())), "Fatal error while opening file", JOptionPane.ERROR_MESSAGE);
            currentFile = null;
            opened = false;
        }
        if (opened) {
            SetUIModel(patch);
        } else {
            GlobalLogger.log("No file opened");
            DefaultMutableTreeNode node = new DefaultMutableTreeNode("Go to \"File -> Open\" to browse to the location of your mod.");
            node.add(new DefaultMutableTreeNode("Use the buttons in the file explorer to find the correct Binaries folder to place mods in."));
            node.add(new DefaultMutableTreeNode("Never execute more than 1 file. Use the import function to combine multiple files into 1."));
            jTree1.setModel(new DefaultTreeModel(node));
            jTree1.setEnabled(false);
            // Ensure that our Recent Files menu is populated, if possible
            this.updateFileHistoryMenu();
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

        jPanel1 = new javax.swing.JPanel();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new CheckBoxTree();
        jPanel2 = new javax.swing.JPanel();
        gameTypePanel = new GameSelectionPanel();
        jSpinner1 = new javax.swing.JSpinner();
        fontSizeLabel = new javax.swing.JLabel();
        themeComboBox = new javax.swing.JComboBox<>(ThemeManager.getAllInstalledThemes().toArray(new Theme[0]));
        timedLabel = new TimedLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        newFileMenuButton = new javax.swing.JMenuItem();
        openMenuButton = new javax.swing.JMenuItem();
        saveMenuButton = new javax.swing.JMenuItem();
        saveToFileMenuButton = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        importModMenuButton = new javax.swing.JMenuItem();
        importModFolderMenuButton = new javax.swing.JMenuItem();
        importModZipMenuButton = new javax.swing.JMenuItem();
        getMoreModsMenuButton = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        quitMenuButton = new javax.swing.JMenuItem();
        ToolsMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();
        setupGameFilesButton = new javax.swing.JMenuItem();
        objectExplorerButton = new javax.swing.JMenuItem();
        getDataPackMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        iniTweaksMenuItem = new javax.swing.JMenuItem();
        hexEditsMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenu1 = new javax.swing.JMenu();
        bpdnumMenuItem = new javax.swing.JMenuItem();
        profileMenu = new javax.swing.JMenu();
        HelpMenu = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        uninstallMenuItem = new javax.swing.JMenuItem();

        setTitle("dummy");

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Made by LightChaosman");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Opening your file. Please wait.");
        treeNode1.add(treeNode2);
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jScrollPane1.setViewportView(jTree1);

        jLayeredPane1.setLayer(jScrollPane1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 862, Short.MAX_VALUE)
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 525, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );

        javax.swing.GroupLayout gameTypePanelLayout = new javax.swing.GroupLayout(gameTypePanel);
        gameTypePanel.setLayout(gameTypePanelLayout);
        gameTypePanelLayout.setHorizontalGroup(
            gameTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 52, Short.MAX_VALUE)
        );
        gameTypePanelLayout.setVerticalGroup(
            gameTypePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 24, Short.MAX_VALUE)
        );

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(12, 8, 36, 1));
        jSpinner1.setMinimumSize(new java.awt.Dimension(40, 20));
        jSpinner1.setPreferredSize(new java.awt.Dimension(60, 25));
        jSpinner1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinner1StateChanged(evt);
            }
        });

        fontSizeLabel.setText("Global font size");

        themeComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                themeComboBoxItemStateChanged(evt);
            }
        });

        timedLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        timedLabel.setText("jLabel4");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(gameTypePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(timedLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 553, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(themeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fontSizeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(fontSizeLabel)
                        .addComponent(themeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(gameTypePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(timedLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        FileMenu.setText("File");

        newFileMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        newFileMenuButton.setText("New file");
        newFileMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFileMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(newFileMenuButton);

        openMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openMenuButton.setText("Open");
        openMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(openMenuButton);

        saveMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        saveMenuButton.setText("Save");
        saveMenuButton.setEnabled(false);
        saveMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(saveMenuButton);

        saveToFileMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        saveToFileMenuButton.setText("Save as");
        saveToFileMenuButton.setEnabled(false);
        saveToFileMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveToFileMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(saveToFileMenuButton);
        FileMenu.add(jSeparator1);

        importModMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        importModMenuButton.setText("Import mod file(s)");
        importModMenuButton.setEnabled(false);
        importModMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importModMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(importModMenuButton);

        importModFolderMenuButton.setText("Import mods folder");
        importModFolderMenuButton.setEnabled(false);
        importModFolderMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importModFolderMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(importModFolderMenuButton);

        importModZipMenuButton.setText("Import zip folder");
        importModZipMenuButton.setEnabled(false);
        importModZipMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importModZipMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(importModZipMenuButton);

        getMoreModsMenuButton.setFont(new java.awt.Font("SansSerif", 2, 13)); // NOI18N
        getMoreModsMenuButton.setText("Get more mods");
        getMoreModsMenuButton.setToolTipText("Opens the BLCM github, the place where most mods are stored");
        getMoreModsMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getMoreModsMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(getMoreModsMenuButton);
        FileMenu.add(jSeparator3);

        quitMenuButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        quitMenuButton.setText("Quit");
        quitMenuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuButtonActionPerformed(evt);
            }
        });
        FileMenu.add(quitMenuButton);

        jMenuBar1.add(FileMenu);

        ToolsMenu.setText("Tools");

        settingsMenuItem.setText("Settings");
        settingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(settingsMenuItem);

        setupGameFilesButton.setText("Setup game files for mods");
        setupGameFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setupGameFilesButtonActionPerformed(evt);
            }
        });
        ToolsMenu.add(setupGameFilesButton);

        objectExplorerButton.setText("Object explorer");
        objectExplorerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                objectExplorerButtonActionPerformed(evt);
            }
        });
        ToolsMenu.add(objectExplorerButton);

        getDataPackMenuItem.setFont(new java.awt.Font("sansserif", 2, 13)); // NOI18N
        getDataPackMenuItem.setText("Download OE Datapacks");
        getDataPackMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getDataPackMenuButtonActionPerformed(evt);
            }
        });
        ToolsMenu.add(getDataPackMenuItem);
        ToolsMenu.add(jSeparator4);

        iniTweaksMenuItem.setText("INI Tweaks");
        iniTweaksMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                iniTweaksMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(iniTweaksMenuItem);

        hexEditsMenuItem.setText("Legacy Hex Edits");
        hexEditsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexEditsMenuItemActionPerformed(evt);
            }
        });
        ToolsMenu.add(hexEditsMenuItem);
        ToolsMenu.add(jSeparator2);

        jMenu1.setText("Various tools");

        bpdnumMenuItem.setText("Behavior Number Converter");
        bpdnumMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bpdnumMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(bpdnumMenuItem);

        ToolsMenu.add(jMenu1);

        jMenuBar1.add(ToolsMenu);

        profileMenu.setText("Profiles");
        jMenuBar1.add(profileMenu);

        HelpMenu.setText("Help");

        jMenuItem11.setText("About");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        HelpMenu.add(jMenuItem11);

        jMenuItem1.setText("View changelog");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        HelpMenu.add(jMenuItem1);

        uninstallMenuItem.setText("Uninstall OpenBLCMM");
        uninstallMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uninstallMenuItemActionPerformed(evt);
            }
        });
        HelpMenu.add(uninstallMenuItem);

        jMenuBar1.add(HelpMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        JDialog jDialog = new JDialog(this, "About | " + Meta.NAME + " version " + Meta.VERSION);
        jDialog.add(new AboutPanel(true));
        jDialog.setModal(true);
        // TODO: this minimum size setting is kind of a total hack.  If we *don't*
        // set it, the dialog freaks the hell out when it's shrunk even a little.
        // These values happen to let it work on my system, but will that be enough
        // on others?  I expect that can't be guaranteed.  This keeps on getting
        // expanded as more things get shoved into the dialog, alas.  At the moment
        // it'll end up looking a bit too big ordinarily -- one of our potential
        // database errors can span multiple lines, though, so we need at least this
        // to account for one of those.  (If more than one throws that error, it's
        // likely it'll break anyway, but eh...  I suppose we're gonna have AoDK
        // data in here soon enough, too.)
        jDialog.setMinimumSize(new Dimension(640, 630));
        jDialog.pack();
        jDialog.setLocationRelativeTo(this);
        jDialog.setVisible(true);
        this.requestFocus();
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void iniTweaksMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_iniTweaksMenuItemActionPerformed
        MainGUI.showIniTweaks();
        this.requestFocus();
    }//GEN-LAST:event_iniTweaksMenuItemActionPerformed

    private void objectExplorerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_objectExplorerButtonActionPerformed
        this.launchObjectExplorerWindow();
    }//GEN-LAST:event_objectExplorerButtonActionPerformed

    private void settingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsMenuItemActionPerformed
        MasterSettingsPanel panel = new MasterSettingsPanel();
        JOptionPane.showMessageDialog(this, panel, "Settings", JOptionPane.PLAIN_MESSAGE);

        /*
         * We don't actually have anything which does this anymore, and I want
         * to comment out the promptRestart (and associated) methods in Startup,
         * since they sort of don't work anymore, but maybe we'll want to do
         * something like it in the future, and it'd be nice to have a place to
         * start.  So anyway, commenting this entirely for now.
        if (panel.needsToolReset()) {
            Startup.promptRestart();
        }
        /**/

        // Resize our tree if need be.
        if (panel.needsTreeResize()) {
            ((CheckBoxTree) jTree1).updateTreeLengths();
            SwingUtilities.updateComponentTreeUI(this);
        }
        this.requestFocus();
    }//GEN-LAST:event_settingsMenuItemActionPerformed

    private void quitMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuButtonActionPerformed
        MainGUI.INSTANCE.dispose();
    }//GEN-LAST:event_quitMenuButtonActionPerformed

    private void importModZipMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importModZipMenuButtonActionPerformed
        ImportAnomalyLog.INSTANCE.clear();
        JFileChooser fc = new BLCMM_FileChooser(this.getImportDialogPath());
        fc.setFileFilter(new FileNameExtensionFilter("ZIP files", "zip"));
        fc.setDialogTitle("Import Zip Folder");
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                this.cursorWait();
                File unzipped = Utilities.unzip(file);
                int number = GUI_IO_Handler.addMods(unzipped, patch);
                Utilities.deepDelete(unzipped);

                Options.INSTANCE.setLastImport(file);
                this.cursorNormal();
                GUI_IO_Handler.reportImportResults(number, false, patch);
            } catch (IOException ex) {
                this.cursorNormal();
                Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this,
                        "Unable to import zipfile  " + fc.getSelectedFile() + "\n"
                        + ex.getMessage(),
                        "Error importing zipfile", JOptionPane.ERROR_MESSAGE);
            }
        }
        this.requestFocus();
    }//GEN-LAST:event_importModZipMenuButtonActionPerformed

    private void importModFolderMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importModFolderMenuButtonActionPerformed
        String binaries = GameDetection.getBinariesDir(patch.getType());
        File initial;
        if (binaries == null) {
            initial = currentFile.getAbsoluteFile().getParentFile();
        } else {
            initial = new File(binaries + File.separator + "mods" + File.separator);
            if (!initial.exists()) {
                initial.mkdir();
            }
        }
        BLCMM_FileChooser chooser = new BLCMM_FileChooser(initial);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Import Mod Folders");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File mods = chooser.getSelectedFile();
            ImportAnomalyLog.INSTANCE.clear();
            this.cursorWait();
            int number = GUI_IO_Handler.addMods(mods, patch);
            this.cursorNormal();
            GUI_IO_Handler.reportImportResults(number, false, patch);
        }
        this.requestFocus();
    }//GEN-LAST:event_importModFolderMenuButtonActionPerformed

    private void importModMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importModMenuButtonActionPerformed
        ImportAnomalyLog.INSTANCE.clear();
        JFileChooser fc = new BLCMM_FileChooser(this.getImportDialogPath());
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Import Mods");
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFiles();
            this.cursorWait();
            int mods = GUI_IO_Handler.addMods(files, patch);
            if (files.length > 0) {
                Options.INSTANCE.setLastImport(files[files.length - 1]);
            }
            this.cursorNormal();
            GUI_IO_Handler.reportImportResults(mods, false, patch);
        }
        this.requestFocus();
    }//GEN-LAST:event_importModMenuButtonActionPerformed

    private void saveToFileMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveToFileMenuButtonActionPerformed
        this.saveToFileAction();
    }//GEN-LAST:event_saveToFileMenuButtonActionPerformed

    private void saveMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuButtonActionPerformed
        this.saveAction();
    }//GEN-LAST:event_saveMenuButtonActionPerformed

    private void openMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuButtonActionPerformed
        if (this.promptUnsavedContinue()) {
            JFileChooser fc = new BLCMM_FileChooser(this.getOpenDialogPath());
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                currentFile = file;
                exportPath = null;
                boolean open = openPatch(file);
                if (open) {
                    addCurrentFileToFrontOfPreviousFiles();
                } else {
                    JOptionPane.showMessageDialog(this, "Unable to open " + file.getName(), "Error opening file", JOptionPane.ERROR_MESSAGE);
                }
            }
            this.requestFocus();
        }
    }//GEN-LAST:event_openMenuButtonActionPerformed

    private void newFileMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newFileMenuButtonActionPerformed
        if (this.promptUnsavedContinue()) {
            CompletePatch emptypatch = new CompletePatch();
            emptypatch.setType(getGameSelectionPanel().getNonNullGameType());
            emptypatch.setRoot(new Category(Category.DEFAULT_ROOT_NAME));
            emptypatch.createNewProfile("default");
            patch = emptypatch;
            this.disablePatchRootStatuses();
            SetUIModel(emptypatch);
            currentFile = null;
            openedType = null;
            getTimedLabel().putString("saveStatus", createNewDynamicString(false), 3);
            enableIOButtons();
            this.updateTitle("Untitled");
        }
    }//GEN-LAST:event_newFileMenuButtonActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {
            JTextPane area = new JTextPane();
            area.setContentType("text/html");
            InputStream resourceAsStream = ClassLoader.getSystemClassLoader().getResourceAsStream("CHANGELOG.md");
            BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            // Render the markdown
            Parser parser = Parser.builder().build();
            Node document = parser.parse(sb.toString());
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            renderer.render(document);

            area.setText(renderer.render(document));
            area.setEditable(false);
            area.setCaretPosition(0);
            //area.setFont(new java.awt.Font(CODE_FONT_NAME, 0, Options.INSTANCE.getFontsize()));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(500, 500));
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, scroll, "Changelog", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException ex) {
            Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void getMoreModsMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getMoreModsMenuButtonActionPerformed
        try {
            URL faq = new URL("https://github.com/BLCM/ModCabinet/wiki");
            Desktop.getDesktop().browse(faq.toURI());
        } catch (IOException | URISyntaxException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(this,
                    "Unable to launch browser: " + ex.getMessage(),
                    "Error launching Browser", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_getMoreModsMenuButtonActionPerformed

    private void uninstallMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uninstallMenuItemActionPerformed
        File uninstallFile = Paths.get(Utilities.getMainInstallDir().toString(), "unins000.exe").toFile();
        if (OSInfo.CURRENT_OS == OSInfo.OS.WINDOWS
                && System.getProperty("java.vm.version").startsWith("GraalVM")
                && uninstallFile.exists()
                ) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This will remove the " + Meta.NAME + " application from your system.\n"
                    + "Data packs, logfiles, and configuration will remain on disk and\n"
                    + "must be cleaned out manually if you wish to remove them.\n"
                    + "\n"
                    + "If you have data packs, they will be installed at:\n"
                    + "    " + Utilities.getMainInstallDir().getAbsolutePath() + "\n"
                    + "\n"
                    + "Logfiles and configuration files can be found at:\n"
                    + "    " + Utilities.getBLCMMDataDir() + "\n"
                    + "\n"
                    + "Proceed?",
                    "Proceed with uninstall?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                GlobalLogger.log("Triggering uninstall application and exiting");
                Runtime.getRuntime().exec(new String[] {uninstallFile.getAbsolutePath()});
                this.dispose();
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                JOptionPane.showMessageDialog(null,
                        "Something went wrong when preparing for uninstalling " + Meta.NAME + ".",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    Meta.NAME + " only has a built-in uninstallation procedure when it was\n"
                    + "installed via the official installer on Windows systems.  For other\n"
                    + "platforms, you should just remove the directory in which you unzipped\n"
                    + "the application.\n"
                    + "\n"
                    + "It looks like you can find that directory here:\n"
                    + "    " + Utilities.getMainInstallDir().getAbsolutePath() + "\n"
                    + "\n"
                    + "Logfiles and configuration files can be found at:\n"
                    + "    " + Utilities.getBLCMMDataDir() + "\n",
                    "Uninstallation Information",
                    JOptionPane.OK_OPTION);
        }
    }//GEN-LAST:event_uninstallMenuItemActionPerformed

    private void bpdnumMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bpdnumMenuItemActionPerformed
        ForceClosingJFrame jFrame = new ForceClosingJFrame("Behavior number converter");
        jFrame.add(new IntegerConverter());
        jFrame.pack();
        jFrame.setLocationRelativeTo(this);
        jFrame.setVisible(true);
    }//GEN-LAST:event_bpdnumMenuItemActionPerformed

    private void themeComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_themeComboBoxItemStateChanged
        if (themeComboBox.getSelectedItem() == ThemeManager.getTheme()) {
            return;
        }
        Options.INSTANCE.setTheme((Theme) themeComboBox.getSelectedItem());
        setTheme((Theme) themeComboBox.getSelectedItem());
        repaint();
        jMenuBar1.repaint();
    }//GEN-LAST:event_themeComboBoxItemStateChanged

    private void jSpinner1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinner1StateChanged
        int newSize = (Integer) jSpinner1.getValue();
        if (newSize != Options.INSTANCE.getFontsize()) {
            Options.INSTANCE.setFontSize(newSize);
            updateFontSizes();
        }
    }//GEN-LAST:event_jSpinner1StateChanged

    private void setupGameFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setupGameFilesButtonActionPerformed
        JDialog dialog = new JDialog(this, "Setup Game Files for Mods");
        dialog.add(new SetupGameFilesPanel());
        dialog.setModal(true);
        dialog.setMinimumSize(new Dimension(650, 400));
        dialog.setPreferredSize(new Dimension(650, 400));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        this.requestFocus();
    }//GEN-LAST:event_setupGameFilesButtonActionPerformed

    private void hexEditsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexEditsMenuItemActionPerformed
        MainGUI.showHexEdits();
        this.requestFocus();
    }//GEN-LAST:event_hexEditsMenuItemActionPerformed

    private void getDataPackMenuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getDataPackMenuButtonActionPerformed
        try {
            URL faq = new URL(Meta.DATA_DOWNLOAD_URL);
            Desktop.getDesktop().browse(faq.toURI());
        } catch (IOException | URISyntaxException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(this,
                    "Unable to launch browser: " + ex.getMessage(),
                    "Error launching Browser", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_getDataPackMenuButtonActionPerformed

    private void gameSelectionAction(ItemEvent e) {
        PatchType type = getGameSelectionPanel().getNonNullGameType();
        this.dmm.setPatchType(type);
        patch.setType(type);
        if (!this.currentlyLoadingPatch) {
            ((CheckBoxTree) jTree1).setChanged(true);
        }
    }

    @Override
    public void dispose() {
        if (isReadyToSuperDispose()) {
            superDispose();
        }
    }

    public boolean isReadyToSuperDispose() {
        // Save our main window dimensions, even if we don't end up actually
        // quitting.
        Options.INSTANCE.setMainWindowWidth(MainGUI.INSTANCE.getWidth());
        Options.INSTANCE.setMainWindowHeight(MainGUI.INSTANCE.getHeight());
        Options.INSTANCE.setMainWindowMaximized((MainGUI.INSTANCE.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH
        );
        if (RightMouseButtonAction.isShowingEditWindow()) {
            RightMouseButtonAction.bringEditWindowToFront();
            return false;
        }
        return this.promptUnsavedContinue();

    }

    /**
     * Cancels our version-check thread, if it's still active, and removes
     * any version-check notices from our TimedLabel
     */
    public void cancelVersionCheckNotices() {
        if (INSTANCE.versionCheckThread != null) {
            INSTANCE.versionCheckThread.cancel(true);
        }
        ((TimedLabel) timedLabel).remove("version");
    }

    /**
     * Cancel any ongoing background threads that might be running
     */
    public void superDispose() {
        if (INSTANCE.backupThread != null) {
            INSTANCE.backupThread.cancel(true);
        }
        if (INSTANCE.versionCheckThread != null) {
            INSTANCE.versionCheckThread.cancel(true);
        }
        disposed = true;
        super.dispose();//Will delete log
    }

    boolean isDisposed() {
        return disposed;
    }

    /**
     * Displays our INI Tweaks dialog
     */
    public static void showIniTweaks() {
        IniTweaksPanel panel = new IniTweaksPanel();
        JOptionPane.showMessageDialog(null, panel, "INI Tweaks", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Displays our Hex Edits dialog
     */
    public static void showHexEdits() {
        HexEditPanel panel = new HexEditPanel();
        JOptionPane.showMessageDialog(null, panel, "Legacy Hex Edits", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Returns True if we're allowed to proceed with unsaved changes. If there
     * have been no changes, this returns True. Otherwise, a Cancel/Yes/No
     * dialog is presented to the user. On Cancel, we return false. On No, we
     * return True and discard the current patch. On Yes, we attempt to save the
     * patch, and return true if we were successful, and false otherwise.
     *
     * @return True if we have permission to proceed, False otherwise.
     */
    public boolean promptUnsavedContinue() {
        if (((CheckBoxTree) jTree1).isChanged()) {
            int x = JOptionPane.showConfirmDialog(this, "Save unsaved changes?");
            this.requestFocus();
            switch (x) {
                case JOptionPane.CANCEL_OPTION:
                    return false;
                case JOptionPane.NO_OPTION:
                    return true;
                case JOptionPane.YES_OPTION:
                    return this.saveAction();
                default:
                    // Shouldn't be able to get here.  Cancel just to be safe.
                    return false;
            }
        } else {
            return true;
        }
    }

    /**
     * Returns the GameSelectionPanel UI component
     *
     * @return The game selection panel
     */
    private GameSelectionPanel getGameSelectionPanel() {
        return (GameSelectionPanel) gameTypePanel;
    }

    /**
     * Returns the PatchType currently selected by our game dropdown. Just a
     * convenience function to avoid repetitive typing, etc.
     *
     * @return The PatchType currently selected
     */
    private PatchType getSelectedGame() {
        return getGameSelectionPanel().getNonNullGameType();
    }

    /**
     * Returns a File object which can be used to set the current directory of
     * an "Open" action. If there is a current file loaded, this will be the
     * directory that file lives in. If not, it will be the "binaries" directory
     * of the currently-selected game type. As a final fallback, use the parent
     * directory of the system-defined user directory.
     *
     * @return A File object suitable for passing to
     * JFileChooser.setCurrentDirectory
     */
    public File getOpenDialogPath() {
        if (currentFile == null || currentFile.getPath().equals("")) {
            String binariesDir = GameDetection.getBinariesDir(getGameSelectionPanel().getNonNullGameType());
            if (binariesDir != null) {
                return new File(binariesDir);
            }
        } else {
            return currentFile.getAbsoluteFile().getParentFile();
        }
        return Utilities.getDefaultOpenLocation();
    }

    public File getExportDialogPath() {
        if (exportPath == null) {
            return getOpenDialogPath();
        }
        return exportPath;
    }

    public void setExportDialogPath(File file) {
        exportPath = file;
    }

    /**
     * Returns a File object which can be used to set the current directory of
     * an "Import" action. If the user has imported a file previously, that
     * directory will be returned. If not, the "binaries" directory of the
     * currently-selected game type. As a final fallback, use the parent
     * directory of the system-defined user directory.
     *
     * @return A File object suitable for passing to
     * JFileChooser.setCurrentDirectory
     */
    public File getImportDialogPath() {
        String lastImport = Options.INSTANCE.getLastImport();
        if (lastImport == null || lastImport.equals("")) {
            String binariesDir = GameDetection.getBinariesDir(this.getSelectedGame());
            if (binariesDir != null) {
                return new File(binariesDir);
            }
        } else {
            File lastImportDir = new File(lastImport).getParentFile();
            if (lastImportDir.exists()) {
                return lastImportDir;
            }
        }
        return Utilities.getDefaultOpenLocation();
    }

    /**
     * Returns our main CheckBoxTree object.
     *
     * @return The CheckBoxTree object.
     */
    public CheckBoxTree getMainTree() {
        return (CheckBoxTree) jTree1;
    }

    public boolean openPatch(File f) {
        if (f == null || !f.exists()) {
            return false;
        }
        ImportAnomalyLog.INSTANCE.clear();
        long start = System.currentTimeMillis();
        CompletePatch newpatch = GUI_IO_Handler.parseFile(f);
        if (newpatch == null) {
            return false;
        }
        patch = newpatch;
        currentFile = f;
        this.disablePatchRootStatuses();
        SetUIModel(patch);
        updateProfileMenu();
        enableIOButtons();
        String filename2 = truncateFileName(f, Options.INSTANCE.getFilenameTruncationLength());
        this.updateTitle(filename2);
        openedType = patch.getType();
        long time = (System.currentTimeMillis() - start) / 1000;
        String postfix = time > 4 ? " (" + time + " seconds)" : "";
        getTimedLabel().showTemporary("Successfully opened " + f.getName() + postfix);
        getTimedLabel().putString("saveStatus", createNewDynamicString(false), 3);
        updateFileHistoryMenu();

        if (ImportAnomalyLog.INSTANCE.size() > 0) {
            GUI_IO_Handler.reportImportResults(1, true, patch);
        }
        GlobalLogger.log("Opened " + Utilities.hideUserName(f.toString()));
        return true;

    }

    /**
     * Disables statuses on our root element (error highlighting, etc), and also
     * on our top "mods" folder, if it exists.
     */
    private void disablePatchRootStatuses() {
        if (this.patch.getRoot() != null) {
            this.patch.getRoot().getTransientData().disableStatuses();
            for (ModelElement e : this.patch.getRoot().getElements()) {
                if (e instanceof Category
                        && ((Category) e).getName().equals("mods")) {
                    e.getTransientData().disableStatuses();
                    break;
                }
            }
        }
    }

    public void enableIOButtons() {
        importModFolderMenuButton.setEnabled(true);
        importModMenuButton.setEnabled(true);
        importModZipMenuButton.setEnabled(true);
        saveMenuButton.setEnabled(true);
        saveToFileMenuButton.setEnabled(true);
        getGameSelectionPanel().setEnabled(true);
    }

    void SetUIModel(CompletePatch patch) {
        ((CheckBoxTree) jTree1).setPatch(patch);
        this.currentlyLoadingPatch = true;
        getGameSelectionPanel().setType(patch.getType());
        this.currentlyLoadingPatch = false;
        jTree1.setEnabled(true);
        jTree1.getActionMap().put("Search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocus();
            }
        });
        jTree1.getInputMap().put(KeyStroke.getKeyStroke("control F"), "Search");
        updateProfileMenu();
    }

    /**
     * Function to save our patch.
     *
     * @return True if the patch was successfully saved, False otherwise.
     */
    private boolean saveAction() {
        if (currentFile != null && currentFile.exists()) {
            return savePatch(patch, currentFile, false);
        } else {
            return saveToFileAction();
        }
    }

    /**
     * Function to save to a specific filename ("save as").
     *
     * @return True if the patch was successfully saved, False otherwise.
     */
    private boolean saveToFileAction() {
        BLCMM_FileChooser fc;
        if (currentFile != null && currentFile.exists()) {
            fc = new BLCMM_FileChooser(this.getOpenDialogPath(), currentFile.getName(), true, true);
        } else {
            fc = new BLCMM_FileChooser(this.getOpenDialogPath(), "", true, true);
        }
        int returnVal = fc.showSaveDialog(this);
        this.requestFocus();

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            currentFile = file;
            boolean result = savePatch(patch, file, false);
            if (result) {
                addCurrentFileToFrontOfPreviousFiles();
            }

            return result;
        }
        return false;
    }

    /**
     * Saves a patch file out to the specified file.  Runs various sanity checks
     * given the current state of the patch in the GUI, and handles the work
     * of opening the file for writing.  The actual file contents are all handled
     * in PatchIO via the writeToFile method.
     *
     * @param patch The patchset to save
     * @param file The file to save to
     * @param exporting Whether or not we're exporting a mod
     * @return Whether or not the file was actually saved
     */
    public boolean savePatch(CompletePatch patch, File file, boolean exporting) {
        if (patch != null && openedType != null) {
            if (patch.getType() != openedType) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "It seems you are converting " + openedType.getGameNameWithArticle() + " patch to " + patch.getType().getGameNameWithArticle() + " patch.\n"
                        + "\n"
                        + "Continue?",
                        "Confirm Patch Type Change",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (choice != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        if (file.getAbsolutePath().startsWith(AutoBackupper.getDestination())) {
            int choice = JOptionPane.showConfirmDialog(this, "It seems you are saving to the backup directory.\n\nContinue?",
                    "Saving to backup directory",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        if (patch == null) {
            throw new NullPointerException();
        }
        if (exporting) {
            exportPath = file;
            this.getTimedLabel().showTemporary(
                    "<html>Exported to <tt>" + file.getName() + "</tt>");
        } else {//saving
            currentFile = file;
            String filename2 = truncateFileName(file, Options.INSTANCE.getFilenameTruncationLength());
            this.updateTitle(filename2);
            getTimedLabel().putString("saveStatus", createNewDynamicString(true), 3);
            getTimedLabel().showTemporary("Mod saved", ThemeManager.getColor(ThemeManager.ColorType.UIText));
            addCurrentFileToFrontOfPreviousFiles();
        }
        if (file.exists() && !file.canWrite()) {
            int confirm = JOptionPane.showConfirmDialog(this, ""
                    + "The file " + file.getName() + " appears to be set to read-only.\n"
                    + "Can not save to a read-only file.\n"
                    + "Remove the read-only restriction?",
                    "Read-only detected", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                file.setWritable(true, false);
            }
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            List<String> res = PatchIO.writeToFile(patch, bw, exporting);
            bw.close();
            ((CheckBoxTree) jTree1).setChanged(false);
            for (String report : res) {
                JOptionPane.showMessageDialog(null, report, "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                    "An error was encountered while saving the file: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Sets our theme based on the given ThemeManager.Theme
     *
     * @param theme The theme to set
     */
    public static void setTheme(Theme theme) {

        // First assign all our colors via the theme class
        ThemeManager.setTheme(theme);

        // Now propagate those changes through the UI in various ways
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        EventQueue.invokeLater(() -> {
            if (INSTANCE != null) {
                INSTANCE.themeComboBox.setSelectedItem(ThemeManager.getTheme());
                SwingUtilities.updateComponentTreeUI(INSTANCE);
                SwingUtilities.updateComponentTreeUI(INSTANCE.jMenuBar1);
                INSTANCE.searchField.setForeground(ThemeManager.getTheme().get(ThemeManager.ColorType.CodeText));
            }
            if (ObjectExplorer.INSTANCE != null) {
                SwingUtilities.updateComponentTreeUI(ObjectExplorer.INSTANCE);
            }
            if (RightMouseButtonAction.isShowingEditWindow()) {
                SwingUtilities.updateComponentTreeUI(RightMouseButtonAction.getOpenEditDialog());
            }
        });
    }

    /**
     * Updates our main Tree, useful for when the tree contents sizes may have
     * changed, such as at the end of a drag-n-drop operation. Just a simple
     * wrapper around a SwingUtilities call.
     */
    public void updateComponentTreeUI() {
        SwingUtilities.updateComponentTreeUI(this);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenu HelpMenu;
    private javax.swing.JMenu ToolsMenu;
    private javax.swing.JMenuItem bpdnumMenuItem;
    private javax.swing.JLabel fontSizeLabel;
    private javax.swing.JPanel gameTypePanel;
    private javax.swing.JMenuItem getDataPackMenuItem;
    private javax.swing.JMenuItem getMoreModsMenuButton;
    private javax.swing.JMenuItem hexEditsMenuItem;
    private javax.swing.JMenuItem importModFolderMenuButton;
    private javax.swing.JMenuItem importModMenuButton;
    private javax.swing.JMenuItem importModZipMenuButton;
    private javax.swing.JMenuItem iniTweaksMenuItem;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JTree jTree1;
    private javax.swing.JMenuItem newFileMenuButton;
    private javax.swing.JMenuItem objectExplorerButton;
    private javax.swing.JMenuItem openMenuButton;
    private javax.swing.JMenu profileMenu;
    private javax.swing.JMenuItem quitMenuButton;
    private javax.swing.JMenuItem saveMenuButton;
    private javax.swing.JMenuItem saveToFileMenuButton;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JMenuItem setupGameFilesButton;
    private javax.swing.JComboBox<Theme> themeComboBox;
    private javax.swing.JLabel timedLabel;
    private javax.swing.JMenuItem uninstallMenuItem;
    // End of variables declaration//GEN-END:variables

    private void updateProfileMenu() {
        profileMenu.removeAll();
        JMenuItem newProfileButton = new JMenuItem("New profile");
        newProfileButton.setFont(newProfileButton.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
        newProfileButton.addActionListener(e -> {
            String name1 = promptUserForNewProfileName();
            MainGUI.INSTANCE.requestFocus();
            if (name1 == null) {
                return;
            }
            patch.createNewProfile(name1);
            updateProfileMenu();
            profileChanged();
        });
        profileMenu.add(newProfileButton);
        JMenuItem infoButton = new JMenuItem("What are profiles?");
        infoButton.setFont(newProfileButton.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
        infoButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(MainGUI.this,
                    "<html>Profiles are a way to save multiple configurations in the same file.<br/>"
                    + "Each profile saves what's selected and what's not, so you can go back and<br/>"
                    + "forth between selection choices easily.  (You may have a set of mods you<br/>"
                    + "want to use while running Digistruct Peak which you don't want active<br/>"
                    + "otherwise, for instance.)",
                    "Profile information", JOptionPane.INFORMATION_MESSAGE);
        });
        profileMenu.add(infoButton);
        if (!patch.getProfiles().isEmpty()) {
            profileMenu.add(new JSeparator());
            for (final Profile profile : patch.getProfiles()) {
                String name = profile.getName();
                JMenuItem menu = new JMenu(name);
                menu.setFont(menu.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
                menu.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        patch.setCurrentProfile(profile);
                        profileChanged();
                        updateProfileMenu();
                        javax.swing.MenuSelectionManager.defaultManager().clearSelectedPath();
                    }

                });
                JMenuItem renameButton = new JMenuItem("Rename profile");
                renameButton.setFont(renameButton.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
                renameButton.addActionListener(e -> {
                    String name1 = promptUserForNewProfileName();
                    MainGUI.INSTANCE.requestFocus();
                    if (name1 == null) {
                        return;
                    }
                    patch.renameProfile(profile, name1);
                    updateProfileMenu();
                });
                menu.add(renameButton);
                if (patch.getProfiles().size() > 1) {
                    JMenuItem deleteButton = new JMenuItem("Delete profile");
                    deleteButton.setFont(deleteButton.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
                    deleteButton.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(MainGUI.INSTANCE, "Are you sure you wish to delete the profile '" + profile.getName() + "'?", "Confirm deletion", JOptionPane.YES_NO_OPTION);
                        MainGUI.INSTANCE.requestFocus();
                        if (confirm != JOptionPane.YES_OPTION) {
                            return;
                        }
                        patch.deleteProfile(profile);
                        updateProfileMenu();
                    });
                    menu.add(deleteButton);
                }
                if (profile == patch.getCurrentProfile()) {
                    menu.setFont(menu.getFont().deriveFont(Font.BOLD));
                    menu.setForeground(new Color(200, 0, 0));
                    menu.setOpaque(true);
                }
                profileMenu.add(menu);
            }
        }
    }

    private String promptUserForNewProfileName() {
        String name = null;
        while (name == null) {
            String input = JOptionPane.showInputDialog(MainGUI.INSTANCE, "Insert name", "Insert name", JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return null;
            }
            input = input.trim();
            boolean onlyValidCharacter = true;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (!(Character.isAlphabetic(c) || Character.isDigit(c) || c == ' ' || c == '-' || c == '_')) {
                    onlyValidCharacter = false;
                    break;
                }
            }
            if (onlyValidCharacter) {
                Profile already = patch.getProfile(input);
                if (input.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Profile name may not be empty.", "Invalid name", JOptionPane.PLAIN_MESSAGE);
                } else if (already == null) {
                    name = input;//This will break the loop
                } else {
                    JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Profile " + input + " already exists", "Profile already exists", JOptionPane.PLAIN_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Profile names may only contain alphanumeric values", "Invalid name", JOptionPane.PLAIN_MESSAGE);
            }
        }
        return name;
    }

    public void profileChanged() {
        SetUIModel(patch);
        ((CheckBoxTree) jTree1).setChanged(true);
    }

    public CompletePatch getCurrentPatch() {
        return patch;
    }

    public void updateFontSizes() {
        setUIFont(new javax.swing.plaf.FontUIResource(new Font(DEFAULT_FONT_NAME, Font.PLAIN, Options.INSTANCE.getFontsize())));
        updateFontsizes(this);
        ((CheckBoxTree) jTree1).updateFontSizes();
        ((BasicTreeUI) jTree1.getUI()).setLeftChildIndent(15);//Since our icons do not change with font size
    }

    private void updateFontsizes(Container main) {
        for (Component c : main.getComponents()) {
            c.setFont(c.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
            if (c instanceof Container) {
                updateFontsizes((Container) c);
            }
        }
        if (main instanceof JMenu) {
            JMenu main2 = (JMenu) main;
            for (Component c : main2.getMenuComponents()) {
                c.setFont(c.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
                if (c instanceof Container) {
                    updateFontsizes((Container) c);
                }

            }
        }
        SwingUtilities.updateComponentTreeUI(main);
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }

    public void setChangePatchTypeEnabled(boolean selected) {
        getGameSelectionPanel().setEnabled(selected);
    }

    private void addCurrentFileToFrontOfPreviousFiles() {
        String[] fileHistory = Options.INSTANCE.getFileHistory();
        String thisFile = currentFile.getAbsolutePath();
        LinkedList<String> l = new LinkedList<>();
        l.addAll(Arrays.asList(fileHistory));
        while (l.contains(thisFile)) {
            l.remove(thisFile);
        }
        l.addFirst(thisFile);
        for (int i = 0; i < l.size(); i++) {
            if (!new File(l.get(i)).exists()) {
                l.remove(i--);
            }
        }
        while (l.size() > 9) {
            l.removeLast();
        }

        Options.INSTANCE.setFileHistory(l.toArray(new String[0]));
        updateFileHistoryMenu();
    }

    private void updateFileHistoryMenu() {
        String[] history = Options.INSTANCE.getFileHistory();
        JMenu fileMenu = FileMenu;
        JMenu prevMenu = null;
        for (Component c : fileMenu.getMenuComponents()) {
            if (c instanceof JMenu && ((JMenu) c).getText().equals("Recent files")) {
                prevMenu = (JMenu) c;
            }
        }
        if (prevMenu != null && history.length == 0) {
            fileMenu.remove(prevMenu);
            return;
        }
        if (prevMenu == null && history.length > 0) {
            prevMenu = new JMenu("Recent files");
            fileMenu.add(prevMenu, fileMenu.getMenuComponentCount() - 1);
        }
        if (prevMenu != null && history.length > 0) {
            fileHistoryMenu = prevMenu;
            prevMenu.removeAll();
            for (final String f : history) {
                final File file = new File(f);
                String name = truncateFileName(file, Options.INSTANCE.getFilenameTruncationLength());
                JMenuItem item = new JMenuItem(name);
                item.addActionListener(e -> {
                    if (MainGUI.INSTANCE.promptUnsavedContinue()) {
                        if (file.exists()) {
                            if (openPatch(file)) {
                                currentFile = file;
                                addCurrentFileToFrontOfPreviousFiles();
                            } else {
                                JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                                        "Could not open the specified file.",
                                        "Error opening file",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                                    "The specified file does not exist!",
                                    "Error opening file",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                prevMenu.add(item);
            }
        }
    }

    private static String truncateFileName(File file, int maxlength) {
        return truncateFileName(Utilities.hideUserName(file.getAbsolutePath()), maxlength);
    }

    private static String truncateFileName(String filename, int maxLength) {
        if (filename.length() <= maxLength) {
            return filename;
        }
        String splitc = File.separator;
        String[] split = filename.split(splitc.replace("\\", "\\\\"));
        int c = 0;
        int l = 0;

        // Prepend the very first path element at all times.  Some very
        // paranoid checking here, which I can't imagine will ever be needed,
        // but whatever.
        if (split.length > 0) {
            l += split[0].length() + 1;
            if (split.length > 1 && OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
                l += split[1].length() + 1;
            }
        }

        // Figure out how many path elements, from the end, we can add.
        for (int i = split.length - 1; i >= 0; i--) {
            l += split[i].length() + 1;
            if (l > maxLength) {
                break;
            }
            c++;
        }

        // Now construct the string to display
        StringBuilder sb = new StringBuilder();
        if (split.length > 0) {
            sb.append(split[0]);
            sb.append(splitc);
            if (split.length > 1 && OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
                sb.append(split[1]);
                sb.append(splitc);
            }
        }
        sb.append("...");
        for (int i = split.length - c; i < split.length; i++) {
            sb.append(splitc).append(split[i]);
        }
        return sb.toString();
    }

    /**
     * When focusing the app, default to focusing on the main tree. That will
     * ensure that keyboard shortcuts will work properly once focus is returned
     * to the window.
     */
    @Override
    public void requestFocus() {
        super.requestFocus();
        jTree1.requestFocus();
    }

    /**
     * Forces focus on the app, bringing the main window to the front if
     * necessary. This abuses .setAlwaysOnTop() a bit because in some
     * circumstances (certain Linux window manager configurations, perhaps),
     * .toFront() doesn't actually do anything useful. This is primarily just
     * used after opening an Edit dialog, since that isn't actually modal, and
     * our main app window can get buried behind other windows otherwise.
     */
    public void forceFocus() {
        this.setAlwaysOnTop(true);
        this.requestFocus();
        this.toFront();
        this.setAlwaysOnTop(false);
    }

    /**
     * Return our TimedLabel, in case anything needs to add a notification to
     * it.
     *
     * @return our TimedLabel object
     */
    public TimedLabel getTimedLabel() {
        return ((TimedLabel) timedLabel);
    }

    private TimedLabel.DynamicString createNewDynamicString(boolean save) {
        return new TimedLabel.DynamicString() {
            private final long create = System.currentTimeMillis();

            @Override
            public String toString() {
                if (!((CheckBoxTree) jTree1).isChanged()) {
                    return "No changes since " + (save ? "last save" : "opening file");
                }

                long cur = System.currentTimeMillis();
                long secs = (cur - create) / (1000);
                long mins = secs / 60;
                long hours = mins / 60;
                long days = hours / 24;
                String report;
                if (days > 1) {
                    report = days + " days";
                } else if (hours > 1) {
                    report = hours + " hours";
                } else if (mins > 1) {
                    report = mins + " minutes";
                } else {
                    report = secs + " seconds";
                }
                return String.format("Unsaved changes in %s - %s %s ago",
                        currentFile == null ? "new file" : Utilities.hideUserName(currentFile.getName()),
                        (save ? "saved" : "opened"),
                        report);
            }
        };
    }

    public void setTreeEditable(boolean b) {
        jTree1.setEnabled(b);
        openMenuButton.setEnabled(b);
        importModFolderMenuButton.setEnabled(b);
        importModMenuButton.setEnabled(b);
        importModZipMenuButton.setEnabled(b);
        profileMenu.setEnabled(b);
        if (fileHistoryMenu != null) {
            fileHistoryMenu.setEnabled(b);
        }
    }

    public CheckBoxTree getTree() {
        return (CheckBoxTree) jTree1;
    }

    /**
     * Register the status of our Object Explorer Window
     *
     * @param status True if OE is open, False otherwise
     */
    public void registerObjectExplorerWindowStatus(boolean status) {
        this.objectExplorerWindowOpen = status;
        this.checkThemeSwitchAllowed();
    }

    /**
     * Register the status of our edit/insert window.
     *
     * @param status True if the window is open, False otherwise.
     */
    public void registerEditWindowStatus(boolean status) {
        this.editWindowOpen = status;
        this.checkThemeSwitchAllowed();
    }

    /**
     * Check to see if we are allowed to switch themes.
     *
     * @return Whether theme switching is allowed
     */
    public boolean isThemeSwitchAllowed() {
        return this.themeSwitchAllowed;
    }

    /**
     * Gets a tooltip which can be used to tell the user why theme switching is
     * not allowed. This is just here so that the same tooltip can be used on
     * both the main GUI window and the settings menu.
     *
     * @return A tooltip to give to the user
     */
    public String getThemeSwitchDeniedTooltip() {
        return "Theme cannot be switched while the insert/edit window or Object Explorer is open.";
    }

    /**
     * Check to see whether theme switching is allowed, and enable/disable the
     * main dropdown if needed. This is all needed thanks to a weird bug
     * relating to syntax highlighting in EditPanel/myStylizedDocument. If the
     * user switches themes with one of those active, then clicks into the text
     * area, we get a crash-to-desktop. Rather than figure out a way to fix the
     * actual problem, we're preventing it from popping up in the first place.
     */
    public void checkThemeSwitchAllowed() {
        boolean previous = this.themeSwitchAllowed;
        this.themeSwitchAllowed = !this.objectExplorerWindowOpen && !this.editWindowOpen;
        if (previous != this.themeSwitchAllowed) {
            if (this.themeSwitchAllowed) {
                this.themeComboBox.setEnabled(true);
                this.themeComboBox.setToolTipText(null);
            } else {
                this.themeComboBox.setEnabled(false);
                this.themeComboBox.setToolTipText(
                        this.getThemeSwitchDeniedTooltip());
            }
        }
    }

    public void cursorWait() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void cursorNormal() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void updateTitle() {
        this.updateTitle(null);
    }

    public void updateTitle(String filename) {
        String titleFilename = "";
        if (filename != null) {
            titleFilename = " | " + filename;
        }
        setTitle(Meta.NAME + (Utilities.isCreatorMode() ? " (creator mode)" : "")
                + " | " + Meta.VERSION + titleFilename + titlePostfix);
    }

    /**
     * Gets our main DataManagerManager object
     *
     * @return The main DataManagerManager
     */
    public DataManagerManager getDMM() {
        return this.dmm;
    }

    /**
     * Returns the current DataManager that we're working with (will match the
     * patch type of the current patch).
     *
     * @return The current DataManager
     */
    public DataManager getCurrentDataManager() {
        return this.dmm.getCurrentDataManager();
    }

    /**
     * Returns our detected new version of OpenBLCMM, if any.
     *
     * @return The new available version
     */
    public String getNewVersion() {
        return this.newVersion;
    }

    /**
     * Launches the Object Explorer window in a non-immediate fashion (the
     * main app will remain usable while OE is on its way up).
     */
    public void launchObjectExplorerWindow() {
        this.launchObjectExplorerWindow(false);
    }

    /**
     * Launches the Object Explorer window, either immediately (so the main
     * GUI is blocked until OE is up) or non-immediate (so the main app will
     * remain usable while OE's on its way up).
     *
     * @param immediate Whether or not to launch immediately.
     */
    public void launchObjectExplorerWindow(boolean immediate) {
        if (ObjectExplorer.INSTANCE == null) {
            if (immediate) {
                new ObjectExplorer(this.dmm).setVisible(true);
            } else {
                EventQueue.invokeLater(() -> {
                    new ObjectExplorer(this.dmm).setVisible(true);
                });
            }
        } else {
            ObjectExplorer.INSTANCE.toFront();
        }

    }

}