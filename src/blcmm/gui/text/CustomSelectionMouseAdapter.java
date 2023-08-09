/*
 * Copyright (C) 2023 Christopher J. Kucera
 * <cj@apocalyptech.com>
 * <https://apocalyptech.com/contact.php>
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import javax.swing.text.JTextComponent;

/**
 * Custom MouseAdapter which provides some friendlier-to-Borderlands-data
 * selection behavior for doubleclicks.  Specifically, the default double-click-
 * to-select-word behavior in Swing/AWT doesn't include numbers in its sense of
 * what a "word" is.  So if you double-click on either end of
 * `SMG_Bandit_2_Uncommon`, for instance, you'll either select `SMG_Bandit`
 * or `Uncommon`.  This class allows the entire string to be selected, instead.
 *
 * The list of selection delimiters is quite a bit wider than the default
 * behavior -- this will also select entire URLs and the like.  The list of
 * delimiters is stored in a static HashSet near the top of the class, and
 * could possibly use expanding, though I think the current set is pretty good.
 *
 * The "better" way to solve this would be to figure out where AWT/Swing or
 * whatever keeps its definitions of what characters are part of a "word," and
 * then override those.  I'm not sure if such a thing is possible to override,
 * and got tired of digging through ever-more-obscure APIs, so I just went
 * ahead with this, instead.
 *
 * @author apocalyptech
 */
public class CustomSelectionMouseAdapter extends MouseAdapter {

    private final JTextComponent component;

    /**
     * The set of delimiters which are considered boundaries of a "word," in
     * terms of a double-click selection.
     */
    private final static HashSet<Character> delimiters = new HashSet<> ();
    static {
        delimiters.add('=');
        delimiters.add(' ');
        delimiters.add(',');
        delimiters.add('\t');
        delimiters.add('\n');
        delimiters.add('\r');
    }

    public CustomSelectionMouseAdapter(JTextComponent component) {
        this.component = component;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        this.handleSelection(me);
    }

    /**
     * Given a MouseEvent, handle any potential double-click which would result
     * in word selection.  Note that this investigates the state of the default
     * selection which happens, and that's only possible in mouseClicked.
     * Handling this in mousePressed won't work -- the default selection
     * behavior will end up overriding us.
     *
     * Note that the text component selection *will* visibly expand as the
     * result of this.  The default selection behavior will be visible for
     * a brief moment before this routine runs and (potentially) expands it.
     *
     * @param me The mouse event to process
     */
    private void handleSelection(MouseEvent me) {
        if (me.getButton() == 1 && me.getClickCount() == 2) {
            int selectStart = this.component.getSelectionStart();
            int selectEnd = this.component.getSelectionEnd();
            if (this.component.getSelectedText() != null && selectStart != selectEnd) {
                String text = this.component.getText();
                int maxSelect = text.length();
                for (; selectStart > 0; selectStart--) {
                    if (delimiters.contains(text.charAt(selectStart-1))) {
                        break;
                    }
                }
                for (; selectEnd < maxSelect; selectEnd++) {
                    if (delimiters.contains(text.charAt(selectEnd))) {
                        break;
                    }
                }
                this.component.setSelectionStart(selectStart);
                this.component.setSelectionEnd(selectEnd);
            }
        }
    }

}
