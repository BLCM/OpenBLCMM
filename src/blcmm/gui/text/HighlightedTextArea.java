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
package blcmm.gui.text;

import blcmm.data.lib.DataManagerManager;
import blcmm.data.lib.UEObject;
import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.ObjectExplorer;
import blcmm.gui.panels.ObjectExplorerPanel;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import blcmm.utilities.Options.MouseLinkAction;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.undo.CannotRedoException;

/**
 *
 * @author LightChaosman
 * @author FromDarkHell
 */
@SuppressWarnings("serial")
public final class HighlightedTextArea extends JTextPane {

    private final List<Object> highlights = new ArrayList<>();
    private final UndoManager undoManager;
    private final DataManagerManager dmm;
    private final AutoCompleteAttacher autoCompleteAttacher;
    private final ObjectExplorerPanel panel;
    private final FontInfo fontInfo;

    public HighlightedTextArea(DataManagerManager dmm, FontInfo fontInfo) {
        this(dmm, fontInfo, true);
    }

    public HighlightedTextArea(DataManagerManager dmm, FontInfo fontInfo, boolean allowEdit) {
        this(dmm, fontInfo, allowEdit, null);
    }

    public HighlightedTextArea(DataManagerManager dmm, FontInfo fontInfo, boolean allowEdit, ObjectExplorerPanel panel) {
        super();
        this.dmm = dmm;
        this.fontInfo = fontInfo;
        this.panel = panel;
        //link = false;
        setDocument(new myStylizedDocument());//Syntax highlighting
        setFont(new Font(MainGUI.CODE_FONT_NAME, Font.PLAIN, fontInfo.getFont().getSize()));
        setCaretColor(UIManager.getColor("text"));

        if (allowEdit) {
            this.undoManager = addUndoRedo(HighlightedTextArea.this);
            addCaretListener(e -> {
                try {
                    caretMoved(e);
                } catch (BadLocationException ex) {
                    Logger.getLogger(HighlightedTextArea.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            this.autoCompleteAttacher = new AutoCompleteAttacher(this) {
                @Override
                protected AutoCompleteAttacher.AutoCompleteRequirements getAutoCompleteRequirements(boolean advanced) throws BadLocationException {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    AutoCompleteAttacher.AutoCompleteRequirements reqs;
                    try {
                        reqs = getAutoCompleteRequirements2(advanced);
                    } catch (BadLocationException x) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        throw (x);
                    }
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    return reqs;
                }
            };
            this.setDragEnabled(Options.INSTANCE.getDragAndDropEnabled());
        } else {
            this.setEditable(false);
            this.undoManager = null;
            this.autoCompleteAttacher = null;
            // Let the caret be visible even when in readonly mode, so the user
            // can still use keyboard easily to select/copy text, if wanted.
            this.getCaret().setVisible(true);
        }
        addMouseListeners();
    }

    /**
     * Sets the font for the text area.  At the moment this is forcing our
     * default "code" font, but it *will* inherit the style and size of the
     * passed-in font.
     *
     * @param f The font to base our own font on.
     */
    @Override
    public void setFont(Font f) {
        super.setFont(new Font(MainGUI.CODE_FONT_NAME, f.getStyle(), f.getSize()));

        // Also set our tab stops, if we can.  We're standardizing on four chars.
        // Note that JTextPane tab stops are pixel-based, not character-based,
        // as the widget's only fixed-width if you happen to set a fixed-width
        // font on it.  We're using "four" as the string to measure, but we
        // could use any four-character string.
        //
        // Note that this ends up getting called in the constructor, during
        // super(), at which time the document is still null.  Hence checking
        // for that here.
        Document d = this.getDocument();
        if (d != null && d instanceof myStylizedDocument) {

            JLabel tabMeasure = new JLabel("four");
            tabMeasure.setFont(this.getFont());
            int tabWidth = tabMeasure.getPreferredSize().width;
            int currentTab = tabWidth;
            int numTabs = 30;
            TabStop[] tabs = new TabStop[numTabs];
            for (int i=0; i<numTabs; i++) {
                tabs[i] = new TabStop(currentTab, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
                currentTab += tabWidth;
            }
            TabSet tabset = new TabSet(tabs);
            StyleContext sc = StyleContext.getDefaultStyleContext();
            AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
            ((myStylizedDocument)d).setParagraphAttributes(0, d.getLength(), aset, false);
        }
    }

    @Override
    public String getText() {
        try {
            return getDocument().getText(0, getDocument().getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(HighlightedTextArea.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public AutoCompleteAttacher getAutoCompleteAttacher() {
        return autoCompleteAttacher;
    }

    private void caretMoved(CaretEvent e) throws BadLocationException {
        Highlighter hl = getHighlighter();
        DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        highlights.forEach(o -> hl.removeHighlight(o));
        highlights.clear();

        int index = e.getDot() - 1;
        String text = getText();
        if (index == -1) {
            return;
        }

        if (text.charAt(index) == ')') {
            int depth = 0;
            int i = 1;
            while (depth != -1 && index - i >= 0) {
                if (text.charAt(index - i) == '(') {
                    depth--;
                } else if (text.charAt(index - i) == ')') {
                    depth++;
                }
                i++;
            }
            if (depth == -1) {
                Object o1 = hl.addHighlight(index, index + 1, painter);
                Object o2 = hl.addHighlight(index - i + 1, index - i + 2, painter);
                highlights.add(o1);
                highlights.add(o2);
            }

        } else if (text.charAt(index) == '(') {
            int depth = 0;
            int i = 1;
            while (depth != -1 && index + i < text.length()) {
                if (text.charAt(index + i) == '(') {
                    depth++;
                } else if (text.charAt(index + i) == ')') {
                    depth--;
                }
                i++;
            }
            if (depth == -1) {
                Object o1 = hl.addHighlight(index, index + 1, painter);
                Object o2 = hl.addHighlight(index + i - 1, index + i, painter);
                highlights.add(o1);
                highlights.add(o2);
            }
        }
    }

    private boolean isDelimiter(String character) {
        String operands = ";:{}()[]+-/%<=>!&|^~*,";
        return Character.isWhitespace(character.charAt(0))
                || operands.contains(character);
    }

    private AutoCompleteAttacher.AutoCompleteRequirements getAutoCompleteRequirements2(boolean advanced) throws BadLocationException {

        if (this.dmm.getCurrentDataManager() == null) {
            return null;
        }
        final Document doc = getDocument();
        int caret = getCaret().getDot();
        int begin = caret - 1;
        if (begin <= 0) {
            return null;
        }
        int depth = doc.getText(0, caret + 1).split("\\(").length - doc.getText(0, caret + 1).split("\\)").length;
        int wordIndex;
        {
            int begin2 = begin;
            while (begin2 > 0 && !doc.getText(begin, 1).equals("\n")) {
                begin2--;
            }
            String line = doc.getText(begin2, caret - begin2);
            wordIndex = line.split("[ ]+").length + (Character.isWhitespace(line.charAt(line.length() - 1)) ? 1 : 0);
        }
        while (begin > 0 && !isDelimiter(doc.getText(begin, 1))) {
            begin--;
        }
        if (begin >= caret) {
            return null;
        }
        if (begin > 0) {
            begin++;
        }
        int beginCurrentWord = begin;
        String currentWord = doc.getText(begin, caret - begin).trim();
        begin--;
        while (begin > 0 && Character.isWhitespace(doc.getText(begin, 1).charAt(0)) && doc.getText(begin, 1).charAt(0) != '\n') {
            begin--;
        }
        char c = begin <= 1 ? '\n' : doc.getText(begin, 1).charAt(0);

        Collection<String> words;
        int from;
        int to = caret;
        int from_inner;
        if (currentWord.contains("'")) {
            // This stanza matches on instances of a "full" object referene, with classname in front
            String className = currentWord.substring(0, currentWord.indexOf("'"));
            String prefix = currentWord.substring(currentWord.indexOf("'") + 1);

            if (advanced) {
                from = to - prefix.length();
                words = this.dmm.getCurrentDataManager().getDeepAutocompleteResults(prefix, className);
            } else {
                from_inner = Integer.max(0, Integer.max(prefix.lastIndexOf("."), prefix.lastIndexOf(":")));
                from = to - (prefix.length() - from_inner);
                words = this.dmm.getCurrentDataManager().getShallowAutocompleteResults(prefix, from_inner, className);
            }
        } else if (c == '=') {
            // This matches on the inside of a stanza, after an "=" is seen.  It's got processing
            // here to look backwards to find the attribute name, so the autocomplete could
            // theoretically adapt based on the attr name.  With the 2023 Datalib rewrite, though,
            // we're ignoring that completely.  In this rewrite, we're *only* doing enum
            // autocompletes here, from the complete set of enum values in the entire game.  Not
            // super ideal, but it should serve for what folks probably mostly use it for.

            //GlobalLogger.log("Got equals-sign for currentWord \"" + currentWord + "\", doing a value for a specific attr.");
            words = this.dmm.getCurrentDataManager().getEnumAutocompleteResults(currentWord);
            from = beginCurrentWord;

            /* Original processing w/ closed-source datalib; includes searching
             * backwards for the attr name
            begin--;
            while (begin > 0 && Character.isWhitespace(doc.getText(begin, 1).charAt(0)) && doc.getText(begin, 1).charAt(0) != '\n') {
                begin--;
            }
            int end = begin;
            while (begin > 0 && !isDelimiter(doc.getText(begin, 1))) {
                begin--;
            }
            String field = doc.getText(begin, end - begin + 1).trim();
            GlobalLogger.log(" - The field: " + field);
            if (!advanced) {
                words = dict.getArrayValuesWithPrefix(currentWord);
                words = dict.adHocFilterBasedOnFieldname(words, field);
            } else {
                words = dict.getArrayValuesContaining(currentWord);
            }
            /* */
        } else if (depth > 0) {
            // This stanza matches for attribute names while inside a larger stanza of
            // code.  The original BLCMM autocomplete maybe includes all valid attr
            // names which might appear in there.  The new version in the 2023 datalib
            // rewrite, however, is at the moment only autocompleting "top-level"
            // attr names.  Far from ideal, but maybe better than nothing?
            //GlobalLogger.log("Got a depth>0 for currentWord \"" + currentWord + "\"");
            words = this.dmm.getCurrentDataManager().getFieldAutocompleteResults(currentWord);
            from = beginCurrentWord;
        } else {
            if (wordIndex == 3) {
                // This stanza handles autocompleting attr names in basic "set foo bar" constructs.
                // Technically it only checks to see if we're the *third* word in a line, which means
                // that it could certainly match on other stuff that it shouldn't, but I guess it
                // seems good enough.  At the moment, it autocompletes from the complete list of
                // possible attr names from all class types, though I'd like to expand it to look
                // up the object (at wordIndex 2) to get its class, and then restrict the autocomplete
                // to just the attrs for that class.
                //GlobalLogger.log("Doing attr autocomplete for currentWord \"" + currentWord + "\"");

                // Search backwards for the previous token, which Should:tm: be an object name
                begin--;
                while (begin > 0 && Character.isWhitespace(doc.getText(begin, 1).charAt(0)) && doc.getText(begin, 1).charAt(0) != '\n') {
                    begin--;
                }
                int end = begin;
                while (begin > 0 && !isDelimiter(doc.getText(begin, 1))) {
                    begin--;
                }
                String objName = doc.getText(begin, end - begin + 2).trim();
                //GlobalLogger.log("Got object name: " + objName);

                // Now that we (theoretically) have an object name, grab the object if
                // possible, and if we have it in our DB (and thus know the class),
                // restrict our results to just fields from that class.  Otherwise,
                // return all possibilities from our full attr list.
                UEObject ueObject = this.dmm.getCurrentDataManager().getObjectByName(objName);
                if (ueObject == null || ueObject.getUeClass() == null) {
                    words = this.dmm.getCurrentDataManager().getFieldAutocompleteResults(currentWord);
                } else {
                    words = this.dmm.getCurrentDataManager().getFieldFromClassAutocompleteResults(ueObject.getUeClass(), currentWord);
                }
                from = beginCurrentWord;
            } else {
                // Finally, if we didn't match anything else, this starts an autocomplete for just a
                // "bare" object name, unrestricted by class.
                if (advanced) {
                    from = beginCurrentWord;
                    words = this.dmm.getCurrentDataManager().getDeepAutocompleteResults(currentWord);
                } else {
                    from_inner = Integer.max(0, Integer.max(currentWord.lastIndexOf("."), currentWord.lastIndexOf(":")));
                    from = beginCurrentWord + from_inner;
                    words = this.dmm.getCurrentDataManager().getShallowAutocompleteResults(currentWord, from_inner);
                }
            }
        }
        return new AutoCompleteAttacher.AutoCompleteRequirements(from, to, words);
    }

    private static UndoManager addUndoRedo(JTextComponent textcomp) {
        final UndoManager undo = new UndoManager();
        Document doc = textcomp.getDocument();

        // Listen for undo and redo events
        doc.addUndoableEditListener(undo);

        // Create an undo action and add it to the text component
        textcomp.getActionMap().put("Undo", new AbstractAction("Undo") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    undo.finalizeUndo();
                    if (undo.canUndo()) {
                        undo.undo();
                    }
                } catch (Exception e) {//TODO better fix
                    e.printStackTrace();
                }
            }
        });

        // Bind the undo action to ctl-Z
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

        // Create a redo action and add it to the text component
        textcomp.getActionMap().put("Redo", new AbstractAction("Redo") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    undo.finalizeUndo();
                    if (undo.canRedo()) {
                        undo.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }
        });

        // Bind the redo action to ctl-Y
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        return undo;
    }

    /**
     * Our UndoManager2 starts in a non-processing state, so it doesn't get the
     * initial text-area population in its undo stack. This can be used to set
     * its state.
     *
     * @param processUndo Whether to enable Undo processing or not.
     */
    public void setProcessUndo(boolean processUndo) {
        if (this.undoManager != null) {
            this.undoManager.setProcessUndo(processUndo);
        }
    }

    /**
     * Used to completely clear out our undo data, so we can start again.
     */
    public void discardAllUndoData() {
        if (this.undoManager != null) {
            this.undoManager.discardAllEdits();
        }
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        if (this.undoManager != null) {
            this.undoManager.discardAllEdits();
        }
    }

    private void addMouseListeners() {
        MouseListener[] ls = getMouseListeners();
        for (MouseListener l : ls) {
            removeMouseListener(l);
        }
        CustomSelectionMouseAdapter adapter = new CustomSelectionMouseAdapter(this) {
            String search = null;

            @Override
            public void mousePressed(MouseEvent e) {
                MouseLinkAction a = Options.INSTANCE.processMouseLinkClick(e);
                if (a != null) {
                    if (search == null) {
                        switch (a) {
                            case Back:
                                if (panel != null) {
                                    panel.doBackButton();
                                }
                                break;
                            case Forward:
                                if (panel != null) {
                                    panel.doForwardButton();
                                }
                                break;
                        }
                    } else {
                        boolean launch = true;
                        boolean newTab = false;
                        switch (a) {
                            case New:
                                newTab = true;
                                break;
                            case Current:
                                newTab = false;
                                break;
                            case None:
                            default:
                                launch = false;
                                break;
                        }
                        if (launch) {
                            MainGUI.INSTANCE.launchObjectExplorerWindow(true);
                            GlobalLogger.log("Dumping " + search + " after clicking link. newTab = " + newTab);
                            ObjectExplorer.DumpOptions options = new ObjectExplorer.DumpOptions(search, true, newTab, false);
                            ObjectExplorer.INSTANCE.dump(options);
                            mouseMoved(e);
                        }
                        return;
                    }
                }

                //use original mouselisteners
                //For some reason the original mouselistners transfer the current attribute when linking.
                for (MouseListener l : ls) {
                    l.mousePressed(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);
                for (MouseListener l : ls) {
                    l.mouseClicked(me);
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {//TODO - might need removal
                for (MouseListener l : ls) {//this generates the CaretEvent
                    l.mouseReleased(me);
                }
            }

            //The trailing hyperlink problem is in the MouseReleased method(s)
            //of the original mouselisteners.
            //Since they don't seem to serve any real purpose, we just don't
            //execute them, ever.
            //Trying to prevent them from executing with a flag set in the
            //MousePressed event preceding said mouseReleased event does not
            //solve the problem for some reason.
            //
            // Also: JTextComponent.viewToModel() was renamed to JTextComponent.viewToModel2D()
            // in Java 9, but that name just doesn't exist in Java 8.  We can't properly
            // clean up the deprecation warning until we decide to drop support for
            // Java 8.
            //
            // See: https://github.com/BLCM/OpenBLCMM/issues/21
            @Override
            @SuppressWarnings("deprecation")
            public void mouseMoved(MouseEvent e) {
                myStylizedDocument doc = (myStylizedDocument) getDocument();
                int offset = viewToModel(e.getPoint());
                Element el = doc.getCharacterElement(offset);
                Object att = el.getAttributes().getAttribute("URL");
                if (att != null) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    search = (String) att;
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    search = null;
                }

            }

        };
        this.addMouseListener(adapter);
        this.addMouseMotionListener(adapter);
    }

}
