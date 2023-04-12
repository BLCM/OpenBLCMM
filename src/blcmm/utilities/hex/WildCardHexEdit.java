/*
 * Copyright (C) 2018-2020  LightChaosman
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
 */
package blcmm.utilities.hex;

import java.util.Arrays;

/**
 *
 * @author LightChaosman
 */
class WildCardHexEdit extends HexEdit {

    //The pattern to look for
    public final short[] searchPattern;

    WildCardHexEdit(String searchPattern, String original, String replacement) {
        this(HexUtilities.convertToShortArray(searchPattern), original, replacement);
    }

    WildCardHexEdit(short[] searchPattern, String original, String replacement) {
        this(searchPattern, HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    WildCardHexEdit(short[] searchPattern, byte[] original, byte[] replacement) {
        super(original, replacement);
        this.searchPattern = searchPattern;
    }

    @Override
    public int requiredBufferSize() {
        return searchPattern.length;
    }

    @Override
    public int requiredNegativeBufferSize() {
        return 0;
    }

    @Override
    public boolean match(byte[] bytes, int startIndexInArray, int globalOffset) {
        return HexUtilities.matches(bytes, startIndexInArray, searchPattern);
    }

    @Override
    public HexEditor.HexResultStatus replace(byte[] bytes, int startIndexInArray, int globalOffset, boolean force) {
        if (startIndexInArray + replacement.length > bytes.length) {
            throw new IndexOutOfBoundsException(); //Should never happen, but maybe in the last iteration
        }
        if (!match(bytes, startIndexInArray, globalOffset)) {
            return null;
        } else if (force) {
            HexUtilities.replace(bytes, startIndexInArray, fillInWildCards(replacement));
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else if (HexUtilities.matches(bytes, startIndexInArray, fillInWildCards(replacement))) {
            return HexEditor.HexResultStatus.HEXEDIT_ALREADY_DONE;
        } else if (HexUtilities.matches(bytes, startIndexInArray, fillInWildCards(original))) {
            HexUtilities.replace(bytes, startIndexInArray, fillInWildCards(replacement));
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else {
            return HexEditor.HexResultStatus.ERROR_UNKNOWN_BYTE_PATTERN_FOUND;
        }
    }

    @Override
    public HexInspectResult inspect(byte[] bytes, int startIndexInArray, int globalOffset) {
        byte[] bts = new byte[searchPattern.length];
        System.arraycopy(bytes, startIndexInArray, bts, 0, bts.length);
        return new HexInspectResult(this, startIndexInArray + globalOffset, bts) {

            @Override
            public boolean matchesEdited() {
                return Arrays.equals(found, fillInWildCards(replacement));
            }

            @Override
            public boolean matchesOriginal() {
                return Arrays.equals(found, fillInWildCards(original));
            }

        };
    }

    private byte[] fillInWildCards(byte[] fill) {
        byte[] bts = new byte[searchPattern.length];
        int index = 0;
        for (int i = 0; i < bts.length; i++) {
            if (searchPattern[i] == HexUtilities.WILDCARD) {
                bts[i] = fill[index++];
            } else {
                bts[i] = (byte) searchPattern[i];
            }

        }
        return bts;
    }

    @Override
    public WildCardHexEdit getInvertedCopy() {
        return new WildCardHexEdit(searchPattern, replacement, original);
    }

}
