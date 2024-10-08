/*
 * Copyright (C) 2018-2020  LightChaosman
 * Copyright (C) 2023 Christopher J. Kucera
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
package blcmm.utilities;

import SteamVDF.VDF.VDF;
import SteamVDF.VDF.VDFElement;
import blcmm.model.PatchType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.filechooser.FileSystemView;

/**
 * Library for various methods related to game detection, for all platforms
 * and games.
 *
 * Note that the AoDK stuff in here is essentially all guesses, though I'd
 * expect them to be not *too* far off.
 */
public class GameDetection {

    //A boolean indicating if we've already done an effort to detect game files
    private static boolean run = false;

    // A couple of booleans to reduce clutter of logging.
    private static final HashSet<PatchType> LOGGED_BIN = new HashSet<>();
    private static final HashSet<PatchType> LOGGED_EXE = new HashSet<>();
    private static final HashSet<PatchType> LOGGED_INI = new HashSet<>();

    //For windows, this path
    private static String bl2Path, blTPSPath, blAODKPath;

    //For Linux, some cached attributes to let us know if we're using Proton
    private static final HashSet<PatchType> UNIX_SCANNED_PROTON = new HashSet<>();
    private static final HashMap<PatchType, Boolean> UNIX_USING_PROTON = new HashMap<>();

    /**
     * Silly little class to help streamline registry lookups.  Takes in a
     * path and then a name of a key at that path.
     */
    private static class RegKeyLookup {

        public String path;
        public String name;

        public RegKeyLookup(String path, String name) {
            this.path = path;
            this.name = name;
        }

    }

    /**
     * A way to set the path manually for BL2, in case game detection failed
     *
     * @param path
     */
    public static void setBL2PathManually(String path) {
        assert run;
        LOGGED_BIN.add(PatchType.BL2);
        bl2Path = path;
        GlobalLogger.log("Set BL2 path manually to: " + Utilities.hideUserName(path));
    }

    /**
     * A way to set the path manually for TPS, in case game detection failed
     *
     * @param path
     */
    public static void setTPSPathManually(String path) {
        assert run;
        LOGGED_BIN.add(PatchType.TPS);
        blTPSPath = path;
        GlobalLogger.log("Set TPS path manually to: " + Utilities.hideUserName(path));
    }

    /**
     * A way to set the path manually for AODK, in case game detection failed
     *
     * @param path
     */
    public static void setAODKPathManually(String path) {
        assert run;
        LOGGED_BIN.add(PatchType.AODK);
        blAODKPath = path;
        GlobalLogger.log("Set AODK path manually to: " + Utilities.hideUserName(path));
    }

    public static String getPath(PatchType type) {
        switch (type) {
            case BL2:
                return getBL2Path();
            case TPS:
                return getTPSPath();
            case AODK:
                return getAODKPath();
            default:
                return null;
        }
    }

    public static String getBL2Path() {
        findGames();
        return bl2Path;
    }

    public static String getTPSPath() {
        findGames();
        return blTPSPath;
    }

    public static String getAODKPath() {
        findGames();
        return blAODKPath;
    }

    private static void findGames() {
        if (run) {
            return;
        }
        run = true;
        if (OSInfo.CURRENT_OS == OSInfo.OS.WINDOWS) {
            windowsGameDetection();
        } else {
            unixGameDetection();
        }
        GlobalLogger.log("Games folders found:");
        GlobalLogger.log("Borderlands 2:              " + Utilities.hideUserName(bl2Path));
        GlobalLogger.log("Borderlands The Pre Sequel: " + Utilities.hideUserName(blTPSPath));
        GlobalLogger.log("Assault on Dragon Keep:     " + Utilities.hideUserName(blAODKPath));
    }

