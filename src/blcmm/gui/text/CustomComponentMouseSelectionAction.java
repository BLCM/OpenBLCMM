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
 * selection behavior for doubleclicks.  Specifically, the default double-click-
 * to-select-word behavior in Swing/AWT doesn't include numbers in its sense of
 * what a "word" is.  So if you double-click on either end of
 * `SMG_Bandit_2_Uncommon`, for instance, you'll either select `SMG_Bandit`
 * or `Uncommon`.  This class allows the entire string to be selected, instead.
 *
 * This also does some custom shenanigans to ensure that copying the type and
 * object name from the initial "Property dump" line only includes the data
 * the user's likely to want.
 *
 * Additionally, this implementation sidesteps a weird issue which happens with
 * this sort of selection, where the end of the "word" isn't visible in the
 * containing JScrollPane -- see the top of the `actionPerformed` method for
 * details.  I have no idea how to solve that "properly," or for instances where
 * you've otherwise got "vanilla" AWT/Swing widgets.
 *
 * See also: CustomComponentKeySelectionAction, which provides similar
 * improvements to the keyboard-based ctrl-shift-(arrow) selection mechanism.
 *
 * @author apocalyptech
 */
public class CustomComponentMouseSelectionAction extends AbstractAction {

    private final JTextComponent component;
    private long lastTriggeredTime = 0;

    /**
     * We behave a bit differently depending on the type of character under the
     * Caret.  The regular behavior is to expand until we hit a delimiter of
     * any sort.  If the Caret is on whitespace, we select all adjacent
     * whitespace.  If the Caret's on a delimiter, select as many contiguous
     * chars of *that* delimiter as possible.  (Be sure to check for whitespace
     * first, since whitespace is also technically a delimiter!)
     */
    private enum ExpansionType {
        REGULAR,
        WHITESPACE,
        DELIMITER,
    };

    /**
     * Constructor.  Pass in the component that we'll be acting on.  Note that
     * this isn't really intended to be called directly by other classes;
     * instead use the `addToComponent` static method.
     *
     * @param component The component
     */
    protected CustomComponentMouseSelectionAction(JTextComponent component) {
        this.component = component;
    }

    /**
     * Adds our event handlers to the specified component.
     *
     * @param component The component to act on.
     */
    public static void addToComponent(JTextComponent component) {
        // I'm assuming we can rely on DefaultEditorKit.* to provide the proper event
        // name for us, here?  I suspect we could also just hardcode the string
        // itself and be very nearly as safe as using the constant, but I
        // suppose this is probably technically better.
        component.getActionMap().put(DefaultEditorKit.selectWordAction, new CustomComponentMouseSelectionAction(component));
    }

    /**
     * Actually do our selection work.  There is probably plenty here which
     * could be abstracted out to make it more compact (especially when
     * considering CustomComponentKeySelectionAction too); as it stands, it's
     * pretty unrolled, so to speak.  But whatever, it'll do for now.
     *
     * @param e The event (which we entirely ignore)
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // Ignore double-click requests which come in <150ms after the last one.
        // We're doing this due to a really weird issue I found which happens
        // even with "stock" Java widgets.  If the *end* of the word-to-be-selected
        // is scrolled off to the right, a *second* select-word event gets
        // triggered at the very end of the line, which then selects the
        // newline and leaves the Caret at the beginning of the next line, with
        // the word no-longer-selected.  No idea what causes that, but it's
        // present in entirely vanilla AWT/Swing stuff, and reproduceable from
        // Java 8 through Java 20, at least.  Small test case:
        //
        //    https://gist.github.com/apocalyptech/3585566f4079c53a18b01262689b892e
        //
        // Anyway, a real stupid way to "fix" this is to just keep track of the
        // timestamps when we're triggered, and deny an event if it's come in
        // too close to the previous one.  In testing I was getting about
        // 70-80ms, so having a 150ms threshhold seems like it should accomodate
        // slower systems (or larger blocks-of-selected-text) sufficiently.
        // This means we'd only be able to support ~6 double-clicks per second,
        // but really, who's double-clicking that quickly anyway?
        long currentTime = System.currentTimeMillis();
        //GlobalLogger.log("Checking event, orig: " + this.lastTriggeredTime + ", new: " + currentTime + ", diff: " + (currentTime - this.lastTriggeredTime));
        if (currentTime < this.lastTriggeredTime + 150) {
            return;
        }
        this.lastTriggeredTime = currentTime;

        // Pull a bunch of info about what we're doing
        String text = this.component.getText();
        int textLength = text.length();
        int maxSelect = textLength-1;
        Caret caret = this.component.getCaret();
        int startPos = caret.getDot();
        int selectStart = startPos;
        int selectEnd = startPos;
        boolean goForward = startPos < textLength;
        boolean goBack = startPos > 0;
        ExpansionType expansionType = ExpansionType.REGULAR;
        Character delimChar = null;

        //GlobalLogger.log("Got double-click-to-select at position: " + startPos);

        // See what type of char we're starting on and set our expansion type
        // appropriately.
        if (goForward) {
            char startChar = text.charAt(startPos);
            if (CustomSelectionDefinition.whitespace.contains(startChar)) {
                expansionType = ExpansionType.WHITESPACE;
            } else if (CustomSelectionDefinition.delimiters.contains(startChar)) {
                delimChar = startChar;
                expansionType = ExpansionType.DELIMITER;
            }
        } else if (textLength > 0) {
            char startChar = text.charAt(startPos-1);
            if (CustomSelectionDefinition.whitespace.contains(startChar)) {
                expansionType = ExpansionType.WHITESPACE;
            } else if (CustomSelectionDefinition.delimiters.contains(startChar)) {
                delimChar = startChar;
                expansionType = ExpansionType.DELIMITER;
            }
        }

        // Now expand our selection appropriately.  Backwards first!
        char loopChar;
        if (goBack) {
            loop: for (; selectStart > 0; selectStart--) {
                loopChar = text.charAt(selectStart-1);
                switch (expansionType) {
                    case WHITESPACE:
                        if (!CustomSelectionDefinition.whitespace.contains(loopChar)) {
                            break loop;
                        }
                        break;
                    case DELIMITER:
                        if (loopChar != delimChar) {
                            break loop;
                        }
                        break;
                    case REGULAR:
                    default:
                        if (CustomSelectionDefinition.delimiters.contains(loopChar)) {
                            break loop;
                        }
                        break;
                }
            }
        }

        // Now forwards
        if (goForward) {
            loop: while (true) {
                selectEnd++;
                if (selectEnd > maxSelect) {
                    break loop;
                }
                loopChar = text.charAt(selectEnd);
                switch (expansionType) {
                    case WHITESPACE:
                        if (!CustomSelectionDefinition.whitespace.contains(loopChar)) {
                            break loop;
                        }
                        break;
                    case DELIMITER:
                        if (loopChar != delimChar) {
                            break loop;
                        }
                        break;
                    case REGULAR:
                    default:
                        if (CustomSelectionDefinition.delimiters.contains(loopChar)) {
                            break loop;
                        }
                        break;
                }
            }
        }

        // Finally, update the caret
        if (selectStart == selectEnd) {
            caret.setDot(selectStart);
            caret.setSelectionVisible(false);
        } else {
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

            // Now, finally, update our caret
            caret.setDot(selectStart);
            caret.moveDot(selectEnd);
            caret.setSelectionVisible(true);
        }

    }

}
