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
import javax.swing.text.JTextComponent;

/**
 * Custom MouseAdapter which provides some friendlier-to-Borderlands-data
 * selection behavior for doubleclicks.  Specifically, the default double-click-
 * to-select-word behavior in Swing/AWT doesn't include numbers in its sense of
 * what a "word" is.  So if you double-click on either end of
 * `SMG_Bandit_2_Uncommon`, for instance, you'll either select `SMG_Bandit`
 * or `Uncommon`.  This class allows the entire string to be selected, instead.
 *
 * See also: CustomComponentKeySelectionAction, which provides similar
 * improvements to the keyboard-based ctrl-shift-(arrow) selection mechanism.
 *
 * @author apocalyptech
 */
public class CustomSelectionMouseAdapter extends MouseAdapter {

    private final JTextComponent component;

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
                    if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart-1))) {
                        break;
                    }
                }
                for (; selectEnd < maxSelect; selectEnd++) {
                    if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd))) {
                        break;
                    }
                }

                // Now for some messy custom tweaks, to make object-name
                // selection a bit friendlier.  Object dump headers start out
                // with this:
                //
                //    *** Property dump for object 'Type ObjName' ***
                //
                // When someone doubleclicks that `Type`, we don't want the
                // leading apostrophe, and when someone doubleclicks `ObjName`,
                // we don't want the trailing apostrophe.  However, when
                // double-clicking "full" object name references like this:
                //
                //     Type'ObjName'
                //
                // ... we *do* want both apostrophes in there.  So the thought
                // is: don't ever include a leading apostrophe, and only
                // include a trailing apostrophe if there's also an apostrophe
                // somewhere inside the "inner" text.  I'm going to go so far
                // as to say that we should only do a trailing apostrophe when
                // there is *exactly* one inner apostrophe.

                // So, omit leading apostrophes:
                if (text.charAt(selectStart) == '\''
                        && selectStart < selectEnd-1)
                {
                    selectStart++;
                }

                // ... and do a check if we have a trailing one:
                if (text.charAt(selectEnd-1) == '\''
                        && selectEnd-1 > selectStart) {
                    int innerApostropheCount = 0;
                    for (int i=selectStart+1; i<selectEnd-2; i++) {
                        if (text.charAt(i) == '\'') {
                            innerApostropheCount++;
                        }
                    }
                    //GlobalLogger.log("Inner apostrophe count: " + innerApostropheCount);
                    if (innerApostropheCount != 1) {
                        selectEnd--;
                    }
                }

                // Finally, update our selection
                this.component.setSelectionStart(selectStart);
                this.component.setSelectionEnd(selectEnd);
            }
        }
    }

}
