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
import blcmm.utilities.Options;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 *
 * @author LightChaosman
 */
public class AutoCompleteList extends JScrollPane {

    JList<String> list;
    private final JTextComponent textComponent;
    AutoCompleteAttacher auto;

    int from = 0, to = 0;
    private JPopupMenu menu;
    private boolean useValueInParens;

    public AutoCompleteList(JTextComponent textComponent) {
        this(textComponent, false);
    }

    public AutoCompleteList(JTextComponent textComponent, boolean useValueInParens) {
        list = new JList<>();
        super.setViewportView(list);
        this.useValueInParens = useValueInParens;

        this.textComponent = textComponent;
        list.setSelectionForeground(new Color(200, 0, 0));
        list.setFont(new java.awt.Font(MainGUI.CODE_FONT_NAME, 0, Options.INSTANCE.getFontsize()));
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                    action();
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    menu.setVisible(false);
                    textComponent.requestFocusInWindow();
                }
            }

        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    action();
                }
            }

        });
    }

    public void moveDown() {
        int index = list.getSelectedIndex();
        if (index < list.getModel().getSize() - 1) {
            index++;
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    public void moveUp() {
        int index = list.getSelectedIndex();
        if (index > 0) {
            index--;
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    public void setModel(ListModel<String> model) {
        list.getSelectionModel().setAnchorSelectionIndex(0);
        list.getSelectionModel().setSelectionInterval(0, 0);
        list.setModel(model);
        int w = list.getPreferredSize().width;
        int h = list.getPreferredSize().height;
        setPreferredSize(new Dimension(w + 25, Math.min(h + 15, 475)));
    }

    public void setFromTo(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public void action() {
        String x = list.getSelectedValue();
        if (x != null) {
            action(x);
        }
    }

    private void action(String w) {
        try {
            if (this.useValueInParens && w.contains("(")) {
                w = w.substring(w.indexOf("(") + 1, w.indexOf(")"));
            }
            textComponent.getDocument().insertString(to, w, null);
            textComponent.getDocument().remove(from, to - from);
            menu.setVisible(false);
            textComponent.requestFocus();
        } catch (BadLocationException ex) {
            Logger.getLogger(HighlightedTextArea.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void setMenu(JPopupMenu menu) {
        this.menu = menu;
    }

}
