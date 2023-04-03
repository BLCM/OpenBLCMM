/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm.gui.components;

import blcmm.Meta;
import blcmm.model.PatchIO;
import blcmm.model.PatchType;
import blcmm.utilities.AutoBackupper;
import blcmm.utilities.GameDetection;
import blcmm.utilities.IconManager;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class BLCMM_FileChooser extends JFileChooser {

    private final boolean appendBlcmExtension;

    public BLCMM_FileChooser(File path) {
        this(path == null ? null : path.getAbsolutePath(), "", false, false);
    }

    public BLCMM_FileChooser(File path, String currentFileName, boolean appendBlcmExtension, boolean saveAs) {
        this(path == null ? null : path.getAbsolutePath(), currentFileName, appendBlcmExtension, saveAs);
    }

    public BLCMM_FileChooser(String path, String currentFileName, boolean appendBlcmExtension, boolean saveAs) {
        super(path == null ? Utilities.getDefaultOpenLocation().toString() : path);
        addDirectoryShortcutsToFileChooser(BLCMM_FileChooser.this);
        this.appendBlcmExtension = appendBlcmExtension;
        super.setPreferredSize(new Dimension(750, 400));
        super.setSelectedFile(new File(currentFileName));
        if (saveAs) {
            super.addChoosableFileFilter(new FilterToolFileFilter());
            super.addChoosableFileFilter(new StructurelessFileFilter());
        }
    }

    public PatchIO.SaveFormat getFormat() {
        if (this.getFileFilter() instanceof StructurelessFileFilter) {
            return PatchIO.SaveFormat.STRUCTURELESS;
        } else if (this.getFileFilter() instanceof FilterToolFileFilter) {
            return PatchIO.SaveFormat.FT;
        }
        return PatchIO.SaveFormat.BLCMM;
    }

    @Override
    public void approveSelection() {
        if (getFormat() == PatchIO.SaveFormat.STRUCTURELESS) {
            if (!Options.INSTANCE.getHasSeenExportWarning()) {
                Options.INSTANCE.setHasSeenExportWarning(true);
                JOptionPane.showMessageDialog(this, "This will save ONLY the checked codes and hotfixes.\n"
                        + "All structural information will be deleted.\n"
                        + "All deselected codes & hotfixes will be deleted."
                );
            }
        }
        if (getDialogType() == SAVE_DIALOG) {
            File f = getSelectedFile();

            // Append .blcm extension if we've been told to, if the file doesn't
            // already exist, and if it doesn't already have an extension.
            if (appendBlcmExtension
                    && !f.exists()
                    && !f.getName().contains(".")) {
                f = new File(f.getAbsolutePath() + ".blcm");
                setSelectedFile(f);
            }

            // Check to see if the file exists and prompt to overwrite.
            if (f.exists() && getFileSelectionMode() != JFileChooser.DIRECTORIES_ONLY) {
                int result = JOptionPane.showConfirmDialog(this,
                        "<html>The file \"<tt>" + f.getName() + "</tt>\" already exists.  Overwrite?",
                        "Existing File",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result) {
                    case JOptionPane.YES_OPTION:
                        super.approveSelection();
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        cancelSelection();
                        return;
                    default:
                        return;
                }
            }

        }
        super.approveSelection();
    }

    /**
     * Adds a collection of buttons into a JFileChooser's "accessories" area, to
     * allow the user to quickly navigate to a number of different locations on
     * the filesystem which aren't otherwise trivial to navigate to.
     *
     * @param fc The FileChooser to add buttons to.
     */
    private static void addDirectoryShortcutsToFileChooser(JFileChooser fc) {

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.gridx = cs.gridy = 0;
        cs.gridwidth = cs.gridheight = 1;
        cs.weightx = cs.weighty = 1;
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.anchor = GridBagConstraints.NORTH;
        String tempDir;

        // BL2 Binaries dir
        tempDir = GameDetection.getBinariesDir(PatchType.BL2);
        if (tempDir != null) {
            panel.add(directoryShortcutButton(fc,
                    "BL2 Binaries Dir",
                    tempDir,
                    new ImageIcon(PatchType.BL2.getIcon(16))), cs);
            cs.gridy++;
        }

        // TPS Binaries dir
        tempDir = GameDetection.getBinariesDir(PatchType.TPS);
        if (tempDir != null) {
            panel.add(directoryShortcutButton(fc,
                    "TPS Binaries Dir",
                    tempDir,
                    new ImageIcon(PatchType.TPS.getIcon(16))), cs);
            cs.gridy++;
        }

        // Last imported dir
        tempDir = Options.INSTANCE.getLastImport();
        if (tempDir != null && !tempDir.equals("")) {
            panel.add(directoryShortcutButton(fc,
                    "Last Imported Dir",
                    new File(tempDir).getParent(),
                    new ImageIcon(IconManager.getIcon("/resources/folder.png", 16))), cs);
            cs.gridy++;
        }

        // See if we should also offer a "current" directory.  Only do that
        // if it differs from our install dir (which, in at least some
        // cases, it will).
        File installFile = Utilities.getMainInstallDir();
        String curDir = System.getProperty("user.dir");
        File curFile = new File(curDir);
        if (installFile == null || !installFile.equals(curFile)) {
            panel.add(directoryShortcutButton(fc,
                    "Working Dir",
                    curDir,
                    new ImageIcon(IconManager.getIcon("/resources/folder.png", 16))), cs);
            cs.gridy++;
        }

        // The BLCMM install dir itself, if it's found.
        if (installFile != null) {
            panel.add(directoryShortcutButton(fc,
                    Meta.NAME + " Install Dir",
                    installFile.toString(),
                    new ImageIcon(IconManager.getBLCMMIcon(16))), cs);
            cs.gridy++;
        }

        // Backup dir
        tempDir = AutoBackupper.getDestination();
        if (new File(tempDir).exists()) {
            panel.add(directoryShortcutButton(fc,
                    "Backup Dir",
                    tempDir,
                    new ImageIcon(IconManager.getBLCMMIcon(16))), cs);
            cs.gridy++;
        }
        // 2023-02-11 -- I've taken out Plugin functionality, but leaving this in place anyway,
        // in case there's someone out there with plugin output they still want to get to.
        tempDir = System.getProperty("user.dir") + "/plugin_output";
        if (new File(tempDir).exists()) {
            panel.add(directoryShortcutButton(fc,
                    "Plugin Output",
                    tempDir,
                    new ImageIcon(IconManager.getBLCMMIcon(16))), cs);
            cs.gridy++;
        }

        cs.weighty = 100;
        panel.add(new JPanel(), cs);
        // Actually add the panel as an accessory.
        fc.setAccessory(panel);
    }

    /**
     * Returns a JButton used in our FileChooser dialogs which allow the user to
     * quickly jump to some convenient locations in the filesystem.
     *
     * @param fc The FileChooser the button will live inside
     * @param label Label for the button, visible to the user
     * @param path The path to set the chooser to, when the button is clicked.
     * This will also be the tooltip text for the button.
     * @param icon
     * @return The JButton
     */
    private static JButton directoryShortcutButton(JFileChooser fc, String label, String path, Icon icon) {
        JButton button = new JButton(label);
        button.addActionListener(e -> fc.setCurrentDirectory(new File(path)));
        button.setToolTipText(path);
        if (icon != null) {
            button.setIcon(icon);
            button.setHorizontalAlignment(SwingConstants.LEFT);
        }
        return button;
    }

    public final static class FilterToolFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            return "FilterTool Format (All Files)";
        }
    }

    public final static class StructurelessFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return true;
        }

        @Override
        public String getDescription() {
            return "Structureless File - saves only checked codes and hotfixes (All Files)";
        }
    }
}
