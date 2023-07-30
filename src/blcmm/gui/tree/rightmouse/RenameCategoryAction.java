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

import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import java.awt.Dimension;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class RenameCategoryAction extends RightMouseButtonAction {

    private final FontInfo fontInfo;

    public RenameCategoryAction(CheckBoxTree tree, int hotkey, boolean ctrl, FontInfo fontInfo) {
        super(tree, "Rename category", hotkey, ctrl, new Requirements(false, true, false));
        this.fontInfo = fontInfo;

    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        return paths.length == 1 && ((DefaultMutableTreeNode) paths[0].getLastPathComponent()).getUserObject() instanceof Category;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        String name = AdHocDialog.askForString(MainGUI.INSTANCE,
                this.fontInfo,
                AdHocDialog.IconType.QUESTION,
                "New Category Name",
                "New Category Name",
                new Dimension(325, 100),
                node.toString());
        if (name == null) {
            return;
        }
        if (!isInputCategoryNameValid(name)) {
            return;
        }
        Category cat = (Category) node.getUserObject();
        cat.setName(name);
        ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
        tree.setChanged(true);
    }

}
