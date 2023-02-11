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
package blcmm.utilities.hex;

/**
 *
 * @author LightChaosman
 */
public class HexUtilities {

    public final static short WILDCARD = '?' + 1024;

    private HexUtilities() {
    }

    protected static final byte[] convertToByteArray(String stringVersion) throws NumberFormatException {
        return convertToByteArray(convertStringToArray(stringVersion));
    }

    protected static final byte[] convertToByteArray(String[] stringVersion) throws NumberFormatException {
        byte[] bts = new byte[stringVersion.length];
        for (int i = 0; i < stringVersion.length; i++) {
            bts[i] = (byte) Integer.parseInt(stringVersion[i], 16);
        }
        return bts;
    }

    protected static final short[] convertToShortArray(byte[] byteVersion) {
        short[] res = new short[byteVersion.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = byteVersion[i];
        }
        return res;
    }

    /**
     * Use this method if wildcards should be accepted
     *
     * @param stringVersion
     * @return
     * @throws NumberFormatException
     */
    static short[] convertToShortArray(String stringVersion) throws NumberFormatException {
        return convertToShortArray(HexUtilities.convertStringToArray(stringVersion));
    }

    /**
     * Use this method if wildcards should be accepted
     *
     * @param stringVersion
     * @return
     * @throws NumberFormatException
     */
    protected static short[] convertToShortArray(String[] stringVersion) throws NumberFormatException {
        short[] bts = new short[stringVersion.length];
        for (int i = 0; i < stringVersion.length; i++) {
            if (stringVersion[i].startsWith("?")) {
                bts[i] = WILDCARD;
            } else {
                bts[i] = (byte) Integer.parseInt(stringVersion[i], 16);
            }
        }
        return bts;
    }

    protected static final String[] convertStringToArray(String base) {
        return base.replaceAll("0x", "").split("[\\s,]+");
    }

    protected static boolean matches(byte[] bytes, int offset, byte[] toMatch) {
        for (int i = 0; i < toMatch.length; i++) {
            if (bytes[offset + i] != toMatch[i]) {
                return false;
            }
        }
        return true;
    }

    protected static boolean matches(byte[] bytes, int offset, short[] toMatch) {
        for (int i = 0; i < toMatch.length; i++) {
            if (toMatch[i] != HexUtilities.WILDCARD && bytes[offset + i] != ((byte) toMatch[i])) {
                return false;
            }
        }
        return true;
    }

    protected static final void replace(byte[] bytes, int offset, byte[] replacement) {
        System.arraycopy(replacement, 0, bytes, offset, replacement.length);
    }

}
