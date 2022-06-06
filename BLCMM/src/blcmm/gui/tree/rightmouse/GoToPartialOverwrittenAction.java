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
import blcmm.gui.tree.OverwriteChecker;
import blcmm.model.ModelElement;
import blcmm.model.SetCommand;
import blcmm.model.TransientModelData;
import general.utilities.GlobalLogger;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class GoToPartialOverwrittenAction extends RightMouseButtonAction {

    public GoToPartialOverwrittenAction(CheckBoxTree tree) {
        super(tree, "Go to partially overwritten statement", Requirements.NO_REQUIREMENTS);
    }

    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) {
            return false;
        }
        if (paths.length != 1) {
            return false;
        }
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        ModelElement element = (ModelElement) node.getUserObject();
        if (!(element instanceof SetCommand)) {
            return false;
        }
        if (element.getTransientData().getOverwriteState() != TransientModelData.OverwriteState.PartialOverwriter) {
            return false;
        }
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        TreePath firstPath = paths[0];
        DefaultMutableTreeNode firstSelectedNode = (DefaultMutableTreeNode) firstPath.getLastPathComponent();
        SetCommand command = (SetCommand) firstSelectedNode.getUserObject();
        List<SetCommand> partialOverwrittens = OverwriteChecker.getPartialOverwrittens(command);
        if (partialOverwrittens.size() > 0) {
            SetCommand s = partialOverwrittens.get(partialOverwrittens.size() - 1);
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            DefaultMutableTreeNode nodeToExpand = scan(root, s);
            TreePath path = new TreePath(nodeToExpand.getPath());
            tree.expandPath(path.getParentPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        } else {
            GlobalLogger.log("ERROR: Could not find partially-overwritten statement for: " + command.toString());
        }
    }

    private DefaultMutableTreeNode scan(DefaultMutableTreeNode root, final SetCommand command) {
        if (root.getUserObject() == command) {
            return root;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode res = scan((DefaultMutableTreeNode) root.getChildAt(i), command);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

}
