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
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import java.awt.Dimension;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Action to handle adding a new Category to a tree (as opposed to "introducing"
 * a Category on an already-existing entry.
 *
 * @author apocalyptech
 */
public class AddCategoryAction extends RightMouseButtonAction {

    public AddCategoryAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Create new empty category", hotkey, ctrl, new Requirements(false, true, false));
    }

    /**
     * Specifies whether this action can be taken. We must be selecting a single
     * category and have structural edits enabled.
     *
     * @return Whether or not to enable the action
     */
    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null
                || paths.length != 1) {
            return false;
        }
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return (node.getUserObject() instanceof Category);
    }

    /**
     * Actually perform the action. Will prompt user for the name of the new
     * category.
     */
    @Override
    public void action() {

        // Grab some objects we'll need
        TreePath[] paths = tree.getSelectionPaths();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        ModelElement el = (ModelElement) node.getUserObject();

        // This shouldn't ever happen on acconut of couldBeEnabled(), but
        // paranoia is a virtue.
        if (!(el instanceof Category)) {
            return;
        }

        // Ask user for the new category name and validate it.
        String name = AdHocDialog.askForString(MainGUI.INSTANCE,
                this.tree.getFontInfo(),
                AdHocDialog.IconType.QUESTION,
                "New Category Name",
                "New Category Name:",
                new Dimension(325, 100));
        if (name == null || !this.isInputCategoryNameValid(name)) {
            return;
        }

        // Create a new category, make sure it's in the patch properly.
        Category newCategory = new Category(name);
        tree.getPatch().insertElementInto(newCategory, (Category) el);

        // If we happen to have created a new "mods" category off of the root
        // category, disable its transient statuses.
        if (newCategory.getParent() != null
                && newCategory.getParent().getParent() == null
                && name.equals("mods")) {
            newCategory.getTransientData().disableStatuses();
        }

        // Cleanup
        refreshNode(node);
        tree.setChanged(true);

    }
}
