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
package blcmm.gui.panels;

import blcmm.gui.MainGUI;
import blcmm.gui.components.EnhancedFormattedTextField;
import blcmm.gui.components.InfoLabel;
import blcmm.gui.text.AutoCompleteAttacher;
import blcmm.gui.text.HighlightedTextArea;
import blcmm.model.Category;
import blcmm.model.Comment;
import blcmm.model.CompletePatch;
import blcmm.model.HotfixCommand;
import blcmm.model.HotfixType;
import blcmm.model.HotfixWrapper;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.PatchIO;
import blcmm.model.PatchType;
import blcmm.model.PatchType.BLMap;
import blcmm.model.SetCMPCommand;
import blcmm.model.SetCommand;
import blcmm.model.TransientModelData;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.model.properties.PropertyChecker;
import blcmm.utilities.CancelConfirmer;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.InputValidator;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
public class EditPanel extends javax.swing.JPanel implements InputValidator, CancelConfirmer {

    private final HighlightedTextArea textElement;
    private final JSpinner deformatSpinner;

    private String initialText;
    private final boolean commentsOnly;
    private boolean hotfix;
    private final boolean allowEdit;
    private final PatchType patchType;
    private final Collection<String> levelnames;
    private final Collection<String> packages;
    private TextSearchDialog searchDialog = null;

