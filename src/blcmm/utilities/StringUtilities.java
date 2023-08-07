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

package blcmm.utilities;

/**
 * Miscellaneous string utilities for OpenBLCMM.  This class was reimplemented based
 * on the calls BLCMM made into BLCMM_Utilities.jar, without reference to the
 * original sourcecode.
 *
 * This class used to live under general.utilities, but it's been moved
 * under blcmm.utilities as part of its opensourcing.
 *
 * @author apocalyptech
 */
public class StringUtilities {

    /**
     * Returns whether or not a substring of the given StringBuilder `sb` is
     * equal to `searchString`.  See the String version below for some more
     * detailed notes.
     *
     * @param sb The StringBuilder in which to search
     * @param startIndex The start index where we expect to find our search
     * @param searchString The string we're searching for
     * @return Whether or not the search string is found at the specified index
     */
    public static boolean substringStartsWith(StringBuilder sb, int startIndex, String searchString) {
        return StringUtilities.substringStartsWith(sb.toString(), startIndex, searchString);
    }

    /**
     * Returns whether or not a substring of the given String `s` is equal to
     * `searchString`.  Really this is checking for substring *equality* but
     * with an assumed substring length.
     *
     * The original implementation of this function for OpenBLCMM was just
     * doing a naive `s.substring(startIndex).startsWith(searchString)`, but
     * it turns out that that's pretty inefficient, probably related to having
     * to instantiate a new String object for the substring step.  So, this was
     * streamlined for v1.4.0, and probably more closely resembles the original
     * BLCMM implementation.  The slower version was noticeable when formatting
     * large blocks of code in v1.3.× (though v1.4.0 ended up getting rid of
     * the function making most of the calls in here, so this streamlining isn't
     * actually necessary to speed that back up, anymore).
     *
     * @param s The string in which to search
     * @param startIndex The start index where we expect to find our search
     * @param searchString The string we're searching for
     * @return Whether or not the search string is found at the specified index
     */
    public static boolean substringStartsWith(String s, int startIndex, String searchString) {
        int endIndex = startIndex + searchString.length();
        if (endIndex > s.length()) {
            return false;
        }
        int searchLength = searchString.length();
        for (int i=0; i<searchLength; i++) {
            if (s.charAt(startIndex+i) != searchString.charAt(i)) {
                return false;
            }
        }
        return true;
    }

}
