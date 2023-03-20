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

import blcmm.data.lib.DataManager;
import blcmm.data.lib.DataManager.Dump;
import blcmm.data.lib.DataManagerManager;
import blcmm.data.lib.UEClass;
import blcmm.data.lib.UEObject;
import blcmm.gui.ObjectExplorer;
import blcmm.gui.components.InfoLabel;
import blcmm.gui.text.AutoCompleteAttacher;
import blcmm.gui.text.HighlightedTextArea;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author FromDarkHell
 * @author LightChaosman
 */
public class ObjectExplorerPanel extends javax.swing.JPanel {

    private static class HistoryEntry {

        private final String query;
        public String text;
        public Dump dump;
        private int caret;
        private Point viewport;

        HistoryEntry(String query, String text) {
            this(query, text, null);
        }

        HistoryEntry(String query, String text, Dump dump) {
            this.query = query;
            this.text = text;
            this.dump = dump;
        }
    }

    private static final Color BUTTONCOLOR = new JButton().getBackground();
    private static final String STAR_OPEN = "☆";
    private static final String STAR_FILLED = "★";

    private final HighlightedTextArea textElement;
    private String previousQuery;
    int historyIndex = -1;
    private final LinkedList<HistoryEntry> history = new LinkedList<>();
    private final JSpinner deformatSpinner;
    Worker worker;
    boolean controlState = false;
    private DataManagerManager dmm;
    private Dump currentDump;

