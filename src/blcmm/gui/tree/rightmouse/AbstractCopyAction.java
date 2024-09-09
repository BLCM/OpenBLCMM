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
import blcmm.model.ModelElement;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
abstract class AbstractCopyAction extends RightMouseButtonAction {

    private static TreePath[] pointerToLastCopiedPaths;
    private static boolean copiedElementContainsMUTElements;
    private static boolean oneTimeCopy = false;

    public static boolean isCut() {
        return oneTimeCopy;
    }

    public static void removePointers() {
        pointerToLastCopiedPaths = null;
    }

    public static TreePath[] getPointerToLastCopiedPaths() {
        return pointerToLastCopiedPaths;
    }

    public static boolean doCopiedElementContainsMUTElements() {
        return copiedElementContainsMUTElements;
    }

    private final boolean cut;

    AbstractCopyAction(CheckBoxTree tree, int hotkey, boolean ctrl, String name, boolean cut) {
        super(tree, name, hotkey, ctrl, new Requirements(false, cut, false));
        this.cut = cut;
    }

    @Override
    public boolean couldBeEnabled() {

        // Require a single valid path
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        if (paths.length == 0) {
            return false;
        }
        if (paths.length > 1) {
            TreePath parentPath = paths[0].getParentPath();
            for (int i = 1; i < paths.length; i++) {
                if (paths[i].getParentPath() != parentPath) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        pointerToLastCopiedPaths = paths;
        oneTimeCopy = cut;
        copiedElementContainsMUTElements = false;
        for (TreePath path : paths) {
            Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (userObject instanceof Category) {
                copiedElementContainsMUTElements = copiedElementContainsMUTElements || containsMUT((Category) userObject);
            }
        }
    }

    private boolean containsMUT(Category category) {
        if (category.isMutuallyExclusive()) {
            return true;
        }
        for (ModelElement el : category.getElements()) {
            if (el instanceof Category && containsMUT((Category) el)) {
                return true;
            }
        }
        return false;
    }

}
