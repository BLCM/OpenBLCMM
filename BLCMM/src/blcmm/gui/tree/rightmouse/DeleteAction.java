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

import blcmm.gui.MainGUI;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.SetCommand;
import blcmm.utilities.Options;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class DeleteAction extends RightMouseButtonAction {

    public DeleteAction(CheckBoxTree tree, int hotkey, boolean ctrl) {
        super(tree, "Delete", hotkey, ctrl, new Requirements(true, false, true, false));
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return false;
        }
        for (TreePath path : paths) {
            if (path.getPath().length == 1) {
                return false;
            }
            if (!Options.INSTANCE.isInDeveloperMode() && !(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject() instanceof Category)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();

        int children = Arrays.stream(paths)
                .map(path -> (ModelElement) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject())
                .mapToInt(el -> {
                    if (el instanceof Category) {
                        return ((Category) el).getNumberOfLeafDescendants();
                    }
                    return 1;
                })
                .sum();
        int codes = Arrays.stream(paths)
                .map(path -> (ModelElement) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject())
                .mapToInt(el -> {
                    if (el instanceof Category) {
                        return ((Category) el).getNumberOfCommandsDescendants();
                    }
                    return (el instanceof SetCommand ? 1 : 0);
                })
                .sum();

        String items = children + (children > 1 ? " items" : " item");
        String functional = " functional " + (codes > 1 ? "lines" : "line") + " of code";
        JLabel label = new JLabel("<html>Are you sure you wish to delete these elements?"
                + (children == 0 ? "" : "<br/>The selection contains " + (children != codes
                                ? (items + (codes > 0 ? (", " + codes + " of which are " + functional) : "") + ".")
                                : (codes + functional + "."))));

        JCheckBox box = new JCheckBox("Do not remind me when only deleting comments");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(label);
        if (codes == 0) {//Only show option to disable when only deleting categories and comments
            panel.add(Box.createVerticalStrut(5));
            panel.add(box);
        }
        if ((Options.INSTANCE.getShowDeletionConfirm() || codes > 0) && children > 0) {
            int option = JOptionPane.showConfirmDialog(MainGUI.INSTANCE,
                    panel,
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option != JOptionPane.YES_OPTION) {
                return;
            } else if (box.isSelected()) {
                Options.INSTANCE.setShowDeleteConfirmation(false);
            }
        }

        tree.removeNodesFromTheirParents(paths);
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            ModelElement c = (ModelElement) node.getUserObject();
            tree.getPatch().removeElementFromParentCategory(c);
        }

        tree.setChanged(
                true);
    }
}
