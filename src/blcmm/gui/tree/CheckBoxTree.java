/*
 * Copyright (C) 2018-2020  LightChaosman
 * Copyright (C) 2023 Christopher J. Kucera
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
package blcmm.gui.tree;

import blcmm.Meta;
import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.tree.rightmouse.*;
import blcmm.model.*;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.OSInfo;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public final class CheckBoxTree extends JTree {

    // Defining data structure that will enable to fast check-indicate the state of each node
    // It totally replaces the "selection" mechanism of the JTree
    static class CheckedNode {

        boolean isSelected;
        boolean hasChildren;
        boolean allChildrenSelected;

        public CheckedNode(boolean isSelected_, boolean hasChildren_, boolean allChildrenSelected_) {
            isSelected = isSelected_;
            hasChildren = hasChildren_;
            allChildrenSelected = allChildrenSelected_;
        }

        @Override
        public String toString() {
            return isSelected + " " + hasChildren + " " + allChildrenSelected;
        }

    }

    private static class CheckChangeEvent extends EventObject {

        private static final long serialVersionUID = -8100230309044193368L;

        public CheckChangeEvent(Object source) {
            super(source);
        }
    }

    private static final long serialVersionUID = -4194122328392241790L;

    boolean change = false;
    private CompletePatch patch;

    private HashMap<TreePath, CheckedNode> nodesCheckingState;
    private HashSet<TreePath> checkedPaths = new HashSet<>();
    private final CheckboxTreeMouseAdapter adapter;

    private final FontInfo fontInfo;

    public CheckBoxTree(FontInfo fontInfo) {
        super(new String[]{"Made by LightChaosman"});
        this.fontInfo = fontInfo;
        // Overriding cell renderer by new one defined above
        setFont(new Font(MainGUI.CODE_FONT_NAME, Font.PLAIN, Options.INSTANCE.getFontsize()));
        this.setCellRenderer(new CheckBoxTreeCellRenderer(fontInfo));
        this.setRootVisible(true);
        setDragEnabled(true);
        TreeTransferHandler treeTransferHandler = new TreeTransferHandler(fontInfo) {
            @Override
            protected void callback() {
                setChanged(true);
                repaint();
            }
        };
        setTransferHandler(treeTransferHandler);

        setDropMode(DropMode.ON_OR_INSERT);
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // Calling checking mechanism on mouse click
        final MouseListener orig = this.getMouseListeners()[0];
        adapter = new CheckboxTreeMouseAdapter(orig, treeTransferHandler, this);
        this.removeMouseListener(orig);//We incorporate the original mouselistener in our own.
        this.addMouseListener(adapter);//*/

        this.addKeyListener(new KeyAdapter() {
            EasterEggs easterEggs = new EasterEggs();

            // So Toolkit.getMenuShortcutKeyMask() and InputEvent.getModifiers()
            // are deprecated in favor of the versions which have "Ex" at the end.
            // Unfortunately, we still want to support Java 8, at the moment, and
            // Toolkit.getMenuShortcutKeyMaskEx() wasn't introduced until Java 10.
            // So, we're just letting it be.  When we drop Java 8 support, we
            // should be able to just hop over to the 'Ex' versions in here.
            //
            // See: https://github.com/BLCM/OpenBLCMM/issues/21
            @Override
            @SuppressWarnings("deprecation")
            public void keyPressed(KeyEvent e) {
                easterEggs.checkAllCodes(e.getKeyCode());
                for (RightMouseButtonAction action : adapter.actions) {
                    int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                    int thismask = e.getModifiers();
                    if (action.getHotKey() != -1
                            && e.getExtendedKeyCode() == action.getHotKey()
                            && action.isEnabled()
                            && (action.getCTRL() ? ((thismask & mask) == mask) : true)) {
                        action.action();
                        MainGUI.INSTANCE.requestFocus();
                    }
                }
            }
        });
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public CompletePatch getPatch() {
        return patch;
    }

    public FontInfo getFontInfo() {
        return this.fontInfo;
    }

    public boolean isSelected(DefaultMutableTreeNode treenode) {
        TreePath tp = new TreePath(treenode.getPath());
        CheckedNode cn = nodesCheckingState.get(tp);
        return cn.isSelected;
    }

    // Returns true in case that the node is selected, has children but not all of them are selected
    public boolean isSelectedCompletely(DefaultMutableTreeNode treenode) {
        TreePath tp = new TreePath(treenode.getPath());
        CheckedNode cn = nodesCheckingState.get(tp);
        if (cn.hasChildren) {
            return cn.allChildrenSelected && cn.isSelected;
        } else {
            return cn.isSelected;
        }
    }

    CheckedNode getCheckedNode(TreePath path) {
        return nodesCheckingState.get(path);
    }

    public void updateFontSizes() {
        for (RightMouseButtonAction item : adapter.actions) {
            item.getButton().setFont(item.getButton().getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
        }
    }

    /**
     * Updates our elements' internal lengths, in response to changes in our
     * truncation settings. Note that you should call
     * SwingUtilities.updateComponentTreeUI(MainGUI.INSTANCE) after calling this
     * -- Otherwise, nodes whose lengths have changed won't have their widths
     * changed, and the UI will go a bit wonky.
     */
    public void updateTreeLengths() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
        Object cat = node.getUserObject();
        if (cat instanceof ModelElementContainer) {
            ((ModelElementContainer) cat).recreateLengthsRecursive();
        }
    }

    // Defining a new event type for the checking mechanism and preparing event-handling mechanism
    protected EventListenerList listenerList = new EventListenerList();

    public interface CheckChangeEventListener extends EventListener {

        public void checkStateChanged(CheckChangeEvent event);
    }

    public void addCheckChangeEventListener(CheckChangeEventListener listener) {
        listenerList.add(CheckChangeEventListener.class, listener);
    }

    public void removeCheckChangeEventListener(CheckChangeEventListener listener) {
        listenerList.remove(CheckChangeEventListener.class, listener);
    }

    void fireCheckChangeEvent(CheckChangeEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == CheckChangeEventListener.class) {
                ((CheckChangeEventListener) listeners[i + 1]).checkStateChanged(evt);
            }
        }
    }

    public void setPatch(CompletePatch patch) {
        this.patch = patch;
        if (patch.getRoot() == null) {
            patch.setRoot(new Category(Category.DEFAULT_ROOT_NAME));
        }
        DefaultMutableTreeNode top = createTree(patch.getRoot());
        this.setModel(new DefaultTreeModel(top));
        this.getModel().nodeStructureChanged(top);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
        Object ob = root.getUserObject();
        if (ob instanceof Category) {
            isEverythingAllright();
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                if (child.getUserObject() instanceof Category && ((Category) child.getUserObject()).getName().equals("mods")) {
                    setExpandedState(new TreePath(new Object[]{root, child}), true);
                }
            }
        }
        ColorGiver.reset(patch.getRoot());
        repaint();
    }

    // Override
    @Override
    public void setModel(TreeModel newModel) {
        super.setModel(newModel);
        resetCheckingState();
        change = false;
    }

    // New method that returns only the checked paths (totally ignores original "selection" mechanism)
    public TreePath[] getCheckedPaths() {
        return checkedPaths.toArray(new TreePath[checkedPaths.size()]);
    }

    public boolean isChanged() {
        return change;
    }

    public void setChanged(boolean flag) {
        change = flag;
        if (flag) {
            isEverythingAllright();
            resetCheckingState();
            ColorGiver.reset(patch.getRoot());
        }
    }

    public void resetCheckingState() {
        nodesCheckingState = new HashMap<>();
        checkedPaths = new HashSet<>();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) getModel().getRoot();
        if (node == null) {
            return;
        }
        addSubtreeToCheckingStateTracking(node);
    }

    // Creating data structure of the current model for the checking mechanism
    private void addSubtreeToCheckingStateTracking(DefaultMutableTreeNode node) {
        TreeNode[] path = node.getPath();
        TreePath tp = new TreePath(path);
        boolean checked = false;
        if (node.getUserObject() instanceof SetCommand) {
            checked = ((SetCommand) node.getUserObject()).isSelected();
        } else if (node.getUserObject() instanceof ModelElement) {
        } else {
            System.err.println(node.getUserObject());
            return;
        }

        CheckedNode cn = new CheckedNode(checked, node.getChildCount() > 0, checked);
        nodesCheckingState.put(tp, cn);
        if (node.getChildCount() == 0) {
            if (node.getParent() == null) {
                return;
            }
            return;
        }
        boolean all = true;
        boolean some = false;
        for (int i = 0; i < node.getChildCount(); i++) {
            TreePath tpx = tp.pathByAddingChild(node.getChildAt(i));
            DefaultMutableTreeNode x = (DefaultMutableTreeNode) tpx.getLastPathComponent();
            addSubtreeToCheckingStateTracking(x);
            Object el = x.getUserObject();
            if (true
                    && !(el instanceof Comment)
                    && !((el instanceof Category) && ((Category) el).getNumberOfCommandsDescendants() == 0)
                    && true) {
                all = all && nodesCheckingState.get(tpx).allChildrenSelected && nodesCheckingState.get(tpx).isSelected;
                some = some || nodesCheckingState.get(tpx).allChildrenSelected || nodesCheckingState.get(tpx).isSelected;
            }
        }

        cn.allChildrenSelected = all;
        if (node.getUserObject() instanceof ModelElement) {
            cn.isSelected = some;
        }
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (getCellRenderer() instanceof CheckBoxTreeCellRenderer) {
            ((CheckBoxTreeCellRenderer) getCellRenderer()).setFont(font);
        }
    }

    public void checkNode(TreePath tp, boolean checkMode) {
        checkSubTree(tp, checkMode);
        updatePredecessorsWithCheckMode(tp, checkMode);
        // Firing the check change event
        fireCheckChangeEvent(new CheckChangeEvent(new Object()));
        // Repainting tree after the data structures were updated
        repaint();
        change = true;
    }

    // When a node is checked/unchecked, updating the states of the predecessors
    private void updatePredecessorsWithCheckMode(TreePath tp, boolean check) {
        TreePath parentPath = tp.getParentPath();
        // If it is the root, stop the recursive calls and return
        if (parentPath == null) {
            return;
        }
        CheckedNode parentCheckedNode = nodesCheckingState.get(parentPath);
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        parentCheckedNode.allChildrenSelected = true;
        parentCheckedNode.isSelected = false;
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            Object el = child.getUserObject();
            if ((el instanceof Category && ((Category) el).getNumberOfCommandsDescendants() == 0) || el instanceof Comment) {
                continue;
            }
            TreePath childPath = parentPath.pathByAddingChild(child);
            CheckedNode childCheckedNode = nodesCheckingState.get(childPath);
            // It is enough that even one subtree is not fully selected
            // to determine that the parent is not fully selected
            if (!childCheckedNode.allChildrenSelected) {
                parentCheckedNode.allChildrenSelected = false;
            }
            // If at least one child is selected, selecting also the parent
            if (childCheckedNode.isSelected) {
                parentCheckedNode.isSelected = true;
            }
        }
        if (parentCheckedNode.isSelected) {
            checkedPaths.add(parentPath);
        } else {
            checkedPaths.remove(parentPath);
        }
        // Go to upper predecessor
        updatePredecessorsWithCheckMode(parentPath, check);
    }

    // Recursively checks/unchecks a subtree
    private void checkSubTree(TreePath tp, boolean check) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
        ModelElement code = (ModelElement) node.getUserObject();
        int toCheckChildCount = node.getChildCount();
        if (code instanceof Category) {
            Category cat = (Category) code;
            if (cat.isMutuallyExclusive() && check) {
                toCheckChildCount = 1;
            } else if (cat.isLocked()) {
                //return;
            }
        }
        if (code instanceof SetCommand) {
            patch.setSelected((SetCommand) code, check);
        }
        CheckedNode cn = nodesCheckingState.get(tp);
        cn.isSelected = check;

        for (int i = 0; i < toCheckChildCount; i++) {
            checkSubTree(tp.pathByAddingChild(node.getChildAt(i)), check);
        }
        cn.allChildrenSelected = check && toCheckChildCount == node.getChildCount();
        if (check) {
            checkedPaths.add(tp);
        } else {
            checkedPaths.remove(tp);
        }
    }

    public void removeNodesFromTheirParents(DefaultMutableTreeNode[] nodes) {
        TreePath[] paths = new TreePath[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            paths[i] = new TreePath(nodes[i].getPath());
        }
        removeNodesFromTheirParents(paths);
    }

    public void removeNodesFromTheirParents(TreePath[] paths) {
        HashMap<DefaultMutableTreeNode, LinkedHashSet<DefaultMutableTreeNode>> parents = new HashMap<>();
        HashMap<DefaultMutableTreeNode, HashSet> parentsToExpandedDescendants = new HashMap<>();
        for (TreePath t : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) t.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (!parents.containsKey(parent)) {
                parents.put(parent, new LinkedHashSet<>());
                for (int i = 0; i < parent.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
                    parents.get(parent).add(child);
                }
                parentsToExpandedDescendants.put(parent, new HashSet());
                Enumeration descs = parent.preorderEnumeration();
                while (descs.hasMoreElements()) {
                    DefaultMutableTreeNode desc = (DefaultMutableTreeNode) descs.nextElement();
                    if (isExpanded(new TreePath(desc.getPath()))) {
                        parentsToExpandedDescendants.get(parent).add(desc.getUserObject());
                    }
                }
            }
            parents.get(parent).remove(node);
        }
        for (DefaultMutableTreeNode parent : parents.keySet()) {
            parent.removeAllChildren();
            for (DefaultMutableTreeNode remainingChild : parents.get(parent)) {
                parent.add(remainingChild);
            }
            getModel().nodeStructureChanged(parent);
            Enumeration descs = parent.preorderEnumeration();
            while (descs.hasMoreElements()) {
                DefaultMutableTreeNode desc = (DefaultMutableTreeNode) descs.nextElement();
                if (parentsToExpandedDescendants.get(parent).contains(desc.getUserObject())) {
                    expandPath(new TreePath(desc.getPath()));
                }
            }
        }
    }

    @Override
    public DefaultTreeModel getModel() {
        return (DefaultTreeModel) super.getModel();
    }

    public static DefaultMutableTreeNode createTree(Category c3) {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(c3);
        for (ModelElement c : c3.getElements()) {
            if (c instanceof Category) {
                top.add(createTree((Category) c));
            } else if (c instanceof HotfixWrapper) {
                for (SetCommand command : ((HotfixWrapper) c).getElements()) {
                    top.add(new DefaultMutableTreeNode(command));
                }
            } else {
                top.add(new DefaultMutableTreeNode(c));
            }
        }
        return top;
    }

    private boolean isEverythingAllright() {
        isEverythingAllright((DefaultMutableTreeNode) getModel().getRoot());
        return true;
    }

    private static boolean isEverythingAllright(DefaultMutableTreeNode treenode) {
        Object o = treenode.getUserObject();
        if (o instanceof Category) {
            Category cat = (Category) o;
            if (cat.sizeIncludingHotfixes() != treenode.getChildCount()) {
                throw new IllegalStateException("Model has " + cat.sizeIncludingHotfixes() + " children, while tree has " + treenode.getChildCount() + " (" + treenode + ")");
            }
            List<ModelElement> catContent = cat.getElements();
            int j = 0;
            for (int i = 0; i < catContent.size(); i++) {
                ModelElement element = catContent.get(i);
                if (element.getParent() != cat) {
                    throw new IllegalStateException();
                }
                if (element instanceof HotfixWrapper) {
                    HotfixWrapper wrap = (HotfixWrapper) element;
                    for (int k = 0; k < wrap.size(); k++) {
                        if (!wrap.get(k).equals(((DefaultMutableTreeNode) treenode.getChildAt(j)).getUserObject())) {
                            throw new NullPointerException();
                        }
                        j++;
                    }
                    j--;
                } else if (!element.equals(((DefaultMutableTreeNode) treenode.getChildAt(j)).getUserObject())) {
                    throw new NullPointerException();
                }
                j++;

            }
            for (int i = 0; i < treenode.getChildCount(); i++) {
                isEverythingAllright((DefaultMutableTreeNode) treenode.getChildAt(i));
            }

        } else if (treenode.getChildCount() > 0) {
            throw new NullPointerException();
        }
        return true;
    }

    @Override
    public void scrollPathToVisible(TreePath treePath) {
        if (treePath != null) {
            makeVisible(treePath);
            Rectangle bounds = getPathBounds(treePath);
            if (bounds != null) {
                bounds.x = getVisibleRect().x;
                scrollRectToVisible(bounds);
            }
        }
    }

    public int[] search(String st, boolean includeCode) {
        TreePath tp = new TreePath(getModel().getRoot());
        int[] rows = getSelectionRows();
        int row = rows.length > 0 ? rows[0] : -1;
        int[] rescount = {0, 0};
        TreePath fr = search(st.toLowerCase(), includeCode, tp, null, new int[]{row}, rescount);
        int[] rows2 = getSelectionRows();
        int row2 = rows2.length > 0 ? rows2[0] : -1;
        if (row2 == row && fr != null) {
            rescount[0] = 1;
            setSelectionPath(fr);
            scrollPathToVisible(fr);
        }
        return rescount;
    }

    private TreePath search(final String st, boolean includeCode, TreePath tp, TreePath firstResult, int[] currentRow, int[] rescount) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
        if (node.isLeaf() && !includeCode) {
            return firstResult;
        }
        String s;
        if (node.getUserObject() instanceof SetCommand) {
            s = ((SetCommand) node.getUserObject()).getCode();
        } else {
            s = node.toString();
        }
        if (s.toLowerCase().contains(st)) {
            rescount[1]++;
            int[] currentRows2 = getSelectionRows();
            int currentRow2 = currentRows2.length > 0 ? currentRows2[0] : -1;
            expandPath(tp.getParentPath());//Expanding a path prior to our current row will increase the current row
            int[] currentRows3 = getSelectionRows();
            int currentRow3 = currentRows3.length > 0 ? currentRows3[0] : -1;
            if (currentRow2 == currentRow[0]) {
                currentRow[0] = currentRow3;
            }
            if (firstResult == null) {
                firstResult = tp;
            }
            int row = getRowForPath(tp);
            if (row > currentRow[0] && currentRow3 == currentRow[0]) {
                rescount[0] = rescount[1];
                setSelectionPath(tp);
                scrollPathToVisible(tp);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            firstResult = search(st, includeCode, tp.pathByAddingChild(node.getChildAt(i)), firstResult, currentRow, rescount);
        }

        return firstResult;
    }

    /**
     * Returns the paths of all selected values, ordered by how they appear in
     * the tree, instead of JTree's default, which is the order in which they
     * were selected.
     *
     * @return an array of TreePath objects indicating the selected nodes,
     * or null if nothing is currently selected
     */
    @Override
    public TreePath[] getSelectionPaths() {
        int [] treeRows = this.getSelectionRows();
        if (treeRows == null || treeRows.length == 0) {
            return null;
        }
        Arrays.sort(treeRows);
        TreePath[] paths = new TreePath[treeRows.length];
        for (int i=0; i<treeRows.length; i++) {
            paths[i] = this.getPathForRow(treeRows[i]);
        }
        return paths;
    }

    @Override
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        tip.setFont(this.fontInfo.getFont());
        return tip;
    }

    private static class CheckboxTreeMouseAdapter extends MouseAdapter {

        private RightMouseButtonAction introduceCategoryAction;
        private RightMouseButtonAction renameCategoryAction;
        private RightMouseButtonAction insertCategoryAction;
        private RightMouseButtonAction insertAction;
        private RightMouseButtonAction deleteAction;
        private RightMouseButtonAction copyAction;
        private RightMouseButtonAction cutAction;
        private RightMouseButtonAction pasteAction;
        private RightMouseButtonAction editAction;
        private RightMouseButtonAction sortAction;
        private RightMouseButtonAction fullyExpandAction;
        private RightMouseButtonAction fullyCollapseAction;
        private RightMouseButtonAction mutuallyExclusiveAction;
        private RightMouseButtonAction lockAction;
        private RightMouseButtonAction exportCategeroryAction;
        private RightMouseButtonAction copyModListAction;
        private RightMouseButtonAction goToOverwriterAction;
        private RightMouseButtonAction goToPartialOverwrittenAction;
        private RightMouseButtonAction goToCompleteOverwrittenAction;
        private RightMouseButtonAction importModAction;
        private RightMouseButtonAction[] actions;
        CheckBoxTree tree;

        private final MouseListener orig;
        private final TreeTransferHandler treeTransferHandler;

        public CheckboxTreeMouseAdapter(MouseListener orig, TreeTransferHandler treeTransferHandler, CheckBoxTree tree) {
            this.orig = orig;
            this.treeTransferHandler = treeTransferHandler;
            this.tree = tree;
            initializeRightMenuButtons();
        }

        private void initializeRightMenuButtons() {

            introduceCategoryAction = new IntroduceCategoryAction(tree, KeyEvent.VK_G, true);
            renameCategoryAction = new RenameCategoryAction(tree, KeyEvent.VK_R, true);
            insertCategoryAction = new AddCategoryAction(tree, KeyEvent.VK_H, true);
            insertAction = new InsertAction(tree, OSInfo.CURRENT_OS == OSInfo.OS.MAC ? KeyEvent.VK_PLUS : KeyEvent.VK_INSERT, false);
            deleteAction = new DeleteAction(tree, OSInfo.CURRENT_OS == OSInfo.OS.MAC ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE, OSInfo.CURRENT_OS == OSInfo.OS.MAC);
            copyAction = new CopyAction(tree, KeyEvent.VK_C, true);
            cutAction = new CutAction(tree, KeyEvent.VK_X, true);
            pasteAction = new PasteAction(tree, KeyEvent.VK_V, true);
            editAction = new EditAction(tree, KeyEvent.VK_E, true);
            sortAction = new SortCategoryAction(tree);
            fullyExpandAction = new ExpandCategoryCompletelyAction(tree);
            fullyCollapseAction = new CollapseCategoryCompletelyAction(tree);
            mutuallyExclusiveAction = new MutuallyExclusiveAction(tree);
            lockAction = new LockAction(tree);
            exportCategeroryAction = new ExportCategoryAsModAction(tree);
            copyModListAction = new CopyModListAction(tree);
            goToOverwriterAction = new GoToOverwriterAction(tree);
            goToPartialOverwrittenAction = new GoToPartialOverwrittenAction(tree);
            goToCompleteOverwrittenAction = new GoToCompleteOverwrittenAction(tree);
            importModAction = new ImportModAction(tree);
            List<RightMouseButtonAction> actions2 = new ArrayList<>();
            actions2.add(introduceCategoryAction);
            actions2.add(renameCategoryAction);
            actions2.add(insertCategoryAction);
            actions2.add(insertAction);
            actions2.add(deleteAction);
            actions2.add(copyAction);
            actions2.add(cutAction);
            actions2.add(pasteAction);
            actions2.add(editAction);
            actions2.add(sortAction);
            actions2.add(fullyExpandAction);
            actions2.add(fullyCollapseAction);
            actions2.add(mutuallyExclusiveAction);
            actions2.add(lockAction);
            actions2.add(exportCategeroryAction);
            actions2.add(copyModListAction);
            actions2.add(importModAction);
            actions2.add(goToOverwriterAction);
            actions2.add(goToPartialOverwrittenAction);
            actions2.add(goToCompleteOverwrittenAction);
            actions = actions2.toArray(new RightMouseButtonAction[0]);
        }
        TreePath tpfield;//Used to store the treepath before firing the first mouselistener.

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (mouseEvent.isControlDown() || mouseEvent.isShiftDown() || !tree.isEnabled()) {
                return;
            }
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                handleRightClick(mouseEvent);
                return;
            }
            TreePath tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (tp == null) {
                return;
            }
            Object userObject = ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject();
            if (!(userObject instanceof ModelElement)) {
                return;
            }
            if (mouseEvent.getClickCount() >= 2) {
                if (editAction.isEnabled()) {
                    editAction.action();
                } else if (editAction.isEnabled(new RightMouseButtonAction.Requirements(false, false, true))) {
                    RightMouseButtonAction.bringEditWindowToFront();
                } else {
                    if (!(userObject instanceof Category) && (userObject instanceof ModelElement) && !Options.INSTANCE.isInDeveloperMode()) {
                        JLabel label = new JLabel(
                                "<html>You can't edit code until you enable developer mode.<br/>"
                                + "Doing so will let you insert, edit, and delete the<br/>"
                                + "actual mod code, and will enable various syntax checks<br/>"
                                + "on the code.  Click the checkbox now, to enable it, or<br/>"
                                + "do so via the Settings dialog.<br/>"
                        );
                        label.setFont(this.tree.getFontInfo().getFont());
                        JPanel panel = new JPanel();
                        JCheckBox box = new JCheckBox("Enable now");
                        box.setFont(this.tree.getFontInfo().getFont());
                        box.setHorizontalTextPosition(SwingConstants.LEFT);

                        panel.setLayout(new GridBagLayout());
                        GridBagConstraints c = new GridBagConstraints();
                        c.gridx = c.gridy = 0;
                        c.gridheight = c.gridwidth = 1;
                        c.insets = new Insets(0, 0, 0, 0);
                        c.anchor = GridBagConstraints.EAST;
                        panel.add(label, c);
                        c.gridy = 1;
                        c.insets.top = 10;
                        panel.add(box, c);
                        AdHocDialog.run(MainGUI.INSTANCE,
                                this.tree.getFontInfo(),
                                AdHocDialog.IconType.INFORMATION,
                                "Enable developer mode first",
                                panel);
                        if (box.isSelected()) {
                            Options.INSTANCE.setDeveloperMode(true);
                            MainGUI.INSTANCE.toggleDeveloperMode(true);
                        }
                    }
                }
                orig.mouseClicked(mouseEvent);
                MainGUI.INSTANCE.requestFocus();
                return;
            }
            TreePath tp2 = tree.getPathForLocation(mouseEvent.getX() - 20, mouseEvent.getY());

            if (tp2 != null) {
                return;
            }

            //Don't allow checking of leafs by default
            if ((tp == tpfield)) {
                boolean cancelbecauseLeaf = false;
                if (((DefaultMutableTreeNode) tp.getLastPathComponent()).isLeaf()
                        && !Options.INSTANCE.getLeafSelectionAllowed()
                        && Options.INSTANCE.isInDeveloperMode()) {
                    AdHocDialog.Button allow = AdHocDialog.run(tree,
                            tree.getFontInfo(),
                            AdHocDialog.IconType.QUESTION,
                            "Allow toggling individual statements?",
                            "<html>" + Meta.NAME + " usually disallows toggling individual statements.<br/><br/>"
                            + "Enable that functionality anyway?",
                            AdHocDialog.ButtonSet.YES_NO);
                    if (allow == AdHocDialog.Button.YES) {
                        Options.INSTANCE.setLeafSelectionAllowed(true);
                    }
                    cancelbecauseLeaf = !Options.INSTANCE.getLeafSelectionAllowed();
                }
                if (!cancelbecauseLeaf) {
                    boolean checkMode = !tree.nodesCheckingState.get(tp).isSelected;
                    Object valid = isValidCheck(checkMode, tp);
                    ModelElement el = (ModelElement) userObject;
                    if (valid instanceof Category) {
                        if (valid == el) {
                            // This will only happen if a mutually-exclusive category
                            // is being checked.  Check its first option instead, and
                            // expand the MUT Category if it's not already, so the user
                            // knows what happened.
                            DefaultMutableTreeNode tn = (DefaultMutableTreeNode) tp.getLastPathComponent();
                            try {
                                tree.checkNode(
                                        tp.pathByAddingChild(tn.getFirstChild()),
                                        checkMode);
                                tree.setExpandedState(tp, true);
                            } catch (NoSuchElementException ex) {
                                // A MUT category with no children?  Weird.  Just
                                // ignore, but log it.
                                GlobalLogger.log("Found a MUT category with no children");
                            }
                        } else {
                            AdHocDialog.run(MainGUI.INSTANCE,
                                    this.tree.getFontInfo(),
                                    AdHocDialog.IconType.ERROR,
                                    "Can't check this category",
                                    "<html>Category '" + valid + "' is mutually exclusive.<br/><br/>"
                                    + "Please explicitly select its child containing this category first.");
                        }
                    } else if (valid instanceof TreePath) {
                        //This means we're switching from one selection to another, uncheck the returned path
                        tree.checkNode((TreePath) valid, false);
                        tree.checkNode(tp, checkMode);
                    } else if (el.hasLockedAncestor()) {
                        AdHocDialog.run(MainGUI.INSTANCE,
                                this.tree.getFontInfo(),
                                AdHocDialog.IconType.ERROR,
                                "Can't " + (checkMode ? "" : "un") + "check this category",
                                "This category is locked, you can not change it");
                    } else if (confirmCheck(tp)) {
                        tree.checkNode(tp, checkMode);
                    }
                    MainGUI.INSTANCE.requestFocus();
                    ColorGiver.reset(tree.getPatch().getRoot());
                }
            }
            orig.mouseClicked(mouseEvent);
        }

        @Override
        public void mousePressed(MouseEvent arg0) {
            tpfield = tree.getPathForLocation(arg0.getX(), arg0.getY());
            if (SwingUtilities.isRightMouseButton(arg0) && !arg0.isShiftDown()
                    && tree.getSelectionPaths() != null && tree.getSelectionPaths().length <= 1) {
                tree.setSelectionPath(tpfield);//allow for right mouse button selection
            }
            treeTransferHandler.mousePressed();
            orig.mousePressed(arg0);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            orig.mouseReleased(e);
        }

        /**
         * Checks to see if the user's requested "check" action is valid.
         * Returns null if the check request is an "uncheck" request (and is
         * therefore always valid, since this method does not check for lock
         * status. Returns a Category object if the selected check is a
         * mutually-exclusive category. Returns a treepath if we're switching
         * from one category to another.
         *
         * @param checkMode
         * @param path
         * @return
         */
        private Object isValidCheck(boolean checkMode, TreePath path) {
            if (!checkMode) {
                return null;
            }
            ModelElement element = (ModelElement) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (element instanceof Category && ((Category) element).isMutuallyExclusive()) {
                return element;
            }
            ModelElementContainer parenttemp = element.getParent();
            if (parenttemp == null) {
                return null;//root
            }
            Category parent = parenttemp instanceof Category ? (Category) parenttemp : parenttemp.getParent();
            if (parent != null && parent.isMutuallyExclusive()) {
                TreePath parentPath = path.getParentPath();
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                for (int i = 0; i < parentNode.getChildCount(); i++) {
                    if (tree.isSelected((DefaultMutableTreeNode) parentNode.getChildAt(i))) {
                        return parentPath.pathByAddingChild(parentNode.getChildAt(i));
                    }
                }
            }
            return isValidCheck(path);
        }

        private Category isValidCheck(TreePath path) {
            TreePath parentPath = path.getParentPath();
            if (parentPath == null) {
                return null;
            }
            Category parentC = (Category) ((DefaultMutableTreeNode) parentPath.getLastPathComponent()).getUserObject();
            CheckedNode cn = tree.nodesCheckingState.get(parentPath);
            if (!parentC.isMutuallyExclusive() && cn.isSelected) {
                return null;
            } else if (!parentC.isMutuallyExclusive()) {
                return isValidCheck(parentPath);
            }
            if (!cn.isSelected) {
                return null;
            }
            return parentC;
        }

        private boolean confirmCheck(TreePath tp) {
            CheckedNode cn = tree.nodesCheckingState.get(tp);
            if (!Options.INSTANCE.getShowConfirmPartiaclCategory()) {
                return true;
            }
            if (cn.isSelected && cn.hasChildren && !cn.allChildrenSelected) {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
                JLabel confirmLabel = new JLabel("Are you sure you wish to check/uncheck this entire category?");
                confirmLabel.setFont(this.tree.getFontInfo().getFont());
                panel.add(confirmLabel);
                JCheckBox check = new JCheckBox("Do not ask again");
                check.setFont(this.tree.getFontInfo().getFont());
                panel.add(check);
                AdHocDialog.Button res = AdHocDialog.run(MainGUI.INSTANCE,
                        this.tree.getFontInfo(),
                        AdHocDialog.IconType.QUESTION,
                        "Confirm",
                        panel,
                        AdHocDialog.ButtonSet.YES_NO);
                if (check.isSelected()) {
                    Options.INSTANCE.setShowConfirmPartiaclCategory(false);
                }
                return res == AdHocDialog.Button.YES;
            }
            return true;
        }

        private void handleRightClick(MouseEvent mouseEvent) {
            int x = mouseEvent.getX();
            int y = mouseEvent.getY();
            JPopupMenu menu = new JPopupMenu();
            for (RightMouseButtonAction action : actions) {
                if (action.couldBeEnabled()) {
                    if (action.isEnabled()) {
                        action.getButton().setEnabled(true);
                        action.getButton().setToolTipText(null);
                    } else {
                        action.getButton().setEnabled(false);
                        action.getButton().setToolTipText(action.getDisabledTooltip(RightMouseButtonAction.Requirements.NO_REQUIREMENTS));
                    }
                    Utilities.changeCTRLMasks(action.getButton());
                    menu.add(action.getButton());
                    if (action.isEnabled() && action instanceof CheckMarkRightMouseButtonAction) {
                        ((CheckMarkRightMouseButtonAction) action).update();
                    }
                }
            }
            if (menu.getComponentCount() > 0) {
                menu.show(tree, x, y);
            }
        }
    }

}
