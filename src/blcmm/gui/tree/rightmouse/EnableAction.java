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
 */
package blcmm.gui.tree.rightmouse;

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.SetCommand;
import blcmm.utilities.Options;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Action to enable/disable any number of statements/categories.  Obviously the
 * usual way to do this is via the checkboxes, but having a keyboard shortcut
 * should be useful, and this way you can enable/disable multiple at the same
 * time.
 *
 * Will refuse to do anything if anything in the selection list is a top-level
 * MUT member, or if anything's locked.  (Or, actually, if anything isn't a
 * category or `set` command.  I wonder if we should allow comments too and
 * just ignore them.)
 *
 * @author apocalyptech
 */
public class EnableAction extends RightMouseButtonAction {

    private final boolean enable;
    private final String label;

    public EnableAction(CheckBoxTree tree, int hotkey, boolean ctrl, boolean enable, String label) {
        super(tree, label, hotkey, ctrl, new RightMouseButtonAction.Requirements(false, true, false));
        this.enable = enable;
        this.label = label.toLowerCase();
    }


    @Override
    public boolean couldBeEnabled() {
        TreePath[] paths = tree.getSelectionPaths();

        // Make sure we have something selected
        if (paths == null || paths.length == 0) {
            return false;
        }

        // Make sure that everything selected is a command or a category
        // (we don't have to check for locked -- that's already taken care of
        // my our Requirements, above)
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            ModelElement element = (ModelElement) node.getUserObject();
            if (!(element instanceof SetCommand || element instanceof Category)) {
                return false;
            }

            // Don't allow if it's a SetCommand and the user doesn't have the
            // setting to allow individual toggles.
            if (element instanceof SetCommand && !Options.INSTANCE.getLeafSelectionAllowed()) {
                return false;
            }

            // Also, don't allow if we're a top-level member of a MUT category
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent != null) {
                Category parentCat = (Category) parent.getUserObject();
                if (parentCat.isMutuallyExclusive()) {
                    return false;
                }
            }
        }

        // If we got here, we should be good!
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return;
        }

        // If the user is getting enabled-a-bunch-of-commands confirmations,
        // check to see if any of our selected paths are categories -- if so,
        // show that warning.
        if (Options.INSTANCE.getShowConfirmPartialCategory()) {

            int statements = 0;
            boolean gotCategory = false;

            for (TreePath path : paths) {
                ModelElement element = (ModelElement) ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
                if (element instanceof Category) {
                    gotCategory = true;
                    statements += ((Category)element).getNumberOfCommandsDescendants();
                } else {
                    // Otherwise, we must be a single `set` command (hotfix or
                    // otherwise)
                    statements++;
                }
            }

            if (gotCategory && statements>0) {
                String plural = (statements == 1 ? "" : "s");
                if (!tree.confirmEnableCategory(
                        "<html>Are you sure you wish to " + this.label + " everything currently selected?<br/>"
                        + "<br/>"
                        + "This will " + this.label + " <b>" + statements + "</b> statement" + plural + ".<br/>"
                        + "<br/>"
                        )) {
                    return;
                }
            }
        }

        // If we got here, we're apparently good to go.
        for (TreePath path : paths) {
            this.tree.checkNode(path, this.enable);
        }

    }

}
