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
 * The original BLCMM data library wasn't opensourced along with the core
 * components, so this is a complete rewrite.  The original version used a
 * combination of mostly text-based indexes along with the datafiles, direct
 * dump parsing, and generated SDK-like code to provide some very rich access
 * methods into Borderlands data.  This version is missing a lot of the
 * fancier behavior possible in the original, but it is at least capable of
 * fully running Object Explorer and the other basic data needs of the main
 * BLCMM application.
 *
 * This version primarily relies on the presence of a SQLite database to
 * know about the dump files -- the only time the dump files themselves are
 * read is to do fulltext/refs searches, and to pull specific dumps out of
 * the files.  The generation scripts which collect all the data and generate
 * the SQLite data are actually part of the "DataDumper" PythonSDK project,
 * which is also responsible for the raw data extraction right from the
 * games.  You can find info on that over here:
 *
 *      https://github.com/BLCM/DataDumper
 *
 * Rather than generalizing too much, the methods in here tend to focus on
 * the specific needs of the apps calling into it.  As such, there's some
 * overlap between methods, and I'm sure a lot of this could be generalized
 * somehow.  Still, the SQLite database is well-indexed, and for some tasks
 * this version provides a nice performance improvement over the original.
 *
 * This was written without reference to the original datalib code.
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

    // Lots of various similar lookups; a bit silly to be storing this much
    // info multiple times like this, but there's not *that* many classes.
    private ArrayList<UEClass> allClasses = new ArrayList<> ();
    private ArrayList<String> allClassNames = new ArrayList<> ();
    private HashMap<String, UEClass> classNameToClass = new HashMap<> ();
    private HashMap<Integer, UEClass> classIdToClass = new HashMap<> ();
    private int totalDatafiles;

    private HashMap<Integer, OESearch> categoryIdMap = new HashMap<> ();
    private HashMap<OESearch, TreeSet<UEClass>> categoryToClass = new HashMap<> ();
    private TreeSet<UEClass> curClassesByEnabledCategory = new TreeSet<> ();

    // We may as well hold on to our PreparedStatements so we can reuse them
    // easily without having to rebuild all the time.  In practice, rebuilding
    // all the time isn't really noticeable, but whatever.
    private PreparedStatement autocompleteShallowNoParentWithoutClassStmt;
    private PreparedStatement autocompleteShallowNoParentByClassStmt;
    private PreparedStatement autocompleteShallowWithParentWithoutClassStmt;
    private PreparedStatement autocompleteShallowWithParentByClassStmt;
    private PreparedStatement autocompleteDeepWithoutClassStmt;
    private PreparedStatement autocompleteDeepByClassStmt;

    /**
     * Custom Exception we can throw when our data isn't found, or is in a
     * state we don't expect.  Lets the main application know that data for
     * the specified game isn't available.
     */
    public class NoDataException extends Exception {
        public NoDataException(String message) {
            super(message);
        }
        public NoDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Wrapper around dump info, containing both the UEObject and its
     * text dump.  Basically a glorified tuple.
     */
    public class Dump {
        public UEObject ueObject;
        public String text;
        public Dump(UEObject ueObject, String text) {
            this.ueObject = ueObject;
            this.text = text;
        }
    }

    /**
     * Main DataManager class, and window into the data we can query.  Note
     * that unlike the original BLCMM version, this is instantiated with a
     * specific game.
     *
     * @param patchType The game data to load
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
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

    /**
     * Get the game type associated with this data.
     *
     * @return The PatchType
     */
    public PatchType getPatchType() {
        return this.patchType;
    }

    /**
     * Returns a collection of all UEClasses we know about
     *
     * @return The class list
     */
    public Collection<UEClass> getAllClasses() {
        return this.allClasses;
    }

    /**
     * Returns a collection of all class names we know about.
     *
     * @return The list of class names
     */
    public Collection<String> getAllClassNames() {
        return this.allClassNames;
    }

    /**
     * The user can toggle various categories on/off for fulltext/refs searches
     * in OE.  When those toggles are changed, call in to this method to update
     * the internal cache of classes we should return for those operations.
     *
     * @param enabledCategories The set of enabled categories for fulltext/refs searches
     */
    public void updateClassesByEnabledCategory(Set<OESearch> enabledCategories) {
        this.curClassesByEnabledCategory.clear();
        for (OESearch oeSearch : enabledCategories) {
            this.curClassesByEnabledCategory.addAll(this.categoryToClass.get(oeSearch));
        }
    }

    /**
     * Returns our list of enabled classes for use in fulltext/refs searches
     *
     * @return An ordered set of classes (sorted by name)
     */
    public TreeSet<UEClass> getAllClassesByEnabledCategory() {
        return this.curClassesByEnabledCategory;
    }

    /**
     * Given a name, returns the UEClass object associated with it, or null.
     * The matching is done case-insensitively.
     *
     * @param name The name of the class to load.
     * @return The UEClass object
     */
    public UEClass getClassByName(String name) {
        String nameLower = name.toLowerCase();
        if (this.classNameToClass.containsKey(nameLower)) {
            return this.classNameToClass.get(nameLower);
        } else {
            return null;
        }
    }

    /**
     * Returns our "root" UEClass, which for our known data will always end
     * up being the `Object` class.
     *
     * @return The root UEClass
     */
    public UEClass getRootClass() {
        return this.rootClass;
    }

    /**
     * Returns the total number of datafiles (storing object dumps) in the
     * database.  In practice, this value isn't actually used anywhere,
     * because in all cases where it would be useful, the user's category
     * preferences are taken into account, so the app needs to do some math
     * anyway.
     *
     * @return The total number of datafiles in the dataset.
     */
    public int getTotalDatafiles() {
        return this.totalDatafiles;
    }

    /**
     * Given a UEClass, return a set of class IDs describing the class itself,
     * plus all its subclasses.  For instance, passing in the ItemPoolDefinition
     * class would return its ID plus the IDs for KeyedItemPoolDefinition and
     * CrossDLCItemPoolDefinition.
     *
     * @param ueClass The class to search
     * @return A set of Class IDs
     */
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

    /**
     * Given a UEClass, return a list of UEClass describing the class itself,
     * plus all its subclasses.  For instance, passing in the ItemPoolDefinition
     * class would return that class plus the KeyedItemPoolDefinition and
     * CrossDLCItemPoolDefinition classes.
     *
     * @param ueClass The class to search
     * @return A list of UEClass objects.
     */
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

    /**
     * Given a UEClass, return a set of UEClass describing the class itself,
     * plus all its subclasses.  For instance, passing in the ItemPoolDefinition
     * class would return that class plus the KeyedItemPoolDefinition and
     * CrossDLCItemPoolDefinition classes.
     *
     * @param ueClass The class to search
     * @return A set of UEClass objects.
     */
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

    /**
     * Given a class, return all objects in the database which are of that
     * specific class.  For instance, querying for ItemPoolDefinition objects
     * would *not* return any KeyedItemPoolDefinition objects, even though
     * that's a subclass of ItemPoolDefinition.  The list will be sorted by
     * object name.
     *
     * @param ueClass The class to search for
     * @return A sorted list of matching UEObject objects.
     */
    public List<UEObject> getAllObjectsInSpecificClass(UEClass ueClass) {
        return this.getAllObjectsInSpecificClass(ueClass, true);
    }

    /**
     * Given a class, return all objects in the database which are of that
     * specific class, specifying whether or not to order them by object
     * name.  For instance, querying for ItemPoolDefinition objects
     * would *not* return any KeyedItemPoolDefinition objects, even though
     * that's a subclass of ItemPoolDefinition.
     *
     * @param ueClass The class to search for
     * @param ordered Whether or not to sort the output
     * @return A list of matching UEObject objects.
     */
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

    /**
     * Given a class, return all objects in the database which are of that
     * class or any of its subclasses.  For instance, querying for
     * ItemPoolDefinition objects would also return any KeyedItemPoolDefinition
     * objects, etc.  The list will be sorted by object name.
     *
     * @param ueClass The class to search for
     * @return A sorted list of matching UEObject objects.
     */
    public List<UEObject> getAllObjectsInClassTree(UEClass ueClass) {
        return this.getAllObjectsInClassTree(ueClass, true);
    }

    /**
     * Given a class, return all objects in the database which are of that
     * class or any of its subclasses, specifying whether to sort them by
     * object name.  For instance, querying for ItemPoolDefinition objects
     * would also return any KeyedItemPoolDefinition objects, etc.
     *
     * @param ueClass The class to search for
     * @param ordered Whether or not to sort the output
     * @return A list of matching UEObject objects.
     */
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

    /**
     * Given a UEClass object, return all UEObjects which eventually lead to
     * objects of that class at the "root" of the object tree.  Used by OE
     * to generate the Object Browser panel.
     *
     * @param ueClass The class to browse
     * @return A list of UEObjects at the root of the object tree
     */
    public List<UEObject> getTreeObjectsFromClass(UEClass ueClass) {
        return this.getTreeObjectsFromClass(ueClass, null);
    }

    /**
     * Given a UEClass object, and a location on the object tree, return all
     * UEObjects found immediately under that location which eventually lead
     * to objects of that class (or objects of that class themselves).  Used
     * by OE to generate the Object Browser panel.  If parentObject is null,
     * this will start at the "root" of the object tree.
     *
     * @param ueClass The class to browse
     * @param parentObject The location in the object tree to start
     * @return A list of UEObjects
     */
    public List<UEObject> getTreeObjectsFromClass(UEClass ueClass, UEObject parentObject) {
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

    /**
     * Given an object name, returns a Dump of the object, if possible.
     *
     * @param objectName The name of the object to dump
     * @return A Dump object, containing the relevant UEObject and the String dump.
     */
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
     * Return all list of all datafiles containing dumps for the specified
     * UEClass.
     *
     * TODO: This really shouldn't expose something like a File, since that may
     * change (and probably will) in the future.
     *
     * @param ueClass The class for which to find datafiles
     * @return A list of Files pointing at the datafiles
     */
    public List<File> getAllDatafilesForClass(UEClass ueClass) {
        ArrayList<File> list = new ArrayList<> ();
        for (int i=1; i<=ueClass.getNumDatafiles(); i++) {
            String dataFileName = ueClass.getName() + ".dump." + i;
            list.add(Paths.get(this.dumpFilePath, dataFileName).toFile());
        }
        return list;
    }

    /**
     * Prepare SQL statements used for "shallow" autocomplete activities.  The
     * shallow autocompletes will only autocomplete the most recent component
     * of the pathname being typed in, pausing at each tree branch.  This method
     * in particular is just used to set up the PreparedStatements so that we
     * only have to do it once, since these can end up getting called quite
     * frequently as a user types.
     *
     * @param hasRoot If we're generating SQL with a given root node
     * @param hasClass If we're generating SQL restricted by class type
     * @return a PreparedStatement for use later on
     * @throws SQLException
     */
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

    /**
     * Returns "Shallow" autocomplete reults for the given prefix, with the
     * last path separator found at the given index.  Shallow autocompletes
     * only operate on the last path component.  If the prefix is not at
     * the root of the tree, the autocomplete suggestions will include the
     * path separator as their first character (either a period or colon).
     *
     * @param prefix The currently-typed in object name (minus any class label)
     * @param lastSeparator The string index of the last path separator in the prefix
     * @return A list of suggestions for the autocomplete engine
     */
    public List<String> getShallowAutocompleteResults(String prefix, int lastSeparator) {
        return this.getShallowAutocompleteResults(prefix, lastSeparator, (UEClass)null);
    }

    /**
     * Returns "Shallow" autocomplete reults for the given prefix, with the
     * last path separator found at the given index, restricted to paths which
     * eventually lead to the specified class name.  Shallow autocompletes
     * only operate on the last path component.  If the prefix is not at
     * the root of the tree, the autocomplete suggestions will include the
     * path separator as their first character (either a period or colon).
     *
     * @param prefix The currently-typed in object name (minus any class label)
     * @param lastSeparator The string index of the last path separator in the prefix
     * @param className The class name to restrict the eventual completions to
     * @return A list of suggestions for the autocomplete engine
     */
    public List<String> getShallowAutocompleteResults(String prefix, int lastSeparator, String className) {
        return this.getShallowAutocompleteResults(prefix, lastSeparator, this.getClassByName(className));
    }

    /**
     * Returns "Shallow" autocomplete reults for the given prefix, with the
     * last path separator found at the given index, restricted to paths which
     * eventually lead to the specified class.  Shallow autocompletes
     * only operate on the last path component.  If the prefix is not at
     * the root of the tree, the autocomplete suggestions will include the
     * path separator as their first character (either a period or colon).
     *
     * @param prefix The currently-typed in object name (minus any class label)
     * @param lastSeparator The string index of the last path separator in the prefix
     * @param inClass The class to restrict the eventual completions to
     * @return A list of suggestions for the autocomplete engine
     */
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

    /**
     * Returns "Deep" autocomplete results for the given query.  Deep
     * autocompletes are basically wildcard searches for full object names, and
     * don't bother processing path components "smartly."  The given suggestions
     * will replace the entire given query, if chosen.
     *
     * @param query The currently-typed-in string
     * @return A list of suggestions
     */
    public List<String> getDeepAutocompleteResults(String query) {
        return this.getDeepAutocompleteResults(query, (UEClass)null);
    }

    /**
     * Returns "Deep" autocomplete results for the given query, restricted to
     * objects of the given class name.  Deep autocompletes are basically
     * wildcard searches for full object names, and don't bother processing path
     * components "smartly."  The given suggestions will replace the entire
     * given query, if chosen.
     *
     * @param query The currently-typed-in string
     * @param inClassName The class name to restrict suggestions to
     * @return A list of suggestions
     */
    public List<String> getDeepAutocompleteResults(String query, String inClassName) {
        return this.getDeepAutocompleteResults(query, this.getClassByName(inClassName));
    }

    /**
     * Returns "Deep" autocomplete results for the given query, restricted to
     * objects of the given class.  Deep autocompletes are basically
     * wildcard searches for full object names, and don't bother processing path
     * components "smartly."  The given suggestions will replace the entire
     * given query, if chosen.
     *
     * @param query The currently-typed-in string
     * @param inClass The class to restrict suggestions to
     * @return A list of suggestions
     */
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
