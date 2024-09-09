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

import blcmm.gui.GUI_IO_Handler;
import blcmm.gui.MainGUI;
import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.utilities.ImportAnomalyLog;
import blcmm.utilities.Options;
import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public class ImportModAction extends RightMouseButtonAction {

    private int insertIndex;
    private Category newParent;
    private DefaultMutableTreeNode parentNode;

    public ImportModAction(CheckBoxTree tree) {
        super(tree, "Import mod", Requirements.NO_REQUIREMENTS);
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

        if (node.getUserObject() instanceof Category) {
            newParent = (Category) node.getUserObject();
            insertIndex = node.getChildCount();
            parentNode = node;
        } else {
            ModelElementContainer parent = ((ModelElement) node.getUserObject()).getParent();
            if (!(parent instanceof Category)) {
                newParent = parent.getParent();
            } else {
                newParent = (Category) parent;
            }
            parentNode = (DefaultMutableTreeNode) node.getParent();
            insertIndex = parentNode.getIndex(node) + 1;
        }

        // Can not paste in locked categories
        if (newParent.hasMUTAncestor() || newParent.isMutuallyExclusive()) {//To prevent nested muts.
            return false;
        }
        return true;
    }

    @Override
    public void action() {
        Category mutparent = newParent;
        ModelElement current = newParent;
        boolean needToDeselect1 = false;//While we currently disallow importing into MUT categories, it doesn't hurt to have this logic in place.
        while (mutparent != null && !mutparent.isMutuallyExclusive()) {
            current = mutparent;
            mutparent = mutparent.getParent();
        }
        if (mutparent != null) {
            for (ModelElement el : mutparent.getElements()) {
                if (el.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class) > 0) {
                    needToDeselect1 = (el != current);
                }
            }
        }
        BLCMM_FileChooser chooser = new BLCMM_FileChooser(this.tree.getFontInfo(), MainGUI.INSTANCE.getImportDialogPath());
        chooser.setDialogTitle("Import Mods");
        int confirm = chooser.showOpenDialog(null);
        final boolean needToDeselect = needToDeselect1;
        if (confirm == JFileChooser.APPROVE_OPTION) {
            Options.INSTANCE.setLastImport(chooser.getSelectedFile());
            EventQueue.invokeLater(() -> {
                ImportAnomalyLog.INSTANCE.clear();
                int number = GUI_IO_Handler.addMods(chooser.getSelectedFile(), tree.getPatch(), newParent, insertIndex, needToDeselect);
                GUI_IO_Handler.reportImportResults(number, false, tree.getPatch(), newParent);
                tree.setChanged(true);
            });
        }
    }
}
