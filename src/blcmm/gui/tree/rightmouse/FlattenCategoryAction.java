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

import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.model.properties.PropertyChecker;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * An action to *completely* flatten the specified Category, replacing the
 * category itself with a big ol' list of comments + commands.
 *
 * @author apocalyptech
 */
public class FlattenCategoryAction extends RightMouseButtonAction {

    public FlattenCategoryAction(CheckBoxTree tree) {
        super(tree, "Totally flatten category", new Requirements(true, true, false));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();

        // Make sure we have a path
        if (paths == null) {
            return false;
        }

        // Also I only want to support this with a single selection
        if (paths.length != 1) {
            return false;
        }

        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        // Only allow operating on categories
        if (!(node.getUserObject() instanceof Category)) {
            return false;
        }
        Category cat = (Category) node.getUserObject();

        // Don't allow doing this on MUT categories
        if (cat.isMutuallyExclusive()) {
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

        // Also don't allow if any of our *children* are mutually exclusive, or
        // locked.  We're going to cheat a bit here and use our transient data,
        // rather than walk the tree.  Since we know that we aren't MUT/locked
        // ourselves, if we've received a propagated MUT/Lock property, we'll be
        // off-limits.
        for (PropertyChecker checker : cat.getTransientData().getProperties()) {
            if (checker instanceof GlobalListOfProperties.MUTChecker
                    || checker instanceof GlobalListOfProperties.LockChecker) {
                return false;
            }
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
        Category toRemoveCategory = (Category)toRemove.getUserObject();

        // Show a confirmation dialog
        int commandCount = toRemoveCategory.getNumberOfCommandsDescendants();
        String plural = "s";
        if (commandCount == 1) {
            plural = "";
        }
        AdHocDialog.Button response = AdHocDialog.run(MainGUI.INSTANCE,
                this.tree.getFontInfo(),
                AdHocDialog.IconType.QUESTION,
                "Really flatten category?",
                "<html>This action will completely 'flatten' the specified category, replacing the"
                + " category with a list of every comment and command found within, and removing"
                + " all nested categories.  This will result in"
                + " <b>" + commandCount + " command" + plural + "</b>"
                + " being shuffled around.<br/><br/>"
                + "Proceed?",
                AdHocDialog.ButtonSet.YES_NO_CANCEL,
                new Dimension(530, 180));
        if (response != AdHocDialog.Button.YES) {
            return;
        }

        // Walk the tree and gather our gigantic list of children.  Note that
        // the count of children here may be larger than the transient command
        // descendents we reported on above, since comments may be in here too.
        ArrayList<TreeNode> children = new ArrayList<> ();
        ArrayList<TreeNode> categories = new ArrayList<> ();
        getChildrenRecursive(toRemove, children, categories);

        // Now do some work.
        tree.removeNodesFromTheirParents(children.toArray(new DefaultMutableTreeNode[0]));
        tree.removeNodesFromTheirParents(categories.toArray(new DefaultMutableTreeNode[0]));
        tree.getPatch().removeElementFromParentCategory(toRemoveCategory);
        for (TreeNode child : children) {
            ModelElement el = (ModelElement)((DefaultMutableTreeNode)child).getUserObject();
            tree.getPatch().insertElementInto(el, parentCategory, toRemoveIndex++);
        }

        // Finish and clean up
        refreshNode(parentNode);
        tree.setChanged(true);

    }

    /**
     * Given a starting point in the tree, recursively walk the tree, adding
     * to a couple of lists.  `foundNodes` will contain any commands/comments
     * found in the tree, and `foundCategories` will contain any categories
     * found in the tree.  The starting point itself will be added to the
     * `foundCategories` list.
     *
     * @param start The starting point in the tree
     * @param foundNodes A list of non-Category nodes to add to
     * @param foundCategories A list of Category nodes to add to
     */
    private void getChildrenRecursive(TreeNode start,
            List<TreeNode> foundNodes,
            List<TreeNode> foundCategories) {
        foundCategories.add(start);
        Enumeration children = start.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
            if (child.getUserObject() instanceof Category) {
                getChildrenRecursive(child, foundNodes, foundCategories);
            } else {
                foundNodes.add(child);
            }
        }
    }

}
