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
     * Returns whether or not a substring of the given CharSequence `s` is equal
     * to `searchString`.  Really this is checking for substring *equality* but
     * with an assumed substring length.
     *
     * OpenBLCMM v1.3.0 had some performance issues related to this method when
     * doing code formatting, though some v1.4.0 changes bypassed this method
     * as a bottleneck entirely, prior to discovering that problem.  A couple
     * of things were done to address it, regardless:
     *
     *   1. This originally did a naive `s.substring(startIndex).startsWith(searchString)`,
     *      which is slower than what we're now doing (potentially as much
     *      as 2x slower).  In the end, that wasn't the biggest deal, though.
     *
     *   2. OpenBLCMM originally had two functions -- one that took a String,
     *      and another which took a StringBuilder (which did a .toString() to
     *      pass through to the String version).  *This* is what was being super
     *      slow, in the end.  The right thing to do is to just use a
     *      CharSequence instead, which lets us get right to charAt.  To do
     *      that, we did need to do #1 as well, so at least that wasn't wasted
     *      effort.
     *
     * @param s The charsequence in which to search
     * @param startIndex The start index where we expect to find our search
     * @param searchString The string we're searching for
     * @return Whether or not the search string is found at the specified index
     */
    public static boolean substringStartsWith(CharSequence s, int startIndex, String searchString) {
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
