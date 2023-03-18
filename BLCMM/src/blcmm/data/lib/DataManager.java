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

import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options.OESearch;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * New SQLite-backed Data Library.
 *
 * All this needs testing!  I'm sure it's wrong.
 *
 * @author apocalyptech
 */
public class DataManager {

    private PatchType patchType;
    private String dataBaseDir;
    private String dbFilePath;
    private String dumpFilePath;
    private Connection dbConn;
    private UEClass rootClass;
    private ArrayList<UEClass> allClasses = new ArrayList<> ();
    private ArrayList<String> allClassNames = new ArrayList<> ();
    private HashMap<String, UEClass> classNameToClass = new HashMap<> ();
    private HashMap<Integer, UEClass> classIdToClass = new HashMap<> ();
    private int totalDatafiles;

    private HashMap<Integer, OESearch> categoryIdMap = new HashMap<> ();
    private HashMap<OESearch, TreeSet<UEClass>> categoryToClass = new HashMap<> ();
    private TreeSet<UEClass> curClassesByEnabledCategory = new TreeSet<> ();

    private PreparedStatement autocompleteShallowNoParentWithoutClassStmt;
    private PreparedStatement autocompleteShallowNoParentByClassStmt;
    private PreparedStatement autocompleteShallowWithParentWithoutClassStmt;
    private PreparedStatement autocompleteShallowWithParentByClassStmt;
    private PreparedStatement autocompleteDeepWithoutClassStmt;
    private PreparedStatement autocompleteDeepByClassStmt;

    public class NoDataException extends Exception {
        public NoDataException(String message) {
            super(message);
        }
        public NoDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public class Dump {
        public UEObject ueObject;
        public String text;
        public Dump(UEObject ueObject, String text) {
            this.ueObject = ueObject;
            this.text = text;
        }
    }

    public DataManager(PatchType patchType) throws NoDataException {
        this.patchType = patchType;
        this.dataBaseDir = Paths.get("data", patchType.toString()).toString();
        this.dbFilePath = Paths.get(this.dataBaseDir, "data.db").toString();
        this.dumpFilePath = Paths.get(this.dataBaseDir, "dumps").toString();
        this.totalDatafiles = 0;

        // Database stuff!
        try {
            this.dbConn = DriverManager.getConnection("jdbc:sqlite:" + this.dbFilePath);

            // Load in our categories
            Statement stmt = this.dbConn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from category");
            String catName;
            OESearch oeSearch;
            while (rs.next()) {
                catName = rs.getString("name");
                try {
                    oeSearch = OESearch.valueOf(catName);
                } catch (IllegalArgumentException e) {
                    throw new NoDataException("Encountered an unknown class category in data: " + catName);
                }
                this.categoryIdMap.put(rs.getInt("id"), oeSearch);
                this.categoryToClass.put(oeSearch, new TreeSet<> ());
            }
            rs.close();
            stmt.close();

            // Load in all classes.  We make some assumptions about the database
            // here.  Namely: there is a single root element (this'll be "Object",
            // but the code here doens't care), `id` values start at 1, and the
            // database was constructed in "tree" order, so by looping through
            // in order of `id`, we don't have to worry about unknown IDs
            // popping up while we set up parent/child relationships.
            stmt = this.dbConn.createStatement();
            rs = stmt.executeQuery("select * from class order by id");
            HashMap <Integer, UEClass> classMap = new HashMap<>();
            UEClass ueClass;
            UEClass parent;
            while (rs.next()) {
                ueClass = UEClass.getFromDbRow(rs);
                if (ueClass.getParentId() > 0) {
                    if (!classMap.containsKey(ueClass.getParentId())) {
                        throw new NoDataException("Encountered an unknown parent ID in class data: " + ueClass.getParentId());
                    }
                    parent = classMap.get(ueClass.getParentId());
                    ueClass.setParent(parent);
                    parent.addChild(ueClass);
                }
                classMap.put(ueClass.getId(), ueClass);
                if (this.rootClass == null) {
                    this.rootClass = ueClass;
                }
                this.allClasses.add(ueClass);
                this.allClassNames.add(ueClass.getName());
                this.classNameToClass.put(ueClass.getName().toLowerCase(), ueClass);
                this.classIdToClass.put(ueClass.getId(), ueClass);
                this.totalDatafiles += ueClass.getNumDatafiles();
                oeSearch = this.categoryIdMap.get(ueClass.getCategoryId());
                this.categoryToClass.get(oeSearch).add(ueClass);
            }
            Collections.sort(this.allClasses);
            Collections.sort(this.allClassNames);
            rs.close();
            stmt.close();

            // Pre-prepare some statements for autocompletion.  To be honest, the
            // autocompletes seem speedy enough without worrying about this kind
            // of thing, but I'd still rather just construct these once, since
            // it'll otherwise get reconstructed at every keystroke, when the
            // user's typing w/ the autocomplete open.
            this.autocompleteShallowNoParentWithoutClassStmt = this.getShallowAutocompleteResultsStatement(false, false);
            this.autocompleteShallowNoParentByClassStmt = this.getShallowAutocompleteResultsStatement(false, true);
            this.autocompleteShallowWithParentWithoutClassStmt = this.getShallowAutocompleteResultsStatement(true, false);
            this.autocompleteShallowWithParentByClassStmt = this.getShallowAutocompleteResultsStatement(true, true);
            this.autocompleteDeepWithoutClassStmt = this.dbConn.prepareStatement(
                    "select o.* from object o where name like ? order by o.name"
            );
            this.autocompleteDeepByClassStmt = this.dbConn.prepareStatement(
                    "select o.* from"
                    + " object o, class_subclass sub"
                    + " where o.name like ?"
                    + " and sub.subclass=o.class"
                    + " and sub.class=?"
                    + " order by o.name"
            );

        } catch (SQLException e) {
            throw new NoDataException("Unable to load database: " + e.toString(), e);
        }

    }

