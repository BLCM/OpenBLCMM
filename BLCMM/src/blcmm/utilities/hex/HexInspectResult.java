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
public class HexInspectResult {

    public final HexEdit orginialHexEdit;
    public final byte[] found;
    public final int adress;

    HexInspectResult(HexEdit orginialHexEdit, int adress, byte[] found) {
        this.adress = adress;
        this.orginialHexEdit = orginialHexEdit;
        this.found = found;
    }

    public HexEdit getOrginialHexEdit() {
        return orginialHexEdit;
    }

    public byte[] getFound() {
        return found;
    }

    public int getAdress() {
        return adress;
    }

    public boolean matchesOriginal() {
        return HexUtilities.matches(found, 0, orginialHexEdit.original);
    }

    public boolean matchesEdited() {
        return HexUtilities.matches(found, 0, orginialHexEdit.replacement);
    }

}