    /**
     * Creates new form HotfixPanel
     *
     * @param patch
     * @param parent
     * @param elements
     * @param allowEdit
     * @param disallowEditReason
     */
    public EditPanel(CompletePatch patch, Category parent, List<ModelElement> elements, boolean allowEdit, String disallowEditReason) {
        this.commentsOnly = parent.isMutuallyExclusive();
        this.allowEdit = allowEdit;
        this.patchType = patch.getType();
        packages = patch.getType().getOnDemandPackages();
        //packages = patch.getType().getOnDemandPackages().stream().map(s -> s.getStreamingPackage()).collect(Collectors.toList());
        levelnames = new HashSet<>();
        this.patchType.getLevels().forEach(map -> levelnames.add(map.code));

        initComponents();
        textElement = new HighlightedTextArea(MainGUI.INSTANCE.getDMM(), true, allowEdit);
        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(textElement);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(16);
        for (Component c : super.getComponents()) {
            setProperFontSize(c);
        }

        deformatSpinner = new JSpinner();
        deformatSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 0, null, 1));
        Dimension d = new Dimension(deformatButton.getPreferredSize().width + deformatSpinner.getPreferredSize().width, deformatButton.getPreferredSize().height + deformatButton.getMargin().top);
        deformatButton.setLayout(new BorderLayout());
        deformatButton.add(deformatSpinner, BorderLayout.EAST);
        deformatButton.setMargin(new Insets(0, 0, 0, 0));
        deformatButton.setBorder(null);
        deformatButton.setMaximumSize(d);
        deformatButton.setMinimumSize(d);
        deformatButton.setPreferredSize(d);
        hotfixTypeCombobox.setModel(new DefaultComboBoxModel<>(HotfixType.values()));
        // Make sure that our first call to hotfixCheckboxStateChanged updates
        // the GUI properly.
        hotfix = !hotfixCheckbox.isSelected();
        hotfixCheckboxItemStateChanged(null);

        Utilities.makeWindowOfComponentResizable(EditPanel.this);
        analyzeInput(elements);
        textElement.setCaretPosition(0);
        if (allowEdit) {
            attachAutoComplete();
            textElement.setProcessUndo(true);
        } else {
            if (disallowEditReason == null) {
                jLabel2.setText("<html><b>Read-Only Mode</b>");
            } else {
                jLabel2.setText("<html><b>" + disallowEditReason + "</b>");
            }
            hotfixCheckbox.setEnabled(false);
            hotfixTypeCombobox.setEnabled(false);
            parameterTextField.setEnabled(false);
            nameTextField.setEnabled(false);
        }
        this.addSearch();
        Utilities.changeCTRLMasks(EditPanel.this);

        this.initialText = textElement.getText();
        EventQueue.invokeLater(textElement::requestFocusInWindow);

        if (!Options.INSTANCE.getShowHotfixNames()) {
            nameLabel.setVisible(false);
            nameTextField.setVisible(false);
        }
    }

    public EditPanel(CompletePatch patch, Category parent) {
        this(patch, parent, Collections.emptyList(), true, null);
    }

    /**
     * Adds a TextSearchDialog to ourselves.
     */
    private void addSearch() {
        textElement.getActionMap().put("Search", new AbstractAction("Search") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // Create the search dialog
                if (searchDialog == null || !searchDialog.isDisplayable()) {
                    Window parentWindow = SwingUtilities.getWindowAncestor(EditPanel.this);
                    searchDialog = new TextSearchDialog(parentWindow, textElement, "", false);
                    searchDialog.setVisible(true);
                } else {
                    searchDialog.requestFocus();
                }
            }
        });
        textElement.getActionMap().put("Replace", new AbstractAction("Replace") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                // Create the search dialog
                if (searchDialog == null || !searchDialog.isDisplayable()) {
                    Window parentWindow = SwingUtilities.getWindowAncestor(EditPanel.this);
                    searchDialog = new TextSearchDialog(parentWindow, textElement, "", true);
                    searchDialog.setVisible(true);
                } else {
                    searchDialog.requestFocus();
                }
            }
        });
        textElement.getInputMap().put(KeyStroke.getKeyStroke("control F"), "Search");
        textElement.getInputMap().put(KeyStroke.getKeyStroke("control H"), "Replace");
    }

    /**
     * Gets rid of our TextSearchDialog, if it exists.
     */
    public void disposeSearch() {
        if (searchDialog != null) {
            searchDialog.dispose();
            searchDialog = null;
        }
    }

    private void setProperFontSize(Component c) {
        c.setFont(c.getFont().deriveFont((float) Options.INSTANCE.getFontsize()));
    }

    private void attachAutoComplete() {
        new AutoCompleteAttacher(parameterTextField, true) {
            @Override
            protected AutoCompleteAttacher.AutoCompleteRequirements getAutoCompleteRequirements(boolean advanced) throws BadLocationException {
                if (hotfixTypeCombobox.getSelectedItem() == HotfixType.LEVEL) {
                    List<String> list = new ArrayList<>();
                    list.add("None");
                    int minl = 0;
                    for (BLMap key : patchType.getLevels()) {
                        minl = Math.max(minl, key.name.length());
                    }
                    for (BLMap map : patchType.getLevels()) {
                        String k1 = map.name;
                        while (k1.length() < minl) {
                            k1 += " ";
                        }
                        list.add(k1 + " (" + map.code + ")");
                    }
                    return new AutoCompleteRequirements(0, parameterTextField.getText().length(), list);
                } else if (hotfixTypeCombobox.getSelectedItem() == HotfixType.ONDEMAND) {
                    return new AutoCompleteRequirements(0, parameterTextField.getText().length(), packages);
                }
                return null;
            }

        };
    }

    /**
     * Sets an action which will be called on Ctrl-Enter. If we're allowed to
     * edit, this will get called by AutoCompleteAttacher; otherwise we just
     * attach it manually.
     *
     * @param ctrlEnterAction The action to call
     */
    public void setCtrlEnterAction(final ActionListener ctrlEnterAction) {
        if (this.allowEdit) {
            KeyAdapter keyAdap = new KeyAdapter() {
                private final int ctrlShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER
                            && ((e.getModifiers() & ctrlShortcutMask) == ctrlShortcutMask)) {
                        ctrlEnterAction.actionPerformed(null);
                    }
                }
            };
            /**
             * Disabled as part of the opensourcing project -- relies on stuff that's
             * no longer there.
             */
            /*
            textElement.removeKeyListener(textElement.getAutoCompleteAttacher().getKeyAdapter());
            textElement.addKeyListener(keyAdap);
            textElement.addKeyListener(textElement.getAutoCompleteAttacher().getKeyAdapter());
            */
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        textElement.requestFocus();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new InfoLabel(InfoLabel.BASIC_1+InfoLabel.BASIC_2_EDIT_ONLY+InfoLabel.BASIC_3);
        jLabel3 = new javax.swing.JLabel();
        hotfixTypeCombobox = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        parameterTextField = new EnhancedFormattedTextField<>(this::validateParameter,s->s);
        nameLabel = new javax.swing.JLabel();
        deformatButton = new javax.swing.JButton();
        formatButton = new javax.swing.JButton();
        hotfixCheckbox = new javax.swing.JCheckBox();
        nameTextField = new EnhancedFormattedTextField<>(this::validateName, s->s);
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        skipEmptyCheckCheckBox = new javax.swing.JCheckBox();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 1, 0));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Code:");

        jLabel3.setText("Type:");

        hotfixTypeCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                hotfixTypeComboboxItemStateChanged(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText(" ");
        jLabel4.setToolTipText("");

        parameterTextField.setEnabled(false);

        nameLabel.setLabelFor(nameTextField);
        nameLabel.setText("Name:");

        deformatButton.setText("   Deformat");
        deformatButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        deformatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deformatButtonActionPerformed(evt);
            }
        });

        formatButton.setText("Auto format");
        formatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatButtonActionPerformed(evt);
            }
        });

        hotfixCheckbox.setText("Hotfix");
        hotfixCheckbox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                hotfixCheckboxItemStateChanged(evt);
            }
        });

        nameTextField.setText("Hotfix");
        nameTextField.setEnabled(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 781, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanel2);

        skipEmptyCheckCheckBox.setText("Do not prompt for empty commands");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(skipEmptyCheckCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hotfixCheckbox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deformatButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(formatButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hotfixTypeCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(parameterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(nameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nameLabel)
                    .addComponent(parameterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(hotfixTypeCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(formatButton)
                    .addComponent(deformatButton)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hotfixCheckbox)
                    .addComponent(skipEmptyCheckCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatButtonActionPerformed
        GlobalLogger.log("Pressed format");
        String ntext = Utilities.CodeFormatter.formatCode(textElement.getText().replaceAll("\n", " ").replaceAll("[ ]+", " "));
        setTextRetainCursor(textElement, ntext);
    }//GEN-LAST:event_formatButtonActionPerformed

    private void hotfixTypeComboboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_hotfixTypeComboboxItemStateChanged
        if (hotfix) {
            HotfixType val = (HotfixType) hotfixTypeCombobox.getSelectedItem();
            GlobalLogger.log("Changed hotfix type to " + val + " (" + parameterTextField.getText() + ")");
            switch (val) {
                case LEVEL:
                    jLabel4.setText("Level:");
                    parameterTextField.setEnabled(true);
                    if (parameterTextField.getText().isEmpty()) {
                        parameterTextField.setText("None");
                    }
                    break;
                case ONDEMAND:
                    jLabel4.setText("Package:");
                    parameterTextField.setEnabled(true);
                    break;
                case PATCH:
                    jLabel4.setText(" ");
                    parameterTextField.setEnabled(false);
                    break;
                default:
                    throw new NullPointerException();
            }
            ((EnhancedFormattedTextField) parameterTextField).updateTooltip();
        }
    }//GEN-LAST:event_hotfixTypeComboboxItemStateChanged

    private void deformatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deformatButtonActionPerformed
        GlobalLogger.log("Pressed deformat");
        String ntext = Utilities.CodeFormatter.deFormatCodeInnerNBrackets(textElement.getText(), (Integer) deformatSpinner.getValue());
        setTextRetainCursor(textElement, ntext);
    }//GEN-LAST:event_deformatButtonActionPerformed

    private void hotfixCheckboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_hotfixCheckboxItemStateChanged
        if (hotfix != hotfixCheckbox.isSelected()) {
            hotfix = hotfixCheckbox.isSelected();
            GlobalLogger.log((hotfix ? "Checked" : "Unchecked") + " hotfix");
            hotfixTypeCombobox.setEnabled(hotfix);
            parameterTextField.setEnabled(hotfix);
            nameTextField.setEnabled(hotfix);
            hotfixTypeCombobox.setVisible(hotfix);
            parameterTextField.setVisible(hotfix);
            nameTextField.setVisible(hotfix && Options.INSTANCE.getShowHotfixNames());
            jLabel3.setVisible(hotfix);
            jLabel4.setVisible(hotfix);
            nameLabel.setVisible(hotfix && Options.INSTANCE.getShowHotfixNames());
            hotfixTypeComboboxItemStateChanged(null);
        }
    }//GEN-LAST:event_hotfixCheckboxItemStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton deformatButton;
    private javax.swing.JButton formatButton;
    private javax.swing.JCheckBox hotfixCheckbox;
    private javax.swing.JComboBox<HotfixType> hotfixTypeCombobox;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JTextField parameterTextField;
    private javax.swing.JCheckBox skipEmptyCheckCheckBox;
    // End of variables declaration//GEN-END:variables

    private void setTextRetainCursor(JTextPane area, String text) {
        final long nonWhiteCharsPreceedingCaret = area.getText().chars()
                .limit(area.getCaretPosition())
                .filter(i -> !Character.isWhitespace(i))
                .count();
        area.setText(text);
        int c = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                c++;
                if (c == nonWhiteCharsPreceedingCaret + 1) {
                    textElement.setCaretPosition(i);
                    break;
                }
            }
        }
    }

    private void analyzeInput(List<ModelElement> input) {
        StringBuilder sb = new StringBuilder();
        boolean show = false;
        HotfixType hotfixType = null;
        for (ModelElement el : input) {
            if (el instanceof Comment) {
                sb.append(((Comment) el).getComment());
            } else if (el instanceof SetCommand) {
                String formattedCode = Utilities.CodeFormatter.formatCode(((SetCommand) el).getCode());
                sb.append(formattedCode);
                if (!show && ((SetCommand) el).getCode().split(" ").length <= 3) {
                    show = true;
                }
                ModelElementContainer parent = el.getParent();
                if (parent instanceof HotfixWrapper) { //hotfix
                    HotfixWrapper wrap = (HotfixWrapper) parent;
                    hotfixCheckbox.setSelected(true);
                    nameTextField.setText(wrap.getName());
                    hotfixTypeCombobox.setSelectedItem(wrap.getType());
                    parameterTextField.setText(wrap.getParameter());
                    hotfixType = wrap.getType();
                }
            }
            sb.append("\n");
        }
        if (hotfixType != null) {
            hotfixCheckboxItemStateChanged(null);
            hotfixTypeComboboxItemStateChanged(null);
        }
        if (hotfixType != HotfixType.PATCH) {
            hotfixTypeCombobox.removeItem(HotfixType.PATCH);
        }
        textElement.setText(sb.toString());
        if (show) {
            skipEmptyCheckCheckBox.setSelected(true);
            skipEmptyCheckCheckBox.addChangeListener(ce -> skipEmptyCheckCheckBox.setEnabled(false));
        } else {
            skipEmptyCheckCheckBox.setVisible(false);
        }
    }

    private boolean isInputHotfixValid(String name, String newValue, String parameter) throws HeadlessException {
        String check;
        if ((check = validateParameter(parameter)) != null) {
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, check, "Illegal parameter", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if ((check = validateName(name)) != null) {
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, check, "Illegal name", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Illegal Value
        if (newValue.trim().startsWith("#<")) {
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Value may not start with '#<'", "Illegal start of value", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (newValue.trim().isEmpty()) {
            JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Value may not be empty", "Illegal value", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            List<String> parts = splitIntoParts();
            for (String part : parts) {
                SetCommand.validateCommand(part, true);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            JOptionPane.showMessageDialog(MainGUI.INSTANCE,
                    "Could not parse the code. Check your syntax: " + e.getMessage(),
                    "Parse error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private String validateParameter(String parameter) {
        HotfixType hotfixType = (HotfixType) hotfixTypeCombobox.getSelectedItem();
        if (hotfixType != HotfixType.PATCH) {
            if (parameter.trim().startsWith("#<")) {
                return "Level/Package may not start with '#<'";
            } else if (parameter.trim().isEmpty()) {
                return "Level/Package may not be empty";
            } else if (parameter.contains("\"") && !parameter.contains("\\\"")) {
                return "Level/Package may not contain quotations (\"), they crash the game";
            } else if (hotfixType == HotfixType.LEVEL) {
                if (packages.contains(parameter)) {
                    return "Level may not equal a Package. Try converting the hotfix type to an OnDemand hotfix.";
                } else if (!parameter.equalsIgnoreCase("None") && !levelnames.isEmpty() && !levelnames.contains(parameter)) {
                    return "That is not an existing level in the current game. Use autocomplete for a list of levels";
                }
            } else if (hotfixType == HotfixType.ONDEMAND && !packages.contains(parameter)) {
                return "Package must be in the list of OnDemand packages";
            }
            for (String s : PatchIO.FORBIDDEN_KEYWORDS) {
                if (parameter.contains(s)) {
                    return "Level/Package may not contain '" + s + "'";
                }
            }
        }
        return null;
    }

    private String validateName(String name) {
        if (name.trim().startsWith("#<")) {
            return "Name may not start with '#<'";
        } else if (name.trim().isEmpty()) {
            return "Name may not be empty";
        } else if (name.contains("\"")) {
            return "Name may not contain quotations (\"), they crash the game.";
        }
        for (String s : PatchIO.FORBIDDEN_KEYWORDS) {
            if (name.contains(s)) {
                return "Name may not contain '" + s + "'";
            }
        }

        return null;
    }

    private boolean isInputCodeValid(List<ModelElement> elements) {
        for (ModelElement el : elements) {
            if (commentsOnly && !(el instanceof Comment)) {
                JOptionPane.showMessageDialog(null, "Only comments are allowed in this context.", "Comments only", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if (!isEditingHotfixes() && el instanceof SetCMPCommand) {
                JOptionPane.showMessageDialog(null, "You need to be making hotfixes to use set_cmp commands.", "No non-hotfix set_cmp commands", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            String toString = el.toString();
            for (String s : PatchIO.FORBIDDEN_KEYWORDS) {
                if (toString.contains(s)) {
                    JOptionPane.showMessageDialog(MainGUI.INSTANCE, "Code may not contain '" + s + "'.", "Illegal code", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        boolean askExec = true;
        HashSet<PropertyChecker> toAsk = new HashSet<>();
        HashSet<PropertyChecker> toAskWarnings = new HashSet<>();
        for (ModelElement element : elements) {
            if (element instanceof SetCommand) {
                TransientModelData transientData = element.getTransientData();
                for (PropertyChecker property : transientData.getProperties()) {
                    String desc = property.getPropertyDescription();
                    PropertyChecker.DescType type = property.getPropertyDescriptionType();
                    if (desc != null && !desc.isEmpty()) {
                        if (type == PropertyChecker.DescType.ContentError || type == PropertyChecker.DescType.SyntaxError) {
                            toAsk.add(property);
                        } else if (type == PropertyChecker.DescType.Warning) {
                            toAskWarnings.add(property);
                        }
                    }
                }
                if (((SetCommand) element).getValue().isEmpty()) {
                    if (!skipEmptyCheckCheckBox.isSelected()) {
                        int answer = JOptionPane.showConfirmDialog(MainGUI.INSTANCE, "Your code has a command without a value, or that is missing the object or field attribute. Continue?", "Missing value", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (answer != JOptionPane.YES_OPTION) {
                            return false;
                        }
                        skipEmptyCheckCheckBox.setSelected(true);
                    }
                }
            }
            if (element.getTransientData().getNumberOfOccurences(GlobalListOfProperties.CommentChecker.Exec.class) > 0 && askExec) {
                int answer = JOptionPane.showConfirmDialog(MainGUI.INSTANCE,
                        "You entered an 'exec' command.\n"
                        + "In almost all cases it is prefered to merge mods using BLCMM.\n"
                        + "Continue anyway?", "exec command", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) {
                    return false;
                }
                askExec = false;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (!toAsk.isEmpty()) {
            sb.append("Your code has the following properties:");
            for (PropertyChecker check : toAsk) {
                sb.append("\n").append(check.getPropertyDescription());
            }
        }
        if (!toAskWarnings.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Your code has the following warnings:");
            for (PropertyChecker check : toAskWarnings) {
                sb.append("\n").append(check.getPropertyDescription());
            }
        }
        if (sb.length() > 0) {
            sb.append("\n\nContinue anyway?");
            int answer = JOptionPane.showConfirmDialog(MainGUI.INSTANCE, sb.toString(), "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void fixFixableInputs() {
        if (isEditingHotfixes()) {
            // Get rid of any whitespace surrounding the parameter.
            parameterTextField.setText(parameterTextField.getText().trim());
        }
    }

    @Override
    public boolean hasValidInput() {
        if (isEditingHotfixes()) {
            if (commentsOnly) {
                return false;
            }
            boolean b = isInputHotfixValid(nameTextField.getText(), textElement.getText(), parameterTextField.getText());
            if (!b) {
                return false;
            }
        }
        return isInputCodeValid(getElements());
    }

    @Override
    public boolean userConfirmCancel() {
        if (this.initialText.equals(textElement.getText())) {
            return true;
        } else {
            int answer = JOptionPane.showConfirmDialog(MainGUI.INSTANCE, "Really discard unsaved changes to this code?", "Discard Changes?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            return (answer == JOptionPane.YES_OPTION);
        }
    }

    public boolean isEditingHotfixes() {
        return hotfix;
    }

    /**
     * Returns a list of modelelements without parents, capturing the user
     * input. In case of hotfix, it will return a single hotfixwrapper.
     *
     * @return
     */
    public List<ModelElement> getElements() {
        List<String> parts = splitIntoParts();
        List<ModelElement> list = new ArrayList<>();

        for (String s : parts) {
            final ModelElement com;
            if (SetCommand.isValidCommand(s)) {
                if (s.startsWith("set_cmp")) {
                    com = new SetCMPCommand(s);
                } else {
                    com = new SetCommand(s);
                }
            } else if (s.startsWith("set_cmp") && s.length() > 7 && Character.isWhitespace(s.charAt(7))) {
                String[] split = s.split("\\s+");
                if (split.length < 3) {
                    com = new Comment(s);
                } else {
                    com = new SetCMPCommand(split[1], split[2], split.length > 3 ? split[3] : "", "");
                }
            } else if (s.startsWith("set") && s.length() > 3 && Character.isWhitespace(s.charAt(3))) {
                String[] split = s.split("\\s+");
                if (split.length < 3) {
                    com = new Comment(s);
                } else {
                    com = new SetCommand(split[1], split[2], "");
                }
            } else {
                com = new Comment(s);
            }
            list.add(com);
        }
        if (isEditingHotfixes()) {
            List<HotfixCommand> scs = new ArrayList<>();
            for (ModelElement el : list) {
                scs.add(el instanceof HotfixCommand ? (HotfixCommand) el : new HotfixCommand((SetCommand) el));
            }
            HotfixType type = (HotfixType) hotfixTypeCombobox.getSelectedItem();
            String param;
            switch (type) {
                case PATCH:
                    param = null;
                    break;
                case ONDEMAND:
                    param = parameterTextField.getText();
                    break;
                case LEVEL:
                    param = parameterTextField.getText();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            HotfixWrapper wrap = new HotfixWrapper(nameTextField.getText(),
                    type,
                    param,
                    scs, true);
            list.clear();
            list.add(wrap);
        }

        return list;
    }

    private List<String> splitIntoParts() {
        if (!textElement.getText().toLowerCase().trim().startsWith("set")) {
            return Arrays.asList(textElement.getText().split("\n"));
        }
        String formatted = Utilities.CodeFormatter.formatCode(textElement.getText());
        List<String> parts = new ArrayList<>();
        String[] lines = formatted.split("\n");
        StringBuilder sb = new StringBuilder();
        sb.append(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("#") && !line.startsWith("set")) {
                sb.append("\n").append(line);
            } else {
                parts.add(sb.toString());
                sb = new StringBuilder();
                sb.append(line);
            }
        }
        parts.add(sb.toString());
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if (part.startsWith("set")) {
                int split = part.indexOf("\n");
                if (split != -1) {
                    String head = part.substring(0, split);
                    String tail = part.substring(split + 1);
                    part = head.trim() + " " + Utilities.CodeFormatter.deFormatCode(tail).trim();
                }
            }
            parts.set(i, part);
        }
        return parts;
    }
}
