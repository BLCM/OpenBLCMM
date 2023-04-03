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
package blcmm.gui;

import blcmm.data.lib.DataManager;
import blcmm.data.lib.DataManagerManager;
import blcmm.data.lib.UEClass;
import blcmm.data.lib.UEObject;
import blcmm.gui.components.ForceClosingJFrame;
import blcmm.gui.components.SimpleGameSelectionComboBox;
import blcmm.gui.components.VariableTabsTabbedPane;
import blcmm.gui.panels.ObjectExplorerPanel;
import blcmm.gui.panels.TextSearchDialog;
import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
    private DataManager dm;

    /**
     * Creates new form DumpFrame
     *
     * @param dmm The DataManagerManager to use for this instance of Object Explorer
     */
    public ObjectExplorer(DataManagerManager dmm) {
        INSTANCE = this;
        this.dmm = new DataManagerManager(dmm);
        this.dm = null;
        GlobalLogger.log("Opened Object Explorer");
        initComponents();

        // Set up our game selection dropdown
        getGameSelectionComboBox().addItemListenerToComboBox(this::gameSelectionAction);
        getGameSelectionComboBox().setType(this.dmm.getCurrentPatchType());

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
        HideLeftHandPanelsAdapter adap = new HideLeftHandPanelsAdapter();
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) topLevelSplitPane.getUI()).getDivider();
        divider.addMouseListener(adap);
        if (!Options.INSTANCE.getOELeftPaneVisible()) {
            adap.toggle();
        }

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

        topLevelSplitPane = new javax.swing.JSplitPane();
        leftHandPanel = new javax.swing.JPanel();
        gameTypeComboBox = new SimpleGameSelectionComboBox();
        leftHandSplitPlane = new javax.swing.JSplitPane();
        classBrowserPanel = new javax.swing.JPanel();
        classBrowserScrollPane = new javax.swing.JScrollPane();
        classBrowserTree = new javax.swing.JTree();
        objectBrowserPanel = new javax.swing.JPanel();
        objectBrowserScrollPane = new javax.swing.JScrollPane();
        objectBrowserTree = new javax.swing.JTree();
        oePanelTabbedPane = new OETabbedPane();

        setTitle("Object Explorer");

        topLevelSplitPane.setDividerLocation(400);

        leftHandPanel.setLayout(new javax.swing.BoxLayout(leftHandPanel, javax.swing.BoxLayout.Y_AXIS));

        gameTypeComboBox.setMinimumSize(new java.awt.Dimension(0, 24));
        leftHandPanel.add(gameTypeComboBox);

        leftHandSplitPlane.setDividerLocation(355);
        leftHandSplitPlane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        classBrowserPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Class Browser"));

        classBrowserTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                classBrowserTreeValueChanged(evt);
            }
        });
        classBrowserScrollPane.setViewportView(classBrowserTree);

        javax.swing.GroupLayout classBrowserPanelLayout = new javax.swing.GroupLayout(classBrowserPanel);
        classBrowserPanel.setLayout(classBrowserPanelLayout);
        classBrowserPanelLayout.setHorizontalGroup(
            classBrowserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(classBrowserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(classBrowserScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addContainerGap())
        );
        classBrowserPanelLayout.setVerticalGroup(
            classBrowserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(classBrowserPanelLayout.createSequentialGroup()
                .addComponent(classBrowserScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
                .addContainerGap())
        );

        leftHandSplitPlane.setTopComponent(classBrowserPanel);

        objectBrowserPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Object Browser"));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        objectBrowserTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        objectBrowserTree.setRootVisible(false);
        objectBrowserTree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
                objectBrowserTreeTreeWillExpand(evt);
            }
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {
            }
        });
        objectBrowserTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                objectBrowserTreeValueChanged(evt);
            }
        });
        objectBrowserScrollPane.setViewportView(objectBrowserTree);

        javax.swing.GroupLayout objectBrowserPanelLayout = new javax.swing.GroupLayout(objectBrowserPanel);
        objectBrowserPanel.setLayout(objectBrowserPanelLayout);
        objectBrowserPanelLayout.setHorizontalGroup(
            objectBrowserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(objectBrowserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(objectBrowserScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                .addContainerGap())
        );
        objectBrowserPanelLayout.setVerticalGroup(
            objectBrowserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(objectBrowserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(objectBrowserScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE)
                .addContainerGap())
        );

        leftHandSplitPlane.setBottomComponent(objectBrowserPanel);

        leftHandPanel.add(leftHandSplitPlane);

        topLevelSplitPane.setLeftComponent(leftHandPanel);
        topLevelSplitPane.setRightComponent(oePanelTabbedPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topLevelSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1102, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(topLevelSplitPane)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void classBrowserTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_classBrowserTreeValueChanged
        UEClass selected = this.getCurrentClassSelection();
        if (selected != null) {
            setPackageBrowserData(selected);
        }
    }//GEN-LAST:event_classBrowserTreeValueChanged

    private void objectBrowserTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_objectBrowserTreeValueChanged
        if (this.dm == null) {
            return;
        }
        TreePath selectionPath = objectBrowserTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }
        Object selectedObject = ((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
        if (!(selectedObject instanceof UEObject)) {
            return;
        }
        String objectName = ((UEObject)selectedObject).getName();
        dump(new DumpOptions(objectName, false, false, false));
    }//GEN-LAST:event_objectBrowserTreeValueChanged

    private void objectBrowserTreeTreeWillExpand(javax.swing.event.TreeExpansionEvent evt)throws javax.swing.tree.ExpandVetoException {//GEN-FIRST:event_objectBrowserTreeTreeWillExpand
        // We're never going to deny the tree expansion, but I did want this to happen *before* the
        // expansion actually happens, just so there's no flickering or whatever as the "dummy"
        // entry is replaced by the actual content, etc.

        // First get our selected class
        // TODO: Maybe that should live inside UEObject, actually.  Whatever, for now this will do.
        TreePath classSelectionPath = classBrowserTree.getSelectionPath();
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
    }//GEN-LAST:event_objectBrowserTreeTreeWillExpand

    public JTabbedPane getObjectExplorerTabbedPane() {
        return this.oePanelTabbedPane;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel classBrowserPanel;
    private javax.swing.JScrollPane classBrowserScrollPane;
    private javax.swing.JTree classBrowserTree;
    private javax.swing.JComboBox<String> gameTypeComboBox;
    private javax.swing.JPanel leftHandPanel;
    private javax.swing.JSplitPane leftHandSplitPlane;
    private javax.swing.JPanel objectBrowserPanel;
    private javax.swing.JScrollPane objectBrowserScrollPane;
    private javax.swing.JTree objectBrowserTree;
    private javax.swing.JTabbedPane oePanelTabbedPane;
    private javax.swing.JSplitPane topLevelSplitPane;
    // End of variables declaration//GEN-END:variables


    /**
     * The action to take when the game selection dropdown is changed (ie:
     * reloading the browser trees, and sending the signal to the other
     * panel to do any work there.
     *
     * @param e The event that triggered the action.
     */
    private void gameSelectionAction(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            UEClass currentClassSelection = this.getCurrentClassSelection();
            PatchType type = getGameSelectionComboBox().getNonNullGameType();
            this.dmm.setPatchType(type);
            this.dm = this.dmm.getCurrentDataManager();
            if (currentClassSelection != null) {
                DefaultMutableTreeNode foundNode = setClassBrowserData(currentClassSelection.getName());
                setPackageBrowserData(null);
                if (foundNode != null) {
                    ArrayList<TreeNode> nodes = new ArrayList<>();
                    nodes.add(foundNode);
                    TreeNode treeNode = foundNode.getParent();
                    while (treeNode != null) {
                        nodes.add(0, treeNode);
                        treeNode = treeNode.getParent();
                    }
                    TreePath selectedPath = new TreePath(nodes.toArray());
                    classBrowserTree.scrollPathToVisible(selectedPath);
                    classBrowserTree.setSelectionPath(selectedPath);
                }
            } else {
                setClassBrowserData();
                setPackageBrowserData(null);
            }
            ObjectExplorerPanel panel = (ObjectExplorerPanel) oePanelTabbedPane.getComponentAt(oePanelTabbedPane.getSelectedIndex());
            panel.updateGame();
        }
    }


    /**
     * Returns the GameSelectionComboBox UI component
     *
     * @return The game selection panel
     */
    private SimpleGameSelectionComboBox getGameSelectionComboBox() {
        return (SimpleGameSelectionComboBox) gameTypeComboBox;
    }

    /**
     * Returns the PatchType currently selected by our game dropdown. Just a
     * convenience function to avoid repetitive typing, etc.
     *
     * @return The PatchType currently selected
     */
    private PatchType getSelectedGame() {
        return getGameSelectionComboBox().getNonNullGameType();
    }

    /**
     * Gets the current UEClass selected by the Class Browser.  Used to persist
     * that selection when the user switches games.
     *
     * @return The currently-selected UEClass
     */
    private UEClass getCurrentClassSelection() {
        if (this.dm == null) {
            return null;
        }
        TreePath selectionPath = classBrowserTree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        return (UEClass)((DefaultMutableTreeNode)selectionPath.getLastPathComponent()).getUserObject();
    }

    public void dump(DumpOptions options) {
        if (options.createNewTabForResult) {
            oePanelTabbedPane.setSelectedIndex(oePanelTabbedPane.getTabCount() - 1);//This will turn the "+" tab into a new tab
        }
        ObjectExplorerPanel panel = (ObjectExplorerPanel) oePanelTabbedPane.getComponentAt(oePanelTabbedPane.getSelectedIndex());
        boolean success = panel.dump(this.dmm.getCurrentDataManager(), options);
    }

    /**
     * Populates the main "Class Browser" panel, which contains a tree of all
     * classes in the specified game.
     */
    private void setClassBrowserData() {
        this.setClassBrowserData(null);
    }

    /**
     * Populates the main "Class Browser" panel, which contains a tree of all
     * classes in the specified game, while also looking for a specific class
     * while building.  This is used by the game-selection dropdown so that
     * if a user has a class selected, that same class can be selected when
     * the new game's tree is populated
     *
     * @param classToLookFor The classname to look for while building the tree
     * @return The TreeNode containing the class, in the new tree
     */
    private DefaultMutableTreeNode setClassBrowserData(String classToLookFor) {
        ArrayList<DefaultMutableTreeNode> foundNodes = new ArrayList<>();
        TitledBorder classborder = (TitledBorder) classBrowserPanel.getBorder();
        classborder.setTitle("Class Browser - " + this.dmm.getCurrentPatchType().toString());
        classBrowserPanel.repaint();
        DefaultMutableTreeNode node;
        if (this.dm == null) {
            node = new DefaultMutableTreeNode("No Data Present");
        } else {
            node = this.buildClassTree(this.dm.getRootClass(), classToLookFor, foundNodes);
            sortNode(node);
        }
        classBrowserTree.setModel(new DefaultTreeModel(node));
        classBrowserTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        classBrowserScrollPane.getVerticalScrollBar().setMaximum(500);
        if (!foundNodes.isEmpty()) {
            return foundNodes.get(0);
        } else {
            return null;
        }
    }

    /**
     * Recursively loops through our UEClass data structure to build out the
     * Class Browser tree data.  Will also keep an eye out for a specific class
     * name, if classToLookFor is not null, and report which TreeNodes were
     * built for class(es) which match that name.  (In practice that should
     * only ever be a single node.)
     *
     * @param root The root UEClass node to recurse from
     * @param classToLookFor The classname to look for while building the node
     * @param foundNodes A structure used for reporting which nodes matched classToLookFor
     * @return A DefaultMutableTreeNode which can be added to the tree
     */
    private DefaultMutableTreeNode buildClassTree(UEClass root, String classToLookFor, ArrayList<DefaultMutableTreeNode> foundNodes) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);
        if (classToLookFor != null && root.getName().equalsIgnoreCase(classToLookFor)) {
            foundNodes.add(node);
        }
        for (UEClass child : root.getChildren()) {
            node.insert(this.buildClassTree(child, classToLookFor, foundNodes), node.getChildCount());
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
        TitledBorder objectborder = (TitledBorder) objectBrowserPanel.getBorder();
        if (this.dm == null) {
            objectborder.setTitle("Object Browser");
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("No Data Present");
            objectBrowserTree.setRootVisible(true);
            objectBrowserTree.setModel(new DefaultTreeModel(root));
        } else if (ueClass == null) {
            objectborder.setTitle("Object Browser");
            objectBrowserPanel.repaint();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
            root.add(new DefaultMutableTreeNode("Select a class above to list objects in that class."));
            root.add(new DefaultMutableTreeNode("Choosing \"Object\" will give you the full object tree."));
            root.add(new DefaultMutableTreeNode("Most mod-useful classes are inside GBXDefinition."));
            objectBrowserTree.setRootVisible(false);
            objectBrowserTree.setModel(new DefaultTreeModel(root));
            objectBrowserTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        } else {
            objectborder.setTitle("Object Browser - " + ueClass.getName());
            objectBrowserPanel.repaint();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
            this.addPackageData(ueClass, root);
            objectBrowserTree.setRootVisible(false);
            objectBrowserTree.setModel(new DefaultTreeModel(root));
            objectBrowserTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        }
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

    /**
     * An adapter to hide/show the left-hand panels in the OE window
     */
    private class HideLeftHandPanelsAdapter extends MouseAdapter {

        int pos = 300;

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getClickCount() == 2) {
                toggle();
            }
        }

        public void toggle() {
            if (leftVisible) {
                pos = topLevelSplitPane.getDividerLocation();
                topLevelSplitPane.remove(leftHandPanel);
            } else {
                topLevelSplitPane.setLeftComponent(leftHandPanel);
                topLevelSplitPane.setDividerLocation(pos);
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
