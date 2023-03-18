/*
 * Copyright (C) 2023 CJ Kucera
 *
 * BLCMM is free software: you can redistribute it and/or modify
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
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */

package blcmm.data;

/**
 * Some custom processing methods for BehaviorProviderDefinitions.
 *
 * This is mainly just here to support conversion of ArrayIndexAndLength
 * and LinkIdAndLinkedBehavior values to more human-useful numbers, for
 * modders doing BPD modding.  These are shown in the main BLCMM window
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
