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
 */
package blcmm.gui.panels;

import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.components.AdHocDialog;
import blcmm.gui.components.EnhancedFormattedTextField;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Utilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 *
 * @author FromDarkHell initial functionality
 * @author LightChaosman replacement & regex
 * @author apocalyptech Convert to custom layout, for font-scaling compat
 */
public class TextSearchDialog extends javax.swing.JDialog {

    private String previous = "";
    public JTextComponent textcomp;

    private JCheckBox regularExpressionCheckBox;
    private JCheckBox matchCaseCheckBox;
    private JCheckBox wrapAroundCheckBox;
    private JLabel statusLabel;
    private EnhancedFormattedTextField searchTextField;
    private JTextField replaceTextField = null;
    private final FontInfo fontInfo;

    public TextSearchDialog(Window parent, JTextComponent textcomponent, String previousSearch, FontInfo fontInfo) {
        this(parent, textcomponent, previousSearch, fontInfo, true);
    }

    /**
     * Creates new form TextSearchDialog
     *
     * @param parent The parent window which we belong to
     * @param textcomponent The text component we read the text from.
     * @param previousSearch The previousSearch so we can update it.
     * @param fontInfo Information about the font we should use in the dialog.
     * @param replace true iff replace UI elements should be shown
     */
    public TextSearchDialog(Window parent,
            JTextComponent textcomponent,
            String previousSearch,
            FontInfo fontInfo,
            boolean replace) {
        super(parent);
        super.setModalityType(Dialog.ModalityType.MODELESS);
        this.textcomp = textcomponent;
        this.previous = previousSearch;
        this.fontInfo = fontInfo;
        super.setIconImages(MainGUI.INSTANCE.getIconImages());
        super.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        getContentPane().add(mainPanel);

        // Layout constraints
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0;
        gc.weighty = 0;

        // Search For Label
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(5, 5, 5, 5);
        JLabel searchForLabel = new JLabel("Search For:");
        searchForLabel.setFont(fontInfo.getFont());
        mainPanel.add(searchForLabel, gc);

        // Search For Input
        gc.gridx++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridwidth = 2;
        gc.weightx = 500;
        this.searchTextField = new EnhancedFormattedTextField<>(fontInfo, this::isValidRegex, s->s);
        this.searchTextField.setFont(fontInfo.getFont());
        mainPanel.add(this.searchTextField, gc);

        // Find Prev Button
        gc.gridx++;
        gc.gridx++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridwidth = 1;
        gc.insets = new Insets(5, 2, 5, 0);
        JButton findPreviousButton = new JButton("<html><b>←</b>");
        findPreviousButton.setFont(fontInfo.getFont().deriveFont(fontInfo.getFont().getSize2D() + 2f));
        findPreviousButton.addActionListener((ActionEvent ae) -> {
            this.searchButtonAction(true);
        });
        mainPanel.add(findPreviousButton, gc);

        // Find Next Button
        gc.gridx++;
        gc.insets = new Insets(5, 0, 5, 2);
        JButton findNextButton = new JButton("<html><b>→</b>");
        findNextButton.setFont(fontInfo.getFont().deriveFont(fontInfo.getFont().getSize2D() + 2f));
        findNextButton.addActionListener((ActionEvent ae) -> {
            searchButtonAction(false);
        });
        mainPanel.add(findNextButton, gc);

        // If we're including replace-with, then render that.
        if (replace) {

            // First the label
            gc.weightx = 0;
            gc.gridx = 0;
            gc.gridy++;
            gc.fill = GridBagConstraints.NONE;
            gc.insets = new Insets(0, 5, 5, 5);
            JLabel replaceWithLabel = new JLabel("Replace With:");
            replaceWithLabel.setFont(fontInfo.getFont());
            mainPanel.add(replaceWithLabel, gc);

            // Now the box
            gc.gridx++;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 500;
            gc.gridwidth = 2;
            this.replaceTextField = new JTextField();
            this.replaceTextField.setFont(fontInfo.getFont());
            mainPanel.add(this.replaceTextField, gc);

            // Now the replace button
            gc.gridx++;
            gc.gridx++;
            gc.weightx = 0;
            gc.insets = new Insets(0, 2, 5, 2);
            JButton replaceButton = new JButton("Replace");
            replaceButton.setFont(fontInfo.getFont());
            replaceButton.addActionListener((ActionEvent ae) -> {
                this.replaceButtonActionPerformed();
            });
            mainPanel.add(replaceButton, gc);

        }

        // Now a container for our checkbox options
        gc.gridx = 0;
        gc.gridy++;
        gc.weightx = 0;
        gc.gridwidth = 2;
        gc.anchor = GridBagConstraints.SOUTHWEST;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, 5, 7, 2);
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new GridBagLayout());
        mainPanel.add(checkBoxPanel, gc);

        // Status Label
        gc.gridx++;
        gc.gridx++;
        gc.weightx = 500;
        gc.weighty = 500;
        gc.anchor = GridBagConstraints.SOUTH;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridwidth = 1;
        gc.insets = new Insets(0, 2, 7, 2);
        this.statusLabel = new JLabel() {
            @Override
            public void setText(String text) {
                if (!text.toLowerCase().startsWith("<html>")) {
                    text = "<html>" + text;
                }
                super.setText(text);
            }
        };
        this.statusLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        this.statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.statusLabel.setFont(fontInfo.getFont());
        mainPanel.add(this.statusLabel, gc);

        // Container for other buttons (Replace All + Cancel)
        gc.gridx++;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.anchor = GridBagConstraints.SOUTHEAST;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridwidth = 2;
        gc.insets = new Insets(0, 2, 7, 2);
        JPanel otherButtonPanel = new JPanel();
        otherButtonPanel.setLayout(new GridBagLayout());
        mainPanel.add(otherButtonPanel, gc);

        // Add in our checkboxes
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.SOUTHWEST;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(5, 0, 0, 0);
        this.regularExpressionCheckBox = new JCheckBox("Regular expression");
        this.regularExpressionCheckBox.setFont(fontInfo.getFont());
        this.regularExpressionCheckBox.addItemListener((ItemEvent ie) -> {
            this.searchTextField.updateTooltip();
        });
        checkBoxPanel.add(this.regularExpressionCheckBox, gc);
        gc.gridy++;
        this.matchCaseCheckBox = new JCheckBox("Match case");
        this.matchCaseCheckBox.setFont(fontInfo.getFont());
        checkBoxPanel.add(this.matchCaseCheckBox, gc);
        gc.gridy++;
        this.wrapAroundCheckBox = new JCheckBox("Wrap around");
        this.wrapAroundCheckBox.setFont(fontInfo.getFont());
        this.wrapAroundCheckBox.setSelected(true);
        checkBoxPanel.add(this.wrapAroundCheckBox, gc);

        // ... aand our remaining buttons
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        if (replace) {
            gc.anchor = GridBagConstraints.NORTHWEST;
            gc.fill = GridBagConstraints.HORIZONTAL;
            JButton replaceAllButton = new JButton("Replace All");
            replaceAllButton.setFont(fontInfo.getFont());
            replaceAllButton.addActionListener((ActionEvent ae) -> {
                this.replaceAllButtonActionPerformed();
            });
            otherButtonPanel.add(replaceAllButton, gc);
            gc.gridy++;
        }

        // Spacer
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 500;
        JLabel spacer = new JLabel();
        otherButtonPanel.add(spacer, gc);

        // And finally the cancel button
        gc.gridy++;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.SOUTHEAST;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(fontInfo.getFont());
        cancelButton.addActionListener((ActionEvent ae) -> {
            this.dispose();
        });
        cancelButton.setVisible(true);
        otherButtonPanel.add(cancelButton, gc);

        // Set our size
        this.pack();
        int height = 130;
        if (replace) {
            height += 40;
        }
        Dimension size = Utilities.scaleAndClampDialogSize(new Dimension(570, height), fontInfo, parent);
        this.setPreferredSize(size);
        this.setMinimumSize(size);

        // A keyAdapter to read for an Enter key and then do a Find Next
        KeyAdapter keyAdapater = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchButtonAction(e.isShiftDown());
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        };
        super.addKeyListener(keyAdapater);
        searchTextField.addKeyListener(keyAdapater);

        // WindowListener to refocus our textarea if we're closed
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (textcomp != null && textcomp.isDisplayable()) {
                    textcomp.requestFocus();
                }
            }
        });

        super.setLocationRelativeTo(parent);
        searchTextField.requestFocus();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Search");
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));
        getAccessibleContext().setAccessibleName("Find");
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    private void replaceButtonActionPerformed() {
        String search = searchTextField.getText();
        String replacement = replaceTextField.getText();
        if (!isRegex()) {
            Range range = search(false, textcomp.getCaret().getDot(), true, true, isRegex());
            if (range.offset != -1) {
                try {
                    textcomp.getDocument().remove(range.offset, range.length);
                    textcomp.getDocument().insertString(range.offset, replacement, null);
                    selectInDocument(range.offset, replacement.length());
                    statusLabel.setText("");
                } catch (BadLocationException ex) {
                    //Should never happen, by contract of our search method
                }
            } else {
                displayNoResultsMessage();
            }
        } else {
            try {
                Pattern p = Pattern.compile(search, isMatchCase() ? 0 : Pattern.CASE_INSENSITIVE);

                int start = textcomp.getSelectionStart();
                String full = textcomp.getText();
                String head = full.substring(0, start);
                String tail = full.substring(start);
                StringBuffer builder = new StringBuffer();
                Matcher matcher = p.matcher(tail);
                String pre, post;
                int startOfReplacement = -1, endOfReplacement = -1;
                if (matcher.find()) {
                    builder.append(head);
                    startOfReplacement = builder.length() + matcher.start();
                    pre = matcher.group();
                    matcher.appendReplacement(builder, replacement);
                    endOfReplacement = builder.length();
                    post = builder.substring(startOfReplacement, endOfReplacement);
                    matcher.appendTail(builder);
                } else if (isWrapAround()) {
                    matcher = p.matcher(full);
                    if (matcher.find()) {
                        startOfReplacement = builder.length() + matcher.start();
                        pre = matcher.group();
                        matcher.appendReplacement(builder, replacement);
                        endOfReplacement = builder.length();
                        post = builder.substring(startOfReplacement, endOfReplacement);
                        matcher.appendTail(builder);
                    }
                }
                if (builder.length() != 0) {
                    textcomp.setText(builder.toString());
                    selectInDocument(startOfReplacement, endOfReplacement - startOfReplacement);
                    statusLabel.setText("");
                } else {
                    displayNoResultsMessage();
                }
            } catch (java.util.regex.PatternSyntaxException e) {
                displayRegexMessage(e);
            }
        }
    }

    private void replaceAllButtonActionPerformed() {
        String search = searchTextField.getText();
        String replacement = replaceTextField.getText();
        if (!isRegex()) {
            List<Range> ranges = new ArrayList<>();
            Range cur = new Range(0, 0);
            while ((cur = search(false, cur.offset, false, false, isRegex())).offset != -1) {
                ranges.add(cur);
                cur = new Range(cur.offset + search.length(), 0);
            } //process the offsets backwards, so edits don't influence other results.
            for (int i = ranges.size() - 1; i >= 0; i--) {
                try {
                    Range range = ranges.get(i);
                    textcomp.getDocument().remove(range.offset, range.length);
                    textcomp.getDocument().insertString(range.offset, replacement, null);
                } catch (BadLocationException ex) {
                    //Should never happen, by contract of our search method
                }
            }
            if (ranges.size() > 0) {
                statusLabel.setText("Replaced " + ranges.size() + " instances of '" + search + "' by '" + replacement + "'.");
            } else {
                displayNoResultsMessage();
            }
        } else {
            try {
                Pattern p = Pattern.compile(search, isMatchCase() ? 0 : Pattern.CASE_INSENSITIVE);
                Matcher matcher = p.matcher(textcomp.getText());
                int count = 0;
                StringBuffer builder = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(builder, replacement);
                    count++;
                }
                if (count > 0) {
                    matcher.appendTail(builder);
                    textcomp.setText(builder.toString());
                    statusLabel.setText("Replaced " + count + " instances of '" + search + "' by '" + replacement + "'.");
                } else {
                    displayNoResultsMessage();
                }
            } catch (java.util.regex.PatternSyntaxException e) {
                displayRegexMessage(e);
            }

        }
    }

    public void updateTextComponent(JTextComponent textComp) {
        this.textcomp = textComp;
    }

    private Range search(boolean searchPrevious) {
        return search(searchPrevious, textcomp.getCaret().getDot(), false, isWrapAround(), isRegex());
    }

    private Range search(
            boolean searchBackwards,
            int initialIndex,
            boolean includeCurrentResult,//If true, and initialIndex is at the *end* of a match, initialIndex will be returned
            //This is used for non-regex `replace`, so you can find first, then replace
            boolean wrapAround,
            boolean regex) {
        String search = searchTextField.getText();
        if (search != null && !search.isEmpty()) {
            try {
                // This is the text we're searching in, say an object dump
                String text = textcomp.getDocument().getText(0, textcomp.getDocument().getLength());
                if (!isMatchCase()) {
                    text = text.toLowerCase();
                    search = search.toLowerCase();
                }
                this.previous = search;
                if (!regex) {
                    if (includeCurrentResult && initialIndex >= search.length()
                            && text.substring(initialIndex - search.length(), initialIndex).equals(search)) {
                        return new Range(initialIndex, search.length());
                    }
                    int offset = searchBackwards
                            ? text.lastIndexOf(search, initialIndex - search.length() - 1)
                            : text.indexOf(search, initialIndex);
                    if (offset == -1 && wrapAround) {
                        if (searchBackwards) {
                            //Just search from the end
                            offset = text.lastIndexOf(search);
                        } else {
                            //Just search from the start
                            offset = text.indexOf(search);
                        }
                        if (offset == initialIndex) {
                            //There is just 1 result, and we're already there
                            offset = -1;
                        }
                    }
                    return new Range(offset, search.length());
                } else {//Regex search
                    Pattern p = Pattern.compile(search);

                    Matcher matcher = p.matcher(text);
                    //Both for backwards searching and checking if we're already at a result, we need to scan the whole text.
                    //Might as well do it in all cases, to keep the code clean
                    List<Range> res = new ArrayList<>();
                    while (matcher.find()) {
                        res.add(new Range(matcher.start(), matcher.end() - matcher.start()));
                    }
                    if (res.isEmpty()) {
                        return new Range(-1, 0);
                    }
                    OptionalInt opt = IntStream.range(0, res.size()).filter(i -> res.get(i).offset < initialIndex).max();
                    int idxToCheck = opt.isPresent() ? opt.getAsInt() : 0;
                    Range r = res.get(idxToCheck);
                    if (r.offset + r.length == initialIndex && includeCurrentResult) {
                        return r;
                    } else if (searchBackwards) {
                        for (int j = idxToCheck; j >= 0; j--) {
                            r = res.get(j);
                            assert r.offset < initialIndex;
                            if (r.offset + r.length < initialIndex) {
                                return r;
                            }
                        }
                    } else {
                        for (int j = idxToCheck; j < res.size(); j++) {
                            r = res.get(j);
                            if (r.offset >= initialIndex) {
                                return r;
                            }
                        }
                    }
                    if (wrapAround) {
                        if (searchBackwards) {
                            return res.get(res.size() - 1);
                        }
                        return res.get(0);
                    }
                    return new Range(-1, 0);

                }

            } catch (BadLocationException ex) {
                GlobalLogger.log("Got a bad location exception while searching: " + ex.toString());
                //never? happens
            } catch (StringIndexOutOfBoundsException ex) {
                GlobalLogger.log(ex);
                // Happens on an empty dump with previous search, do nothing for now
            }
        }
        return new Range(-1, -1);
    }

    private void selectInDocument(Range r) {
        selectInDocument(r.offset, r.length);
    }

    private void selectInDocument(final int offset, final int length) {
        textcomp.setCaretPosition(offset);
        EventQueue.invokeLater(() -> {//We do this bit later, so the viewport scrolls to the start of our match first
            textcomp.setSelectionStart(offset);//And then, select the thing
            textcomp.setSelectionEnd(offset + length);
        });
        //Now some magic to get stuff to focus properly
        final Component curFocus = this.getFocusOwner();
        if (SwingUtilities.getWindowAncestor(textcomp).getFocusOwner() != textcomp) {
            EventQueue.invokeLater(() -> {
                textcomp.requestFocus(true);//Get the focus to the text element
                EventQueue.invokeLater(() -> {
                    // This can sometimes be null, most often if the user is clicking
                    // "next" a bunch really quickly.  (Or, presumably, "prev").
                    // Anyway, if it's null, just cope without doing anything.
                    if (curFocus != null) {
                        curFocus.requestFocus();
                    }
                });//Later, get it back to us
            });

        }
    }

    private boolean isWrapAround() {
        return wrapAroundCheckBox.isSelected();
    }

    private boolean isMatchCase() {
        return matchCaseCheckBox.isSelected();
    }

    private boolean isRegex() {
        return regularExpressionCheckBox.isSelected();
    }

    private void searchButtonAction(boolean backwards) {
        try {
            Range res = search(backwards);
            if (res.offset != -1) {
                selectInDocument(res);
                statusLabel.setText("");
            } else {
                displayNoResultsMessage();
            }
        } catch (PatternSyntaxException e) {
            displayRegexMessage(e);
        }
    }

    private void displayNoResultsMessage() {
        statusLabel.setText("No results for '" + searchTextField.getText() + "'");
    }

    private void displayRegexMessage(PatternSyntaxException e) {
        AdHocDialog.run(this,
                this.fontInfo,
                AdHocDialog.IconType.ERROR,
                "Faulty regular expression",
                "<html>Your regular expression could not be parsed:<br/><br/>"
                + "<blockquote>" + e.getMessage() + "</blockquote>");
    }

    private String isValidRegex(String arg) {
        if (!isRegex()) {
            return null;
        }
        try {
            Pattern p = Pattern.compile(arg);
            return null;
        } catch (PatternSyntaxException e) {
            return e.getMessage();
        }
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        searchTextField.requestFocus();
    }

    private static class Range {

        int offset, length;

        public Range(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String toString() {
            return "(" + offset + " " + length + ")";
        }

    }

}
