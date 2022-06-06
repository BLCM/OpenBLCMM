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

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class LockAction extends CheckMarkRightMouseButtonAction {

    public LockAction(CheckBoxTree tree) {
        // We set false for requiresUnlocked because we're allowed to change
        // this when WE are locked, just not when ancestors are locked, so
        // we do so in a custom way in couldBeEnabled(), below.
        super(tree, "Lock category", false);
    }

    @Override
    public void update() {
        TreePath[] paths = tree.getSelectionPaths();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        Category cat = (Category) node.getUserObject();
        ((JCheckBoxMenuItem) getButton()).setSelected(cat.isLocked());
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length != 1) {
            return false;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
        if (!(node.getUserObject() instanceof Category)) {
            return false;
        }

        // It's okay to activate if WE are the locked element, but not if any
        // ancestor is locked.
        ModelElementContainer parent = ((ModelElement) node.getUserObject()).getParent();
        if (parent != null && parent.hasLockedAncestor()) {
            return false;
        }

        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Category cat = (Category) node.getUserObject();
        boolean newMode = ((JCheckBoxMenuItem) getButton()).isSelected();
        cat.setLocked(newMode);
        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
        tree.setChanged(true);
    }

}
