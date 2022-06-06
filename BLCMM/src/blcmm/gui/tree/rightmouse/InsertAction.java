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
package blcmm.gui.tree.rightmouse;

import blcmm.gui.MainGUI;
import blcmm.gui.panels.EditPanel;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.utilities.Options;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class InsertAction extends RightMouseButtonAction {

    public InsertAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Insert", hotkey, ctrl, new Requirements(true, true, true, true));
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

        return Options.INSTANCE.isInDeveloperMode() && Options.INSTANCE.getStructuralEdits();

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
        EditPanel panel = new EditPanel(tree.getPatch(), parentCategory);
        panel.setPreferredSize(new Dimension(MainGUI.INSTANCE.getWidth() - 125, MainGUI.INSTANCE.getHeight() - 150));

        showCustomDialog(panel, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewElements(panel,
                        parentCategory,
                        insertIndex,
                        parentnode,
                        tree.isSelected(parentnode));
            }
        }, true);
    }

}
