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

package blcmm.data;

/**
 * Some custom processing methods for BehaviorProviderDefinitions.
 *
 * This is mainly just here to support conversion of ArrayIndexAndLength
 * and LinkIdAndLinkedBehavior values to more human-useful numbers, for
 * modders doing BPD modding.  These are shown in the main OpenBLCMM window
 * when statements are specifically going after a single value, and also
 * available via a little calculator window.
 *
 * @author apocalyptech
 */
public class BehaviorProviderDefinition {

    public static int getIndexFromArrayIndexAndLength(int value) {
        return value >> 16;
    }

    public static int getLengthFromArrayIndexAndLength(int value) {
        return value & 0xFFFF;
    }

    public static int getArrayIndexAndLength(int index, int length) {
        return (index << 16) | (length & 0xFFFF);
    }

    public static int getLinkIdFromLinkIdAndLinkedBehavior(int value) {
        return value >> 24;
    }

    public static int getBehaviorFromLinkIdAndLinkedBehavior(int value) {
        return value & 0xFFFF;
    }

    public static int getLinkIdAndLinkedBehavior(int linkId, int behavior) {
        return (linkId << 24) | (behavior & 0xFFFF);
    }

}
