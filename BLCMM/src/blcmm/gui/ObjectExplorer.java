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
package blcmm.gui;

import blcmm.data.lib.DataManagerManager;
import blcmm.data.lib.UEClass;
import blcmm.data.lib.UEObject;
import blcmm.gui.components.ForceClosingJFrame;
import blcmm.gui.components.VariableTabsTabbedPane;
import blcmm.gui.panels.ObjectExplorerPanel;
import blcmm.gui.panels.TextSearchDialog;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public final class ObjectExplorer extends ForceClosingJFrame {

    public static ObjectExplorer INSTANCE = null;
    boolean init = false;
    boolean leftVisible = true;
    private boolean startedMaximized = false;
    private SwingWorker browserworker;
    private TextSearchDialog searchDialog = null;
    private DataManagerManager dmm;

    /**
     * Creates new form DumpFrame
     *
     * @param dmm The DataManagerManager to use for this instance of Object Explorer
     */
    public ObjectExplorer(DataManagerManager dmm) {
        INSTANCE = this;
        this.dmm = new DataManagerManager(dmm);
        GlobalLogger.log("Opened Object Explorer");
        initComponents();

        // Resize the main window from our preferences
        if (Options.INSTANCE.getOEWindowMaximized()) {
            // If we start maximized, the user's window manager won't have any
            // idea what size to make the window if it un-maximizes, which on
            // some systems would leave it the same size as the desktop.  So
            // we'll keep track of how we started and manually resize if
            // necessary.
            startedMaximized = true;
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            setSize(Options.INSTANCE.getOEWindowWidth(),
                    Options.INSTANCE.getOEWindowHeight());
        }
        setLocationRelativeTo(MainGUI.INSTANCE);

        // Window state listener, to resize from maximized properly
        this.addWindowStateListener(e -> {
            if (e.getNewState() == JFrame.NORMAL && e.getOldState() == JFrame.MAXIMIZED_BOTH) {
                if (startedMaximized) {
                    setSize((int) Options.INSTANCE.getOption(Options.OptionNames.oeWindowWidth).getDefaultData(),
                            (int) Options.INSTANCE.getOption(Options.OptionNames.oeWindowHeight).getDefaultData()
                    );
                    setLocationRelativeTo(MainGUI.INSTANCE);

                    // Only do this once, since the user's window manager
                    // will remember from now on.
                    startedMaximized = false;
                }
            }
        });

        // Close OE on ctrl-Q
        this.getRootPane().getActionMap().put("Quit", new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control Q"), "Quit");

        setIconImages(MainGUI.INSTANCE.getIconImages());
        MainGUI.INSTANCE.registerObjectExplorerWindowStatus(true);
        init = true;
        MyAdapter adap = new MyAdapter();
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) jSplitPane1.getUI()).getDivider();
        divider.addMouseListener(adap);
        if (!Options.INSTANCE.getOELeftPaneVisible()) {
            adap.toggle();
        }

        setClassBrowserData();

        Utilities.changeCTRLMasks(this.getRootPane());
        MainGUI.INSTANCE.cursorNormal();
    }

    @Override
    public void dispose() {

        // Save window geometry between runs
        Options.INSTANCE.setOEWindowWidth(getWidth());
        Options.INSTANCE.setOEWindowHeight(getHeight());
        Options.INSTANCE.setOEWindowMaximized(
                (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH
        );
        INSTANCE = null;
        if (this.searchDialog != null) {
            this.searchDialog.dispose();
            this.searchDialog = null;
        }
        MainGUI.INSTANCE.registerObjectExplorerWindowStatus(false);
        if (browserworker != null) {
            browserworker.cancel(true);
        }
        super.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        packageExplorerTree = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        classExplorerTree = new javax.swing.JTree();
        jTabbedPane1 = new OETabbedPane();

        setTitle("Object Explorer");

        jSplitPane1.setDividerLocation(400);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Object Browser"));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Select a class above to list objects in that class.");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Choosing \"Object\" will give you the full object tree.");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Most mod-useful classes are inside GBXDefinition.");
        treeNode1.add(treeNode2);
        packageExplorerTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        packageExplorerTree.setRootVisible(false);
        packageExplorerTree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
                packageExplorerTreeTreeWillExpand(evt);
            }
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
            }
        });
        packageExplorerTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                packageExplorerTreeValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(packageExplorerTree);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Class Browser"));

        classExplorerTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                classExplorerTreeValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(classExplorerTree);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addGap(4, 4, 4))
        );

        jSplitPane2.setTopComponent(jPanel1);

        jSplitPane1.setLeftComponent(jSplitPane2);
        jSplitPane1.setRightComponent(jTabbedPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1102, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void classExplorerTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_classExplorerTreeValueChanged
        TreePath selectionPath = classExplorerTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }
        UEClass selected = (UEClass)((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
        setPackageBrowserData(selected);
    }//GEN-LAST:event_classExplorerTreeValueChanged

    private void packageExplorerTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_packageExplorerTreeValueChanged
        TreePath selectionPath = packageExplorerTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }
        Object selectedObject = ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
        if (!(selectedObject instanceof UEObject)) {
            return;
        }
        String objectName = ((UEObject)selectedObject).getName();
        dump(new DumpOptions(objectName, false, false, false));
    }//GEN-LAST:event_packageExplorerTreeValueChanged

    private void packageExplorerTreeTreeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {//GEN-FIRST:event_packageExplorerTreeTreeWillExpand
        // We're never going to deny the tree expansion, but I did want this to happen *before* the
        // expansion actually happens, just so there's no flickering or whatever as the "dummy"
        // entry is replaced by the actual content, etc.

        // First get our selected class
        // TODO: Maybe that should live inside UEObject, actually.  Whatever, for now this will do.
        TreePath classSelectionPath = classExplorerTree.getSelectionPath();
        if (classSelectionPath == null) {
            return;
        }
        UEClass selectedClass = (UEClass)((DefaultMutableTreeNode)classSelectionPath.getLastPathComponent()).getUserObject();

        DefaultMutableTreeNode expandingNode = (DefaultMutableTreeNode)evt.getPath().getLastPathComponent();
        Object expandingUserObject = expandingNode.getUserObject();
        if (expandingUserObject instanceof UEObject) {
            UEObject expandingObject = (UEObject)(expandingNode).getUserObject();
            if (!expandingObject.isExpanded()) {
                this.addPackageData(selectedClass, expandingNode);
                expandingObject.setExpanded(true);
            }
        }
    }//GEN-LAST:event_packageExplorerTreeTreeWillExpand

    public JTabbedPane getObjectExplorerTabbedPane() {
        return this.jTabbedPane1;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree classExplorerTree;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTree packageExplorerTree;
    // End of variables declaration//GEN-END:variables

    public void dump(DumpOptions options) {
        if (options.createNewTabForResult) {
            jTabbedPane1.setSelectedIndex(jTabbedPane1.getTabCount() - 1);//This will turn the "+" tab into a new tab
        }
        ObjectExplorerPanel panel = (ObjectExplorerPanel) jTabbedPane1.getComponentAt(jTabbedPane1.getSelectedIndex());
        boolean success = panel.dump(this.dmm.getCurrentDataManager(), options);
    }

    /**
     * Populates the main "Class Browser" panel, which contains a tree of all
     * classes in the specified game.
     */
    private void setClassBrowserData() {
        TitledBorder classborder = (TitledBorder) jPanel1.getBorder();
        classborder.setTitle("Class Browser - " + this.dmm.getCurrentPatchType().toString());
        DefaultMutableTreeNode node = this.buildClassTree(this.dmm.getCurrentDataManager().getRootClass());
        sortNode(node);
        classExplorerTree.setModel(new DefaultTreeModel(node));
        classExplorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * Recursively loops through our UEClass data structure to build out the
     * Class Browser tree data
     * @param root The root UEClass node to recurse from
     * @return A DefaultMutableTreeNode which can be added to the tree
     */
    private DefaultMutableTreeNode buildClassTree(UEClass root) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);
        for (UEClass child : root.getChildren()) {
            node.insert(this.buildClassTree(child), node.getChildCount());
        }
        return node;
    }

    private void sortNode(DefaultMutableTreeNode node) {
        List<TreeNode> list = Collections.list(node.children());
        Collections.sort(list, (node1, node2) -> {
            String s1 = ((DefaultMutableTreeNode)node1).getUserObject().toString();
            String s2 = ((DefaultMutableTreeNode)node2).getUserObject().toString();
            return s1.compareToIgnoreCase(s2);
        });
        node.removeAllChildren();
        for (TreeNode child : list) {
            node.insert((DefaultMutableTreeNode)child, node.getChildCount());
        }
        for (TreeNode child : list) {
            sortNode((DefaultMutableTreeNode)child);
        }
    }

    private void setPackageBrowserData(UEClass ueClass) {
        TitledBorder objectborder = (TitledBorder) jPanel2.getBorder();
        objectborder.setTitle("Object Browser - " + ueClass.getName());
        jPanel2.repaint();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        this.addPackageData(ueClass, root);
        packageExplorerTree.setRootVisible(false);
        packageExplorerTree.setModel(new DefaultTreeModel(root));
        packageExplorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    private void addPackageData(UEClass ueClass, DefaultMutableTreeNode root) {
        List<UEObject> packages;
        Object rootObject = root.getUserObject();
        if (rootObject instanceof UEObject) {
            root.removeAllChildren();
            packages = this.dmm.getCurrentDataManager().getTreeObjectsFromClass(ueClass, (UEObject)rootObject);
        } else {
            packages = this.dmm.getCurrentDataManager().getTreeObjectsFromClass(ueClass);
        }
        DefaultMutableTreeNode newNode;
        for (UEObject pack : packages) {
            newNode = new DefaultMutableTreeNode(pack);
            if (pack.getHasChildrenForClass()) {
                newNode.insert(new DefaultMutableTreeNode("dummy"), newNode.getChildCount());
            }
            root.insert(newNode, root.getChildCount());
        }

    }

    /**
     * Adds a TextSearchDialog to ourselves, using the given textElement as its
     * target. If we already have a search dialog, this will just update its
     * textElement.
     *
     * @param textElement The element we'll be searching on
     */
    public void addSearch(JTextComponent textElement) {
        textElement.getActionMap().put("Search", new AbstractAction("Search") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (searchDialog == null || !searchDialog.isDisplayable()) {
                    searchDialog = new TextSearchDialog(INSTANCE, textElement, "", false);
                    searchDialog.setVisible(true);
                } else {
                    searchDialog.requestFocus();
                }
            }
        });
        textElement.getActionMap().put("Replace", new AbstractAction("Replace") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (searchDialog == null || !searchDialog.isDisplayable()) {
                    searchDialog = new TextSearchDialog(INSTANCE, textElement, "", true);
                    searchDialog.setVisible(true);
                } else {
                    searchDialog.requestFocus();
                }
            }
        });
        textElement.getInputMap().put(KeyStroke.getKeyStroke("control F"), "Search");
        textElement.getInputMap().put(KeyStroke.getKeyStroke("control H"), "Replace");
        this.updateSearch(textElement);
    }

    /**
     * Updates our search element, if possible, to the new text element.
     *
     * @param textElement
     */
    public void updateSearch(JTextComponent textElement) {
        if (searchDialog != null && searchDialog.isDisplayable()) {
            searchDialog.updateTextComponent(textElement);
        }
    }

    public static class DumpOptions {

        public final String objectToDump;

        //These default values are always overwritten, but these are the values given to the fields by the default constructors.
        public boolean searchWhenCantFindObject = true;
        public boolean createNewTabForResult = false;
        public boolean createLogEntry = true;

        public DumpOptions(String objectToDump) {
            this(objectToDump, true);
        }

        public DumpOptions(String objectToDump, boolean searchWhenCantFindObject) {
            this(objectToDump, searchWhenCantFindObject, false);
        }

        public DumpOptions(String objectToDump, boolean searchWhenCantFindObject, boolean createNewTabForResult) {
            this(objectToDump, searchWhenCantFindObject, createNewTabForResult, true);
        }

        public DumpOptions(String objectToDump, boolean searchWhenCantFindObject, boolean createNewTabForResult, boolean createLogEntry) {
            this.objectToDump = objectToDump;
            this.searchWhenCantFindObject = searchWhenCantFindObject;
            this.createNewTabForResult = createNewTabForResult;
            this.createLogEntry = createLogEntry;
        }

    }

    private class OETabbedPane extends VariableTabsTabbedPane<ObjectExplorerPanel> {

        @Override
        protected ObjectExplorerPanel getDefaultNewComponent() {
            return new ObjectExplorerPanel(dmm);
        }

        @Override
        protected Component getDefaultComponentToFocus(ObjectExplorerPanel comp) {
            return ((ObjectExplorerPanel) comp).getQueryTextField();
        }

    }

    private class MyAdapter extends MouseAdapter {

        int pos = 300;

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getClickCount() == 2) {
                toggle();
            }
        }

        public void toggle() {
            if (leftVisible) {
                pos = jSplitPane1.getDividerLocation();
                jSplitPane1.remove(jSplitPane2);
            } else {
                jSplitPane1.setLeftComponent(jSplitPane2);
                jSplitPane1.setDividerLocation(pos);
            }
            leftVisible = !leftVisible;
            Options.INSTANCE.setOELeftPaneVisible(leftVisible);
        }
    }

    public void cursorWait() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void cursorNormal() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

}