    private static void windowsGameDetection() {
        //BL2 detection
        String regKey1 = "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 49520";
        String regKey2 = "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 49520";
        bl2Path = detectWindows(PatchType.BL2, regKey1, regKey2);
        //BLTPS detection
        regKey1 = "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640";
        regKey2 = "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640";
        blTPSPath = detectWindows(PatchType.TPS, regKey1, regKey2);
        //AODK detection
        regKey1 = "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 1712840";
        regKey2 = "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 1712840";
        blAODKPath = detectWindows(PatchType.AODK, regKey1, regKey2);
    }

    private static String detectWindows(PatchType type, String... regKeys) {
        String game = type.getGameName();
        String path = null;
        for (int i = 0; i < regKeys.length && path == null; i++) {
            path = GetRegField(new RegKeyLookup(regKeys[i], "InstallLocation"));
        }
        if (path == null) {
            GlobalLogger.log("Can't find " + game + " Steam installation on registry.");
            path = FindGamePathInLog(type);
        } else if (!new File(path).exists()) {
            GlobalLogger.log("The found registry installation of " + game + " doesn't exist. (" + Utilities.hideUserName(path) + ")");
            path = FindGamePathInLog(type);
        } else if (getExe(type, path) == null) {
            GlobalLogger.log("The found registry installation of " + game + " doesn't have an executable. (" + Utilities.hideUserName(path) + ")");
            path = FindGamePathInLog(type);
        } else {
            GlobalLogger.log("Found " + game + " Steam installation on registry.");
        }
        return path;
    }

    ///String example: "reg query \"HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640\" /v InstallLocation"
    static String GetRegField(RegKeyLookup regKey) {
        String reg = null;
        try {
            Process p = Runtime.getRuntime().exec(new String[] {
                "reg", "query",
                regKey.path,
                "/v", regKey.name
            });
            p.waitFor();

            InputStream in = p.getInputStream();
            byte[] b = new byte[in.available()];
            in.read(b);
            in.close();
            reg = new String(b);
            if (reg.trim().isEmpty()) {
                return null;
            }
            return reg.split("\\s\\s+")[4];
        } catch (IOException | ArrayIndexOutOfBoundsException | InterruptedException t) {
            GlobalLogger.log(reg);
            GlobalLogger.log(t);
            return null;
        }
    }

