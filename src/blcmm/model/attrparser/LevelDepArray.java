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

import java.util.ArrayList;

/**
 * Simple data class to hold a parsed array inside an attribute.  See the
 * LevelDepParser docs for more info on the whole process.  This class was
 * reimplemented based on the calls BLCMM made into BLCMM_Data_Interaction_Library.jar,
 * without reference to the original sourcecode.
 *
 * @param <T> The type of data this array holds
 * @author apocalyptech
 */
public class LevelDepArray<T extends LevelDepData> extends LevelDepData {

    private final ArrayList<T> list;

    public LevelDepArray() {
        this.list = new ArrayList<>();
    }

    public void add(T newElement) {
        this.list.add(newElement);
    }

    public int size() {
        return this.list.size();
    }

    public T get(int index) {
        return this.list.get(index);
    }

    @Override
    public String toString() {
        ArrayList<String> newList = new ArrayList<>();
        for (Object o : this.list) {
            if (o == null) {
                newList.add("");
            } else {
                newList.add(o.toString());
            }
        }
        return "(" + String.join(",", newList) + ")";
    }

}
