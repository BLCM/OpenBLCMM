/*
 * Copyright (C) 2023 Christopher J. Kucera
 * <cj@apocalyptech.com>
 * <https://apocalyptech.com/contact.php>
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
import blcmm.utilities.Options;
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

    /**
     * While processing categories to be flattened, we also want to keep track
     * of the *original* index in which the category used to be placed, so that
     * we can keep track of offsets for other categories which are yet to be
     * processed.  This basically just acts as a simple tuple for us to keep
     * track of both the node, and its original position.
     */
    private class FlattenedCategoryHelper {

        public DefaultMutableTreeNode node;
        public int initialPosition;

        public FlattenedCategoryHelper(DefaultMutableTreeNode node, int initialPosition) {
            this.node = node;
            this.initialPosition = initialPosition;
        }
    }

    public FlattenCategoryAction(CheckBoxTree tree) {
        super(tree, "Flatten category contents", new Requirements(true, true, false));
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

        // Also don't allow if *we* are the top level inside a MUT
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)parentPath.getLastPathComponent();
            if (!(parent.getUserObject() instanceof Category)) {
                return false;
            }
            if (((Category) parent.getUserObject()).isMutuallyExclusive()) {
                return false;
            }
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

        // Finally, also don't allow running this if there are no categories
        // to flatten.
        boolean foundCategories = false;
        for (ModelElement me : cat.getElements()) {
            if (me instanceof Category) {
                foundCategories = true;
                break;
            }
        }
        if (!foundCategories) {
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
        DefaultMutableTreeNode actionNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        Category actionCategory = (Category)actionNode.getUserObject();

        // Show a confirmation dialog, unless we've been told not to.
        if (Options.INSTANCE.getShowFlattenCategoryConfirm()) {
            int commandCount = actionCategory.getNumberOfCommandsDescendants();
            String plural = "s";
            if (commandCount == 1) {
                plural = "";
            }
            AdHocDialog.Button response = AdHocDialog.run(MainGUI.INSTANCE,
                    this.tree.getFontInfo(),
                    AdHocDialog.IconType.QUESTION,
                    "Really flatten category?",
                    "<html>This action will completely 'flatten' the specified category, removing"
                    + " all sub-categories and leaving behind a flat list of every comment and"
                    + " command found within.  This will result in"
                    + " <b>" + commandCount + " command" + plural + "</b>"
                    + " being shuffled around.<br/><br/>"
                    + "Proceed?",
                    AdHocDialog.ButtonSet.YES_NO_CANCEL,
                    new Dimension(530, 180));
            if (response != AdHocDialog.Button.YES) {
                return;
            }
        }

        // The current CompletePatch implementation presents a couple of
        // challenges for the most straightforward implementation of this,
        // relating to how it checks for and enforces various parent/child
        // relationships.  Rather than try to fight through those (and
        // potentially have to start modifying CompletePatch, something I'm
        // a bit hesitant to touch without real good reason), we're gonna do
        // this slightly stupidly, instead.  Find all direct Category
        // children and process them individually, leaving direct comments
        // and commands alone.

        ArrayList<FlattenedCategoryHelper> catsToFlatten = new ArrayList<> ();
        Enumeration children = actionNode.children();
        int index = 0;
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
            if (child.getUserObject() instanceof Category) {
                catsToFlatten.add(new FlattenedCategoryHelper(child, index));
            }
            index++;
        }
        int offset = 0;
        for (FlattenedCategoryHelper helper : catsToFlatten) {
            //Category cat = (Category)helper.node.getUserObject();
            //GlobalLogger.log("Processing category \"" + cat.getName() + "\" at initial pos " + helper.initialPosition + " with offset " + offset + " (" + (helper.initialPosition + offset) + ")");
            offset += this.flattenNode(helper, actionCategory, offset);
        }

        // Finish and clean up
        refreshNode(actionNode);
        tree.setChanged(true);
    }

    /**
     * Inner routine to recursively flatten a category, replacing it with its
     * previous contents.  Basically a supercharged version of
     * RemoveCategoryAction.
     *
     * There's no real *need* to pass newParentCategory -- we could just
     * derive it from newParentNode, after all -- but we've already casted it
     * prior to calling in here, so we may as well pass it.
     *
     * @param helper The FlattenedCategoryHelper describing this node and where it is
     * @param newParentCategory The new parent Category object itself.
     * @param offset The current offset between our original index and where we'll
     *               be now.
     * @return The extra offset added by flattening this category
     */
    private int flattenNode(FlattenedCategoryHelper helper,
            Category newParentCategory,
            int offset) {

        DefaultMutableTreeNode flattenNode = helper.node;
        Category flattenCategory = (Category) flattenNode.getUserObject();
        int toRemoveIndex = helper.initialPosition + offset;

        // Walk the tree and gather our gigantic list of children.  Note that
        // the count of children here may be larger than the transient command
        // descendents we reported on above, since comments may be in here too.
        ArrayList<TreeNode> children = new ArrayList<> ();
        ArrayList<TreeNode> categories = new ArrayList<> ();
        getChildrenRecursive(flattenNode, children, categories);

        // Now do some work.
        tree.removeNodesFromTheirParents(children.toArray(new DefaultMutableTreeNode[0]));
        tree.removeNodesFromTheirParents(categories.toArray(new DefaultMutableTreeNode[0]));
        tree.getPatch().removeElementFromParentCategory(flattenCategory);
        // Our initial offset is -1 because the category itself used to take up a space, so
        // if the category was *empty*, we're actually getting rid of a line, or if there
        // was only a single statement, we've got the *same* number of lines.
        int newOffset = -1;
        for (TreeNode child : children) {
            ModelElement el = (ModelElement)((DefaultMutableTreeNode)child).getUserObject();
            tree.getPatch().insertElementInto(el, newParentCategory, toRemoveIndex++);
            newOffset++;
        }

        return newOffset;

    }

    /**
     * Given a starting point in the tree, recursively walk the tree, adding
     * to a couple of lists.  `foundNodes` will contain any commands/comments
     * found in the tree, and `foundCategories` will contain any categories
     * found in the tree.
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
