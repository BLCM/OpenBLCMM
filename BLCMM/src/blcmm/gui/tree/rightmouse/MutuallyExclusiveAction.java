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

import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.model.properties.PropertyChecker;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class MutuallyExclusiveAction extends CheckMarkRightMouseButtonAction {

    public MutuallyExclusiveAction(CheckBoxTree tree) {
        super(tree, "Mutually exclusive", true);
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
        if (!(node.getUserObject() instanceof Category)) {
            return false;
        }
        Category cat = (Category) node.getUserObject();

        // Always allow if we're currently set to be mutually exclusive, so
        // that it can at least be turned off, if the tree ends up in a weird
        // state (like through DND or something)
        if (cat.isMutuallyExclusive()) {
            return true;
        }

        // Don't allow if we're the root category
        if (cat.getParent() == null) {
            return false;
        }

        // Also don't allow if we're the top-level "mods" Category
        if (cat.getParent().getParent() == null && cat.getName().equals("mods")) {
            return false;
        }

        // Require that the category have contents
        List<ModelElement> content = cat.getElements();
        if (content.isEmpty()) {
            return false;
        }

        // Require that all contents are also Categories.
        for (ModelElement c : content) {
            if (!(c instanceof Category)) {
                return false;
            }
        }

        // Don't allow if any of our ancestors are mutually exclusive
        Category curCat = cat;
        while ((curCat = curCat.getParent()) != null) {
            if (curCat.isMutuallyExclusive()) {
                return false;
            }
        }

        // Also don't allow if any of our *children* are mutually exclusive.
        // We're going to cheat a bit here and use our transient data, rather
        // than walk the tree.  Since we know that we aren't MUT ourselves
        // (thanks to an earlier step), if we've received a propagated MUT
        // property, we'll be off-limits.
        for (PropertyChecker checker : cat.getTransientData().getProperties()) {
            if (checker instanceof GlobalListOfProperties.MUTChecker) {
                return false;
            }
        }

        // If we made it here, we're golden.
        return true;
    }

    @Override
    public void action() {
        TreePath[] paths = tree.getSelectionPaths();
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Category cat = (Category) node.getUserObject();
        boolean newMode = ((JCheckBoxMenuItem) getButton()).isSelected();
        cat.setMutuallyExclusive(newMode);
        if (newMode) {
            tree.checkNode(path, false);
        }
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.nodeStructureChanged(node);
        tree.setChanged(true);
    }

    @Override
    public void update() {
        TreePath[] paths = tree.getSelectionPaths();
        TreePath path = paths[0];
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Category cat = (Category) node.getUserObject();
        ((JCheckBoxMenuItem) getButton()).setSelected(cat.isMutuallyExclusive());
    }

}
