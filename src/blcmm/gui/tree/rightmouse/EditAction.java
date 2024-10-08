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

import blcmm.gui.MainGUI;
import blcmm.gui.panels.EditPanel;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.SetCommand;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class EditAction extends RightMouseButtonAction {

    public EditAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Edit", hotkey, ctrl, new Requirements(true, false, true));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        ModelElementContainer parent = ((ModelElement) node.getUserObject()).getParent();
        for (TreePath path2 : paths) {
            ModelElement el = (ModelElement) ((DefaultMutableTreeNode) path2.getLastPathComponent()).getUserObject();
            if (el.getParent() != parent) {
                return false;//must be of the same parent. This implies only like-hotfixes can be edited together
            }
            if (((DefaultMutableTreeNode) path2.getLastPathComponent()).getUserObject() instanceof Category) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void action() {
        boolean allowEdit = true;
        TreePath[] paths = tree.getSelectionPaths();
        TreePath firstPath = paths[0];
        DefaultMutableTreeNode firstSelectedNode = (DefaultMutableTreeNode) firstPath.getLastPathComponent();
        DefaultMutableTreeNode parentnode = (DefaultMutableTreeNode) firstSelectedNode.getParent();
        int index = parentnode.getIndex(firstSelectedNode);
        ModelElement firstSelectedCode = (ModelElement) firstSelectedNode.getUserObject();
        Category parentCategory = firstSelectedCode.getParent() instanceof Category ? (Category) firstSelectedCode.getParent() : (Category) firstSelectedCode.getParent().getParent();
        List<ModelElement> inputCodes = new ArrayList<>();
        for (TreePath path : paths) {
            ModelElement el = (ModelElement) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            inputCodes.add(el);
            if (el.hasLockedAncestor()) {
                allowEdit = false;
            }
        }
        EditPanel panel = new EditPanel(tree.getPatch(), parentCategory, inputCodes,
                allowEdit, "In Readonly Mode (Statement" + (paths.length == 1 ? " is" : "s are") + " Locked)",
                this.tree.getFontInfo());
        panel.setPreferredSize(new Dimension(MainGUI.INSTANCE.getWidth() - 60, MainGUI.INSTANCE.getHeight() - 130));
        showCustomDialog(panel, (ActionEvent e) -> {
                finishEditAction(paths, panel, parentCategory, parentnode, index);
            }, true, allowEdit);
    }

    private void finishEditAction(TreePath[] paths, EditPanel panel, Category parentCategory, DefaultMutableTreeNode parentnode, int index) {
        tree.removeNodesFromTheirParents(paths);
        Boolean sel = null;
        for (TreePath path : paths) {
            DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) path.getLastPathComponent();
            ModelElement el = (ModelElement) node2.getUserObject();
            if (el instanceof SetCommand) {
                if (sel == null) {
                    sel = ((SetCommand) el).isSelected();
                } else {
                    sel = sel && ((SetCommand) el).isSelected();
                }
            }
            tree.getPatch().removeElementFromParentCategory(el);
        }
        addNewElements(panel, parentCategory, index, parentnode, sel);
    }

}