    private static String FindGamePathInLog(PatchType type) {
        String game = type.getGameName();
        final String start = "Log: Base directory: ";
        String path = null;
        fileLoop:
        for (File log : getLogFiles(type)) {
            if (log != null && log.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(log));
                    String line = br.readLine();
                    lineloop:
                    while (line != null && path == null) {
                        if (line.startsWith(start)) {
                            String path2 = line.substring(start.length());
                            int i = path2.lastIndexOf("\\");
                            i = path2.lastIndexOf("\\", i - 1);
                            i = path2.lastIndexOf("\\", i - 1);
                            // Linux and Mac have an extra "steamassets" dir in the tree
                            if (OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
                                i = path2.lastIndexOf("\\", i - 1);
                            }
                            path2 = path2.substring(0, i);
                            if (!path2.toLowerCase().contains("steam")) {
                                File file = new File(path2 + "\\.egstore"); // Detect Epic Launcher Installs, they have this directory, its easiest
                                if(!file.exists()) {
                                    GlobalLogger.log("Disregarding path due to lack of 'steam' substring: " + Utilities.hideUserName(path2));
                                    break;
                                }
                            } else if (!new File(path2).exists()) {
                                GlobalLogger.log("Disregarding path due to not existing: " + Utilities.hideUserName(path2));
                                break;
                            } else if (getExe(type, path2) == null) {
                                GlobalLogger.log("Disregarding path due to not having an executable: " + Utilities.hideUserName(path2));
                                break;
                            }
                            path = path2;
                            GlobalLogger.log("Found path for " + game + " in the logs: "
                                    + Utilities.hideUserName(path)
                                    + " (using log: " + Utilities.hideUserName(log.toString()) + ")");
                            br.close();
                            break fileLoop;//No need to continue reading the file at this point.
                        }
                        line = br.readLine();
                    }
                    br.close();
                } catch (IOException ex) {
                    GlobalLogger.log("Error while reading log for " + game + ": " + ex.getMessage());
                }
            }
        }
        if (path == null) {
            GlobalLogger.log("Also can't find a log for " + game + ".");
        }
        if (path != null && OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
            return path.replace("\\", "/");
        } else {
            return path;
        }
    }

    /**
     * Detect game installs on Linux, currently only from Steam.  The AoDK
     * path stuff in here is mostly just guesses.
     */
    private static void unixGameDetection() {
        String steamPath;
        ArrayList<String> libraryFolders;

        String initialTestDir;
        if (OSInfo.CURRENT_OS == OSInfo.OS.MAC) {
            // This should be the correct path for Mac
            initialTestDir = "/Library/Application Support/Steam";
        } else {
            // And this should do for Linux
            initialTestDir = "/.steam/steam";
        }
        steamPath = System.getenv("HOME") + initialTestDir;
        libraryFolders = getLibraryFolders(steamPath);

        // If we didn't find that, try ~/.local/share/Steam, which is common
        // on at least a number of distros.
        if (libraryFolders.isEmpty()) {
            steamPath = getSteamDataDir();
            libraryFolders = getLibraryFolders(steamPath);
        }

        GlobalLogger.log("Library folders found:");
        for (String folder : libraryFolders) {
            GlobalLogger.log(Utilities.hideUserName(folder));

            try {
                // Search for our two case possibilities
                File manifestFolder = new File(folder + "/steamapps");
                if (!manifestFolder.exists()) {
                    manifestFolder = new File(folder + "/SteamApps");
                    if (!manifestFolder.exists()) {
                        continue;
                    }
                }
                File[] filesList = manifestFolder.listFiles();

                for (File file : filesList) {
                    switch (file.getName()) {
                        case "appmanifest_49520.acf":
                            bl2Path = manifestFolder.getAbsolutePath() + "/common/Borderlands 2";
                            break;
                        case "appmanifest_261640.acf":
                            blTPSPath = manifestFolder.getAbsolutePath() + "/common/BorderlandsPreSequel";
                            break;
                        case "appmanifest_1712840.acf":
                            // Yes, apparently the install dir is the internal codename, not the game name.
                            // The EGS packaging is a bit more sensible.
                            blAODKPath = manifestFolder.getAbsolutePath() + "/common/Pawpaw";
                            break;
                    }
                }
            } catch (NullPointerException err) {
                GlobalLogger.log("Directory " + Utilities.hideUserName(folder) + " is incorrect, it doesn't have any files");
            }
        }
        if (OSInfo.CURRENT_OS == OSInfo.OS.MAC) {
            if (bl2Path != null) {
                bl2Path = bl2Path + "/Borderlands2.app/Contents";
            }
            if (blTPSPath != null) {
                blTPSPath = blTPSPath + "/BorderlandsPreSequel.app/Contents";
            }
            if (blAODKPath != null) {
                blAODKPath = blAODKPath + "/TTAoDKOneShotAdventure.app/Contents";
            }
        }
        if (bl2Path == null) {
            bl2Path = FindGamePathInLog(PatchType.BL2);
        } else {
            GlobalLogger.log("Succesfully found installation of Borderlands 2");
        }
        if (blTPSPath == null) {
            blTPSPath = FindGamePathInLog(PatchType.TPS);
        } else {
            GlobalLogger.log("Succesfully found installation of Borderlands: The Pre-Sequel");
        }
        if (blAODKPath == null) {
            blAODKPath = FindGamePathInLog(PatchType.AODK);
        } else {
            GlobalLogger.log("Succesfully found installation of Assault on Dragon Keep");
        }
    }

    ///Parser for `libraryfolders.vdf` files. It will get all `SteamLibrary` folders from this file.
    ///Should work on all platforms.
    private static ArrayList<String> getLibraryFolders(String steamPath) {
        ArrayList<String> libraryFolders = new ArrayList<>(1);

        libraryFolders.add(steamPath);

        // This case-based dance is just to support Linux, where the
        // "steamapps" dir might be capitalized or not.  It shouldn't hurt
        // on other platforms though.
        File vdfFile = new File(steamPath + "/steamapps/libraryfolders.vdf");
        if (!vdfFile.exists()) {
            vdfFile = new File(steamPath + "/SteamApps/libraryfolders.vdf");
            if (!vdfFile.exists()) {
                return libraryFolders;
            }
        }

        try {
            VDF parsedVdf = new VDF(vdfFile);
            boolean foundTop = false;
            for (VDFElement top : parsedVdf.getParents()) {
                if (top.getName().equalsIgnoreCase("libraryfolders")) {
                    if (top.numKeys() > 0) {
                        // Older-style libraryfolders.vdf
                        for (String key : top.getKeys()) {
                            try {
                                // Don't actually care what the integer value is, just want to make sure
                                // it *is* an integer value.
                                int foo = Integer.parseInt(key);
                                String newPath = top.getKey(key).toString().replace("\\\\", "\\");
                                libraryFolders.add(newPath);
                            } catch (NumberFormatException e) {
                                GlobalLogger.log("Invalid library key found in libraryfolders.vdf: " + key);
                            }
                        }
                        foundTop = true;
                    } else if (top.numParents() > 0) {
                        // Newer-style libraryfolders.vdf
                        for (VDFElement parent : top.getParents()) {
                            try {
                                // Don't actually care what the integer value is, just want to make sure
                                // it *is* an integer value.
                                int foo = Integer.parseInt(parent.getName());
                                String newPath = parent.getKey("path").toString().replace("\\\\", "\\");
                                libraryFolders.add(newPath);
                            } catch (NumberFormatException e) {
                                GlobalLogger.log("Invalid library key found in libraryfolders.vdf: " + parent.getName());
                            }
                        }
                        foundTop = true;
                    }
                    break;
                }
            }
            if (!foundTop) {
                GlobalLogger.log("Unable to parse Steam libraryfolders.vdf");
            }
        } catch (IOException e) {
            e.printStackTrace();
            GlobalLogger.log("Unable to parse Steam libraryfolders.vdf: " + e.toString());
        }

        return libraryFolders;
    }

    private static String getSteamDataDir() {
        switch (OSInfo.CURRENT_OS) {
            case MAC:
                return System.getenv("HOME") + "/Library/Application Support/Steam";
            case UNIX:
                String dataDir = System.getenv("XDG_DATA_HOME");
                if (dataDir == null) {
                    dataDir = System.getenv("HOME") + "/.local/share";
                }
                return dataDir + "/Steam";
            case WINDOWS:
                return System.getenv("HOME") + "\\AppData\\Roaming\\Steam";
        }
        return null;
    }

    //########################################################################################################
    //Below are several methods that return pre-set paths offset based on the paths detected by the code above
    //########################################################################################################

    /**
     * Returns the path to the "binaries" directory for the specified game.
     * Relies on our data having been initialized via FindGames prior to
     * running.
     *
     * @param type the game type
     * @return A pathname to the "binaries" folder, or null.
     */
    public static String getBinariesDir(PatchType type) {
        String path = getPath(type);
        if (path == null) {
            return null;
        }
        String binariesPath;
        switch (OSInfo.CURRENT_OS) {
            case WINDOWS:
                binariesPath = path + "\\Binaries";
                break;
            case MAC:
                binariesPath = path + "/GameData/Binaries";
                break;
            default:
                if (isUnixUsingProton(type)) {
                    binariesPath = path + "/Binaries";
                } else {
                    binariesPath = path + "/steamassets/binaries";
                }
        }
        File f = new File(binariesPath);
        if (f.exists()) {
            if (!LOGGED_BIN.contains(type)) {
                GlobalLogger.log("Binaries dir: " + Utilities.hideUserName(binariesPath));
                LOGGED_BIN.add(type);
            }
            return binariesPath;
        } else {
            if (LOGGED_BIN.contains(type)) {
                GlobalLogger.log("No binaries path found for " + type.toString() + " (" + Utilities.hideUserName(binariesPath) + ")");
                LOGGED_BIN.add(type);
            }
            return null;
        }
    }

    public static File getTPSExe() {
        return getExe(PatchType.TPS);
    }

    public static File getBL2Exe() {
        return getExe(PatchType.BL2);
    }

    public static File getAODKExe() {
        return getExe(PatchType.TPS);
    }

    public static File getExe(PatchType type) {
        return getExe(type, getPath(type));
    }

    private static File getExe(PatchType type, final String testPath) {
        // Default to false for this -- want to make sure that *some* value exists
        UNIX_USING_PROTON.put(type, false);

        // TODO: arguably this should be done in the PatchType class
        String filename = null;
        switch (type) {
            case BL2:
                filename = "Borderlands2";
                break;
            case TPS:
                filename = "BorderlandsPreSequel";
                break;
            case AODK:
                filename = "TinyTina";
                break;
        }
        if (testPath == null) {
            return null;
        }
        final ArrayList<String> postfixes = new ArrayList<>();
        switch (OSInfo.CURRENT_OS) {
            case UNIX:
                // There are two possibilities here: one for native Linux, one for Proton/Wine
                UNIX_SCANNED_PROTON.add(type);
                postfixes.add("/" + filename);
                postfixes.add("/Binaries/Win32/" + filename + ".exe");
                break;
            case MAC:
                postfixes.add("/MacOS/" + filename + "sub");
                postfixes.add("/MacOS/" + type.toString());//x64 changed the name to just BL2. This hasn't happened for TPS yet, but we might as well put this in
                break;
            default:
                postfixes.add("\\Binaries\\Win32\\" + filename + ".exe");
        }
        File exe = null;
        for (String postfix : postfixes) {
            exe = new File(testPath + postfix);
            if (exe.exists()) {
                if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
                    UNIX_USING_PROTON.put(type, postfix.contains(".exe"));
                }
                break;
            }
        }
        if (exe == null || !exe.exists()) {
            if (!LOGGED_EXE.contains(type)) {
                GlobalLogger.log("No executable found for " + type.toString() + " (" + Utilities.hideUserName(exe.toString()) + ")");
                LOGGED_EXE.add(type);
            }
            return null;
        } else {
            if (!LOGGED_EXE.contains(type)) {
                GlobalLogger.log("Found executable: " + Utilities.hideUserName(exe.getAbsolutePath()));
                LOGGED_EXE.add(type);
            }
        }
        return exe;
    }

    /**
     * Returns the OS that we are "virtually" operating as. For Windows and Mac,
     * this will always be the real OS. For UNIX/Linux, though, we will return
     * WINDOWS if we happen to be using the Wine/Proton version instead of
     * native.
     *
     * @param type The game to query
     * @return The OS to operate as
     */
    public static OSInfo.OS getVirtualOS(PatchType type) {
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            if (isUnixUsingProton(type)) {
                return OSInfo.OS.WINDOWS;
            }
        }
        return OSInfo.CURRENT_OS;
    }

    /**
     * Returns true/false depending on if the UNIX (Linux) host is using the
     * Windows version of the game (via Proton), as opposed to the native Linux
     * client. Proton/Wine is actually more likely than not nowadays, since the
     * native version is so far out of date. Will always return false on
     * non-UNIX OSes.
     *
     * @param type The game type to process
     * @return True if the Proton version is being used
     */
    private static boolean isUnixUsingProton(PatchType type) {
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            if (!UNIX_SCANNED_PROTON.contains(type)) {
                getExe(type);
            }
            return UNIX_USING_PROTON.get(type);
        } else {
            return false;
        }
    }

    public static File getEngineUPK(PatchType type) {
        String enginePathW = "/WillowGame/CookedPCConsole/Engine.upk";
        String enginePathM = "/GameData" + enginePathW;
        String enginePathL;
        if (isUnixUsingProton(type)) {
            // Case is important here!  Make sure Windows path remains capitalized.
            enginePathL = enginePathW;
        } else {
            // Contrariwise, this must be lowercase.
            enginePathL = "/steamassets" + enginePathW.toLowerCase();
        }

        String enginePath = OSInfo.CURRENT_OS == OSInfo.OS.MAC ? enginePathM : (OSInfo.CURRENT_OS == OSInfo.OS.WINDOWS ? enginePathW : enginePathL);
        File engine = new File(getPath(type) + enginePath);
        if (!engine.exists()) {
            GlobalLogger.log("No engine.upk found for " + type.toString() + " (" + Utilities.hideUserName(engine.toString()) + ")");
            return null;
        }
        GlobalLogger.log("Found engine.upk: " + Utilities.hideUserName(engine.getAbsolutePath()));
        return engine;
    }

    //######################################################################################
    //The methods below do not use the code above to return the paths to the indicated files
    //######################################################################################
    //
    /**
     * Returns the directory containing save files, logs and willow ini files.
     *
     * @return
     */
    private static String getPathToGameConfigFiles(PatchType type) {
        String gameDir = type.getGameDir();
        switch (OSInfo.CURRENT_OS) {
            case WINDOWS: {
                String[] myDocuments = getWindowsMyDocumentsFolder();
                String res = null;
                for (String base : myDocuments) {
                    res = String.format("%s\\My games\\%s\\", base, gameDir);
                    if (new File(res).exists()) {
                        break;
                    }
                    GlobalLogger.log("Atempting to find config folder failed for: " + Utilities.hideUserName(res));
                }
                return res;
            }
            case MAC: {
                String home = System.getenv("HOME");
                return String.format("%s/Library/Application Support/%s/", home, gameDir);
            }
            case UNIX: {
                if (isUnixUsingProton(type)) {
                    // TODO: arguably this should be handled by the PatchType class itself
                    String gameId;
                    switch (type) {
                        case BL2:
                            gameId = "49520";
                            break;
                        case TPS:
                            gameId = "261640";
                            break;
                        case AODK   :
                            gameId = "1712840";
                            break;
                        default:
                            gameId = "0";
                            break;
                    }
                    try {
                        // We could use our steam library folder detection again, but just doing
                        // a relative path here will keep us in the correct dir.  The
                        // `getCanonicalPath()` call will get rid of those for us.
                        String configDir = new File(getExe(type).getParent()
                                + "../../../../../compatdata/"
                                + gameId
                                + "/pfx/drive_c/users/steamuser/My Documents/My Games/"
                                + gameDir).getCanonicalPath() + "/";
                        return configDir;
                    } catch (IOException e) {
                        GlobalLogger.log("Attempting to find config folder failed for " + type.toString());
                        return null;
                    }
                } else {
                    String home = System.getenv("HOME");
                    return String.format("%s/.local/share/aspyr-media/%s/", home, gameDir.toLowerCase());
                }
            }
            default:
                return null;
        }
    }

    private static String[] getWindowsMyDocumentsFolder() {
        String myDocuments = GetRegField(new RegKeyLookup("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders", "personal"));
        if (myDocuments != null) {

            //Sometimes, the key we're looking for simply doesn't exist: https://i.imgur.com/aEcfEAj.png
            if (myDocuments.contains("?")) {

                //For some reason, the reg query byte stream does not give us the proper characters  when umlauts (and probably similar things) are in the username.
                //They are given correctly by this statement tho.
                String username = System.getProperty("user.name");
                String[] split = myDocuments.split("\\\\");
                for (String split1 : split) {
                    if (split1.contains("?")) {
                        //GlobalLogger.log("Replacing " + split1 + " with " + username);
                        myDocuments = myDocuments.replace(split1, username);
                    }
                }
            }
        }
        String myDocuments2 = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
        String[] res = myDocuments == null
                ? new String[]{myDocuments2}
                : new String[]{myDocuments, myDocuments2};
        return res;
    }

    public static String getPathToINIFiles(PatchType type) {
        String res = unixFixCaseInsensitive(getGameConfigPathWithPostfix(type, "WillowGame\\Config\\"));
        if (!LOGGED_INI.contains(type)) {
            GlobalLogger.log("Path to INI files of " + type.toString() + ": " + Utilities.hideUserName(res));
            LOGGED_INI.add(type);
        }
        return res;
    }

    /**
     * Determines if the computed path to the game's INI files actually exists.
     * This may not be the case until the game's been run once, and of course
     * assumes that we're even looking in the right spot (which could be
     * trickier on Linux, etc).
     * @param type The game type to look up
     * @return True if the INI file path exists, False otherwise.
     */
    public static boolean iniFilePathExists(PatchType type) {
        File iniPath = new File(getPathToINIFiles(type));
        return iniPath.exists();
    }

    private static String getPathToLogFiles(PatchType type) {
        String res = getGameConfigPathWithPostfix(type, "WillowGame\\Logs\\");
        GlobalLogger.log("Path to log files: " + Utilities.hideUserName(res));
        return res;
    }

    private static String getGameConfigPathWithPostfix(PatchType type, String postfix) {
        String gamedir = getPathToGameConfigFiles(type);
        if (OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
            postfix = postfix.replace("\\", "/");
        }
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX && !gamedir.contains("/pfx/drive_c/")) {
            postfix = postfix.toLowerCase();
        }
        return gamedir + postfix;
    }

    /**
     * This is a hideous little routine to attempt to find case-insensitive
     * matches for paths on Linux which may not exist in their given form.
     * Returns a possibly-case-normalized path, or just the original path if
     * an alternative wasn't found.
     *
     * We had a bug for some INI detection that was somehow not working, and
     * it turned out to be due to a "My games" (note the lowercase G) in the
     * path.  The game itself would launch fine, since Wine has a case-
     * insensitive layer somewhere deep in its code, but any "native" Linux
     * call wouldn't find it.
     *
     * So, if we can't find the given path, we'll loop through all the
     * steps in the path to see if we can find case-insensitive matches to try.
     * Technically we should only be doing this once the path gets into a
     * Wine root, but I'm fine with that.
     *
     * Note that this is hardly used anywhere at the moment; the EXE detection
     * itself is pretty complex and I can't bring myself to debug changes to it
     * at the moment.  So this is really only used in INI detection at the
     * moment.
     *
     * @param path The path to check
     * @return A possibly-case-normalized path, or the original path
     */
    private static String unixFixCaseInsensitive(String path) {
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            // First just check to see if it exists as-is
            File f = new File(path);
            if (f.exists()) {
                return path;
            }

            // If not, loop through each path entry, and if an entry isn't
            // found, see if we can do a case-insensitive match.  Technically
            // we should only be doing this inside Wine roots, but whatever.
            File constructed = new File("");
            File testFile;
            String pathComponent;
            boolean foundMatch;
            for (Path p : f.toPath()) {
                pathComponent = p.toString();
                testFile = new File(constructed.getPath() + "/" + pathComponent);
                foundMatch = false;
                if (testFile.exists()) {
                    foundMatch = true;
                } else {
                    if (constructed.isDirectory()) {
                        for (String s : constructed.list()) {
                            if (pathComponent.equalsIgnoreCase(s)) {
                                testFile = new File(constructed.getPath() + "/" + s);
                                foundMatch = true;
                                break;
                            }
                        }
                    }
                }
                if (foundMatch) {
                    constructed = testFile;
                } else {
                    return path;
                }
            }
            if (path.endsWith("/")) {
                return constructed.getPath() + "/";
            } else {
                return constructed.getPath();
            }
        } else {
            return path;
        }
    }

    private static File[] getLogFiles(PatchType type) {
        File f = new File(getPathToLogFiles(type));
        if (f.exists()) {
            File[] logs = f.listFiles();
            Arrays.sort(logs, (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));
            return logs;
        }
        return new File[0];
    }

    private GameDetection() {
    }

}
