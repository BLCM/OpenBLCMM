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
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the original proprietary BLCMM Launcher, BLCMM
 * Lib Distributor, BLCMM Utilities, or BLCMM Data Interaction Library
 * Jarfiles (or modified versions of those libraries), containing parts
 * covered by the terms of their proprietary license, the licensors of
 * this Program grant you additional permission to convey the resulting
 * work.
 *
 */

package blcmm.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Class used to store tables of strings, used for instance for the
 * "versions.options" file.  This class was reimplemented based on the
 * calls BLCMM made into BLCMM_Utilities.jar, without reference to the original
 * sourcecode.
 *
 * This class used to live under general.utilities, but it's been moved
 * under blcmm.utilities as part of its opensourcing.
 *
 * This whole thing is essentially a poor-man's CSV format.  I'd like to
 * reimplement/replace the whole thing with a "real" CSV formatter; I suspect
 * that Apache Commons presumably has one.  As it is, this version would not
 * play nicely with data that has commas in it.  To convert, we'd need a good
 * way of knowing if we've already converted or not; was thinking that a
 * semi-silent ".csv" appending to the filename might be a decent idea.
 *
 * @author apocalyptech
 */
public class StringTable {

    // Keeping track of a few things as both arrays + more complex objects
    // just for an easy way to keep track of the original order.  Columns
    // will always be written out in the order they were read, and keys (data)
    // will be written in the same order it's seen, as well.
    private final String[] columns;
    private final ArrayList<String> keys;
    private final HashSet<String> keySet;
    private final HashMap<String, HashMap<String, String>> data;

    public StringTable() {
        this(new String[]{});
    }

    public StringTable(String[] columns) {
        this.columns = columns.clone();
        this.keys = new ArrayList<>();
        this.keySet = new HashSet<>();
        this.data = new HashMap<>();
    }

    public void addData(String[] newData) {
        if (newData.length == 0) {
            return;
        }
        HashMap<String, String> dataHash = new HashMap<>();
        this.keys.add(newData[0]);
        this.keySet.add(newData[0]);
        for (int i=0; i<this.columns.length; i++) {
            if (newData.length > i) {
                dataHash.put(this.columns[i], newData[i]);
            } else {
                dataHash.put(this.columns[i], "");
            }
        }
        this.data.put(newData[0], dataHash);
    }

    public Set<String> keySet() {
        return this.keySet;
    }

    public String get(String key, String column) {
        if (this.data.containsKey(key) && this.data.get(key).containsKey(column)) {
            return this.data.get(key).get(column);
        } else {
            return "";
        }
    }

    public void put(String key, String column, String value) {
        HashMap<String, String> row;
        if (!this.data.containsKey(key)) {
            this.keys.add(key);
            this.keySet.add(key);
            this.data.put(key, new HashMap<>());
            row = this.data.get(key);
            for (String col : this.columns) {
                row.put(col, "");
            }
        }
        row = this.data.get(key);
        row.put(column, value);
    }

    public String convertTableToString() {
        String NEWLINE = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", columns));
        sb.append(NEWLINE);
        ArrayList<String> outLine;
        for (String key : this.keys) {
            HashMap<String, String> rowData = this.data.get(key);
            outLine = new ArrayList<>();
            for (String col : this.columns) {
                outLine.add(rowData.get(col));
            }
            sb.append(String.join(",", outLine));
            sb.append(NEWLINE);
        }
        return sb.toString();
    }

    public static StringTable generateTable(String source) {
        String[] lines = source.split("\\R");
        if (lines.length == 0) {
            return new StringTable();
        }
        StringTable table = new StringTable(lines[0].split(","));
        for (int i=1; i<lines.length; i++) {
            table.addData(lines[i].split(","));
        }
        return table;
    }

}