    /**
     * Creates new form ObjectExplorerPanel
     *
     * @param dmm The DataManagerManager object we'll use for all data interaction
     */
    public ObjectExplorerPanel(DataManagerManager dmm) {
        this.dmm = dmm;
        initComponents();
        textElement = new HighlightedTextArea(dmm, true);
        textElement.setEditable(true);
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(textElement);
        jScrollPane1.getVerticalScrollBar().setUnitIncrement(16);
        jScrollPane1.getHorizontalScrollBar().setUnitIncrement(16);
        attachAutoComplete();
        deformatSpinner = new JSpinner();
        deformatSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 0, null, 1));
        deformatButton.setLayout(new BorderLayout());
        Dimension d = new Dimension(deformatButton.getPreferredSize().width + deformatSpinner.getPreferredSize().width - 10, deformatButton.getPreferredSize().height + deformatButton.getMargin().top);
        deformatButton.add(deformatSpinner, BorderLayout.EAST);
        deformatButton.setMargin(new Insets(0, 0, 0, 0));
        deformatButton.setBorder(null);
        setFixedSize(deformatButton, d);
        backButton.setBorder(new EmptyBorder(5, 8, 5, 8));
        backButton.setFont(backButton.getFont().deriveFont(backButton.getFont().getSize2D() + 2f));
        forwardButton.setBorder(new EmptyBorder(5, 8, 5, 8));
        forwardButton.setFont(forwardButton.getFont().deriveFont(forwardButton.getFont().getSize2D() + 2f));
        this.currentDump = null;

        // Make sure we've got data
        int totalDataFiles = this.dmm.getCurrentDataManager().getTotalDatafiles();
        if (totalDataFiles == 0) {
            queryTextField.setText("Download data first");
            textElement.setText("You need to download some data packages from the settings menu for the Object Explorer to do something.");
            queryTextField.setEditable(false);
            textElement.setEditable(false);
        }
        queryTextField.getActionMap().put("Search", new AbstractAction("Search") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                performSearch();
            }
        });
        queryTextField.getActionMap().put("Cancel", new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (worker != null && !worker.stop) {
                    worker.stop();
                }
            }
        });

        // Bind the search action to ctrl-F
        queryTextField.getInputMap().put(KeyStroke.getKeyStroke("control F"), "Search");
        // Bind the cancel action to ctrl-F
        queryTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");

        // Add a search action on the textfield, too.  Is this gonna interfere
        // with the queryTextField Ctrl-F?
        ObjectExplorer.INSTANCE.addSearch(textElement);

        JButton temp = new JButton("Cancel");
        d = new Dimension(Math.max(temp.getPreferredSize().width, refsButton.getPreferredSize().width), Math.max(temp.getPreferredSize().height, refsButton.getPreferredSize().height));

        setFixedSize(refsButton, d);
        //PatchType type = DataManager.isBL2() ? PatchType.BL2 : PatchType.TPS;
        PatchType type = PatchType.BL2;
        gameIconLabel.setIcon(new ImageIcon(type.getIcon(25)));
        gameIconLabel.setToolTipText("Object explorer is currently in " + type + " mode.");
        gameIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gameIconLabel.setVerticalAlignment(SwingConstants.CENTER);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setVerticalAlignment(SwingConstants.CENTER);

        initBookmarks();
        Utilities.changeCTRLMasks(ObjectExplorerPanel.this);
    }

    private void initBookmarks() {
        bookmarkLabel.setFont(bookmarkLabel.getFont().deriveFont(bookmarkLabel.getFont().getSize2D() + 8f));
        // This sets up our bookmark button and all of its funky dealings.
        bookmarkLabel.addMouseListener(new MouseAdapter() {
            private final int doubleClickTime = (int) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
            private boolean doubleClick = false;

            @Override
            public void mouseClicked(MouseEvent e) {

                // Double Click
                if (e.getClickCount() > 1 || SwingUtilities.isRightMouseButton(e)) {
                    doubleClick = true;
                    doubleClickAction();
                } else if (e.getClickCount() == 1) {
                    // A timer that waits for doubleClickTime, to check a double click vs a single click.
                    Timer timer = new Timer(doubleClickTime, e1 -> {
                        if (doubleClick) {
                            // Reset our double click state.
                            doubleClick = false;
                        } else {
                            singleClickAction();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                }
            }

            private void singleClickAction() {
                // This happens when its a simple click
                String objectToBookmark = queryTextField.getText();
                if (objectToBookmark.isEmpty()) {
                    // No empty bookmarks
                    return;
                }
                Dump dump = dmm.getCurrentDataManager().getDump(objectToBookmark);
                if (dump.ueObject == null) {
                    JOptionPane.showMessageDialog(null, "Only dumpable objects can be bookmarked",
                            "Bookmark Error",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // This normalizes capitalization and adds in the class prefix, if possible.
                String toSave = dump.ueObject.getNameWithClassIfPossible();

                String[] bookmarks = Options.INSTANCE.getOEBookmarks(dmm.getCurrentPatchType());

                List<String> bookmarkList = new ArrayList<>(Arrays.asList(bookmarks));
                assert bookmarkList.contains(toSave) == bookmarkLabel.getText().equals(STAR_FILLED);
                if (bookmarkList.contains(toSave) || bookmarkLabel.getText().equals(STAR_FILLED)) {
                    // Our object / query is currently bookmarked. Time to remove it.
                    boolean wasInList = bookmarkList.remove(toSave);
                    Options.INSTANCE.setOEBookmarks(bookmarkList.toArray(new String[0]), dmm.getCurrentPatchType());
                    GlobalLogger.log("Object Explorer - Unbookmarked " + toSave + (!wasInList ? " (Element was not in list)" : ""));
                } else {
                    // Our object isn't currently bookmarked. Time to make it so.
                    String[] result = Arrays.copyOf(bookmarks, bookmarks.length + 1);
                    result[bookmarks.length] = toSave;
                    Options.INSTANCE.setOEBookmarks(result, dmm.getCurrentPatchType());
                    // We bookmarked something fill our star
                    GlobalLogger.log("Object Explorer - Bookmarked " + toSave);
                }
                JTabbedPane tabbedPane = ObjectExplorer.INSTANCE.getObjectExplorerTabbedPane();
                EventQueue.invokeLater(()
                        -> IntStream.range(0, tabbedPane.getTabCount() - 1)
                                .mapToObj(i -> (ObjectExplorerPanel) tabbedPane.getComponentAt(i))
                                .forEach(ObjectExplorerPanel::updateBookmarkButton));
            }

            private void doubleClickAction() {
                JPopupMenu menu = new JPopupMenu();
                menu.setBorder(null);
                menu.setOpaque(false);
                menu.setBorder(null);
                menu.setFocusable(false);
                BookmarkTable table = new BookmarkTable(historyIndex > -1 ? history.get(historyIndex).query : null, dmm.getCurrentDataManager());
                JScrollPane pane = new JScrollPane(table);
                int horPadding = 6;
                table.updateBookmarkBrowser();//This gives us an initial guess for the size, we can now fine-tune
                int height;
                if (table.getPreferredSize().width > queryTextField.getSize().width - horPadding) {
                    //we get a horizontal scrollbar
                    height = Math.min(pane.getPreferredSize().height, table.getPreferredSize().height + 30 + pane.getHorizontalScrollBar().getPreferredSize().height + 1);
                } else {
                    height = Math.min(pane.getPreferredSize().height, table.getPreferredSize().height + 30);
                }
                pane.setPreferredSize(new Dimension(queryTextField.getSize().width - horPadding, height));
                menu.add(pane);
                menu.show(queryTextField, 2, 0);
                table.updateBookmarkBrowser();
            }

        });
    }

    public void reloadTabHistory() {
        if (historyIndex > -1) {
            history.set(historyIndex, new HistoryEntry(queryTextField.getText().trim(), getDocumentText(), currentDump));
        }
    }

    public HighlightedTextArea getTextElement() {
        return this.textElement;
    }

    public JTextField getQueryTextField() {
        return queryTextField;
    }

    private static void setFixedSize(Component c, Dimension d) {
        c.setPreferredSize(d);
        c.setMaximumSize(d);
        c.setMinimumSize(d);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        queryTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        backButton = new javax.swing.JButton();
        forwardButton = new javax.swing.JButton();
        refsButton = new javax.swing.JButton();
        mainProgressBar = new javax.swing.JProgressBar();
        deformatButton = new javax.swing.JButton();
        autoFormatButton = new javax.swing.JButton();
        infoLabel = new InfoLabel(InfoLabel.OE_SPECIFIC + "<br/><br/>" + InfoLabel.BASIC_1+ InfoLabel.BASIC_3);
        gameIconLabel = new javax.swing.JLabel();
        bookmarkLabel = new javax.swing.JLabel();
        collapseArraysToggleButton = new javax.swing.JToggleButton();

        jLabel1.setText("Search:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 958, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 537, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanel1);

        backButton.setFont(new java.awt.Font("Calibri", 1, 12)); // NOI18N
        backButton.setText("←");
        backButton.setToolTipText("Back");
        backButton.setEnabled(false);
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        forwardButton.setFont(new java.awt.Font("Calibri", 1, 12)); // NOI18N
        forwardButton.setText("→");
        forwardButton.setToolTipText("Forward");
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardButtonActionPerformed(evt);
            }
        });

        refsButton.setText("Refs");
        refsButton.setToolTipText("Gives a list of all references to the currently dumped object");
        refsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refsButtonActionPerformed(evt);
            }
        });

        deformatButton.setText("    Deformat");
        deformatButton.setToolTipText("Removes the formatting from the specified number of innermost parentheses");
        deformatButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        deformatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deformatButtonActionPerformed(evt);
            }
        });

        autoFormatButton.setText("Auto format");
        autoFormatButton.setToolTipText("Formats the arrays and structs to be more readable");
        autoFormatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoFormatButtonActionPerformed(evt);
            }
        });

        bookmarkLabel.setText("☆");
        bookmarkLabel.setToolTipText("<html> Click to bookmark the currently dumped object.<br/>\nDouble click to manage your bookmarks.");

        collapseArraysToggleButton.setText("Collapse arrays");
        collapseArraysToggleButton.setToolTipText("<html>If turned on, array field will no longer be split over multiple lines.<br/>This makes it so you can easily copy it for your mod making needs!");
        collapseArraysToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                collapseArraysToggleButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 927, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(gameIconLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(backButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(forwardButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(queryTextField)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(bookmarkLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(refsButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(collapseArraysToggleButton))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(deformatButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(autoFormatButton)))))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(queryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)
                        .addComponent(backButton)
                        .addComponent(forwardButton)
                        .addComponent(refsButton)
                        .addComponent(bookmarkLabel)
                        .addComponent(collapseArraysToggleButton))
                    .addComponent(infoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(gameIconLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(autoFormatButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(deformatButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        reloadTabHistory();
        historyIndex--;
        replaceTextByHistory();
    }//GEN-LAST:event_backButtonActionPerformed

    private void forwardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardButtonActionPerformed
        reloadTabHistory();
        historyIndex++;
        replaceTextByHistory();
    }//GEN-LAST:event_forwardButtonActionPerformed

    private void refsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refsButtonActionPerformed
        if (worker != null && !worker.stop) {
            worker.stop();
            return;
        }

        String query = queryTextField.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        if (query.contains("'")) {
            query = query.substring(query.indexOf("'") + 1);
            if (query.contains("'")) {
                query = query.substring(0, query.indexOf("'"));
            }
        }

        refsButton.setText("Cancel");
        refsButton.setBackground(ThemeManager.getColor(ThemeManager.ColorType.UICancelButtonBackground));
        reloadTabHistory();
        currentDump = null;
        refs(query);
    }//GEN-LAST:event_refsButtonActionPerformed

    private void performSearch() {
        if (worker != null && !worker.stop) {
            worker.stop();
            return;
        }
        String query = queryTextField.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        refsButton.setText("Cancel");
        refsButton.setBackground(ThemeManager.getColor(ThemeManager.ColorType.UICancelButtonBackground));
        reloadTabHistory();
        currentDump = null;
        search(query);
    }

    private void deformatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deformatButtonActionPerformed
        GlobalLogger.log("Deformat pressed");
        if (worker != null) {
            worker.stop();
        }

        String ntext = deformat(getDocumentText(), (Integer) deformatSpinner.getValue());
        setTextRetainCursor(textElement, ntext);
        if (historyIndex > -1) {
            history.get(historyIndex).text = textElement.getText();
        }
    }//GEN-LAST:event_deformatButtonActionPerformed

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

    private String getDocumentText() {
        String s = null;
        try {
            s = textElement.getDocument().getText(0, textElement.getDocument().getLength());
        } catch (BadLocationException ex) {
            GlobalLogger.log(ex);
            GlobalLogger.log("continueing anyway, since this should never happen TODO");
            //log that, mark with T-O-D-O, so we can search logs for it.
        }
        return s;
    }

    private String deformat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        StringBuilder currentBracket = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                if (depth == 1) {
                    currentBracket = new StringBuilder();
                    currentBracket.append(c);
                    continue;
                }
            }
            if (c == ')') {
                depth--;
                if (depth == 0) {
                    currentBracket.append(c);
                    String s2 = Utilities.CodeFormatter.deFormatCodeInnerNBrackets(currentBracket.toString(), n).trim();
                    if (sb.length() != 0 && sb.charAt(sb.length() - 1) == '\n') {
                        sb.setLength(sb.length() - 1);
                    }
                    if (s2.contains("\n")) {
                        sb.append("\n");
                    }
                    sb.append(s2);
                    currentBracket = new StringBuilder();
                    continue;
                }
            }
            if (depth == 0) {
                if (c == '\n' && sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                } else {
                    sb.append(c);
                }
            } else {
                currentBracket.append(c);
            }
        }
        return sb.toString().trim();
    }

    private void collapseArraysToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_collapseArraysToggleButtonActionPerformed
        if (collapseArraysToggleButton.isSelected()) {
            textElement.setText(collapseArrays(deformat(getDocumentText(), 5000)));
        }
    }//GEN-LAST:event_collapseArraysToggleButtonActionPerformed

    private void autoFormatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoFormatButtonActionPerformed
        GlobalLogger.log("AutoFormat pressed");
        if (worker != null) {
            worker.stop();
        }
        String ntext = deformat(getDocumentText(), 0);
        setTextRetainCursor(textElement, ntext);
        if (historyIndex > -1) {
            history.get(historyIndex).text = textElement.getText();
        }
    }//GEN-LAST:event_autoFormatButtonActionPerformed

    public void bookmarkCurrentObject() {
        MouseEvent me = new MouseEvent(bookmarkLabel, 0, 0, 0, 100, 100, 1, false);
        bookmarkLabel.getMouseListeners()[0].mouseClicked(me);
    }

    void updateBookmarkButton() {
        if (historyIndex != -1) {
            updateBookmarkButton(history.get(historyIndex));
        }
    }

    private void updateBookmarkButton(HistoryEntry entry) {
        boolean check = false;
        if (entry != null) {
            //GlobalLogger.log("Updating bookmark button based on query: " + entry.query);
            if (entry.dump != null && entry.dump.ueObject != null) {
                // TODO: I really don't like this construct; should be able to compare against
                // a HashSet or something rather than looping over a list
                check = Arrays.asList(Options.INSTANCE.getOEBookmarks(this.dmm.getCurrentPatchType())).contains(entry.dump.ueObject.getNameWithClassIfPossible());
            }
        }
        bookmarkLabel.setText(check ? STAR_FILLED : STAR_OPEN);
        bookmarkLabel.setForeground(ThemeManager.getColor(check ? ThemeManager.ColorType.UINimbusAlertYellow : ThemeManager.ColorType.UIText));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton autoFormatButton;
    private javax.swing.JButton backButton;
    private javax.swing.JLabel bookmarkLabel;
    private javax.swing.JToggleButton collapseArraysToggleButton;
    private javax.swing.JButton deformatButton;
    private javax.swing.JButton forwardButton;
    private javax.swing.JLabel gameIconLabel;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JProgressBar mainProgressBar;
    private javax.swing.JTextField queryTextField;
    private javax.swing.JButton refsButton;
    // End of variables declaration//GEN-END:variables

    private void attachAutoComplete() {
        new AutoCompleteAttacher(queryTextField) {

            @Override
            protected AutoCompleteAttacher.AutoCompleteRequirements getAutoCompleteRequirements(boolean advanced) throws BadLocationException {
                Collection<String> res;
                String word = queryTextField.getText().substring(0, queryTextField.getCaretPosition()).trim();

                int from = 0;
                int to;
                String className = null;

                // Split out our class identifier, if it exists
                to = queryTextField.getText().length();
                if (word.contains("'")) {
                    className = word.substring(0, word.indexOf("'"));
                    from = word.indexOf("'") + 1;
                    word = word.substring(from);
                    if (word.startsWith("'")) {
                        word = word.substring(1);
                    }
                }

                // Find our last tree branch point and adjust our positioning if needed
                int from_inner = 0;
                if (!advanced) {
                    from_inner = Integer.max(0, Integer.max(word.lastIndexOf("."), word.lastIndexOf(":")));
                    from += from_inner;
                }

                //GlobalLogger.log("Starting autocomplete with class " + className + " and word " + word + ", advanced: " + advanced);
                //GlobalLogger.log("From: " + from + ", to:" + to);

                // Now do the query!
                if (className == null) {
                    if (advanced) {
                        res = dmm.getCurrentDataManager().getDeepAutocompleteResults(word);
                    } else {
                        res = dmm.getCurrentDataManager().getShallowAutocompleteResults(word, from_inner);
                    }
                } else {
                    if (advanced) {
                        res = dmm.getCurrentDataManager().getDeepAutocompleteResults(word, className);
                    } else {
                        res = dmm.getCurrentDataManager().getShallowAutocompleteResults(word, from_inner, className);
                    }
                }

                return new AutoCompleteAttacher.AutoCompleteRequirements(from, to, res);
            }

            @Override
            protected void enter(KeyEvent e) {
                boolean oldstate = controlState;
                controlState = e.isControlDown();//Not MAC friendly
                if (!oldstate) {
                    enterKeyPressed();
                }
            }
        };
    }

    private void enterKeyPressed() {
        String query = queryTextField.getText().trim();
        if (query.isEmpty() || query.equals(previousQuery)) {
            return;
        }
        //System.out.println(query + " : " + controlState);
        reloadTabHistory();
        if (query.toLowerCase().startsWith("getall ")) {
            getAll(query.substring("getall".length()).trim());
        } else if (!query.contains(" ") && this.dmm.getCurrentDataManager().getClassByName(query) != null) {
            getAll(query);

        } else if ((!query.contains(" ") || query.contains(".") && !query.toLowerCase().contains("inclass:") && !query.matches(".*(\\^|\\\\|\\||\\*|\\+|\\(|\\)|\\?).*")) && controlState == false) {

            boolean success = dump(this.dmm.getCurrentDataManager(), new ObjectExplorer.DumpOptions(query));
            if (!success) {
                performSearch();
            }
        } else {
            performSearch();
        }
    }

    private void replaceTextByHistory() {
        if (worker != null) {
            worker.stop();
        }
        queryTextField.setText(history.get(historyIndex).query);
        textElement.setText(history.get(historyIndex).text);
        textElement.setCaretPosition(history.get(historyIndex).caret);
        currentDump = history.get(historyIndex).dump;
        textElement.discardAllUndoData();
        textElement.setProcessUndo(true);
        //jScrollPane1.getViewport().setViewPosition(history.get(historyIndex).viewport);
        updateButtons();
        updateBookmarkButton(history.get(historyIndex));
        textElement.requestFocus();
    }

    public boolean dump(DataManager dm, ObjectExplorer.DumpOptions options) {
        if (worker != null) {
            worker.stop();
        }
        if (options.createLogEntry) {
            GlobalLogger.log("dumping " + options.objectToDump);
        }
        Dump dump = dm.getDump(options.objectToDump);
        String text = dump.text;
        if (dump.ueObject == null) {
            textElement.setText(text);
            currentDump = null;
            return false;
        } else {
            if (collapseArraysToggleButton.isSelected()) {
                text = collapseArrays(text);
            }
            currentDump = dump;
            String normalizedQuery;
            if (Options.INSTANCE.getPreferFullObjInOE()) {
                normalizedQuery = dump.ueObject.getNameWithClassIfPossible();
            } else {
                normalizedQuery = dump.ueObject.getName();
            }
            HistoryEntry newHistory = setQueryAndText(normalizedQuery, text);
            updateBookmarkButton(newHistory);
            return true;
        }

    }

    private void updateButtons() {
        backButton.setEnabled(historyIndex > 0);
        forwardButton.setEnabled(historyIndex < history.size() - 1);
        if (worker != null && worker.stop) {
            refsButton.setText("Refs");
            refsButton.setBackground(BUTTONCOLOR);
        }
    }

    private String collapseArrays(String dump) {
        StringBuilder sb = new StringBuilder();
        String curname = null;
        StringJoiner sj = null;
        for (String line : dump.split("\n")) {
            boolean join = false;
            int idx = line.indexOf("=");
            if (idx > 0) {
                if (line.charAt(idx - 1) == ')') {
                    int idx2 = line.lastIndexOf('(', idx - 2);
                    if (idx2 > 0) {//There must be something in front of the \\([0-9]+\\)= pattern
                        join = true;
                        String fieldname = line.substring(0, idx2).trim();//We might as well optimize
                        String value = line.substring(idx + 1);
                        if (curname == null) {//new array
                            sj = new StringJoiner(",", "(", ")\n");
                            curname = fieldname;
                            sj.add(value);
                        } else if (curname.equals(fieldname)) {//next element
                            assert sj != null;
                            sj.add(value);
                        } else {//new array, but handle old one first
                            assert sj != null;
                            sb.append(curname).append("=").append(sj.toString());
                            sj = new StringJoiner(",", "(", ")\n");
                            curname = fieldname;
                            sj.add(value);
                        }
                    }
                }
            }
            if (!join) {
                if (curname != null) {
                    assert sj != null;//since curname !=null
                    sb.append(curname).append("=").append(sj.toString());
                    sj = null;
                    curname = null;
                }
                sb.append(line).append("\n");
            }
        }
        assert curname == null;//We should always end with the Object properties, being non-arrays
        return sb.toString();
    }

    private HistoryEntry setQueryAndText(String query, String text) {
        if (historyIndex > -1) {
            history.get(historyIndex).caret = textElement.getCaretPosition();
            history.get(historyIndex).viewport = jScrollPane1.getViewport().getViewPosition();
        }
        textElement.setText(text.replace("\r", ""));
        textElement.setCaretPosition(0);
        textElement.discardAllUndoData();
        textElement.setProcessUndo(true);

        queryTextField.setText(query);
        while (historyIndex < history.size() - 1) {
            history.removeLast();
        }
        history.add(new HistoryEntry(query, text, currentDump));
        while (history.size() > 10) {
            history.removeFirst();
        }
        historyIndex = history.size() - 1;
        updateButtons();
        return history.get(historyIndex);
    }

    private void refs(String query) {
        updateBookmarkButton(null);
        // Log
        GlobalLogger.log("Trying to refs " + query);
        if (worker != null) {
            worker.stop();
        }
        worker = new Worker(this.dmm.getCurrentDataManager(), query) {
            @Override
            public void loop(BufferedReader br, TreeMap<String, Boolean> matches) throws IOException {
                refsLoop(br, matches, query);
            }
        };
        worker.execute();
    }

    private void search(String query) {
        updateBookmarkButton(null);

        // I'm not entirely sure I agree with how this is done...  I think I'd
        // prefer that regex searches have to be prefixed with `/` or something.
        // However, for now I'm just keeping it as-is.
        boolean RegexBox = query.matches(".*(\\^|\\\\|\\||\\*|\\+|\\?).*") || query.matches(".*(\\(.*[^0-9].*\\)).*");
        if (RegexBox) {
            GlobalLogger.log("Trying to search with pattern: \"" + query + "\"");
        } else {
            GlobalLogger.log("Trying to search with query: \"" + query + "\"");
        }

        // Stop worker if something else is already working
        if (worker != null) {
            worker.stop();
        }

        if (RegexBox) {
            try {
                Pattern compile = Pattern.compile(query);
                worker = new Worker(this.dmm.getCurrentDataManager(), query) {// Create new worker
                    @Override
                    public void loop(BufferedReader br, TreeMap<String, Boolean> matches) throws IOException {
                        regexSearchLoop(br, matches, compile);
                    }
                };
            } catch (PatternSyntaxException e) {
                JOptionPane.showMessageDialog(this,
                        "The regular expression you entered is invalid. Please fix the expression:\" " + e.getDescription() + "\"",
                        "Error in regular expression",
                        JOptionPane.ERROR_MESSAGE);
                worker = null;
            }
        } else {
            String[] query2 = query.split(" ");
            List<String> positives = new ArrayList<>();
            List<String> negatives = new ArrayList<>();
            String className = null;
            for (String s : query2) {
                if (s.startsWith("inclass:")) {
                    className = s.substring(8);
                } else if (s.startsWith("-")) {
                    negatives.add(s.substring(1).toLowerCase());
                } else {
                    positives.add(s.toLowerCase());
                }
            }

            // This is needed to avoid errors when passing it to getClassByName, below
            final String finalClassName = className;

            worker = new Worker(this.dmm.getCurrentDataManager(), query) {
                @Override
                protected TreeSet<UEClass> getAvailableClasses() {
                    if (finalClassName == null) {
                        return super.getAvailableClasses();
                    } else {
                        UEClass ueClass = this.dm.getClassByName(finalClassName);
                        if (ueClass == null) {
                            JOptionPane.showMessageDialog(ObjectExplorerPanel.this, "The class you tried to search for using \"inclass:\" was unable to be obtained.", "Error in Search", JOptionPane.ERROR_MESSAGE);
                            worker.cancel(true);
                            return new TreeSet<>();
                        } else {
                            return this.dm.getSubclassesSet(ueClass);
                        }
                    }
                }

                @Override
                public void loop(BufferedReader br, TreeMap<String, Boolean> matches) throws IOException {
                    basicSearchLoop(br, matches, positives.toArray(new String[0]), negatives.toArray(new String[0]));
                }
            };
        }
        // Run worker
        if (worker != null) {
            worker.execute();
        }

    }

    private void refsLoop(BufferedReader br, TreeMap<String, Boolean> matches, String query) throws IOException {
        String query2 = query.toLowerCase() + "'";
        String line = br.readLine();
        String current = null;
        boolean match = false;
        while (line != null) {
            if (line.startsWith("***")) {
                if (match) {
                    reportCurrentObject(current, matches);
                }
                match = false;
                current = line;
            }
            if (line.toLowerCase().contains(query2)) {
                match = true;
            }

            line = br.readLine();
        }
        if (match) {
            reportCurrentObject(current, matches);
        }
    }

    private void regexSearchLoop(BufferedReader br, TreeMap<String, Boolean> matches, Pattern pat) throws IOException {
        String line = br.readLine();
        String current = null;
        boolean match = false;
        while (line != null) {
            if (line.startsWith("***")) {
                if (match) {
                    reportCurrentObject(current, matches);
                }
                match = false;
                current = line;
            }
            if (pat.matcher(line).find()) {
                match = true;
            }
            line = br.readLine();
        }
        if (match) {
            reportCurrentObject(current, matches);
        }
    }

    private void basicSearchLoop(BufferedReader br, TreeMap<String, Boolean> matches, String[] positives, String[] negatives) throws IOException {
        boolean[] positivematches = new boolean[positives.length];
        boolean[] negativematches = new boolean[negatives.length];
        String line = br.readLine();
        String current = null;
        while (line != null) {
            if (line.startsWith("***")) {
                boolean toReport = true;
                for (boolean positive : positivematches) {
                    toReport = toReport && positive;
                }
                for (boolean negative : negativematches) {
                    toReport = toReport && !negative;
                }
                if (toReport && current != null) {
                    reportCurrentObject(current, matches);
                }
                for (int i = 0; i < positivematches.length; i++) {
                    positivematches[i] = false;
                }
                for (int i = 0; i < negativematches.length; i++) {
                    negativematches[i] = false;
                }
                current = line;
            }
            for (int i = 0; i < positives.length; i++) {
                if (line.toLowerCase().contains(positives[i])) {
                    positivematches[i] = true;
                }
            }
            for (int i = 0; i < negatives.length; i++) {
                if (line.toLowerCase().contains(negatives[i])) {
                    negativematches[i] = true;
                }
            }
            line = br.readLine();
        }
        boolean toReport = true;
        for (boolean positive : positivematches) {
            toReport = toReport && positive;
        }
        for (boolean negative : negativematches) {
            toReport = toReport && !negative;
        }
        if (toReport && current != null) {
            reportCurrentObject(current, matches);
        }
    }

    private String objectNameFromDumpHeader(String header) {
        int index = header.indexOf("'") + 1;
        int index2 = header.indexOf(" ", index);
        int index3 = header.indexOf("'", index2);
        String cl = header.substring(index, index2);
        String ob = header.substring(index2 + 1, index3);
        return cl + "'" + ob + "'";
    }

    private void reportCurrentObject(String current, TreeMap<String, Boolean> matches) {
        matches.put(this.objectNameFromDumpHeader(current), false);
    }

    public void getAll(String query) {
        updateBookmarkButton(null);
        currentDump = null;
        if (worker != null) {
            // Stop worker if something else is already working
            worker.stop();
        }
        boolean hasFieldName = query.matches("(?i).*[a-z]\\s[a-z].*");
        UEClass ueClass;
        if (hasFieldName) {
            String[] splitQuery = query.split(" ", 2);
            ueClass = this.dmm.getCurrentDataManager().getClassByName(splitQuery[0]);
            if (ueClass != null) {
                getAllWithField(ueClass, splitQuery[1]);
            }
        } else {
            ueClass = this.dmm.getCurrentDataManager().getClassByName(query);
            if (ueClass != null) {
                getAllNoField(ueClass);
            }
        }
        if (ueClass == null) {
            JOptionPane.showMessageDialog(this,
                    ("Unfortunately, the classs you entered can not be dumped using BLCMM."),
                    "Unknown class",
                    JOptionPane.PLAIN_MESSAGE);
        }
    }

    //We seperate those methods, since we can solve this request with less disk I/O
    public void getAllNoField(UEClass ueClass) {
        GlobalLogger.log("Trying to getall on class: \"" + ueClass.getName() + "\"");
        currentDump = null;
        Collection<UEObject> objects = this.dmm.getCurrentDataManager().getAllObjectsInClassTree(ueClass);
        mainProgressBar.setValue(mainProgressBar.getMaximum());
        if (objects != null && !objects.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Found %s objects of type %s\n", objects.size(), ueClass.getName()));
            objects.forEach(o -> {
                sb.append(o.getNameWithClassIfPossible());
                sb.append("\n");
            });
            setQueryAndText("getall " + ueClass.getName(), sb.toString());
            GlobalLogger.log("Obtained all objects of Class: \"" + ueClass.getName() + "\"");
        } else {
            setQueryAndText("getall " + ueClass.getName(), "No objects found in class \"" + ueClass.getName() + "\"");
            GlobalLogger.log("Unable to getall with a class of: \"" + ueClass.getName() + "\"");
        }
    }

    /**
     * Simulates a console `getall classname attribute`.  I would really like
     * to do this via the Worker background-processing loop, but it turns out
     * that the markup-processing code is *way* too slow when dynamically
     * updated that way.  We can get all the text into the text area pretty
     * quickly (at least for the tests I was doing), but even altering the
     * population code to dump it all in one big chunk, it would process for
     * over a minute compared to 12 seconds in BLCMM classic.  (That was with
     * `getall itempooldefinition balanceditems`)
     *
     * So, we're continuing to handle it this way, alas.  Note that we're
     * duplicating the looping code from the worker-handlers below.  Ah, well.
     * Perhaps there's improvements to the markup-processing code which could
     * allow us to make use of that whole framework instead, but that's for
     * another day.
     *
     * @param ueClass The class to look up
     * @param property The property to display
     */
    public void getAllWithField(UEClass ueClass, String property) {
        property = property.trim();
        GlobalLogger.log("Trying to getall on class: \"" + ueClass.getName() + "\" and property of: \"" + property + "\"");
        currentDump = null;

        // I'd *like* to do this but it seems to only sometimes work?  No idea
        // why it doesn't work all the time.  However, even when it does work,
        // it only shows the spinner while we're in this loop, and for the
        // query I'm primarily testing on (`getall itempooldefinition balanceditems`)
        // the majority of the time is the textarea updating its markup.  So
        // the spinner turns off far too early anyway.  Presumably there *is*
        // an event we could hook into which would trigger once the thing's
        // done with its work, but I don't care enough to dig more, at the
        // moment.
        //ObjectExplorer.INSTANCE.cursorWait();

        String startPatternStandard = "  " + property.toLowerCase() + "=";
        String startPatternArray = "  " + property.toLowerCase() + "(";
        int startPatternLen = startPatternStandard.length();
        String normalizedProperty = null;
        StringBuilder output = new StringBuilder();
        int objectCount = 0;

        for (UEClass loopClass : this.dmm.getCurrentDataManager().getSubclassesSet(ueClass)) {

            for (File dataFile : this.dmm.getCurrentDataManager().getAllDatafilesForClass(loopClass)) {

                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)))) {

                    String toMatch;
                    String current = null;
                    StringBuilder curAttr = new StringBuilder();
                    boolean match = false;
                    boolean found = false;
                    boolean isArray = false;
                    int dataIndex;

                    String line = br.readLine();
                    while (line != null) {
                        if (line.startsWith("***")) {
                            if (match) {
                                objectCount++;
                                output.append(current);
                                if (!curAttr.isEmpty()) {
                                    output.append(" ");
                                    output.append(curAttr);
                                    if (isArray) {
                                        output.append(")");
                                    }
                                }
                                output.append("\n");
                            }
                            // We assume that every object we see during this loop should be
                            // reported on, whether or not we see the relevant attr.
                            match = true;
                            found = false;
                            isArray = false;
                            current = this.objectNameFromDumpHeader(line);
                            curAttr = new StringBuilder();
                        }
                        if ((!found || isArray) && line.length() >= startPatternLen) {
                            dataIndex = 0;
                            toMatch = line.substring(0, startPatternLen).toLowerCase();
                            if (toMatch.startsWith(startPatternStandard)) {
                                found = true;
                                dataIndex = startPatternLen;
                            } else if (toMatch.startsWith(startPatternArray)) {
                                found = true;
                                isArray = true;
                                dataIndex = line.indexOf(")") + 2;
                            }
                            if (found) {
                                if (normalizedProperty == null) {
                                    normalizedProperty = line.substring(2, startPatternLen-1);
                                }
                                if (curAttr.isEmpty()) {
                                    curAttr.append(normalizedProperty);
                                    curAttr.append(": ");
                                    if (isArray) {
                                        curAttr.append("(");
                                    }
                                } else if (isArray) {
                                    curAttr.append(",");
                                }
                                curAttr.append(line.substring(dataIndex));
                            }
                        }
                        line = br.readLine();
                    }
                    if (match) {
                        objectCount++;
                        output.append(current);
                        if (!curAttr.isEmpty()) {
                            output.append(" ");
                            output.append(curAttr);
                            if (isArray) {
                                output.append(")");
                            }
                        }
                        output.append("\n");
                    }

                } catch (IOException ex) {
                    Logger.getLogger(ObjectExplorer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        output.insert(0, "Found " + objectCount + " of class " + ueClass.getName() + " with property " + property + "\n");
        setQueryAndText("getall " + ueClass.getName() + " " + property, output.toString());
        GlobalLogger.log("Obtained all objects of Class: \"" + ueClass.getName() + "\" and property of: \"" + property + "\"");
        //ObjectExplorer.INSTANCE.cursorNormal();

    }

    public void updateGame() {
        //PatchType type = DataManager.isBL2() ? PatchType.BL2 : PatchType.TPS;
        PatchType type = PatchType.BL2;
        gameIconLabel.setIcon(new ImageIcon(type.getIcon(25)));
        gameIconLabel.setToolTipText("Object explorer is currently in " + type + " mode.");
        if (this.worker != null) {
            worker.stop();

        }
    }

    private abstract class Worker extends SwingWorker {

        Exception e;
        boolean stop = false;
        protected final DataManager dm;
        final String query;

        public Worker(DataManager dm, String query) {
            this.dm = dm;
            this.query = query;
        }

        protected TreeSet<UEClass> getAvailableClasses() {
            return this.dm.getAllClassesByEnabledCategory();
        }

        public abstract void loop(BufferedReader br, TreeMap<String, Boolean> matches) throws IOException;

        @Override
        protected Object doInBackground() throws Exception {

            // Update the total length of the progress bar based on how many classes
            // we're actually processing
            int totalDatafiles = 0;
            for (UEClass ueClass: this.getAvailableClasses()) {
                totalDatafiles += ueClass.getNumDatafiles();
            }
            mainProgressBar.setMaximum(totalDatafiles);

            TreeMap<String, Boolean> matches = new TreeMap<>();
            textElement.setEditable(false);
            textElement.discardAllUndoData();
            textElement.setProcessUndo(false);
            try {
                int counter = 0;
                mainProgressBar.setValue(0);
                textElement.setText("");
                boolean news = false;

                for (UEClass ueClass : this.getAvailableClasses()) {

                    for (File dataFile : this.dm.getAllDatafilesForClass(ueClass)) {

                        if (stop) {
                            return null;
                        }
                        int old = matches.size();

                        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)))) {
                            loop(br, matches);
                            news = old != matches.size();

                        } catch (IOException ex) {
                            Logger.getLogger(ObjectExplorer.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (stop) {
                            return null;
                        }

                        if (news) {
                            if (textElement.getText().isEmpty()) {
                                textElement.setText("Found your query (" + query + ") in the following () objects:\n");
                            }

                            Document doc = textElement.getStyledDocument();
                            int idx0 = textElement.getText().indexOf("\n");
                            int idx1 = textElement.getText().lastIndexOf("(", idx0 + 1);
                            int idx2 = textElement.getText().indexOf(")", idx1);

                            doc.remove(idx1, idx2 - idx1 + 1);
                            doc.insertString(idx1, "(" + matches.size() + ")", null);

                            // Converted this next bit when I was trying to get getall-with-property to
                            // go through this framework.  The vast majority of the time was being spent
                            // processing markup, and I thought maybe if all the text was injected into
                            // the document at once, we'd get the performance back.  In the end, that
                            // didn't really help much.  (Or, well, maybe it helped, but it was still way
                            // slower than the previous method.  Anyway, I like this method better
                            // anyway, so I'm keeping it, but the previous method remains commented
                            // below, in case this turns out to have any weird side-effects I haven't
                            // noticed yet.
                            StringBuilder sb = new StringBuilder();
                            for (String key : matches.keySet()) {
                                boolean processed = matches.get(key);
                                if (!processed) {
                                    if (stop) {
                                        return null;
                                    }
                                    sb.append(key);
                                    sb.append("\n");
                                    matches.put(key, true);
                                }
                            }

                            doc.insertString(doc.getEndPosition().getOffset()-1, sb.toString(), null);

                            /*
                            int st = textElement.getText().indexOf("\n") + 1;
                            for (String key : matches.keySet()) {
                                boolean ne = matches.get(key);
                                if (!ne) {

                                    if (stop) {
                                        return null;
                                    }
                                    if (extras.containsKey(key)) {
                                        doc.insertString(st, key + " " + extras.get(key) + "\n", null);
                                    } else {
                                        doc.insertString(st, key + "\n", null);
                                    }
                                    matches.put(key, true);
                                }
                                st += key.length() + 1;
                                if (extras.containsKey(key)) {
                                    st += extras.get(key).length() + 1;
                                }
                            }
                            */
                            news = false;
                        }

                        counter += 1;
                        mainProgressBar.setValue(counter);
                        mainProgressBar.repaint();
                    }
                }
                return null;
            } catch (Exception e2) {
                e = e2;
                return null;
            }

        }

        @Override
        protected void done() {
            if (stop) {
                return;
            }
            done2();
        }

        private void done2() {
            stop = true;
            if (e != null) {
                GlobalLogger.log("Error during worker");
                GlobalLogger.log(e);
                throw new RuntimeException(e);
            }
            GlobalLogger.log("Worker done");
            mainProgressBar.setValue(mainProgressBar.getMaximum());
            setQueryAndText(query, textElement.getText());
            textElement.setEditable(true);
            textElement.discardAllUndoData();
            textElement.setProcessUndo(true);
            updateButtons();
        }

        private void stop() {
            if (stop) {
                return;
            }
            GlobalLogger.log("Stopping previous worker");
            stop = true;
            try {
                get();
                done2();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(ObjectExplorerPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
