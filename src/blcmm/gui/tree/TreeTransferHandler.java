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
package blcmm.gui.tree;

import blcmm.gui.FontInfo;
import blcmm.gui.GUI_IO_Handler;
import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.model.Category;
import blcmm.model.ModelElement;
import blcmm.model.SetCommand;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.ImportAnomalyLog;
import blcmm.utilities.Options;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman Code based on
 * https://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree
 */
abstract class TreeTransferHandler extends TransferHandler {

    private static final DataFlavor NODES_FLAVOR;
    private static final DataFlavor[] FLAVORS = new DataFlavor[1];

    static {
        String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
        DataFlavor flav = null;
        try {
            flav = new DataFlavor(mimeType);
            FLAVORS[0] = flav;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TreeTransferHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        NODES_FLAVOR = flav;
    }

    /**
     * Defensive copy used in createTransferable.
     */
    private static DefaultMutableTreeNode copy(DefaultMutableTreeNode node) {
        ModelElement model = (ModelElement) node.getUserObject();
        ModelElement modelcopy = model.copy();
        if (model instanceof Category) {
            return CheckBoxTree.createTree((Category) modelcopy);
        }
        return new DefaultMutableTreeNode(modelcopy);
    }

    private long timeOfMousePress;
    private boolean intoTool;
    private ImportMode importMode;
    private DefaultMutableTreeNode[] nodesToRemove;
    private FontInfo fontInfo;

    TreeTransferHandler(FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    void mousePressed() {
        timeOfMousePress = System.currentTimeMillis();
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        importMode = null;
        if (support.isDataFlavorSupported(NODES_FLAVOR)) {
            importMode = ImportMode.TREE_ELEMENTS;
        } else if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            importMode = ImportMode.FILE;
        } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            importMode = ImportMode.TEXT;
        } else {
            return false;
        }
        support.setShowDropLocation(true);

        //Gather some info
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (!(target.getUserObject() instanceof ModelElement)) {
            return false;
        }
        ModelElement targetElement = (ModelElement) target.getUserObject();

        // Only allow dropping onto Categories
        if (!(targetElement instanceof Category)) {
            return false;
        }
        // Convenience var to know if we're dropping into a MUT Category
        final boolean dropToMut = ((Category) targetElement).isMutuallyExclusive();

        if (importMode == ImportMode.TREE_ELEMENTS) {//we need to make sure in-tree D&D satify some more things
            // To avoid accidental d&d
            if (System.currentTimeMillis() - timeOfMousePress < 500) {
                return false;
            }

            int[] selRows = tree.getSelectionRows();
            // Do not allow a drop on the drag source selections.
            for (int i = 0; i < selRows.length; i++) {
                if (selRows[i] == dropRow) {
                    return false;
                }
            }

            // Convenience var to know if any of the paths we're dragging contain
            // a MUT category themselves (this is to prevent nested MUTs).
            boolean dragContainsMut = false;

            // Loop through the selections which the user is trying to drag.
            // Ensure we're not dragging into our own child category, and keep
            // track of whether anything in the source path list is checked/selected
            TreePath[] paths = new TreePath[selRows.length];
            DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[selRows.length];
            boolean somethingChecked = false;
            for (int i = 0; i < paths.length; i++) {
                paths[i] = tree.getPathForRow(selRows[i]);
                nodes[i] = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                somethingChecked = somethingChecked || ((CheckBoxTree) tree).isSelected(nodes[i]);

                // Do not allow a non-leaf node to be copied to a level
                // which is less than its source level.
                if (nodes[i].isNodeDescendant(target)) {//No dragging into child categories
                    return false;
                }

                // If dropping into a mutually-exclusive category, our paths must
                // be other Categories, not raw commands (or comments)
                if (dropToMut && (nodes[i].getUserObject() instanceof SetCommand)) {
                    return false;
                }

                // See if this node contains a MUT anywhere in it.  We're going to
                // use TransientData to make use of the already-propagated
                // MUTChecker property.  (Don't bother testing this if we've already
                // found one.)
                if (!dragContainsMut
                        && ((ModelElement) nodes[i].getUserObject()).getTransientData().getNumberOfOccurences(GlobalListOfProperties.MUTChecker.class) > 0) {
                    dragContainsMut = true;
                }
            }

            // Starting at the destination Category, head up the tree and run some
            // checks.  If any component is locked, deny the drop.  Also, if the
            // drag contains any MUT elements, we cannot be dragged into a tree
            // which itself has MUT.
            TreePath pathToRoot = dest;
            while (pathToRoot != null) {
                Object userObject = ((DefaultMutableTreeNode) pathToRoot.getLastPathComponent()).getUserObject();
                if (userObject instanceof Category) {
                    Category c = (Category) userObject;
                    if (c.isLocked()) {
                        return false;
                    } else if (dragContainsMut && c.isMutuallyExclusive()) {
                        return false;
                    }
                }
                pathToRoot = pathToRoot.getParentPath();
            }
        } else {
            // Starting at the destination Category, head up the tree and run some
            // checks.  If any component is locked or is MUT, deny the drop.
            // We *could* allow MUT, as we do with the in-tree drops, above,
            // since we already check for that and deselect things if need be,
            // but it makes a bit less sense to do so in this context.
            TreePath pathToRoot = dest;
            while (pathToRoot != null) {
                Object userObject = ((DefaultMutableTreeNode) pathToRoot.getLastPathComponent()).getUserObject();
                if (userObject instanceof Category) {
                    Category c = (Category) userObject;
                    if (c.isMutuallyExclusive()) {
                        return false;
                    } else if (c.isLocked()) {
                        return false;
                    }
                }
                pathToRoot = pathToRoot.getParentPath();
            }
        }

        // If we made it through that whole battery of tests, we're good
        // to go.
        return true;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        Object o = null;
        try {
            Transferable t = support.getTransferable();
            o = t.getTransferData(importMode.flavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        if (o == null) {
            return false;
        }

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        CheckBoxTree tree = (CheckBoxTree) support.getComponent();
        int childIndex = dl.getChildIndex();
        // Configure for drop mode.
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();

        // Walk down the dest tree to find out if we're inside a MUT selection.
        // If we *are*, we'll inherit the selectedness of the option we're being
        // dragged to (or default to not selected, if we're a new top-level
        // MUT category).
        TreePath curPath = dest;
        Category lastCategory = null;
        boolean unselectSource = false;
        while (curPath != null) {
            Object userObject = ((DefaultMutableTreeNode) curPath.getLastPathComponent()).getUserObject();
            if (userObject instanceof Category) {
                Category c = (Category) userObject;
                if (c.isMutuallyExclusive()) {
                    if (lastCategory == null) {
                        // Dropping directly into a MUT category - unselect everything
                        unselectSource = true;
                    } else {
                        if (lastCategory.getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafSelectedChecker.class) == 0) {
                            // Dropping into a MUT selection which has nothing checked -- unselect everything
                            unselectSource = true;
                        } else {
                            // Dropping into a MUT selection which DOES have things checked -- leave selections alone
                        }
                    }
                    break;
                }
                lastCategory = c;
            }
            curPath = curPath.getParentPath();
        }

        final int index;
        if (childIndex == -1) {
            // DropMode.ON
            index = parent.getChildCount();
        } else {
            // DropMode.INSERT
            index = childIndex;
        }
        switch (importMode) {
            case FILE:
                return importFiles((List) o, dl, tree, index, unselectSource);
            case TREE_ELEMENTS:
                return importNodes((DefaultMutableTreeNode[]) o, dl, tree, index, unselectSource);
            case TEXT:
                return importText((String) o, dl, tree, index, unselectSource);
            default:
                return false;
        }
    }

    private boolean importFiles(List<File> fileList, JTree.DropLocation dl,
            CheckBoxTree tree, final int index, boolean unselectSource) {
        if (fileList.size() < 1) {
            return false;
        }
        final int res;
        if (fileList.size() > 1) {//Opening makes no sense, so do not prompt
            res = 0;
        } else {
            res = AdHocDialog.run(MainGUI.INSTANCE,
                    this.fontInfo,
                    AdHocDialog.IconType.QUESTION,
                    "Import or open?",
                    "Import as mod on selected location, or open as file?",
                    new String[] {"Import", "Open", "Cancel"},
                    0);
            if (res == -1 || res == 2/*cancel*/) {
                return false;
            }
        }
        switch (res) {
            case 0:
                //import
                TreePath dest = dl.getPath();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
                Category parentCategory = (Category) parent.getUserObject();
                final File[] mods = fileList.toArray(new File[0]);
                if (mods.length > 0) {
                    Options.INSTANCE.setLastImport(mods[mods.length-1]);
                }
                EventQueue.invokeLater(() -> {
                    ImportAnomalyLog.INSTANCE.clear();
                    int number = GUI_IO_Handler.addMods(mods, tree.getPatch(), parentCategory, index, unselectSource);
                    GUI_IO_Handler.reportImportResults(number, false, tree.getPatch(), parentCategory);
                });
                break;
            case 1:
                //open
                final File f = fileList.get(0);
                EventQueue.invokeLater(() -> {
                    MainGUI.INSTANCE.openPatch(f);
                });
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean importText(final String s, JTree.DropLocation dl,
            CheckBoxTree tree, final int index, boolean unselectSource) {

        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        Category parentCategory = (Category) parent.getUserObject();

        EventQueue.invokeLater(() -> {
            ImportAnomalyLog.INSTANCE.clear();
            int number = GUI_IO_Handler.addStringMod(s, tree.getPatch(), parentCategory, index, unselectSource);
            GUI_IO_Handler.reportImportResults(number, false, tree.getPatch(), parentCategory);
        });
        return true;
    }

    private boolean importNodes(DefaultMutableTreeNode[] nodes,
            JTree.DropLocation dl, CheckBoxTree tree, int index,
            boolean unselectSource) throws RuntimeException {

        // Get drop location info.
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        DefaultTreeModel model = tree.getModel();

        Category parentCategory = (Category) parent.getUserObject();
        boolean hasLeafChildren = false;
        boolean childrenState = false;
        for (ModelElement c : parentCategory.getElements()) {
            if (c instanceof SetCommand) {
                hasLeafChildren = true;
                childrenState = childrenState || ((SetCommand) c).isSelected();
            }
        }
        // Add data to model.
        try {
            for (DefaultMutableTreeNode node : nodes) {
                ModelElement el = (ModelElement) node.getUserObject();
                if (unselectSource) {
                    // Doing this before inserting just in case anything
                    // recursive happens during the inserts.
                    if (el instanceof SetCommand) {
                        tree.getPatch().setSelected((SetCommand) el, false);
                    } else if (el instanceof Category) {
                        tree.getPatch().deselectEntireCategory((Category) el);
                    } else {
                        GlobalLogger.log("ERROR: Unknown ModelElement type being dropped: " + el.getClass() + " - " + el.toString());
                    }
                }
                tree.getPatch().insertElementInto(el, parentCategory, index);
                model.insertNodeInto(node, parent, index);
                if (!unselectSource && el instanceof SetCommand && hasLeafChildren) {
                    tree.getPatch().setSelected((SetCommand) el, childrenState);
                }
                index++;
            }
        } catch (Throwable t) {//For some reason, errors thrown here are not reported.
            t.printStackTrace();
            throw new RuntimeException(t);
        }
        intoTool = true;

        // This call will make sure that the tree is rendered properly post-DND.
        MainGUI.INSTANCE.updateComponentTreeUI();

        return true;
    }

    @Override
    protected Transferable createTransferable(JComponent c
    ) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        intoTool = false;
        if (paths != null) {
            // Make up a node array of copies for transfer and
            // another for/of the nodes that will be removed in
            // exportDone after a successful drop.
            List<DefaultMutableTreeNode> copies = new ArrayList<>();
            List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            DefaultMutableTreeNode copy = copy(node);
            copies.add(copy);
            toRemove.add(node);
            for (int i = 1; i < paths.length; i++) {
                DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < node.getLevel()) {
                    break;
                } else if (next.getLevel() > node.getLevel()) {
                    // child node
                    //copy.add(copy(next));
                    // node already contains child
                } else {
                    // sibling
                    copies.add(copy(next));
                    toRemove.add(next);
                }
            }
            for (DefaultMutableTreeNode n : toRemove) {
                if (((ModelElement) n.getUserObject()).hasLockedAncestor()) {
                    return null;
                }
            }
            DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
            nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
            return new NodesTransferable(nodes);
        }
        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if (!intoTool) {
            return;
        }
        if ((action & MOVE) == MOVE) {
            CheckBoxTree tree = (CheckBoxTree) source;
            // Remove nodes saved in nodesToRemove in createTransferable.
            tree.removeNodesFromTheirParents(nodesToRemove);
            for (DefaultMutableTreeNode nodeToRemove : nodesToRemove) {
                boolean b = tree.getPatch().removeElementFromParentCategory((ModelElement) nodeToRemove.getUserObject());
                if (!b) {
                    throw new NullPointerException();
                }
            }
            callback();
        }
    }

    protected abstract void callback();

    @Override
    public String toString() {
        return getClass().getName();
    }

    private static enum ImportMode {
        TREE_ELEMENTS(NODES_FLAVOR), FILE(DataFlavor.javaFileListFlavor), TEXT(DataFlavor.stringFlavor);
        public DataFlavor flavor;

        private ImportMode(DataFlavor flavor) {
            this.flavor = flavor;
        }
    }

    private class NodesTransferable implements Transferable {

        DefaultMutableTreeNode[] nodes;

        NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return FLAVORS;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return NODES_FLAVOR.equals(flavor);
        }
    }

}
