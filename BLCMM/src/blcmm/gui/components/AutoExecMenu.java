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
 */
package blcmm.gui.components;

import blcmm.model.PatchType;
import blcmm.utilities.AutoExecFile;
import blcmm.utilities.IconManager;
import java.awt.Image;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

/**
 *
 * @author LightChaosman
 */
public final class AutoExecMenu extends JMenu {

    private final JCheckBoxMenuItem fastMode = new JCheckBoxMenuItem("Fast mode");
    private final JCheckBoxMenuItem forceOffline = new JCheckBoxMenuItem("Force offline");
    private final JCheckBoxMenuItem updateAfterSave = new JCheckBoxMenuItem("Update after each save");
    private final JCheckBoxMenuItem saveBoth = new JCheckBoxMenuItem("Save two patch versions");
    private final JCheckBoxMenuItem saveDistinct = new JCheckBoxMenuItem("Update both modes seperatly");
    private final JMenuItem about = new JMenuItem("Detailed explanation", new ImageIcon(new ImageIcon(getClass().getClassLoader().getResource("resources/Qmark.png")).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));

    private PatchType curType = null;
    private boolean updating = false;

    public AutoExecMenu() {
        super("AutoExec");
        add(fastMode);
        add(forceOffline);
        add(updateAfterSave);
        add(saveBoth);
        add(saveDistinct);
        add(new JSeparator());
        add(about);
        initListeners();
    }

    public void setPatchType(PatchType type) {
        curType = type;
        setEnabled(type != null);
        updateMenu();
    }

    private void initListeners() {
        fastMode.addItemListener(e -> {
            if (updating) {
                return;
            }
            try {
                AutoExecFile file = AutoExecFile.read(curType);
                if (file.isFastMode() != fastMode.isSelected()) {
                    file.setFastMode(fastMode.isSelected());
                    file.save();
                }
            } catch (IOException ex) {

            }
        });
        forceOffline.addItemListener(e -> {
            if (updating) {
                return;
            }
            try {
                AutoExecFile file = AutoExecFile.read(curType);
                if (file.isForceOffline() != forceOffline.isSelected()) {
                    file.setForceOffline(forceOffline.isSelected());
                    file.save();
                }
            } catch (IOException ex) {

            }
        });
        updateAfterSave.addItemListener(e -> {
            if (updating) {
                return;
            }
            try {
                AutoExecFile file = AutoExecFile.read(curType);
                if (file.shouldAdaptAfterEachSave() != updateAfterSave.isSelected()) {
                    file.setShouldAdaptAfterEachSave(updateAfterSave.isSelected());
                    file.save();
                    saveDistinct.setEnabled(!saveBoth.isSelected() && updateAfterSave.isSelected());
                    saveBoth.setEnabled(updateAfterSave.isSelected());
                }
            } catch (IOException ex) {

            }
        });
        saveBoth.addItemListener(e -> {
            if (updating) {
                return;
            }
            try {
                AutoExecFile file = AutoExecFile.read(curType);
                if (file.shouldSaveBothVersionsOnEachSave() != saveBoth.isSelected()) {
                    file.setShouldSaveBothVersionsOnEachSave(saveBoth.isSelected());
                    file.save();
                    saveDistinct.setEnabled(!saveBoth.isSelected());
                }
            } catch (IOException ex) {

            }
        });
        saveDistinct.addItemListener(e -> {
            if (updating) {
                return;
            }
            try {
                AutoExecFile file = AutoExecFile.read(curType);
                if (file.shouldSaveOnlineAndOfflineSeperately() != saveDistinct.isSelected()) {
                    file.setShouldSaveOnlineAndOfflineSeperately(saveDistinct.isSelected());
                    file.save();
                }
            } catch (IOException ex) {

            }
        });
        about.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, ""
                    + "<html><center><table><tr><td></td>Below is some information on the settings available for AutoExec.<br/>"
                    + "To understand what the things below mean, first you need to know something about hotfixes.<br/>"
                    + "Hotfixes are a part of mods that require you to be connected to SHiFT. If you're not connected to SHiFT, they won't work.<br/>"
                    + "If you can't connect to SHiFT, you need to use 'offline hotfixes', which will work in that case.<br/>"
                    + "If you can connect to SHiFT, and use offline hotfixes, they <b>sometimes</b> fail.</td></tr>"
                    + "<tr></tr>"
                    + "<tr><td>Fast mode</td><td>The game boots faster, you reach the main menu quicker.</td></tr>"
                    + "<tr><td>Update after each save</td><td>Every time you save your patch file <b>in your binaries folder</b>,<br/>AutoExec is updated to run that file when the game launches.</td></tr>"
                    + "<tr><td>Force offline</td><td>Disconnects your game from SHiFT forcefully.<br/> Online hotfixes will never work, offline ones will <b>always</b> work.</td></tr>"
                    + "<tr><td>Save two patch versions</td><td>Each time you save <b>in your binaries folder</b>, two versions of your file are saved.<br/>An online <i>and</i> an offline version.<br/>If you are connected to SHiFT, the online version will be executed, otherwise the offline version.</td></tr>"
                    + "<tr><td>Update both modes seperately</td><td>Each time you save <b>in your binaries folder</b>, the online file of AutoExec gets updated if this mod is online, otherwise the offline file gets updated<br>This is not the advised way to manage your AutoExec settings</td></tr>"
                    + "</table>",
                    "AutoExec settings explanation", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(IconManager.getBLCMMIcon(64)));
        });
    }

    private void updateMenu() {
        if (curType == null) {
            return;
        }
        updating = true;
        try {
            AutoExecFile AEF = AutoExecFile.read(curType);
            fastMode.setSelected(AEF.isFastMode());
            forceOffline.setSelected(AEF.isForceOffline());
            saveBoth.setSelected(AEF.shouldSaveBothVersionsOnEachSave());
            saveDistinct.setSelected(AEF.shouldSaveOnlineAndOfflineSeperately());
            updateAfterSave.setSelected(AEF.shouldAdaptAfterEachSave());
            saveDistinct.setEnabled(!AEF.shouldSaveBothVersionsOnEachSave() && AEF.shouldAdaptAfterEachSave());
            saveBoth.setEnabled(AEF.shouldAdaptAfterEachSave());
        } catch (IOException ex) {

        }
        updating = false;
    }
}
