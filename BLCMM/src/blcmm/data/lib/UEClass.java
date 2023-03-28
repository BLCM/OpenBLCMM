/*
 * Copyright (C) 2023 CJ Kucera
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

package blcmm.data.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for a single class entry.
 *
 * This is part of the new opensource data library, reimplemented without
 * reference to the original non-opensourced code.
 *
 * This is a pretty simple data container without much actual functionality.
 * The majority of the logic governing the interactions with the data
 * is handled by the main DataManager class.
 *
 * @author apocalyptech
 */
public class UEClass implements Comparable<UEClass> {

    private final int id;
    private final String name;
    private final int categoryId;
    private UEClass parent;
    private final int parentId;
    private final int numObjects;
    private final ArrayList<UEClass> children;
    private final int numDatafiles;

    public UEClass(int id, String name, int categoryId, Integer parentId, int numObjects, int numDatafiles) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.parentId = parentId;
        this.parent = null;
        this.numObjects = numObjects;
        this.children = new ArrayList<>();
        this.numDatafiles = numDatafiles;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getCategoryId() {
        return this.categoryId;
    }

    public int getParentId() {
        return this.parentId;
    }

    public void setParent(UEClass ueClass) {
        this.parent = ueClass;
    }

    public UEClass getParent() {
        return this.parent;
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    public List<UEClass> getChildren() {
        return this.children;
    }

    public void addChild(UEClass ueClass) {
        this.children.add(ueClass);
    }

    public int getNumObjects() {
        return this.numObjects;
    }

    public int getNumDatafiles() {
        return this.numDatafiles;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(UEClass other) {
        return this.name.compareToIgnoreCase(other.name);
    }

    /**
     * Returns a new UEClass based on a database row ResultSet.
     *
     * @param rs The ResultSet with database data.
     * @return A new UEClass object
     * @throws SQLException
     */
    public static UEClass getFromDbRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int categoryId = rs.getInt("category");
        int parentId = rs.getInt("parent");
        int numObjects = rs.getInt("total_children");
        int numDatafiles = rs.getInt("num_datafiles");
        return new UEClass(id, name, categoryId, parentId, numObjects, numDatafiles);
    }

}
