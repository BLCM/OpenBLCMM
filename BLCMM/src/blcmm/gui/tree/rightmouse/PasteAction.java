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

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.SetCommand;
import blcmm.model.properties.GlobalListOfProperties;
import java.util.Arrays;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class PasteAction extends RightMouseButtonAction {

    private int insertIndex;
    private Category newParent;
    private DefaultMutableTreeNode parentNode;

    public PasteAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Paste", hotkey, ctrl, new Requirements(true, false));
    }

    @Override
    public boolean couldBeEnabled() {

        // Require a single valid path
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        if (paths.length != 1) {
            return false;
        }

        // Require that path to be a Category
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (node.getUserObject() instanceof Category) {
            newParent = (Category) node.getUserObject();
            insertIndex = node.getChildCount();
            parentNode = node;
        } else {
            ModelElementContainer parent = ((ModelElement) node.getUserObject()).getParent();
            if (!(parent instanceof Category)) {
                newParent = parent.getParent();
            } else {
                newParent = (Category) parent;
            }
            parentNode = (DefaultMutableTreeNode) node.getParent();
            insertIndex = parentNode.getIndex(node) + 1;
        }

        TreePath[] copied = AbstractCopyAction.getPointerToLastCopiedPaths();
        if (copied == null) {
            return false;
        }
        for (TreePath copiedPath : copied) {
            ModelElement el = (ModelElement) ((DefaultMutableTreeNode) copiedPath.getLastPathComponent()).getUserObject();
            if (el instanceof SetCommand && newParent.isMutuallyExclusive()) {
                return false;
            }
        }
        if (AbstractCopyAction.isCut()) {
            for (TreePath copiedPath : copied) {
                ModelElement el = (ModelElement) ((DefaultMutableTreeNode) copiedPath.getLastPathComponent()).getUserObject();
                do {
                    if (el == newParent) {
                        return false;
                    }
                } while ((el = el.getParent()) != null);
            }
        }
        if ((newParent.isMutuallyExclusive() || newParent.hasMUTAncestor())
                && AbstractCopyAction.doCopiedElementContainsMUTElements()) {
            return false;
        }
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = AbstractCopyAction.getPointerToLastCopiedPaths();
        ModelElement[] originals = new ModelElement[paths.length];
        ModelElement[] copies = new ModelElement[paths.length];
        for (int i = 0; i < originals.length; i++) {
            originals[i] = (ModelElement) ((DefaultMutableTreeNode) paths[i].getLastPathComponent()).getUserObject();
            copies[i] = originals[i].copy();
        }
        Category mutparent = newParent;
        boolean needToDeselect = false;
        ModelElement current = null;
        while (mutparent != null && !mutparent.isMutuallyExclusive()) {
            current = mutparent;
            mutparent = mutparent.getParent();
        }
        if (mutparent != null) {
            for (ModelElement el : mutparent.getElements()) {
                if (el.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class) > 0) {
                    needToDeselect = (el != current);
                }
            }
        }
        if (AbstractCopyAction.isCut()) {//We delete the old node first, since adding new nodes wraps model elements in new treenodes.
            //This invalidates the treepaths stored in AbstractCopy action, causing nullpointers and model invalidations, since the deletion fails on 1 end.
            TreePath[] path2 = AbstractCopyAction.getPointerToLastCopiedPaths();
            for (TreePath path : path2) {
                DefaultMutableTreeNode copiedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                DefaultMutableTreeNode parentOfCopiedNode = (DefaultMutableTreeNode) path.getParentPath().getLastPathComponent();
                if (parentOfCopiedNode == parentNode
                        && parentOfCopiedNode.getIndex(copiedNode) < insertIndex) {
                    insertIndex--;//Adjust the insetIndex if cutting to the same parent category
                }
            }
            tree.removeNodesFromTheirParents(path2);
            for (TreePath path : paths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                ModelElement c = (ModelElement) node.getUserObject();
                tree.getPatch().removeElementFromParentCategory(c);
            }
            AbstractCopyAction.removePointers();
        }
        addNewElements(copies, newParent, insertIndex, parentNode, true);
        List<ModelElement> copiesList = Arrays.asList(copies);
        if (needToDeselect) {
            DefaultMutableTreeNode node = parentNode;
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                if (copiesList.contains((ModelElement) ((DefaultMutableTreeNode) parentNode.getChildAt(i)).getUserObject())) {
                    node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                }
            }
            tree.checkNode(new TreePath(node.getPath()), false);
        }
        tree.setChanged(true);
    }
}
