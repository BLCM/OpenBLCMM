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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Simple data class to hold a parsed struct/hash/dict/whatever inside an
 * attribute.  See the LevelDepParser docs for more info on the whole process.
 * This class was reimplemented based on the calls BLCMM made into
 * BLCMM_Data_Interaction_Library.jar, without reference to the original
 * sourcecode.
 *
 * @author apocalyptech
 */
public class LevelDepStruct extends LevelDepData {

    // I wanted to retain the order of the attributes in the struct, which is
    // where the LinkedHashMap comes in.  I also wanted to match on keys in
    // a case-insensitive way, hence the HashMap.  A bit silly, but whatever.
    private final LinkedHashMap<String, LevelDepData> data;
    private final HashMap<String, String> normalizedKeys;

    public LevelDepStruct() {
        this.data = new LinkedHashMap<>();
        this.normalizedKeys = new HashMap<>();
    }

    /**
     * Returns the "normalized" key, meaning the version with the initial case
     * that was first added to the object.  Will return the passed-in key
     * unchanged, if the key has not been seen previously.  The struct will
     * *not* remember the case of the passed-in key in that circumstance.
     *
     * @param key The key to check
     * @return The key with "normalized" case
     */
    private String getNormalizedKey(String key) {
        return this.getNormalizedKey(key, false);
    }

    /**
     * Returns the "normalized" key, meaning the version with the initial case
     * that was first added to the object.  Will return the passed-in key
     * unchanged, if the key has not been seen previously.  If the `create`
     * boolean is True, this method will additionally remember the passed-in
     * key as the canonical case to use in the future.
     *
     * @param key The key to check
     * @param create Whether or not to save the case of the key that was passed-in,
     * if it doesn't already exist.
     * @return The key with "normalized" case
     */
    private String getNormalizedKey(String key, boolean create) {
        String keyLower = key.toLowerCase();
        if (this.normalizedKeys.containsKey(keyLower)) {
            key = this.normalizedKeys.get(keyLower);
        } else if (create) {
            this.normalizedKeys.put(keyLower, key);
        }
        return key;
    }

    /**
     * Put a new value in, for the specified attribute name.
     *
     * @param key The attribute name
     * @param value The attribute value
     */
    public void put(String key, LevelDepData value) {
        this.data.put(this.getNormalizedKey(key, true), value);
    }

    /**
     * Returns the specified attr name as a String
     *
     * @param key The attribute name
     * @return The attribute value, as a String
     */
    public String getString(String key) {
        key = this.getNormalizedKey(key);
        if (key != null) {
            return this.data.get(key).toString();
        }
        return null;
    }

    /**
     * Returns the specified attr name as a LevelDepArray
     *
     * @param key The attribute name
     * @return The attribute value, as a LevelDepArray
     */
    public LevelDepArray getArray(String key) {
        key = this.getNormalizedKey(key);
        if (key != null) {
            Object obj = this.data.get(key);
            if (obj instanceof LevelDepArray) {
                return (LevelDepArray) obj;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        ArrayList<String> items = new ArrayList<>();
        for (Entry<String, LevelDepData> e : this.data.entrySet()) {
            if (e.getValue() == null) {
                items.add(e.getKey() + "=");
            } else {
                items.add(e.getKey() + "=" + e.getValue().toString());
            }
        }
        return "(" + String.join(",", items) + ")";
    }

}
