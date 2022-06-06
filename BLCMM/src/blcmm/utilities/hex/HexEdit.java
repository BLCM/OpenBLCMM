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
public abstract class HexEdit {

    public static final int NO_MATCH = 4729;
    public static final int SUCCESFULLY_REPLACED = 43849;
    public static final int WAS_ALREADY_REPLACED = 347983;
    public static final int UNKNOWN_PATTERN_FOUND = 349;

    //The pattern that's originally found 'offset' bytes after 'searchPattern'
    public final byte[] original;
    //The pattern that we replace orginal with if we find it
    public final byte[] replacement;

    protected HexEdit(String original, String replacement) {
        this(HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    protected HexEdit(String[] original, String[] replacement) {
        this(HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    protected HexEdit(byte[] original, byte[] replacement) {
        this.original = original;
        this.replacement = replacement;
        if (this.original.length != this.replacement.length) {
            throw new IllegalArgumentException("replacement and original must be equal length: ");
        }
    }

    public byte[] getOriginal() {
        return original;
    }

    public byte[] getReplacement() {
        return replacement;
    }

    public abstract int requiredBufferSize();

    public abstract boolean match(byte[] bytes, int startIndexInArray, int globalOffset);

    public abstract HexEditor.HexResultStatus replace(byte[] bytes, int startIndexInArray, int globalOffset, boolean force);

    public abstract HexInspectResult inspect(byte[] bytes, int startIndexInArray, int globalOffset);

    public abstract int requiredNegativeBufferSize();

    public abstract HexEdit getInvertedCopy();

}
