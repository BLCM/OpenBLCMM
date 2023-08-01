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
import blcmm.gui.panels.EditPanel;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class InsertAction extends RightMouseButtonAction {

    public InsertAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Insert", hotkey, ctrl, new Requirements(true, true, true));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length != 1) {
            return false;
        }
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        // Disallow inserting commands directly into a MUT category
        if (node.getUserObject() instanceof Category
                && ((Category) node.getUserObject()).isMutuallyExclusive()) {
            return false;
        }

        return true;

    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        DefaultMutableTreeNode selectednode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        int insertIndex;
        Category parentCategory;
        DefaultMutableTreeNode parentnode;
        if (selectednode.getUserObject() instanceof Category) {
            parentnode = selectednode;
            parentCategory = (Category) parentnode.getUserObject();
            insertIndex = parentnode.getChildCount();
        } else {
            parentnode = (DefaultMutableTreeNode) selectednode.getParent();
            parentCategory = (Category) parentnode.getUserObject();
            insertIndex = parentnode.getIndex(selectednode) + 1;
        }
        EditPanel panel = new EditPanel(tree.getPatch(), parentCategory, this.tree.getFontInfo());
        // I'm actually *not* going to Utilities.scaleAndClampDialogSize() these
        // dimensions, since they're based on the main window size.  Presumably
        // users would already have that sized appropriately to their font
        // scaling.
        panel.setPreferredSize(new Dimension(MainGUI.INSTANCE.getWidth() - 125, MainGUI.INSTANCE.getHeight() - 150));

        showCustomDialog(panel, (ActionEvent e) -> {
                addNewElements(panel,
                        parentCategory,
                        insertIndex,
                        parentnode,
                        tree.isSelected(parentnode));
            }, true);
    }

}
