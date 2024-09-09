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
 */
package blcmm.gui.text;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * Custom Action which provides some friendlier-to-Borderlands-data
 * selection behavior for Ctrl-Shift-Left/Right.  This was previously handled
 * with a KeyAdapter-derived class which "cleaned up" after the default action
 * handlers did their selection, but that was a bit screwy and left a few edge
 * cases where we couldn't really get it to work how I wanted.  So, instead,
 * we're now totally overriding those actions.  Note that this is done via a
 * static method here, which will then set up both actions required to support
 * this.
 *
 * Differences to the default ctrl-shift-(arrow) behavior:
 *   1. The delimiters which decide what determines a "word" are much more
 *      accepting.  Most notably, the default behavior doesn't include numbers,
 *      so strings like "SMG_Bandit_2_Uncommon" would require multiple arrow
 *      hits to fully select.  Numbers are just the most notable example; the
 *      delimiter list is quite a bit more permissive than the defaults.
 *   2. Will *not* select trailing whitespace when going forward by words
 *   3. Will skip over newlines when consuming whitespace, rather than requiring
 *      a manual expansion with each line
 *
 * See also: CustomComponentMouseSelectionAction, which provides similar
 * improvements to the double-click-to-select behavior.
 *
 * @author apocalyptech
 */
public class CustomComponentKeySelectionAction extends AbstractAction {

    private final JTextComponent component;
    private final boolean forward;

    /**
     * Constructor.  Pass in the component that we'll be acting on, and a
     * boolean to denote whether we're handling a forward/next selection, or
     * a backwards/prev selection.  Note that this isn't really intended to
     * be called directly by other classes; instead use the `addToComponent`
     * static method.
     *
     * @param component The component
     * @param forward True for fwd/next, False for rev/prev
     */
    protected CustomComponentKeySelectionAction(JTextComponent component, boolean forward) {
        this.component = component;
        this.forward = forward;
    }

    /**
     * Adds our event handlers to the specified component.
     *
     * @param component The component to act on.
     */
    public static void addToComponent(JTextComponent component) {
        // I'm assuming we can rely on DefaultEditorKit.* to provide the proper event
        // names for us, here?  I suspect we could also just hardcode "selection-next-word"
        // and "selection-previous-word" and be very nearly as safe as using these
        // constants, but I suppose this is probably technically better.
        component.getActionMap().put(DefaultEditorKit.selectionNextWordAction, new CustomComponentKeySelectionAction(component, true));
        component.getActionMap().put(DefaultEditorKit.selectionPreviousWordAction, new CustomComponentKeySelectionAction(component, false));
    }

