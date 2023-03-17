/*
 * Copyright (C) 2023 CJ Kucera
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

package blcmm.data.lib;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model for a single object entry.
 *
 * All this needs testing!  I'm sure it's wrong.
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

    public void setHasChildrenForClass(boolean hasChildrenForClass) {
        this.hasChildrenForClass = hasChildrenForClass;
    }

    public boolean getHasChildrenForClass() {
        return this.hasChildrenForClass;
    }

    public void setUeClass(UEClass ueClass) {
        this.ueClass = ueClass;
    }

    public UEClass getUeClass() {
        return this.ueClass;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getNameWithClassIfPossible() {
        if (this.ueClass == null) {
            return this.name;
        } else {
            return this.ueClass.getName() + "'" + this.name + "'";
        }
    }

    @Override
    public String toString() {
        return this.shortName;
    }

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
