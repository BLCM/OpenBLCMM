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
 */
package blcmm.gui.tree.rightmouse;

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class CollapseCategoryCompletelyAction extends RightMouseButtonAction {

    public CollapseCategoryCompletelyAction(CheckBoxTree tree) {
        super(tree, "Fully collapse category", Requirements.NO_REQUIREMENTS);
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
        TreePath[] paths = tree.getSelectionPaths();
        for (TreePath path : paths) {
            collapse(path);
        }
    }

    private void collapse(TreePath path) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        for (int i = 0; i < node.getChildCount(); i++) {
            collapse(path.pathByAddingChild(node.getChildAt(i)));
        }
        tree.collapsePath(path);
    }

}