    public PatchType getPatchType() {
        return this.patchType;
    }

    public Collection<UEClass> getAllClasses() {
        return this.allClasses;
    }

    public void updateClassesByEnabledCategory(Set<OESearch> enabledCategories) {
        this.curClassesByEnabledCategory.clear();
        for (OESearch oeSearch : enabledCategories) {
            this.curClassesByEnabledCategory.addAll(this.categoryToClass.get(oeSearch));
        }
    }

    public TreeSet<UEClass> getAllClassesByEnabledCategory() {
        return this.curClassesByEnabledCategory;
    }

    public Collection<String> getAllClassNames() {
        return this.allClassNames;
    }

    public UEClass getClassByName(String name) {
        String nameLower = name.toLowerCase();
        if (this.classNameToClass.containsKey(nameLower)) {
            return this.classNameToClass.get(nameLower);
        } else {
            return null;
        }
    }

    public UEClass getRootClass() {
        return this.rootClass;
    }

    public int getTotalDatafiles() {
        return this.totalDatafiles;
    }

    public Set<Integer> getSubclassIDs(UEClass ueClass) {
        HashSet<Integer> map = new HashSet<>();
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement(
                    "select * from class_subclass where class=?"
            );
            stmt.setInt(1, ueClass.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                map.add(rs.getInt("subclass"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return map;
    }

    public List<UEClass> getSubclassesList(UEClass ueClass) {
        Set<Integer> idMap = this.getSubclassIDs(ueClass);
        ArrayList<UEClass> list = new ArrayList<>();
        for (int classId : idMap) {
            if (this.classIdToClass.containsKey(classId)) {
                list.add(this.classIdToClass.get(classId));
            }
        }
        return list;
    }

    public TreeSet<UEClass> getSubclassesSet(UEClass ueClass) {
        Set<Integer> idMap = this.getSubclassIDs(ueClass);
        TreeSet<UEClass> classSet = new TreeSet<>();
        for (int classId : idMap) {
            if (this.classIdToClass.containsKey(classId)) {
                classSet.add(this.classIdToClass.get(classId));
            }
        }
        return classSet;
    }

    public List<UEObject> getAllObjectsInSpecificClass(UEClass ueClass) {
        return this.getAllObjectsInSpecificClass(ueClass, true);
    }

    public List<UEObject> getAllObjectsInSpecificClass(UEClass ueClass, boolean ordered) {
        ArrayList<UEObject> list = new ArrayList<>();
        try {
            String suffix = "";
            if (ordered) {
                suffix = " order by o.name";
            }
            PreparedStatement stmt = this.dbConn.prepareStatement(
                    "select o.* from object o, class c where o.class=c.id and c.id=?" + suffix
            );
            stmt.setInt(1, ueClass.getId());
            ResultSet rs = stmt.executeQuery();
            UEObject ueObject;
            while (rs.next()) {
                ueObject = UEObject.getFromDbRow(rs);
                ueObject.setUeClass(ueClass);
                list.add(ueObject);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return list;
    }


    public List<UEObject> getAllObjectsInClassTree(UEClass ueClass) {
        return this.getAllObjectsInClassTree(ueClass, true);
    }

    public List<UEObject> getAllObjectsInClassTree(UEClass ueClass, boolean ordered) {
        ArrayList<UEObject> list = new ArrayList<>();
        Set<Integer> validClasses = this.getSubclassIDs(ueClass);
        try {
            String suffix = "";
            if (ordered) {
                suffix = " order by o.name";
            }
            PreparedStatement stmt = this.dbConn.prepareStatement(
                    "select o.*, c.id class_id from object o, object_show_class_ids i, class c where o.id=i.id and o.class=c.id and i.class=?" + suffix
            );
            stmt.setInt(1, ueClass.getId());
            ResultSet rs = stmt.executeQuery();
            UEObject ueObject;
            int classId;
            while (rs.next()) {
                classId = rs.getInt("class_id");
                if (validClasses.contains(classId)) {
                    ueObject = UEObject.getFromDbRow(rs);
                    if (this.classIdToClass.containsKey(classId)) {
                        ueObject.setUeClass(this.classIdToClass.get(classId));
                    }
                    list.add(ueObject);
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            GlobalLogger.log("Error getting objects from class: " + e.toString());
        }
        return list;
    }


    public List<UEObject> getObjectsFromClass(UEClass ueClass) {
        return this.getObjectsFromClass(ueClass, null);
    }

    public List<UEObject> getObjectsFromClass(UEClass ueClass, UEObject parentObject) {
        ArrayList<UEObject> list = new ArrayList<>();
        try {
            PreparedStatement stmt;
            if (parentObject == null) {
                stmt = this.dbConn.prepareStatement(
                        "select o.*, i.has_children from object o, object_show_class_ids i where o.id=i.id and i.class=? and parent is null;"
                );
                stmt.setInt(1, ueClass.getId());
            } else {
                stmt = this.dbConn.prepareStatement(
                        "select o.*, i.has_children from object o, object_show_class_ids i where o.id=i.id and i.class=? and parent=?;"
                );
                stmt.setInt(1, ueClass.getId());
                stmt.setInt(2, parentObject.getId());
            }
            ResultSet rs = stmt.executeQuery();
            UEObject ueObject;
            while (rs.next()) {
                ueObject = UEObject.getFromDbRow(rs);
                ueObject.setHasChildrenForClass(rs.getBoolean("has_children"));
                list.add(ueObject);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            GlobalLogger.log("Error getting objects from class: " + e.toString());
        }
        return list;
    }

    public Dump getDump(String objectName) {

        // When clicking on in-app links, the objectName will be: ClassType'GD_Class.Path'
        if (objectName.contains("'")) {
            String[] parts = objectName.split("'");
            if (parts.length != 2) {
                return new Dump(null, "Unknown object name format: " + objectName);
            }
            objectName = parts[1];
        }

        // Now load
        try {
            PreparedStatement stmt = this.dbConn.prepareStatement(
                    "select o.*, c.name class_name from object o, class c where o.class=c.id and o.name=?");
            stmt.setString(1, objectName);
            ResultSet rs = stmt.executeQuery();
            UEObject ueObject = UEObject.getFromDbRow(rs);
            if (ueObject.getFileIndex() == 0) {
                return new Dump(null, "Object not found in database: " + objectName);
            }
            int classId = rs.getInt("class");
            if (this.classIdToClass.containsKey(classId)) {
                ueObject.setUeClass(this.classIdToClass.get(classId));
            }
            String dataFileName = rs.getString("class_name") + ".dump." + ueObject.getFileIndex();
            File dataFile = Paths.get(this.dumpFilePath, dataFileName).toFile();
            FileInputStream stream = new FileInputStream(dataFile);
            byte[] data = new byte[ueObject.getBytes()];
            stream.skip(ueObject.getFilePosition());
            stream.read(data);
            rs.close();
            stmt.close();
            return new Dump(ueObject, new String(data, StandardCharsets.ISO_8859_1));
        } catch (SQLException e) {
            GlobalLogger.log(e);
            return new Dump(null, "Error getting object from database: " + e.getMessage());
        } catch (FileNotFoundException e) {
            return new Dump(null, "Object not found in database: " + objectName);
        } catch (IOException|IndexOutOfBoundsException e) {
            GlobalLogger.log(e);
            return new Dump(null, "Error reading object data from datafile: " + e.getMessage());
        } catch (Exception e) {
            GlobalLogger.log(e);
            return new Dump(null, "Unknown error reading object data: " + e.getMessage());
        }
    }

    /**
     * TODO: This really shouldn't expose something like a File, since that may
     * change (and probably will) in the future.
     *
     * @param ueClass
     * @return
     */
    public List<File> getAllDatafilesForClass(UEClass ueClass) {
        ArrayList<File> list = new ArrayList<> ();
        for (int i=1; i<=ueClass.getNumDatafiles(); i++) {
            String dataFileName = ueClass.getName() + ".dump." + i;
            list.add(Paths.get(this.dumpFilePath, dataFileName).toFile());
        }
        return list;
    }

    private PreparedStatement getShallowAutocompleteResultsStatement(boolean hasRoot, boolean hasClass) throws SQLException {
        ArrayList<String> tables = new ArrayList<>();
        ArrayList<String> where = new ArrayList<>();
        tables.add("object o");

        // Specifying where we are in the tree
        if (hasRoot) {
            tables.add("object p");
            tables.add("object_children c");
            where.add("o.id=c.child");
            where.add("p.id=c.parent");
            where.add("p.name=?");
        } else {
            where.add("parent is null");
        }

        // Are we restricting results to a specific class?
        if (hasClass) {
            tables.add("object_show_class_ids oc");
            where.add("o.id=oc.id");
            where.add("oc.class=?");
        }

        // Now mush the SQL together
        return this.dbConn.prepareStatement(
                "select o.* from "
                + String.join(", ", tables)
                + " where "
                + String.join(" and ", where)
                + " order by o.name"
        );
    }

    public List<String> getShallowAutocompleteResults(String prefix, int lastSeparator) {
        return this.getShallowAutocompleteResults(prefix, lastSeparator, (UEClass)null);
    }

    public List<String> getShallowAutocompleteResults(String prefix, int lastSeparator, String className) {
        return this.getShallowAutocompleteResults(prefix, lastSeparator, this.getClassByName(className));
    }

    public List<String> getShallowAutocompleteResults(String prefix, int lastSeparator, UEClass inClass) {
        ArrayList<String> options = new ArrayList<>();
        String root;
        String substr;
        // Playing a bit loose with initial-char-separators, but whatever.
        if (lastSeparator > 0) {
            root = prefix.substring(0, lastSeparator);
            substr = prefix.substring(lastSeparator+1, prefix.length()).toLowerCase();
            //GlobalLogger.log("root: \"" + root + "\", substr: \"" + substr + "\"");
        } else {
            root = null;
            substr = prefix.toLowerCase();
        }
        try {
            // Choose the statement to run and then assign params.  As I mention
            // above, caching them this way is maybe kind of silly, especially
            // since if there's ever a third option, this stanza starts getting
            // ridiculous.  Still, this one especially can get called quite
            // frequently, and in a context where lag might be noticeable.
            PreparedStatement stmt;
            int paramIndex = 1;
            if (root == null) {
                if (inClass == null) {
                    stmt = this.autocompleteShallowNoParentWithoutClassStmt;
                } else {
                    stmt = this.autocompleteShallowNoParentByClassStmt;
                    stmt.setInt(paramIndex++, inClass.getId());
                }
            } else {
                if (inClass == null) {
                    stmt = this.autocompleteShallowWithParentWithoutClassStmt;
                    stmt.setString(paramIndex++, root);
                } else {
                    stmt = this.autocompleteShallowWithParentByClassStmt;
                    stmt.setString(paramIndex++, root);
                    stmt.setInt(paramIndex++, inClass.getId());
                }
            }

            //GlobalLogger.log("Executing: " + stmt.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getString("short_name").toLowerCase().startsWith(substr)) {
                    if (rs.getString("separator") == null) {
                        options.add(rs.getString("short_name"));
                    } else {
                        options.add(rs.getString("separator") + rs.getString("short_name"));
                    }
                }
            }
            rs.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return options;
    }

    public List<String> getDeepAutocompleteResults(String query) {
        return this.getDeepAutocompleteResults(query, (UEClass)null);
    }

    public List<String> getDeepAutocompleteResults(String query, String inClassName) {
        return this.getDeepAutocompleteResults(query, this.getClassByName(inClassName));
    }

    public List<String> getDeepAutocompleteResults(String query, UEClass inClass) {
        ArrayList<String> options = new ArrayList<>();
        try {
            // Choose the statement to run and then assign params.  As I mention
            // above, caching them this way is maybe kind of silly, especially
            // since if there's ever a third option, this stanza starts getting
            // ridiculous.  Still, this one especially can get called quite
            // frequently, and in a context where lag might be noticeable.
            PreparedStatement stmt;
            if (inClass == null) {
                stmt = this.autocompleteDeepWithoutClassStmt;
                stmt.setString(1, "%" + query + "%");
            } else {
                stmt = this.autocompleteDeepByClassStmt;
                stmt.setString(1, "%" + query + "%");
                stmt.setInt(2, inClass.getId());
            }

            //GlobalLogger.log("Executing: " + stmt.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                options.add(rs.getString("name"));
            }
            rs.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return options;
    }

}
