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
 * Simple class to hold a parsed string from inside an attribute.  This class
 * was added during the reimplementation of the datalib, based on the calls
 * BLCMM made into BLCMM_Data_Interaction_Library.jar, but I don't think
 * the original implementation did anything like this.  Judging from the
 * code in PatchIO, I think it just used regular Strings.
 *
 * @author apocalyptech
 */
public class LevelDepString extends LevelDepData {

    private String value;
    private boolean quoted;

    /**
     * Create a new, unquoted String object
     *
     * @param value The value of the string
     */
    public LevelDepString(String value) {
        this(value, false);
    }

    /**
     * Create a new String object, optionally specifying that it should be
     * quoted when serialized back into a string.
     *
     * @param value The value of the string
     * @param quoted Whether or not the string should be quoted
     */
    public LevelDepString(String value, boolean quoted) {
        this.value = value;
        this.quoted = quoted;
    }

    public String getValue() {
        return value;
    }

    public boolean isQuoted() {
        return quoted;
    }

    public boolean equals(LevelDepString other) {
        return this.value.equals(other.value);
    }

    public boolean equalsIgnoreCase(LevelDepString other) {
        if (other == null) {
            return false;
        }
        return this.value.equalsIgnoreCase(other.value);
    }

    @Override
    public String toString() {
        if (this.quoted) {
            return '"' + value.replace("\"", "\\\"") + '"';
        } else {
            return value;
        }
    }

}
