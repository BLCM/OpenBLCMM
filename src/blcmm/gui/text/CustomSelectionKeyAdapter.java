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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

/**
 * Custom KeyAdapter which provides some friendlier-to-Borderlands-data
 * selection behavior for Ctrl-Shift-Left/Right.  Specifically, the default
 * word-boundary behavior in Swing/AWT doesn't include numbers in its sense of
 * what a "word" is.  For instance, doing Ctrl-Shift-Right through the word
 * `SMG_Bandit_2_Uncommon` will take five arrows (SMG_Bandit, _, 2, _, and
 * Uncommon), whereas we'd want to do it in one.
 *
 * @author apocalyptech
 */
public class CustomSelectionKeyAdapter extends KeyAdapter {

    private final JTextComponent component;

    public CustomSelectionKeyAdapter(JTextComponent component) {
        this.component = component;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.isShiftDown() && (e.isControlDown() || e.isMetaDown())) {
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                this.handleSelect(true);
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                this.handleSelect(false);
            }
        }
    }

    /**
     * Handle a selection event, being told whether we're going forward (ie:
     * towards the end of the text selection) or backwards (ie: to the
     * beginning).  This will also inspect the current position of the caret
     * to find out if the caret is to the right or left of the selection start,
     * so we can keep the selection state sensible.  (The caret to the right
     * of the selection start is considered "regular" in the code.)
     *
     * This expects that the relevant KeyEvent has already been checked to make
     * sure that we're processing one of those possibilities.  This routine
     * doesn't have quite as much special cases processing as
     * CustomSelectionMouseAdapter does, since the selection may span many
     * tokens in the text, which would make custom quote handling much more
     * difficult.
     *
     * @param forward True if the key pressed is Right, false if Left
     */
    private void handleSelect(boolean forward) {
        int selectStart = this.component.getSelectionStart();
        int selectEnd = this.component.getSelectionEnd();
        //GlobalLogger.log(selectStart + " -> " + selectEnd + ": \"" + this.component.getSelectedText() + "\"");
        if (this.component.getSelectedText() != null && selectStart != selectEnd) {
            String text = this.component.getText();
            int maxSelect = text.length();
            Caret caret = this.component.getCaret();
            boolean regular = true;

            if (forward) {
                if (caret.getDot() == selectStart) {
                    // Moving the selection start forward
                    for (; selectStart < selectEnd; selectStart++) {
                        if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart-1))) {
                            break;
                        }
                    }
                    regular = false;
                } else {
                    // Moving the selection end forward
                    for (; selectEnd < maxSelect; selectEnd++) {
                        if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd-1))) {
                            break;
                        }
                    }
                }
            } else {
                if (caret.getDot() == selectStart) {
                    // Moving the selection start backwards
                    for (; selectStart > 0; selectStart--) {
                        if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart-1))) {
                            break;
                        }
                    }
                    regular = false;
                } else {
                    // Moving the selection end backwards
                    if (text.charAt(selectEnd) == '\n') {
                        // Bit of a special case to make backing up after a newline
                        // a bit more natural-feeling.
                        selectEnd--;
                    }
                    for (; selectEnd > selectStart; selectEnd--) {
                        if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd))) {
                            break;
                        }
                    }
                }
            }

            // Finally, update the caret
            if (selectStart == selectEnd) {
                caret.setDot(selectStart);
                caret.setSelectionVisible(false);
            } else {
                if (regular) {
                    caret.setDot(selectStart);
                    caret.moveDot(selectEnd);
                } else {
                    caret.setDot(selectEnd);
                    caret.moveDot(selectStart);
                }
                caret.setSelectionVisible(true);
            }
        }
    }

}
