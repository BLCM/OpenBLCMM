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

/**
 *
 * @author LightChaosman
 */
class PatternHexEdit extends HexEdit {

    //The pattern to look for
    public final short[] searchPattern;
    public final int offset;

    PatternHexEdit(String original, String replacement) {
        this(HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    PatternHexEdit(byte[] original, byte[] replacement) {
        this(HexUtilities.convertToShortArray(original), 0, original, replacement);
    }

    PatternHexEdit(String searchPattern, String original, String replacement) {
        this(HexUtilities.convertToShortArray(searchPattern), original, replacement);
    }

    PatternHexEdit(short[] searchPattern, String original, String replacement) {
        this(searchPattern, searchPattern.length, original, replacement);
    }

    PatternHexEdit(short[] searchPattern, byte[] original, byte[] replacement) {
        this(searchPattern, searchPattern.length, original, replacement);
    }

    PatternHexEdit(String searchPattern, int offset, String original, String replacement) {
        this(HexUtilities.convertToShortArray(searchPattern), offset, original, replacement);
    }

    PatternHexEdit(short[] searchPattern, int offset, String original, String replacement) {
        this(searchPattern, offset, HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    PatternHexEdit(short[] searchPattern, int offset, byte[] original, byte[] replacement) {
        super(original, replacement);
        this.searchPattern = searchPattern;
        this.offset = offset;
    }

    @Override
    public int requiredBufferSize() {
        return Math.max(searchPattern.length, offset + replacement.length);
    }

    @Override
    public int requiredNegativeBufferSize() {
        return Math.max(-offset, 0);
    }

    @Override
    public boolean match(byte[] bytes, int startIndexInArray, int globalOffset) {
        return HexUtilities.matches(bytes, startIndexInArray, searchPattern);
    }

    @Override
    public HexEditor.HexResultStatus replace(byte[] bytes, int startIndexInArray, int globalOffset, boolean force) {
        int startOfReplacement = startIndexInArray + offset;
        if (startOfReplacement < 0) {
            throw new IndexOutOfBoundsException(); //should only happen if offset <0 and we find a match in the first -offset bytes in the file
        } else if (startOfReplacement + replacement.length > bytes.length) {
            throw new IndexOutOfBoundsException(); //Should never happen, but maybe in the last iteration
        }
        if (!match(bytes, startIndexInArray, globalOffset)) {
            return null;
        } else if (force) {
            HexUtilities.replace(bytes, startOfReplacement, replacement);
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else if (HexUtilities.matches(bytes, startOfReplacement, replacement)) {
            return HexEditor.HexResultStatus.HEXEDIT_ALREADY_DONE;
        } else if (HexUtilities.matches(bytes, startOfReplacement, original)) {
            HexUtilities.replace(bytes, startOfReplacement, replacement);
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else {
            return HexEditor.HexResultStatus.ERROR_UNKNOWN_BYTE_PATTERN_FOUND;
        }
    }

    @Override
    public HexInspectResult inspect(byte[] bytes, int startIndexInArray, int globalOffset) {
        byte[] bts = new byte[replacement.length];
        System.arraycopy(bytes, startIndexInArray + offset, bts, 0, bts.length);
        return new HexInspectResult(this, offset + startIndexInArray + globalOffset, bts);
    }

    @Override
    public PatternHexEdit getInvertedCopy() {
        return new PatternHexEdit(searchPattern, offset, replacement, original);
    }

}
