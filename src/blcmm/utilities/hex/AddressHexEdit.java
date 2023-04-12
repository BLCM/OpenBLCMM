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
class AddressHexEdit extends HexEdit {

    public final int adress;

    AddressHexEdit(int hardCodedAdress, String original, String replacement) {
        this(hardCodedAdress, HexUtilities.convertToByteArray(original), HexUtilities.convertToByteArray(replacement));
    }

    AddressHexEdit(int hardCodedAdress, byte[] original, byte[] replacement) {
        super(original, replacement);
        this.adress = hardCodedAdress;
    }

    public int getAdress() {
        return adress;
    }

    @Override
    public int requiredBufferSize() {
        return original.length;
    }

    @Override
    public boolean match(byte[] bytes, int startIndexInArray, int globalOffset) {
        return globalOffset + startIndexInArray == adress;
    }

    @Override
    public HexEditor.HexResultStatus replace(byte[] bytes, int startIndexInArray, int globalOffset, boolean force) {
        if (!match(bytes, startIndexInArray, globalOffset)) {
            return null;
        } else if (force) {
            HexUtilities.replace(bytes, startIndexInArray, replacement);
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else if (HexUtilities.matches(bytes, startIndexInArray, replacement)) {
            return HexEditor.HexResultStatus.HEXEDIT_ALREADY_DONE;
        } else if (HexUtilities.matches(bytes, startIndexInArray, original)) {
            HexUtilities.replace(bytes, startIndexInArray, replacement);
            return HexEditor.HexResultStatus.HEXEDIT_SUCCESFUL;
        } else {
            System.out.println(Arrays.toString(original) + Arrays.toString(replacement));
            for (int i = -1; i < original.length + 2; i++) {
                System.out.print(bytes[i + startIndexInArray] + " ");
            }
            System.out.println();
            return HexEditor.HexResultStatus.ERROR_UNKNOWN_BYTE_PATTERN_FOUND;
        }
    }

    @Override
    public int requiredNegativeBufferSize() {
        return 0;
    }

    @Override
    public HexInspectResult inspect(byte[] bytes, int startIndexInArray, int globalOffset) {
        byte[] bts = new byte[replacement.length];
        System.arraycopy(bytes, startIndexInArray, bts, 0, bts.length);
        return new HexInspectResult(this, globalOffset + startIndexInArray, bts);
    }

    @Override
    public HexEdit getInvertedCopy() {
        return new AddressHexEdit(adress, replacement, original);
    }

}
