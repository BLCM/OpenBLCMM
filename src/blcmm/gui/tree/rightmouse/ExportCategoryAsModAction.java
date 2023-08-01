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
package blcmm.gui.tree.rightmouse;

import blcmm.gui.MainGUI;
import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.CompletePatch;
import java.io.File;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class ExportCategoryAsModAction extends RightMouseButtonAction {

    public ExportCategoryAsModAction(CheckBoxTree tree) {
        super(tree, "Export category as mod", Requirements.NO_REQUIREMENTS);
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length != 1) {
            return false;
        }
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.getUserObject() instanceof Category;
    }

    @Override
    public void action() {
        tree.resetCheckingState();
        Category c = (Category) ((DefaultMutableTreeNode) tree.getSelectionPaths()[0].getLastPathComponent()).getUserObject();
        BLCMM_FileChooser fc = new BLCMM_FileChooser(this.tree.getFontInfo(), MainGUI.INSTANCE.getExportDialogPath(), c.getName(), true, true);
        int returnVal = fc.showSaveDialog(MainGUI.INSTANCE);
        if (returnVal == BLCMM_FileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            MainGUI.INSTANCE.setExportDialogPath(file.getAbsoluteFile().getParentFile());
            CompletePatch completePatch = new CompletePatch();
            completePatch.setRoot(c);
            completePatch.setType(tree.getPatch().getType());
            MainGUI.INSTANCE.savePatch(completePatch, file, true);
            MainGUI.INSTANCE.getTimedLabel().showTemporary(
                    "<html>Exported category to <tt>" + file.getName() + "</tt>");
        }
    }

}
