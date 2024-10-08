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

import java.util.HashSet;

/**
 * Shared definitions for our custom selection handling which attempts to make
 * selecting object names in OpenBLCMM a bit easier.  This will be used for
 * both double-click-to-select, and for Ctrl-Shift-Left/Right-to-select.  Since
 * it's got to be available for both Mouse and Key listeners, it made sense to
 * at least pull this out here.  The actual selection handling seems like it's
 * still best left to the individual handlers, since it can be subtly different.
 * Trying to abstract it in here would, IMO, make it a bit too complex.
 *
 * Regardless, at the moment this just defines the characters which we consider
 * to delimit a "word," when selecting.  This is quite a bit less restrictive
 * than the default Java handling -- a single selection can easily select an
 * entire URL, for instance, now.
 *
 * A better way to implement this is probably to figure out how to override
 * the Document's sense of what a "word" is so that the default selection
 * behavior does the "right" thing.  But, after a bit of searching I couldn't
 * even find a hint of where that was stored, and just did this instead.
 *
 * @author apocalyptech
 */
public class CustomSelectionDefinition {

    /**
     * The set of delimiters which are considered boundaries of a "word," in
     * terms of a double-click selection.
     */
    public final static HashSet<Character> delimiters = new HashSet<> ();
    static {
        delimiters.add('=');
        delimiters.add(' ');
        delimiters.add(',');
        delimiters.add(';');
        delimiters.add('(');
        delimiters.add(')');
        // Originally we'd excluded square brakets because I thought it'd be
        // useful to be able to copy whole attributes (including array indexes)
        // but it's apparently too annoying in other contexts.  So, square
        // brackets are in.
        delimiters.add('[');
        delimiters.add(']');
        delimiters.add('\t');
        delimiters.add('\n');
        delimiters.add('\r');
        // Note that we very explicitly *don't* want to add apostrophe to the
        // list of delimiters, because that's used by fully-formed object
        // references, and we want to select all of that, when we can.  Double
        // quote marks will be useful to delimit, though, so add that in.
        delimiters.add('"');
    }

    /**
     * Also, the set of delimiters which are specifically whitespace.  For
     * our ctrl-shift-(left|right) handling, we want to handle these a bit
     * differently.  This is intended to be a subset of `delimiters`.
     */
    public final static HashSet<Character> whitespace = new HashSet<> ();
    static {
        whitespace.add(' ');
        whitespace.add('\t');
        whitespace.add('\n');
        whitespace.add('\r');
    }

}
