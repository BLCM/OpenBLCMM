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
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */
package blcmm.gui.tree.rightmouse;

import blcmm.gui.MainGUI;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class IntroduceCategoryAction extends RightMouseButtonAction {

    public IntroduceCategoryAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Wrap in new category", hotkey, ctrl, new Requirements(true, false, true, false));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        boolean allSameLevel = true;
        int level1 = paths[0].getPathCount();
        for (TreePath path : paths) {
            allSameLevel = allSameLevel && path.getPathCount() == level1;
        }
        return allSameLevel && level1 > 1;
    }

    @Override
    public void action() {
        String name = JOptionPane.showInputDialog(MainGUI.INSTANCE, "Insert category name");
        if (name == null) {
            return;
        }
        if (!isInputCategoryNameValid(name)) {
            return;
        }
        TreePath[] paths = tree.getSelectionPaths();
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) paths[0].getParentPath().getLastPathComponent();

        //remove old tree nodes
        tree.removeNodesFromTheirParents(paths);

        //change it in the model
        ArrayList<ModelElement> elements = new ArrayList<>();
        for (TreePath path : paths) {
            elements.add((ModelElement) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
        }
        Category newCategory = tree.getPatch().introduceCategoryAsParentOfElements(elements, name);

        // If we happen to have created a new "mods" category off of the root
        // category, disable its transient statuses.
        if (newCategory.getParent() != null
                && newCategory.getParent().getParent() == null
                && name.equals("mods")) {
            newCategory.getTransientData().disableStatuses();
        }

        // Cleanup.
        refreshNode(parentNode);
        tree.setChanged(true);
    }

}
