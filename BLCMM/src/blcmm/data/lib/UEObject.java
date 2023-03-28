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

/**
 * Model for a single object entry.
 *
 * This is part of the new opensource data library, reimplemented without
 * reference to the original non-opensourced code.
 *
 * This is a pretty simple data container without much actual functionality.
 * The majority of the logic governing the interactions with the data
 * is handled by the main DataManager class.
 *
 * There's a few attributes in here which don't ordinarily get set when
 * reading from just the "raw" database tables; these end up getting set
 * on a method-by-method basis from DataManager.
 *
 * @author apocalyptech
 */
public class UEObject {

    private final int id;
    private final String name;
    private final String shortName;
    private final int numChildren;
    private final int fileIndex;
    private final int filePosition;
    private final int bytes;
    private boolean hasChildrenForClass;
    private boolean expanded;
    private UEClass ueClass;

    public UEObject(int id, String name, String shortName, int numChildren,
            int fileIndex, int filePosition,  int bytes) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.numChildren = numChildren;
        this.fileIndex = fileIndex;
        this.filePosition = filePosition;
        this.bytes = bytes;
        this.expanded = false;
        this.hasChildrenForClass = false;
        this.ueClass = null;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getShortName() {
        return this.shortName;
    }

    public int getNumChildren() {
        return this.numChildren;
    }

    public int getFileIndex() {
        return this.fileIndex;
    }

    public int getFilePosition() {
        return this.filePosition;
    }

    public int getBytes() {
        return this.bytes;
    }

    public boolean hasChildren() {
        return this.numChildren > 0;
    }

    /**
     * A dynamic attribute used while populating the Object Browser panel
     * when a class is chosen, which determines whether or not this object
     * shows up as a leaf or a folder.  See DataManager.getTreeObjectsFromClass
     * for where this is used.
     *
     * @param hasChildrenForClass Whether or not this object has children, for the class being searched
     */
    public void setHasChildrenForClass(boolean hasChildrenForClass) {
        this.hasChildrenForClass = hasChildrenForClass;
    }

    public boolean getHasChildrenForClass() {
        return this.hasChildrenForClass;
    }

    /**
     * Sets the associated UEClass object.  Only some things need to know
     * about this, so we don't bother by default.  Various DataManager methods
     * will set this attribute, though.
     *
     * @param ueClass The class associated with the object.
     */
    public void setUeClass(UEClass ueClass) {
        this.ueClass = ueClass;
    }

    public UEClass getUeClass() {
        return this.ueClass;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    /**
     * Sets whether or not we've been "expanded" in the Object Browser tree.
     * This is used because Object Browser only populates the currently-visible
     * level.  When an element has children, a "dummy" child is put in place so
     * that the UI correctly shows a folder.  Then when the folder is expanded,
     * some SQL is run to actually populate the contents.  This boolean is
     * used so that we know the SQL doesn't need to be re-run.
     *
     * @param expanded Whether or not we've been expanded.
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Returns our full object name, including class type, if possible.  If
     * we don't have an associated UEClass object, just return our basic
     * object name instead.
     *
     * @return The full name format if possible, or regular name otherwise.
     */
    public String getNameWithClassIfPossible() {
        if (this.ueClass == null) {
            return this.name;
        } else {
            return this.ueClass.getName() + "'" + this.name + "'";
        }
    }

    /**
     * Default toString representation.  We override this so that it shows
     * up as short names in the Object Browser.
     *
     * @return The short name for the object.
     */
    @Override
    public String toString() {
        return this.shortName;
    }

    /**
     * Returns a new UEObject based on a database row ResultSet.
     *
     * @param rs The ResultSet with database data.
     * @return A new UEObject object
     * @throws SQLException
     */
    public static UEObject getFromDbRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String shortName = rs.getString("short_name");
        int numChildren = rs.getInt("num_children");
        int fileIndex = rs.getInt("file_index");
        int filePosition = rs.getInt("file_position");
        int bytes = rs.getInt("bytes");
        return new UEObject(id, name, shortName, numChildren,
                fileIndex, filePosition, bytes);
    }


}