    /**
     * Actually do our selection work.  There is probably plenty here which
     * could be abstracted out to make it more compact; as it stands, it's
     * pretty unrolled, so to speak.  But whatever, it'll do for now.
     *
     * @param e The event (which we entirely ignore)
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        int selectStart = this.component.getSelectionStart();
        int selectEnd = this.component.getSelectionEnd();
        //GlobalLogger.log(selectStart + " -> " + selectEnd + ": \"" + this.component.getSelectedText() + "\"");

        String text = this.component.getText();
        int textLength = text.length();
        int maxSelect = textLength-1;
        Caret caret = this.component.getCaret();
        boolean start_before_end = true;

        if (this.forward) {
            if (caret.getDot() == selectEnd) {
                // Moving the selection end forward (ie: expanding the selection)

                // First, if we're on whitespace, run out any available whitespace
                for (; selectEnd < textLength; selectEnd++) {
                    if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectEnd))) {
                        break;
                    }
                }

                // If that brought us to the end of the string, don't bother doing anything else.
                if (selectEnd < textLength) {

                    // Next, if we're already *on* a delimiter char (which won't be whitespace, since
                    // we just ran that out), select any multiple of that delimiter char and then be
                    // done with it.  This is most likely to occur with the "===" headers in object
                    // dumps.
                    if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd))) {
                        char whichDelimiter = text.charAt(selectEnd);
                        while (true) {
                            selectEnd++;
                            if (selectEnd > maxSelect || text.charAt(selectEnd) != whichDelimiter) {
                                break;
                            }
                        }
                    } else {
                        // Otherwise, loop until we find the next delimiter (or EOS)
                        while (true) {
                            selectEnd++;
                            if (selectEnd > maxSelect || CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd))) {
                                break;
                            }
                        }
                    }
                }

            } else {
                // Moving the selection start forward (ie: shrinking the selection)
                start_before_end = false;

                // Grab our cur char; we know that there's at least one char to read, because
                // otherwise we'd be in the top half of this if (moving selection end forwards)
                char initialChar = text.charAt(selectStart);
                if (CustomSelectionDefinition.whitespace.contains(initialChar)) {
                    // If we're on whitespace, just run out any available whitespace and stop there.
                    for (; selectStart < selectEnd; selectStart++) {
                        if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectStart))) {
                            break;
                        }
                    }
                } else {
                    if (CustomSelectionDefinition.delimiters.contains(initialChar)) {
                        // If we're on a delimiter, deselect as many of that delimiter as we can
                        for (; selectStart < selectEnd; selectStart++) {
                            if (text.charAt(selectStart) != initialChar) {
                                break;
                            }
                        }
                    } else {
                        // Otherwise, loop until we find the next delimiter
                        for (; selectStart < selectEnd; selectStart++) {
                            if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart))) {
                                break;
                            }
                        }
                    }

                    // and finally, since we consumed either a string of the same delimiter, or
                    // a bunch of non-delimiter text, eat any whitespace that might be remaining.
                    for (; selectStart < selectEnd; selectStart++) {
                        if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectStart))) {
                            break;
                        }
                    }
                }

            }
        } else {
            if (caret.getDot() == selectStart) {
                // Moving the selection start backwards (ie: expanding the selection)
                start_before_end = false;

                // First, if we're on whitespace, run out any available whitespace
                for (; selectStart > 0; selectStart--) {
                    if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectStart-1))) {
                        break;
                    }
                }

                // If that brought us to the start of the string, don't bother doing anything else.
                if (selectStart > 0) {

                    // Next, if we're already *on* a delimiter char (which won't be whitespace, since
                    // we just ran that out), select any multiple of that delimiter char and then be
                    // done with it.  This is most likely to occur with the "===" headers in object
                    // dumps.
                    if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart-1))) {
                        char whichDelimiter = text.charAt(selectStart-1);
                        while (true) {
                            selectStart--;
                            if (selectStart == 0 || text.charAt(selectStart-1) != whichDelimiter) {
                                break;
                            }
                        }
                    } else {
                        // Otherwise, loop until we find the next delimiter (or EOS)
                        while (true) {
                            selectStart--;
                            if (selectStart == 0 || CustomSelectionDefinition.delimiters.contains(text.charAt(selectStart-1))) {
                                break;
                            }
                        }
                    }
                }


            } else {
                // Moving the selection end backwards (ie: shrinking the selection)

                // Grab our previous char; we know that there's at least one char to read, because
                // otherwise we'd be in the top half of this if (moving selection start backwards)
                char initialChar = text.charAt(selectEnd-1);
                if (CustomSelectionDefinition.whitespace.contains(initialChar)) {
                    // If we're on whitespace, just run out any available whitespace and stop there.
                    for (; selectEnd > selectStart; selectEnd--) {
                        if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectEnd-1))) {
                            break;
                        }
                    }
                } else {
                    if (CustomSelectionDefinition.delimiters.contains(initialChar)) {
                        // If we're on a delimiter, deselect as many of that delimiter as we can
                        for (; selectEnd > selectStart; selectEnd--) {
                            if (text.charAt(selectEnd-1) != initialChar) {
                                break;
                            }
                        }
                    } else {
                        // Otherwise, loop until we find the next delimiter
                        for (; selectEnd > selectStart; selectEnd--) {
                            if (CustomSelectionDefinition.delimiters.contains(text.charAt(selectEnd-1))) {
                                break;
                            }
                        }
                    }

                    // and finally, since we consumed either a string of the same delimiter, or
                    // a bunch of non-delimiter text, eat any whitespace that might be remaining.
                    for (; selectEnd > selectStart; selectEnd--) {
                        if (!CustomSelectionDefinition.whitespace.contains(text.charAt(selectEnd-1))) {
                            break;
                        }
                    }
                }
            }
        }

        // Finally, update the caret
        if (selectStart == selectEnd) {
            caret.setDot(selectStart);
            caret.setSelectionVisible(false);
        } else {
            if (start_before_end) {
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
