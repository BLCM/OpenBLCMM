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

package blcmm.data.lib;

import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options.OESearch;
import blcmm.utilities.Utilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    // Some basic info
    private PatchType patchType;
    private DataStatus dataStatus;
    private String dataBaseDir;
    private String dbFilePath;
    private Connection dbConn;
    private UEClass rootClass;

    // Jarfile Info
    private String jarFilename;
    private File jarFileObj;
    private JarFile jarFile;
    private String dataPathBase;
    private String dataPathDumps;
    private String jarDumpVersion;

    // Supported database versions, and other metadata
    private final int minDatabaseSupported = 1;
    private final int maxDatabaseSupported = 1;
    private int databaseVersion;
    private String dumpVersion;
    private Date dataCompiled;

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
    private PreparedStatement autocompleteFieldWithoutClassStmt;
    private PreparedStatement autocompleteEnumStmt;

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
     * @param dataStatus A DataStatus object to send messages to
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
    public DataManager(PatchType patchType, DataStatus dataStatus) throws NoDataException {
        this.patchType = patchType;
        this.dataStatus = dataStatus;

        // Extract the sqlite database if required
        boolean extracted = false;
        if (!this.checkDataFiles()) {
            // This will throw a NoDataException if it doesn't extract+verify
            GlobalLogger.log("Extracting " + patchType.name() + " database from jar");
            this.extractDatabase();
            extracted = true;
        }

        // Database stuff!
        this.totalDatafiles = 0;
        try {
            this.doInitialDatabaseConection();

            // Doublecheck that the versions match.  If not, try extracting the
            // data again, unless we've already done an extraction on this run.
            this.jarDumpVersion = this.getJarDumpVersion();
            if (!this.dumpVersion.equals(this.jarDumpVersion)) {
                if (extracted) {
                    throw new NoDataException("Verified database integrity but the dump version does not match: "
                            + "database dump version is: " + this.dumpVersion
                            + ", Jar dump version is: " + this.jarDumpVersion
                    );
                } else {
                    // Close the DB, re-extract, and reconnect.  (And redo the check)
                    this.dbConn.close();
                    GlobalLogger.log("Dump versions didn't match between database and jarfile for " + patchType.name() + ", re-extracting data");
                    this.dataStatus.event("Dump versions didn't match", false);
                    this.extractDatabase();
                    this.doInitialDatabaseConection();

                    // Now doublecheck one more time!
                    if (!this.dumpVersion.equals(this.jarDumpVersion)) {
                        throw new NoDataException("Verified database integrity but the dump version does not match: "
                                + "database dump version is: " + this.dumpVersion
                                + ", Jar dump version is: " + this.jarDumpVersion
                        );
                    }
                }
            }

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
            this.autocompleteFieldWithoutClassStmt = this.dbConn.prepareStatement(
                    "select distinct a.name from"
                    + " attr_name a"
                    + " where a.name like ?"
                    + " order by a.name"
            );
            // Note the `ESCAPE "\"` SQL in here.  The default SQL "LIKE" wildcard
            // interprets underscores as "match any single char", so this lets
            // us escape the user-typed underscores so that they're interpreted
            // as *literal* underscores.
            this.autocompleteEnumStmt = this.dbConn.prepareStatement(
                    "select e.name from"
                    + " enum e"
                    + " where e.name like ? escape \"\\\""
                    + " order by e.name"
            );

        } catch (SQLException e) {
            throw new NoDataException("Unable to load database: " + e.toString(), e);
        }

    }

    /**
     * Checks our data Jar to make sure that it contains everything we need.
     * Returns true if everything is good to go, or False if the sqlite database
     * needs to be extracted/verified again.
     *
     * @return True if all is well, or False if the database needs extraction.
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
    private boolean checkDataFiles() throws NoDataException {

        //this.jarFilename = "blcmm_data_" + patchType.name() + ".jar";
        this.dataStatus.event("Checking datafile integrity...", false);

        // Actually, let's let these filenames be versioned, and always take the most recent
        File thisDir = new File(".");
        String jarPrefix = "blcmm_data_" + patchType.name() + "-";
        File[] jars = thisDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(jarPrefix) && name.endsWith(".jar");
            }
        });
        if (jars.length > 0) {
            Arrays.sort(jars);
            this.jarFileObj = jars[jars.length-1];
            this.jarFilename = this.jarFileObj.toString();
        } else {
            throw new NoDataException("No jarfiles found for " + this.patchType.name());
        }

        // Now start processing
        try {

            this.jarFile = new JarFile(this.jarFileObj);
            GlobalLogger.log("Found data jarfile at: " + this.jarFilename);
            this.dataPathBase = "data/" + patchType.name();
            this.dataPathDumps = this.dataPathBase + "/dumps";
            this.dataBaseDir = Paths.get(Utilities.getBLCMMDataDir(), "extracted-data", patchType.name()).toString();
            this.dbFilePath = Paths.get(this.dataBaseDir, "data.db").toString();

            // Check to see if the database file exists
            File dbFile = new File(this.dbFilePath);
            if (!dbFile.exists()) {
                return false;
            }

            // Check the database file to see if we've already checked it
            long lastVerifiedDb = patchType.getOEDataSuccessTimestampDb();
            long lastVerifiedJar = patchType.getOEDataSuccessTimestampJar();
            if (lastVerifiedDb == 0 || lastVerifiedJar == 0) {
                return false;
            }
            BasicFileAttributes dbAttrs = Files.readAttributes(dbFile.toPath(), BasicFileAttributes.class);
            BasicFileAttributes jarAttrs = Files.readAttributes(this.jarFileObj.toPath(), BasicFileAttributes.class);
            if (dbAttrs.lastModifiedTime().toMillis() == lastVerifiedDb && jarAttrs.lastModifiedTime().toMillis() == lastVerifiedJar) {
                return true;
            }

            // Verify the database checksum
            GlobalLogger.log(patchType.name() + " data jar file modification time doesn't match; re-verifying");
            this.dataStatus.event("File modification time mismatch detected...", false);
            return this.verifyDatabaseChecksum();

        } catch (MalformedURLException e) {
            throw new NoDataException("Couldn't construct path to data jar", e);
        } catch (IOException e) {
            throw new NoDataException("Could not read data jar", e);
        }
    }

    /**
     * Verifies the extracted SQLite database checksum.  Returns True if the
     * checksum succeeded or False otherwise.
     *
     * @return True if the database checksum matches what's in the Jar
     */
    private boolean verifyDatabaseChecksum() {
        this.dataStatus.event("Verifying extacted database checksum...", true);
        try {
            String diskHash = Utilities.sha256(this.getJarStreamBase("data.db"));
            BufferedReader br = this.getJarBufferedReaderBase("data.db.sha256sum");
            if (br == null) {
                GlobalLogger.log("ERROR: Could not find sha256sum for database in data jar");
                return false;
            }
            String checkHash = br.readLine().trim();
            br.close();

            boolean hashSuccess = diskHash.equalsIgnoreCase(checkHash);
            if (hashSuccess) {
                // If the hashes matched, save the mtime so we don't have to
                // check again.
                this.patchType.setOEDataSuccessTimestampDb(Files.readAttributes(Paths.get(this.dbFilePath), BasicFileAttributes.class));
                this.patchType.setOEDataSuccessTimestampJar(Files.readAttributes(this.jarFileObj.toPath(), BasicFileAttributes.class));
            }
            return hashSuccess;

        } catch (IOException|NoSuchAlgorithmException e) {
            GlobalLogger.log(e);
            return false;
        }
    }

    /**
     * Extracts the SQLite database from the Jarfile so it can be accessed
     * directly.  Will throw a NoDataException under various circumstances if
     * the database can't be extracted.  This method will validate the
     * checksum after extraction.
     *
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
    private void extractDatabase() throws NoDataException {
        this.dataStatus.event("Extracting Database...", true);

        // Grab the database entry and its destination dir
        JarEntry entry = this.getJarEntryBase("data.db");
        if (entry == null) {
            throw new NoDataException("SQLite database was not found in data jar");
        }
        File dbDir = new File(this.dataBaseDir);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        // Now check to make sure we have the diskspace for it.  Going to
        // require an extra 50MB of space beyond just the DB ...
        long available = dbDir.getFreeSpace();
        long required = entry.getSize() + 50000000;
        if (required > available) {
            throw new NoDataException("Not enough hard drive space remaining at "
                    + dbDir.toString() + ", need " + Utilities.bytesToHuman(required)
                    + " but only have " + Utilities.bytesToHuman(available)
            );
        }

        // Now try the extraction
        InputStream fromJar = this.getJarStreamBase("data.db");
        if (fromJar == null) {
            throw new NoDataException("SQLite database could not be read from data jar");
        }
        try {
            byte[] buffer = new byte[4096];
            OutputStream toDisk = new FileOutputStream(new File(this.dbFilePath));
            int read;
            while ((read = fromJar.read(buffer)) != -1) {
                toDisk.write(buffer, 0, read);
            }
            toDisk.close();
            fromJar.close();
        } catch (IOException e) {
            GlobalLogger.log(e);
            throw new NoDataException("Could not read database from Jarfile", e);
        }

        // Theoretically at this point we're good to go.  Check the checksum, though.
        GlobalLogger.log("Checking " + patchType.name() + " database integrity, post-extraction");
        if (!this.verifyDatabaseChecksum()) {
            throw new NoDataException("Database checksum could not be verified");
        }

    }

    /**
     * Performs the initial database connection, checks its database version against
     * the values we support, and pulls the rest of the DB metadata in.  This is
     * abstracted out from the main constructor because we may end up calling it
     * more than once if the sqlite database is out-of-sync with the version
     * from the Jar
     *
     * @throws SQLException
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
    private void doInitialDatabaseConection() throws SQLException, NoDataException {

        // Connect to the database
        this.dataStatus.event("Connecting to DB and checking supported version...", false);
        this.dbConn = DriverManager.getConnection("jdbc:sqlite:" + this.dbFilePath);

        // Load in metadata
        Statement stmt = this.dbConn.createStatement();
        ResultSet rs = stmt.executeQuery("select * from metadata");
        rs.next();
        this.databaseVersion = rs.getInt("db_version");
        if (this.databaseVersion < this.minDatabaseSupported) {
            throw new NoDataException("Database is version " + this.databaseVersion
                    + ", but we require at least version " + this.minDatabaseSupported);
        } else if (this.databaseVersion > this.maxDatabaseSupported) {
            throw new NoDataException("Database is version " + this.databaseVersion
                    + ", but we only support up to version " + this.maxDatabaseSupported);
        }
        this.dumpVersion = rs.getString("dump_version");
        this.dataCompiled = rs.getDate("compiled");
        rs.close();
        stmt.close();
    }

    /**
     * Get the data dump version as specified by the Jar filesystem itself.
     * This is used to compare against the version stored in the SQLite
     * database to check for conflicts.
     *
     * @return The dump version specified in the Jar filesystem
     * @throws blcmm.data.lib.DataManager.NoDataException
     */
    private String getJarDumpVersion() throws NoDataException {
        try {
            BufferedReader versionReader = this.getJarBufferedReaderBase("version.txt");
            this.jarDumpVersion = versionReader.readLine();
            versionReader.close();
            return this.jarDumpVersion;
        } catch (IOException e) {
            throw new NoDataException("No dump version information found in jar", e);
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
     * Returns the database version we're currently using.
     *
     * @return The database version;
     */
    public int getDatabaseVersion() {
        return this.databaseVersion;
    }

    /**
     * Returns the dump version that we're currently using.
     *
     * @return The dump version
     */
    public String getDumpVersion() {
        return this.dumpVersion;
    }

    /**
     * Returns the timestamp when our data was compiled/generated.
     *
     * @return The timestamp
     */
    public Date getCompiled() {
        return this.dataCompiled;
    }

    /**
     * Returns the specified JarEntry from our data Jarfile, based on its
     * absolute path.
     *
     * @param path The path to look for
     * @return The JarEntry for the path
     */
    private JarEntry getJarEntry(String path) {
        return this.jarFile.getJarEntry(path);
    }

    /**
     * Returns the specified JarEntry from our data Jarfile, inside the
     * base directory we expect all our data to be contained.
     *
     * @param path The path to look for
     * @return The JarEntry for the path
     */
    private JarEntry getJarEntryBase(String path) {
        return this.getJarEntry(this.dataPathBase + "/" + path);
    }

    /**
     * Returns the specified JarEntry from our data Jarfile, inside the
     * directory which contains all the actual dump information.
     *
     * @param path The path to look for
     * @return The JarEntry for the path
     */
    private JarEntry getJarEntryDumps(String path) {
        return this.getJarEntry(this.dataPathDumps + "/" + path);
    }

    /**
     * Returns an InputStream from our Jar file based on a JarEntry pointing
     * to a specific file.  This is used by Object Explorer to get dump info.
     *
     * @param entry The path inside the Jar file to retreive
     * @return An InputStream to the specified entry
     */
    public InputStream getStreamFromJarEntry(JarEntry entry) {
        if (entry == null) {
            return null;
        } else {
            try {
                return this.jarFile.getInputStream(entry);
            } catch (IOException e) {
                GlobalLogger.log(e);
                return null;
            }
        }
    }

    /**
     * Returns an InputStream pointing to the specified path in the Jar file,
     * inside the base directory which we expect to find all our data.
     *
     * @param path The path relative to our "base" directory
     * @return An InputStream for the specified path
     */
    private InputStream getJarStreamBase(String path) {
        return this.getStreamFromJarEntry(this.getJarEntryBase(path));
    }

    /**
     * Returns an InputStream pointing to the specified path in the Jar file,
     * inside the directory which contains all the actual dump info.
     *
     * @param path The path relative to our dumps directory
     * @return An InputStream for the specified path
     */
    private InputStream getJarStreamDumps(String path) {
        return this.getStreamFromJarEntry(this.getJarEntryDumps(path));
    }

    /**
     * Returns a BufferedReader pointing to the specified path in the Jar file,
     * inside the directory which we expect to find all our data.
     *
     * @param path The path relative to our "base" directory
     * @return A BufferedReader for the path
     */
    private BufferedReader getJarBufferedReaderBase(String path) {
        InputStream stream = this.getJarStreamBase(path);
        if (stream == null) {
            return null;
        } else {
            return new BufferedReader(new InputStreamReader(stream));
        }
    }

    /**
     * Returns a BufferedReader pointing to the specified path in the Jar file,
     * inside the directory which contains all the actual dump info.
     *
     * @param path The path relative to our dumps directory
     * @return A BufferedReader for the path
     */
    private BufferedReader getJarBufferedReaderDumps(String path) {
        InputStream stream = this.getJarStreamDumps(path);
        if (stream == null) {
            return null;
        } else {
            return new BufferedReader(new InputStreamReader(stream));
        }
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
            InputStream stream = this.getJarStreamDumps(dataFileName);
            byte[] data = new byte[ueObject.getBytes()];

            // Skip to the correct spot.  This might be a little overkill, but it
            // turns out that we *do* have to do this for the calls to stream.read()
            // down below, so I figure let's make sure this works too.
            long pos = ueObject.getFilePosition();
            long totalSkipped = 0;
            long thisSkipped = 0;
            thisSkipped = stream.skip(pos-totalSkipped);
            totalSkipped += thisSkipped;
            while (totalSkipped != pos && thisSkipped != 0) {
                thisSkipped = stream.skip(pos-totalSkipped);
                totalSkipped += thisSkipped;
            }
            if (totalSkipped != pos) {
                return new Dump(null, "Error reading dump: tried to seek to " + pos + " but only got to " + totalSkipped);
            }

            // Read the data.  It seems that when reading from a Jarfile, we may
            // not get all the data at once always, so we're doing this stupid
            // little loop.  Ah well.
            int size = ueObject.getBytes();
            int totalRead = 0;
            int thisRead = 0;
            thisRead = stream.read(data, 0, size);
            totalRead += thisRead;
            while (totalRead != size && thisRead != -1) {
                thisRead = stream.read(data, totalRead, size-totalRead);
                totalRead += thisRead;
            }
            if (totalRead != size) {
                return new Dump(null, "Error reading dump: tried to read " + size + " bytes, but only got " + totalRead);
            }

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
     * TODO: This really shouldn't expose something like a JarEntry, since that may
     * change (and probably will) in the future.
     *
     * @param ueClass The class for which to find datafiles
     * @return A list of JarEntry objects pointing at the datafiles
     */
    public List<JarEntry> getAllDatafilesForClass(UEClass ueClass) {
        ArrayList<JarEntry> list = new ArrayList<> ();
        for (int i=1; i<=ueClass.getNumDatafiles(); i++) {
            String dataFileName = ueClass.getName() + ".dump." + i;
            list.add(this.getJarEntryDumps(dataFileName));
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

    /**
     * Returns autocomplete suggestion for field/attribute names without respect
     * to what class is being chosen -- ie: it'll be a list of all possible
     * attribute names that we're aware of.  Only really suitable for top-level
     * attribute autocompletes; anything inner might have names not in this
     * list.
     *
     * @param current The currently-typed text to act as an initial substring
     * @return A list of suggestions
     */
    public List<String> getFieldAutocompleteResults(String current) {
        ArrayList<String> results = new ArrayList<>();
        try {
            this.autocompleteFieldWithoutClassStmt.setString(1, current + "%");
            ResultSet rs = this.autocompleteFieldWithoutClassStmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
            rs.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return results;
    }

    /**
     * Returns autocomplete suggestion for enum values, without respect to
     * whatever attribute's being used (so the possible autocomplete space will
     * include *all* enum values in the game).
     *
     * @param current The currently-typed text to act as an initial substring
     * @return A list of suggestions
     */
    public List<String> getEnumAutocompleteResults(String current) {
        ArrayList<String> results = new ArrayList<>();
        try {
            this.autocompleteEnumStmt.setString(1, current.replace("_", "\\_") + "%");
            ResultSet rs = this.autocompleteEnumStmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("name"));
            }
            rs.close();
        } catch (SQLException e) {
            GlobalLogger.log(e);
        }
        return results;
    }

}
