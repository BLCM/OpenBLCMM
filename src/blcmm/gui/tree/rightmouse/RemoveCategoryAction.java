/*
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
package blcmm.gui.tree.rightmouse;

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import java.util.Collections;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * An action to remove a single category from the tree, effectively replacing
 * it with its prior contents.
 *
 * @author apocalyptech
 */
public class RemoveCategoryAction extends RightMouseButtonAction {

    public RemoveCategoryAction(CheckBoxTree tree) {
        super(tree, "Flatten/Remove single category", new Requirements(false, true, false));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();

        // Make sure we have a path
        if (paths == null) {
            return false;
        }

        // Also I sort of only want to support this with a single selection
        if (paths.length != 1) {
            return false;
        }

        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        // Only allow operating on categories
        if (!(node.getUserObject() instanceof Category)) {
            return false;
        }

        // Don't allow doing this on MUT categories, though
        if (((Category) node.getUserObject()).isMutuallyExclusive()) {
            return false;
        }

        // Don't allow on the top-level element
        TreePath parentPath = path.getParentPath();
        if (parentPath == null) {
            return false;
        }

        // Also don't allow if *we* are the top level inside a MUT
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
        if (!(parent.getUserObject() instanceof Category)) {
            return false;
        }
        if (((Category) parent.getUserObject()).isMutuallyExclusive()) {
            return false;
        }

        // Otherwise, we're good!
        return true;
    }

    @Override
    public void action() {
        // Some of the checks we're doing here would've already been checked
        // by couldBeEnabled(), but it can't hurt to doublecheck.
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length != 1) {
            return;
        }

        // Gather some info
        TreePath parentPath = paths[0].getParentPath();
        if (parentPath == null) {
            return;
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        if (!(parentNode.getUserObject() instanceof Category)) {
            return;
        }
        Category parentCategory = (Category)parentNode.getUserObject();
        DefaultMutableTreeNode toRemove = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        int toRemoveIndex = parentNode.getIndex(toRemove);

        // Now go ahead and do the actual work
        List<TreeNode> children = Collections.list(toRemove.children());
        tree.removeNodesFromTheirParents(children.toArray(new DefaultMutableTreeNode[0]));
        tree.removeNodesFromTheirParents(new DefaultMutableTreeNode[] {toRemove});
        tree.getPatch().removeElementFromParentCategory((ModelElement)toRemove.getUserObject());
        for (TreeNode child : children) {
            ModelElement el = (ModelElement)((DefaultMutableTreeNode)child).getUserObject();
            tree.getPatch().insertElementInto(el, parentCategory, toRemoveIndex++);
        }

        // Finish and clean up
        refreshNode(parentNode);
        tree.setChanged(true);

    }

}
