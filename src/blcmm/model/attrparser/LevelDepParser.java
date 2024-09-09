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

package blcmm.model.attrparser;

/**
 * Attribute parser for LevelList attrs (as part of LevelDependencyList
 * objects), for use in level/map merging.  Implemented as part of the rewrite
 * of the datalib in 2023 -- much of the functionality in here was previously
 * inside the BorderlandsArray and BorderlandsStruct classes in the original
 * BLCMM_Data_Interaction_Library.jar.  This rewrite was done without reference
 * to the original.
 *
 * This class does NOT attempt to be a general-purpose attribute parser, though
 * maybe it wouldn't be hard to expand it to be so.  At a bare minimum it'd
 * need to include support for numeric data, but I'm sure there's plenty of
 * other holes in its parsing functionality.
 *
 * If expanding this to become general purpose ever happens (or, possibly
 * better, if it's rewritten entirely), it'd be worth renaming it to just
 * AttrParser or something (and also rename its associated other classes).  At
 * the moment, it basically just does the bare minimum necessary to support
 * LevelDependencyList "LevelList" attributes, though.  This has not been
 * tested at all on structures other than that.
 *
 * @author apocalyptech
 */
public class LevelDepParser {

    private static int pos;
    private static String data;
    private static char[] chars;

    /**
     * Parse the given attribute.
     *
     * @param dataString The full attribute value to be parsed
     * @return The top-level object describing the data
     */
    public static LevelDepData parse(String dataString) {
        //GlobalLogger.log("Attempting to parse: " + dataString);
        data = dataString;
        pos = 0;
        chars = data.toCharArray();
        return nextBit();
    }

    /**
     * Parse the next "bit" of the attribute, from the current position in the
     * string.
     *
     * @return The LevelDepData describing the next bit of the attribute
     */
    private static LevelDepData nextBit() {
        if (pos >= chars.length) {
            return null;
        }
        switch (chars[pos]) {
            case '(':
                if (pos+1 < chars.length) {
                    pos++;
                    switch (chars[pos]) {
                        case '(':
                        case '"':
                            return parseArray();
                        case ')':
                            pos++;
                            return new LevelDepArray();
                        default:
                            // At this point, we annoyingly have to do a lookahead to figure
                            // out what the next datatype is.  (Well, that or we could be
                            // processing this differently and just not settle on a datatype
                            // until we're sure, which would probably be a better solution
                            // anyway if this ever gets expanded to be more generally useful.)
                            for (int i=pos; i<chars.length; i++) {
                                switch (chars[i]) {
                                    case '=':
                                        // We found an equals sign first; it's a struct
                                        return parseStruct();
                                    case ',':
                                    case ')':
                                        // Looks like an array, instead.
                                        return parseArray();
                                }
                            }
                            // What to do here?  I guess just assume it's an array.
                            return parseArray();
                    }
                } else {
                    return new LevelDepArray();
                }
            case '"':
                return parseQuotedString();
            default:
                return parseString();
        }
    }

    /**
     * Parse the next bit of the attribute as an array/list.  This assumes that
     * the current position in the attribute is already past the opening paren.
     *
     * @return The parsed array
     */
    private static LevelDepArray parseArray() {
        LevelDepArray<LevelDepData> arr = new LevelDepArray();
        outer:
        while (pos < chars.length) {
            switch (chars[pos]) {
                case ',':
                    pos++;
                    arr.add(null);
                    break;
                case ')':
                    pos++;
                    break outer;
                default:
                    arr.add(nextBit());
                    if (chars[pos] == ',') {
                        pos++;
                    }
                    break;
            }
        }
        return arr;
    }

    /**
     * Parse the next bit of the attribute as a struct/hash/dict/whatever.
     * This assumes that the current position in the attribute is already
     * past the opening paren.
     *
     * @return The parsed struct
     */
    private static LevelDepStruct parseStruct() {
        LevelDepStruct struct = new LevelDepStruct();
        StringBuilder keyBuilder;
        outer:
        while (pos < chars.length) {
            keyBuilder = new StringBuilder();
            switch (chars[pos]) {
                case ')':
                    pos++;
                    break outer;
                case ',':
                    pos++;
                    break;
                default:
                    keySearch:
                    while (pos < chars.length) {
                        if (chars[pos] == '=') {
                            pos++;
                            break;
                        } else {
                            keyBuilder.append(chars[pos]);
                        }
                        pos++;
                    }
                    struct.put(keyBuilder.toString(), nextBit());
                    break;
            }
        }
        return struct;
    }

    /**
     * Parse the next bit of the attribute as an unquoted string.  In our
     * current setup, this could be an object reference, enum, or some other
     * unquoted string.
     *
     * @return The parsed string
     */
    private static LevelDepString parseString() {
        StringBuilder sb = new StringBuilder();
        while (pos < chars.length) {
            if (chars[pos] == ',' || chars[pos] == ')') {
                break;
            }
            sb.append(chars[pos]);
            pos++;
        }
        return new LevelDepString(sb.toString());
    }

    /**
     * Parse the next bit of the attribute as a quoted string.  Should hopefully
     * handle escaping properly.  If there's data beyond the closing quote but
     * before the next comma or end-paren, that data will end up getting
     * discarded.
     *
     * @return The parsed string
     */
    private static LevelDepString parseQuotedString() {
        StringBuilder sb = new StringBuilder();
        // It's assumed that this.pos is at the starting quote
        pos++;
        boolean nextIsEscaped = false;
        outer:
        while (pos < chars.length) {
            if (nextIsEscaped) {
                sb.append(chars[pos]);
                nextIsEscaped = false;
            } else {
                switch (chars[pos]) {
                    case '"':
                        pos++;
                        break outer;
                    case '\\':
                        nextIsEscaped = true;
                    default:
                        sb.append(chars[pos]);
                        break;
                }
            }
            pos++;
        }

        // Check to see if we've *actually* reached the end of the field, or
        // if there's more data (due, for instance, to a missing comma, or
        // something).  There are a few options of what to do in this case:
        //
        //   1) Interpret the remainder as another array entry, if we're
        //      processing an array.  This would technically fix a bug in
        //      Fast Travel Farms, but I don't really want to do that globally.
        //
        //   2) Just mush the whole string together and hope for the best.  That's
        //      what BLCMM used to do, but I don't like that either.  Doesn't
        //      seem like predictable behavior to me.
        //
        //   3) Just discard everything after what should be the close-quote.
        //      That's what I'm opting to do here.
        if (pos < chars.length && chars[pos] != ',' && chars[pos] != ')') {
            if (chars[pos] == '"') {
                parseQuotedString();
            } else {
                parseString();
            }
        }

        return new LevelDepString(sb.toString(), true);
    }
}
