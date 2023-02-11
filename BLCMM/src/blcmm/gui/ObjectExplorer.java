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

import blcmm.data.lib.DataManager;
import blcmm.gui.components.ForceClosingJFrame;
import blcmm.gui.components.VariableTabsTabbedPane;
import blcmm.gui.panels.ObjectExplorerPanel;
import blcmm.gui.panels.TextSearchDialog;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.PatchType;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import general.utilities.GlobalLogger;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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

    /**
     * Creates new form DumpFrame
     */
    public ObjectExplorer() {
        INSTANCE = this;
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
        jProgressBar1 = new javax.swing.JProgressBar();
        jTabbedPane1 = new OETabbedPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Object Explorer");

        jSplitPane1.setDividerLocation(400);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Package explorer"));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Select a class above");
        packageExplorerTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Class explorer"));

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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 375, Short.MAX_VALUE)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
        Object lastPathComponent = selectionPath.getLastPathComponent();
        String s = lastPathComponent.toString();
        s = s.contains(">") ? s.substring(s.lastIndexOf(">") + 1) : s;//Remove prepended HTML
        setPackageBrowserData(s);
    }//GEN-LAST:event_classExplorerTreeValueChanged

    private void packageExplorerTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_packageExplorerTreeValueChanged
        TreePath selectionPath = packageExplorerTree.getSelectionPath();
        if (selectionPath == null) {
            return;
        }
        Object[] path = selectionPath.getPath();
        if (path.length < 2) {
            return;//No class selected -> root of original tree selected
        }
        StringBuilder sb = new StringBuilder();
        sb.append(path[1]);
        for (int i = 2; i < path.length; i++) {
            ObjectPath path2 = (ObjectPath) ((DefaultMutableTreeNode) path[i]).getUserObject();
            sb.append(((i == path2.colon + 2) ? ":" : ".") + path2);
        }
        String object = sb.toString();
        GlobalLogger.log("Dumping " + object + " after selecting it in in the package tree");
        dump(new DumpOptions(object, false, false, false));
    }//GEN-LAST:event_packageExplorerTreeValueChanged

    public JTabbedPane getObjectExplorerTabbedPane() {
        return this.jTabbedPane1;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree classExplorerTree;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar1;
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
        panel.dump(options);
    }

    private void setClassBrowserData() {
        String root = "Object";
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(root);
        setClassBrowserData(root, node);
        sortNode(node);
        classExplorerTree.setModel(new DefaultTreeModel(node));
        classExplorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    private void setClassBrowserData(String root, DefaultMutableTreeNode node) {
        Collection<String> subclasses = DataManager.getDictionary().getSubclasses(root);
        for (String subclass : subclasses) {
            String prefix = "";
            if (!DataManager.isDataForClassOrAnySubclassPresent(subclass)) {
                prefix = "<html><font color=\"#D00000\">";
                continue;
            }
            DefaultMutableTreeNode node2 = new DefaultMutableTreeNode(prefix + subclass);

            node.insert(node2, node.getChildCount());
            setClassBrowserData(subclass, node2);
        }
        TitledBorder classborder = (TitledBorder) jPanel1.getBorder();
        classborder.setTitle("Class explorer - " + (DataManager.isBL2() ? PatchType.BL2.toString() : PatchType.TPS.toString()));
    }

    private void sortNode(DefaultMutableTreeNode node) {
        List<DefaultMutableTreeNode> list = Collections.list(node.children());
        Collections.sort(list, (node1, node2) -> {
            if (!node1.isLeaf() && node2.isLeaf()) {
                return -1;
            }
            if (node1.isLeaf() && !node2.isLeaf()) {
                return 1;
            }
            String s1 = node1.getUserObject().toString();
            String s2 = node2.getUserObject().toString();
            s1 = s1.contains(">") ? s1.substring(s1.lastIndexOf(">") + 1) : s1;
            s2 = s2.contains(">") ? s2.substring(s2.lastIndexOf(">") + 1) : s2;
            return s1.compareTo(s2);
        });
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : list) {
            node.insert(child, node.getChildCount());
        }
        for (DefaultMutableTreeNode child : list) {
            sortNode(child);
        }
    }

    private void setPackageBrowserData(String clazz) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        Set<String> prefixes = DataManager.getDictionary().getElementsInClassWithPrefix(clazz, "");
        Set<String> elements = DataManager.getDictionary().getDeepElementsInClassWithPrefix(clazz, "");
        if (elements.size() > 30000) {
            final String message = "<html>You selected a class (%s) which has a very large number of objects in it (%s).<br/>"
                    + "Creating the package explorer for this class can take a lot of time and memory, and <b><u><font color=\"#%s\">might even crash BLCMM!</font></u></b><br/><br/>"
                    + "Proceed anyway?";
            final String message2 = String.format(message, clazz, elements.size(), Integer.toHexString(ThemeManager.getColor(ThemeManager.ColorType.UINimbusAlertYellow).getRGB() & 0xffffff));
            int proceed = JOptionPane.showConfirmDialog(this,
                    message2, "Large class selected", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (proceed != JOptionPane.YES_OPTION) {
                return;
            }
        }
        jProgressBar1.setMaximum(elements.size());
        jProgressBar1.setValue(0);
        browserworker = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                ObjectExplorer.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                classExplorerTree.setEnabled(false);
                for (String prefix : prefixes) {
                    addPackageData(clazz, root, prefix, prefix, 0, -1);
                }
                packageExplorerTree.setModel(new DefaultTreeModel(root));
                packageExplorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                packageExplorerTree.setRootVisible(false);
                ObjectExplorer.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                classExplorerTree.setEnabled(true);
                return null;
            }
        };
        browserworker.execute();

    }

    private void addPackageData(String clazz, DefaultMutableTreeNode root, String prefix, String last, int depth, int colon) {

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ObjectPath(last, colon));
        root.insert(node, root.getChildCount());
        boolean added = false;
        for (String next : DataManager.getDictionary().getElementsInClassWithPrefix(clazz, prefix + ".")) {
            addPackageData(clazz, node, prefix + "." + next, next, depth + 1, colon);
            added = true;
        }
        for (String next : DataManager.getDictionary().getElementsInClassWithPrefix(clazz, prefix + ":")) {
            addPackageData(clazz, node, prefix + ":" + next, next, depth + 1, depth);
            added = true;
        }
        if (!added) {
            jProgressBar1.setValue(jProgressBar1.getValue() + 1);
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

    private static class OETabbedPane extends VariableTabsTabbedPane<ObjectExplorerPanel> {

        @Override
        protected ObjectExplorerPanel getDefaultNewComponent() {
            return new ObjectExplorerPanel();
        }

        @Override
        protected Component getDefaultComponentToFocus(ObjectExplorerPanel comp) {
            return ((ObjectExplorerPanel) comp).getQueryTextField();
        }

    }

    private static class ObjectPath {

        String display;
        int colon;

        public ObjectPath(String display, int colon) {
            this.display = display;
            this.colon = colon;
        }

        @Override
        public String toString() {
            return this.display;
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
}
