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

import blcmm.utilities.Options;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 *
 * @author LightChaosman
 */
public abstract class AutoCompleteAttacher {

    private final JPopupMenu autoCompleteMenu;
    private final AutoCompleteList autoCompleteList;
    private final JTextComponent component;
    private final KeyAdapter keyAdapter;
    private boolean enter = false;
    private final int ctrlShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public AutoCompleteAttacher(JTextComponent component) {
        this(component, false);
    }

    public AutoCompleteAttacher(JTextComponent component, boolean useValueInParens) {
        this.component = component;
        this.autoCompleteMenu = new JPopupMenu();
        this.autoCompleteList = new AutoCompleteList(component, useValueInParens);
        autoCompleteMenu.add(autoCompleteList);
        autoCompleteList.setMenu(autoCompleteMenu);
        autoCompleteMenu.setBorder(null);
        autoCompleteMenu.setOpaque(false);
        autoCompleteMenu.setBorder(null);
        autoCompleteMenu.setFocusable(false);
        keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                enter = false;
                boolean enter2 = true;
                if (autoCompleteMenu.isVisible()) {
                    boolean consume = false;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            autoCompleteList.moveDown();
                            consume = true;
                            break;
                        case KeyEvent.VK_UP:
                            autoCompleteList.moveUp();
                            consume = true;
                            break;
                        case KeyEvent.VK_ENTER:
                            enter2 = false;
                            autoCompleteList.action();
                            consume = true;
                            break;
                        case KeyEvent.VK_ESCAPE:
                            autoCompleteMenu.setVisible(false);
                            consume = true;
                            break;
                        default:
                            break;
                    }
                    if (consume) {
                        e.consume();
                    }
                }

                if (!autoCompleteMenu.isVisible() && e.getKeyCode() == KeyEvent.VK_ENTER && enter2) {
                    enter = true;
                    e.consume();
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if ((e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE) || (autoCompleteMenu.isVisible() && e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)) {
                    boolean advanced;
                    advanced = e.isControlDown() && e.getKeyCode() == KeyEvent.VK_SPACE && autoCompleteMenu.isVisible();
                    try {
                        autoCompleteMenu.setVisible(false);
                        autoComplete(advanced);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(HighlightedTextArea.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (enter) {
                    enter(e);
                }
            }

        };
        component.addKeyListener(keyAdapter);
    }

    public KeyAdapter getKeyAdapter() {
        return keyAdapter;
    }

    private void autoComplete(boolean advanced) throws BadLocationException {
        AutoCompleteRequirements req = getAutoCompleteRequirements(advanced);
        if (req == null) {
            return;
        }
        show(req.words, req.from, req.to);
    }

    protected void enter(KeyEvent e) {
        try {
            Document doc = component.getDocument();
            int caret = component.getCaret().getDot();
            int depth = doc.getText(0, caret + 1).split("\\(").length - doc.getText(0, caret + 1).split("\\)").length;
            String insert = "\n";
            for (int i = 0; i < depth; i++) {
                insert += "    ";
            }
            doc.insertString(caret, insert, null);
            e.consume();
            enter = false;
        } catch (BadLocationException ex) {
            Logger.getLogger(HighlightedTextArea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected abstract AutoCompleteRequirements getAutoCompleteRequirements(boolean advanced) throws BadLocationException;

    private void show(Collection<String> words, int from, int to) throws BadLocationException {
        if (words != null && words.size() >= 0) {
            DefaultListModel<String> model = new DefaultListModel<>();
            for (final String w : words) {
                model.addElement(w);
            }
            autoCompleteList.setModel(model);
            autoCompleteList.setFromTo(from, to);
            Point p = component.modelToView(component.getCaret().getDot()).getLocation();
            autoCompleteMenu.show(component, p.x, p.y + Options.INSTANCE.getFontsize());
        }
    }

    protected static class AutoCompleteRequirements {

        private final int from, to;
        private final Collection<String> words;

        public AutoCompleteRequirements(int from, int to, Collection<String> words) {
            this.from = from;
            this.to = to;
            this.words = words;
        }

    }

}
