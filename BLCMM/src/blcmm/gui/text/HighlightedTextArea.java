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
package blcmm.gui.text;

import blcmm.gui.MainGUI;
import blcmm.gui.ObjectExplorer;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
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
    //private final AutoCompleteAttacher autoCompleteAttacher;

    public HighlightedTextArea(boolean link) {
        this(link, true);
    }

    public HighlightedTextArea(boolean link, boolean allowEdit) {
        super();
        //link = false;
        setFont(new Font(MainGUI.CODE_FONT_NAME, Font.PLAIN, Options.INSTANCE.getFontsize()));
        setDocument(new myStylizedDocument(link));//Syntax highlighting
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

            /**
             * Disabled as part of the opensourcing project -- relies on stuff that's
             * no longer there.
             */
            /*
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
            */
            this.setDragEnabled(Options.INSTANCE.getDragAndDropEnabled());
        } else {
            this.setEditable(false);
            this.undoManager = null;
            
            /**
             * Disabled as part of the opensourcing project -- relies on stuff that's
             * no longer there.
             */
            //this.autoCompleteAttacher = null;
            
            // Let the caret be visible even when in readonly mode, so the user
            // can still use keyboard easily to select/copy text, if wanted.
            this.getCaret().setVisible(true);
        }
        if (link) {
            addLink();
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

    /**
     * Disabled as part of the opensourcing project -- relies on stuff that's
     * no longer there.
     */
    /*
    public AutoCompleteAttacher getAutoCompleteAttacher() {
        return autoCompleteAttacher;
    }
    */

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

    /**
     * Disabled as part of the opensourcing project -- relies on stuff that's
     * no longer there.
     */
    /*
    private AutoCompleteAttacher.AutoCompleteRequirements getAutoCompleteRequirements2(boolean advanced) throws BadLocationException {

        final GlobalDictionary dict = DataManager.getDictionary();

        if (dict == null) {
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
        int from, to = caret;
        if (currentWord.contains("'")) {
            String classname = currentWord.substring(0, currentWord.indexOf("'"));
            String prefix = currentWord.substring(currentWord.indexOf("'") + 1);
            from = to - (prefix.length() - (prefix.contains(".") ? prefix.lastIndexOf(".") + 1 : 0));
            if (!advanced) {
                words = dict.getElementsInClassWithPrefix(classname, prefix);
            } else {
                words = dict.getDeepElementsInClassContaining(classname, prefix);
            }
        } else if (c == '=') {
            begin--;
            while (begin > 0 && Character.isWhitespace(doc.getText(begin, 1).charAt(0)) && doc.getText(begin, 1).charAt(0) != '\n') {
                begin--;
            }
            int end = begin;
            while (begin > 0 && !isDelimiter(doc.getText(begin, 1))) {
                begin--;
            }
            String field = doc.getText(begin, end - begin + 1).trim();
            if (!advanced) {
                words = dict.getArrayValuesWithPrefix(currentWord);
                words = dict.adHocFilterBasedOnFieldname(words, field);
            } else {
                words = dict.getArrayValuesContaining(currentWord);
            }
            from = beginCurrentWord;
        } else if (depth > 0) {
            words = dict.getArrayKeysWithPrefix(currentWord);
            from = beginCurrentWord;
        } else {
            if (wordIndex == 3) {
                words = dict.getFieldsWithPrefix(currentWord);
            } else {
                if (!advanced) {
                    System.out.println("General search for " + currentWord);
                    words = dict.getElementsWithPrefix(currentWord);
                } else {
                    System.out.println("Advanced search for " + currentWord);
                    words = dict.getDeepElementsContaining(currentWord);
                }
            }
            from = beginCurrentWord + (currentWord.contains(".") ? currentWord.lastIndexOf(".") + 1 : 0);
        }
        return new AutoCompleteAttacher.AutoCompleteRequirements(from, to, words);
    }
    */

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

    private void addLink() {
        MouseListener[] ls = getMouseListeners();
        for (MouseListener l : ls) {
            removeMouseListener(l);
        }
        MouseAdapter adapter = new MouseAdapter() {
            String search = null;

            @Override
            public void mousePressed(MouseEvent e) {
                if (search != null && e.getClickCount() == 2) {
                    MainGUI.INSTANCE.launchObjectExplorerWindow();
                    boolean newTab = SwingUtilities.isMiddleMouseButton(e) || e.isControlDown() || e.isMetaDown();
                    GlobalLogger.log("Dumping " + search + "after clicking link. newTab = " + newTab);
                    ObjectExplorer.DumpOptions options = new ObjectExplorer.DumpOptions(search, true, newTab, false);
                    ObjectExplorer.INSTANCE.dump(options);
                    mouseMoved(e);
                } else {//use original mouselisteners
                    //For some reason the original mouselistners transfer the current attribute when linking.
                    for (MouseListener l : ls) {
                        l.mousePressed(e);
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent me) {
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
            @Override
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
