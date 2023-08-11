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
package blcmm.gui.tree.rightmouse;

import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.components.FontInfoJButton;
import blcmm.gui.components.FontInfoJMenuItem;
import blcmm.gui.components.ForceClosingJFrame;
import blcmm.gui.panels.EditPanel;
import blcmm.gui.tree.CheckBoxTree;
import blcmm.model.Category;
import blcmm.model.HotfixWrapper;
import blcmm.model.ModelElement;
import blcmm.model.PatchIO;
import blcmm.model.SetCommand;
import blcmm.utilities.CancelConfirmer;
import blcmm.utilities.InputValidator;
import blcmm.utilities.Options;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
public abstract class RightMouseButtonAction {

    private static ForceClosingJFrame windowAlreadyOpen = null;

    public static void bringEditWindowToFront() {
        if (windowAlreadyOpen != null) {
            windowAlreadyOpen.toFront();
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void closeEditWindow() {
        if (windowAlreadyOpen != null) {
            windowAlreadyOpen.dispose();
        }
    }

    public static boolean isShowingEditWindow() {
        return windowAlreadyOpen != null;
    }

    public static JFrame getOpenEditDialog() {
        return windowAlreadyOpen;
    }

    protected boolean isInputCategoryNameValid(String name) throws HeadlessException {
        if (name.trim().equals("")) {
            AdHocDialog.run(MainGUI.INSTANCE,
                    this.tree.getFontInfo(),
                    AdHocDialog.IconType.ERROR,
                    "Invalid category name",
                    "Category name may not be empty");
            return false;
        }
        if (name.contains("<") || name.contains(">")) {
            AdHocDialog.run(MainGUI.INSTANCE,
                    this.tree.getFontInfo(),
                    AdHocDialog.IconType.ERROR,
                    "Invalid category name",
                    "Category name may not contain '<' or '>'");
            return false;
        }
        String invalidname = null;
        for (String fName : PatchIO.FORBIDDEN_CATEGORY_NAMES) {
            if (name.trim().equals(fName)) {
                invalidname = fName;
                break;
            }
        }
        if (invalidname != null) {
            AdHocDialog.run(MainGUI.INSTANCE,
                    this.tree.getFontInfo(),
                    AdHocDialog.IconType.ERROR,
                    "Invalid category name",
                    "Category name may not equal '" + invalidname + "'");
            return false;
        }
        if (name.startsWith("/")) {
            AdHocDialog.run(MainGUI.INSTANCE,
                    this.tree.getFontInfo(),
                    AdHocDialog.IconType.ERROR,
                    "Invalid category name",
                    "Category name may not start with '/'");
            return false;
        }
        return true;
    }

    protected final CheckBoxTree tree;
    private final JMenuItem button;
    int hotkey = -1;
    boolean ctrl = false;
    private final Requirements reqs;

    protected RightMouseButtonAction(CheckBoxTree tree,
            String buttonName,
            Requirements reqs) {
        this(tree, buttonName, -1, false, reqs);
    }

    protected RightMouseButtonAction(CheckBoxTree tree,
            String buttonName,
            int hotkey,
            boolean ctrl,
            Requirements reqs) {
        this.tree = tree;
        this.button = new FontInfoJMenuItem(buttonName, tree.getFontInfo());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action();
                MainGUI.INSTANCE.requestFocus();
            }
        });
        this.hotkey = hotkey;
        this.ctrl = ctrl;
        if (hotkey != -1) {
            button.setAccelerator(javax.swing.KeyStroke.getKeyStroke(hotkey, ctrl ? java.awt.event.InputEvent.CTRL_DOWN_MASK : 0));
        }
        this.reqs = reqs;
    }

    protected RightMouseButtonAction(CheckBoxTree tree,
            JMenuItem button,
            Requirements reqs) {
        this.tree = tree;
        this.button = button;
        this.reqs = reqs;
    }

    public int getHotKey() {
        return hotkey;
    }

    public boolean getCTRL() {
        return ctrl;
    }

    public JMenuItem getButton() {
        return button;
    }

    public final boolean isEnabled() {
        return isEnabled(Requirements.NO_REQUIREMENTS);
    }

    public final boolean isEnabled(Requirements requirementsToIgnore) {
        if (!Options.INSTANCE.isInDeveloperMode() && (reqs.devmode && !requirementsToIgnore.devmode)) {
            return false;
        } else if (windowAlreadyOpen != null && reqs.window && !requirementsToIgnore.window) {
            return false;
        }

        if (reqs.unlocked && !requirementsToIgnore.unlocked) {
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) {
                return false;
            }
            DefaultMutableTreeNode node;
            for (TreePath path : paths) {
                node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (((ModelElement) node.getUserObject()).hasLockedAncestor()) {
                    return false;
                }
            }
        }
        return couldBeEnabled();
    }

    public final String getDisabledTooltip(Requirements requirementsToIgnore) {
        if (!Options.INSTANCE.isInDeveloperMode() && (reqs.devmode && !requirementsToIgnore.devmode)) {
            return "Enable developer mode to access this button";
        } else if (windowAlreadyOpen != null && reqs.window && !requirementsToIgnore.window) {
            return "The edit window is already open";
        }

        if (reqs.unlocked && !requirementsToIgnore.unlocked) {
            TreePath[] paths = tree.getSelectionPaths();
            if (paths == null) {
                return null;
            }
            DefaultMutableTreeNode node;
            for (TreePath path : paths) {
                node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (((ModelElement) node.getUserObject()).hasLockedAncestor()) {
                    return "This element is locked";
                }
            }
        }
        return null;
    }

    public abstract boolean couldBeEnabled();

    public abstract void action();

    protected void showCustomDialog(JPanel panel, ActionListener OKAction) {
        showCustomDialog(panel, OKAction, false, true);
    }

    protected void showCustomDialog(JPanel panel, ActionListener OKAction, boolean isEditWindow) {
        showCustomDialog(panel, OKAction, isEditWindow, true);
    }

    protected void showCustomDialog(JPanel panel, ActionListener OKAction, boolean isEditWindow, boolean allowEdit) {
        final ForceClosingJFrame dialog = new ForceClosingJFrame(allowEdit ? "Edit window" : "Edit window (readonly mode)");
        MainGUI.INSTANCE.setTreeEditable(false);
        dialog.add(panel);
        dialog.setDefaultCloseOperation(ForceClosingJFrame.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());
        dialog.add(panel, new GridBagConstraints(0, 0, 3, 1, 1, 1, GridBagConstraints.PAGE_START, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // Get rid of our searchDialog if it's open
                if (panel instanceof EditPanel) {
                    ((EditPanel) panel).disposeSearch();
                }
                // Save our window geometry between instances
                Options.INSTANCE.setEditWindowWidth(dialog.getWidth());
                Options.INSTANCE.setEditWindowHeight(dialog.getHeight());
                windowAlreadyOpen = null;
                MainGUI.INSTANCE.setTreeEditable(true);
                MainGUI.INSTANCE.forceFocus();
                if (isEditWindow) {
                    MainGUI.INSTANCE.registerEditWindowStatus(false);
                }
            }

            @Override
            public void windowOpened(WindowEvent we) {
                panel.requestFocus();
            }

        });

        // "OK" Button
        JButton button = new FontInfoJButton("OK", this.tree.getFontInfo());
        dialog.add(button, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
        ActionListener okListener;
        if (allowEdit) {
            okListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (panel instanceof InputValidator) {
                        ((InputValidator) panel).fixFixableInputs();
                        if (((InputValidator) panel).hasValidInput()) {
                            dialog.dispose();
                            OKAction.actionPerformed(e);
                        }
                    } else {
                        dialog.dispose();
                        OKAction.actionPerformed(e);
                    }
                }
            };
        } else {
            okListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            };
        }
        button.addActionListener(okListener);

        // We'd like to allow the master EditPanel to handle Ctrl-Enter,
        // if we happen to be using an EditPanel, so this is where
        // we're checking for that.  This is... rather Wrong?  It's
        // far from the correct way to do this, but I've hit roadblocks
        // with every other method I've tried, and since this one happens
        // to work, I'm sticking with it for now...
        if (panel instanceof EditPanel) {
            ((EditPanel) panel).setCtrlEnterAction(okListener);
        }

        // Cancel Button - Handled a bit differently so that ESC can trigger
        // the same action.
        JButton cbutton = new FontInfoJButton(this.tree.getFontInfo());
        dialog.add(cbutton, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
        AbstractAction cancelAction;
        if (allowEdit) {
            cancelAction = new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (panel instanceof CancelConfirmer) {
                        if (((CancelConfirmer) panel).userConfirmCancel()) {
                            dialog.dispose();
                        }
                    } else {
                        dialog.dispose();
                    }
                }
            };
        } else {
            cancelAction = new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            };
        }
        cancelAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("ESCAPE"));
        cbutton.setAction(cancelAction);
        cbutton.getActionMap().put("cancelButtonAction", cancelAction);
        cbutton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                (KeyStroke) cancelAction.getValue(Action.ACCELERATOR_KEY),
                "cancelButtonAction"
        );

        JPanel dummy = new JPanel();
        dialog.add(dummy, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        dialog.setIconImages(MainGUI.INSTANCE.getIconImages());
        dialog.pack();
        dialog.setSize(
                Options.INSTANCE.getEditWindowWidth(),
                Options.INSTANCE.getEditWindowHeight());
        dialog.setLocationRelativeTo(MainGUI.INSTANCE);
        windowAlreadyOpen = dialog;
        if (isEditWindow) {
            MainGUI.INSTANCE.registerEditWindowStatus(true);
        }
        dialog.setVisible(true);
    }

    protected void addNewElements(EditPanel panel, Category parentCategory, int insertIndex, DefaultMutableTreeNode parentnode, Boolean wasselected) {
        addNewElements(panel.getElements(), parentCategory, insertIndex, parentnode, wasselected);
    }

    protected void addNewElements(ModelElement el, Category parentCategory, int insertIndex, DefaultMutableTreeNode parentnode, Boolean wasselected) {
        addNewElements(new ModelElement[]{el}, parentCategory, insertIndex, parentnode, wasselected);
    }

    protected void addNewElements(ModelElement[] els, Category parentCategory, int insertIndex, DefaultMutableTreeNode parentnode, Boolean wasselected) {
        addNewElements(Arrays.asList(els), parentCategory, insertIndex, parentnode, wasselected);
    }

    protected void addNewElements(List<ModelElement> elements, Category parentCategory, int insertIndex, DefaultMutableTreeNode parentnode, Boolean wasselected) {
        boolean selected;
        if (wasselected != null) {
            selected = wasselected;
        } else {
            selected = true;
        }
        for (int i = 0; i < elements.size(); i++) {
            ModelElement el = elements.get(i);
            HashSet<SetCommand> coms = new HashSet<>();
            if (el instanceof SetCommand) {
                coms.add((SetCommand) el);
            } else if (el instanceof HotfixWrapper) {
                for (SetCommand el2 : ((HotfixWrapper) el).getElements()) {
                    coms.add(el2);
                }
            }
            tree.getPatch().insertElementInto(el, parentCategory, i + insertIndex);
            for (SetCommand s : coms) {
                tree.getPatch().setSelected(s, selected);
            }
        }
        MakeNodeRootOfCategory(parentnode, parentCategory);
    }

    protected void refreshNode(DefaultMutableTreeNode parentNode) {
        MakeNodeRootOfCategory(parentNode, (Category) parentNode.getUserObject());
    }

    protected void MakeNodeRootOfCategory(DefaultMutableTreeNode parentnode, Category parentCategory) {
        assert parentnode.getUserObject() == parentCategory;
        HashSet<Object> expandedElements = new HashSet<>();
        Enumeration affectedNodes = parentnode.preorderEnumeration();
        while (affectedNodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) affectedNodes.nextElement();
            if (tree.isExpanded(new TreePath(node.getPath()))) {
                expandedElements.add(node.getUserObject());
            }
        }
        parentnode.removeAllChildren();
        DefaultMutableTreeNode newP = CheckBoxTree.createTree(parentCategory);
        int count = newP.getChildCount();
        for (int i = 0; i < count; i++) {
            parentnode.insert((DefaultMutableTreeNode) newP.getChildAt(0), i);
        }
        tree.getModel().nodeStructureChanged(parentnode);

        affectedNodes = parentnode.preorderEnumeration();
        while (affectedNodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) affectedNodes.nextElement();
            if (expandedElements.contains(node.getUserObject())) {
                tree.expandPath(new TreePath(node.getPath()));
            }
        }
        tree.expandPath(new TreePath(parentnode.getPath()));
        tree.setChanged(true);
    }

    /**
     * A class to define "basic" requirements for a RightMouseButtonAction to
     * be available to the user.  There are currently three booleans:
     *
     *   1. devmode: True if developer mode is required for the action, or
     *      False if it's an action which can be done whenever.
     *   2. unlocked: True if the selected element must be unlocked, or False
     *      if the action can be taken on a locked element.
     *   3. window: True if the action can't be performed while a code edit
     *      window is open, or False if it can be done at any time.  Honestly
     *      I actually have no idea what this even means, really, because the
     *      tree gets locked whenever the edit window's open anyway, so I don't
     *      think *any* of our actions are usable with an open edit dialog?
     *      Possibly this requirement predates disabling the tree?
     */
    public static class Requirements {

        public static final Requirements NO_REQUIREMENTS = new Requirements(false, false, false);

        private final boolean devmode, unlocked, window;

        public Requirements(boolean devmode, boolean unlocked, boolean window) {
            this.devmode = devmode;
            this.unlocked = unlocked;
            this.window = window;
        }

    }
}
