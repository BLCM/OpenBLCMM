/*
 * Copyright (C) 2018-2020  LightChaosman
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
 */
package blcmm.utilities;

import blcmm.model.PatchType;
import general.utilities.GlobalLogger;
import general.utilities.OSInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.filechooser.FileSystemView;

public class GameDetection {

    //A boolean indicating if we've already done an effort to detect game files
    private static boolean run = false;

    // A couple of booleans to reduce clutter of logging.
    private static final boolean[] LOGGED_BIN = {false, false};
    private static final boolean[] LOGGED_EXE = {false, false};
    private static final boolean[] LOGGED_INI = {false, false};

    //For windows, this path
    private static String bl2Path, blTPSPath;

    //For Linux, some cached attributes to let us know if we're using Proton
    private static final boolean[] UNIX_SCANNED_PROTON = {false, false};
    private static final boolean[] UNIX_USING_PROTON = {false, false};

    /**
     * A way to set the path manually for BL2, in case game detection failed
     *
     * @param path
     */
    public static void setBL2PathManually(String path) {
        assert run;
        LOGGED_BIN[0] = true;
        bl2Path = path;
        GlobalLogger.log("Set BL2 path manually to: " + path);
    }

    /**
     * A way to set the path manually for TPS, in case game detection failed
     *
     * @param path
     */
    public static void setTPSPathManually(String path) {
        assert run;
        LOGGED_BIN[1] = false;
        blTPSPath = path;
        GlobalLogger.log("Set TPS path manually to: " + path);
    }

    public static String getPath(PatchType type) {
        return type == PatchType.BL2 ? getBL2Path() : getTPSPath();
    }

    public static String getPath(boolean BL2) {
        return BL2 ? getBL2Path() : getTPSPath();
    }

    public static String getBL2Path() {
        findGames();
        return bl2Path;
    }

    public static String getTPSPath() {
        findGames();
        return blTPSPath;
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
        GlobalLogger.log("Borderlands 2:              " + bl2Path);
        GlobalLogger.log("Borderlands The Pre Sequel: " + blTPSPath);
    }

    private static void windowsGameDetection() {
        //BL2 detection
        String regKey1 = "reg query \"HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 49520\" /v InstallLocation";
        String regKey2 = "reg query \"HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 49520\" /v InstallLocation";
        bl2Path = detectWindows(true, regKey1, regKey2);
        //BLTPS detection
        regKey1 = "reg query \"HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640\" /v InstallLocation";
        regKey2 = "reg query \"HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640\" /v InstallLocation";
        blTPSPath = detectWindows(false, regKey1, regKey2);
    }

    private static String detectWindows(boolean BL2, String... regKeys) {
        String game = BL2 ? "Borderlands 2" : "Borderlands The Pre-Sequel";
        String path = null;
        for (int i = 0; i < regKeys.length && path == null; i++) {
            path = GetRegField(regKeys[i]);
        }
        if (path == null) {
            GlobalLogger.log("Can't find " + game + " Steam installation on registry.");
            path = FindGamePathInLog(BL2);
        } else if (!new File(path).exists()) {
            GlobalLogger.log("The found registry installation of " + game + " doesn't exist. (" + path + ")");
            path = FindGamePathInLog(BL2);
        } else if (getExe(BL2, path) == null) {
            GlobalLogger.log("The found registry installation of " + game + " doesn't have an executable. (" + path + ")");
            path = FindGamePathInLog(BL2);
        } else {
            GlobalLogger.log("Found " + game + " Steam installation on registry.");
        }
        return path;
    }

    ///String example: "reg query \"HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Steam App 261640\" /v InstallLocation"
    static String GetRegField(String regKey) {
        String reg = null;
        try {
            Process p = Runtime.getRuntime().exec(regKey);
            p.waitFor();

            InputStream in = p.getInputStream();
            byte[] b = new byte[in.available()];
            in.read(b);
            in.close();
            reg = new String(b);
            return reg.split("\\s\\s+")[4];
        } catch (IOException | ArrayIndexOutOfBoundsException | InterruptedException t) {
            GlobalLogger.log(reg);
            GlobalLogger.log(t);
            return null;
        }
    }

    private static String FindGamePathInLog(boolean BL2) {
        String game = BL2 ? "Borderlands 2" : "Borderlands The Pre-Sequel";
        final String start = "Log: Base directory: ";
        String path = null;
        fileLoop:
        for (File log : getLogFiles(BL2)) {
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
                                    GlobalLogger.log("Disregarding path due to lack of 'steam' substring: " + path2);
                                    break;
                                }
                            } else if (!new File(path2).exists()) {
                                GlobalLogger.log("Disregarding path due to not existing: " + path2);
                                break;
                            } else if (getExe(BL2, path2) == null) {
                                GlobalLogger.log("Disregarding path due to not having an executable: " + path2);
                                break;
                            }
                            path = path2;
                            GlobalLogger.log("Found path for " + game + " in the logs: " + path + " (using log: " + log + ")");
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
            GlobalLogger.log(folder);

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
                    }
                }
            } catch (NullPointerException err) {
                GlobalLogger.log("Directory " + folder + " is incorrect, it doesn't have any files");
            }
        }
        if (OSInfo.CURRENT_OS == OSInfo.OS.MAC) {
            if (bl2Path != null) {
                bl2Path = bl2Path + "/Borderlands2.app/Contents";
            }
            if (blTPSPath != null) {
                blTPSPath = blTPSPath + "/BorderlandsPreSequel.app/Contents";
            }
        }
        if (bl2Path == null) {
            bl2Path = FindGamePathInLog(true);
        } else {
            GlobalLogger.log("Succesfully found installation of Borderlands 2");
        }
        if (blTPSPath == null) {
            blTPSPath = FindGamePathInLog(false);
        } else {
            GlobalLogger.log("Succesfully found installation of Borderlands: The Pre-Sequel");
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

        try (BufferedReader br = new BufferedReader(new FileReader(vdfFile));) {
            String line;
            int i = 1;
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split("\"");
                for (int j = 0; j < splittedLine.length; j++) {
                    if (splittedLine[j].equals(Integer.toString(i))) {
                        libraryFolders.add(splittedLine[j + 2]);
                        j += 2;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    //
    /**
     * Returns the path to the "binaries" directory for the specified game.
     * Relies on our data having been initialized via FindGames prior to
     * running.
     *
     * @param type the game type
     * @return A pathname to the "binaries" folder, or null.
     */
    public static String getBinariesDir(PatchType type) {
        return getBinariesDir(type == PatchType.BL2);
    }

    /**
     * Returns the path to the "binaries" directory for the specified game.
     * Relies on our data having been initialized via FindGames prior to
     * running.
     *
     * @param BL2 True for BL2, False for TPS
     * @return A pathname to the "binaries" folder, or null.
     */
    public static String getBinariesDir(boolean BL2) {
        String path = getPath(BL2);
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
                if (isUnixUsingProton(BL2)) {
                    binariesPath = path + "/Binaries";
                } else {
                    binariesPath = path + "/steamassets/binaries";
                }
        }
        File f = new File(binariesPath);
        if (f.exists()) {
            if (!LOGGED_BIN[BL2 ? 0 : 1]) {
                GlobalLogger.log("Binaries dir: " + binariesPath);
                LOGGED_BIN[BL2 ? 0 : 1] = true;
            }
            return binariesPath;
        } else {
            if (LOGGED_BIN[BL2 ? 0 : 1]) {
                GlobalLogger.log("No binaries path found for " + (BL2 ? "BL2" : "TPS") + " (" + binariesPath + ")");
                LOGGED_BIN[BL2 ? 0 : 1] = true;
            }
            return null;
        }
    }

    public static File getTPSExe() {
        return getExe(false);
    }

    public static File getBL2Exe() {
        return getExe(true);
    }

    public static File getExe(boolean BL2) {
        return getExe(BL2, getPath(BL2));
    }

    public static File getExe(PatchType type) {
        return getExe(type == PatchType.BL2, getPath(type));
    }

    private static File getExe(boolean BL2, final String testPath) {
        final String filename = BL2 ? "Borderlands2" : "BorderlandsPreSequel";
        if (testPath == null) {
            return null;
        }
        final ArrayList<String> postfixes = new ArrayList<>();
        switch (OSInfo.CURRENT_OS) {
            case UNIX:
                // There are two possibilities here: one for native Linux, one for Proton/Wine
                UNIX_SCANNED_PROTON[BL2 ? 0 : 1] = true;
                postfixes.add("/" + filename);
                postfixes.add("/Binaries/Win32/" + filename + ".exe");
                break;
            case MAC:
                postfixes.add("/MacOS/" + filename + "sub");
                postfixes.add("/MacOS/" + (BL2 ? "BL2" : "TPS"));//x64 changed the name to just BL2. This hasn't happened for TPS yet, but we might as well put this in
                break;
            default:
                postfixes.add("\\Binaries\\Win32\\" + filename + ".exe");
        }
        File exe = null;
        for (String postfix : postfixes) {
            exe = new File(testPath + postfix);
            if (exe.exists()) {
                if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
                    UNIX_USING_PROTON[BL2 ? 0 : 1] = postfix.contains(".exe");
                }
                break;
            }
        }
        if (exe == null || !exe.exists()) {
            if (!LOGGED_EXE[BL2 ? 0 : 1]) {
                GlobalLogger.log("No executable found for " + (BL2 ? "BL2" : "TPS") + " (" + exe + ")");
                LOGGED_EXE[BL2 ? 0 : 1] = true;
            }
            return null;
        } else {
            if (!LOGGED_EXE[BL2 ? 0 : 1]) {
                GlobalLogger.log("Found executable: " + exe.getAbsolutePath());
                LOGGED_EXE[BL2 ? 0 : 1] = true;
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
     * @param BL2 True for if we are querying BL2, or False for TPS
     * @return The OS to operate as
     */
    public static OSInfo.OS getVirtualOS(boolean BL2) {
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            if (isUnixUsingProton(BL2)) {
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
     * @param BL2 True for if we are querying BL2, or False for TPS.
     * @return True if the Proton version is being used
     */
    private static boolean isUnixUsingProton(boolean BL2) {
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            if (!UNIX_SCANNED_PROTON[BL2 ? 0 : 1]) {
                getExe(BL2);
            }
            return UNIX_USING_PROTON[BL2 ? 0 : 1];
        } else {
            return false;
        }
    }

    public static File getEngineUPK(boolean BL2) {
        String enginePathW = "/WillowGame/CookedPCConsole/Engine.upk";
        String enginePathM = "/GameData" + enginePathW;
        String enginePathL;
        if (isUnixUsingProton(BL2)) {
            // Case is important here!  Make sure Windows path remains capitalized.
            enginePathL = enginePathW;
        } else {
            // Contrariwise, this must be lowercase.
            enginePathL = "/steamassets" + enginePathW.toLowerCase();
        }

        String enginePath = OSInfo.CURRENT_OS == OSInfo.OS.MAC ? enginePathM : (OSInfo.CURRENT_OS == OSInfo.OS.WINDOWS ? enginePathW : enginePathL);
        File engine = new File(getPath(BL2) + enginePath);
        if (!engine.exists()) {
            GlobalLogger.log("No engine.upk found for " + (BL2 ? "BL2" : "TPS") + " (" + engine + ")");
            return null;
        }
        GlobalLogger.log("Found engine.upk: " + engine.getAbsolutePath());
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
    private static String getPathToGameConfigFiles(boolean BL2) {
        String gamename = BL2 ? "Borderlands 2" : "Borderlands The Pre-Sequel";
        switch (OSInfo.CURRENT_OS) {
            case WINDOWS: {
                String[] myDocuments = getWindowsMyDocumentsFolder();
                String res = null;
                for (String base : myDocuments) {
                    res = String.format("%s\\My games\\%s\\", base, gamename);
                    if (new File(res).exists()) {
                        break;
                    }
                    GlobalLogger.log("Atempting to find config folder failed for: " + res);
                }
                return res;
            }
            case MAC: {
                String home = System.getenv("HOME");
                return String.format("%s/Library/Application Support/%s/", home, gamename);
            }
            case UNIX: {
                if (isUnixUsingProton(BL2)) {
                    try {
                        // We could use our steam library folder detection again, but just doing
                        // a relative path here will keep us in the correct dir.  The
                        // `getCanonicalPath()` call will get rid of those for us.
                        String configDir = new File(getExe(BL2).getParent()
                                + "../../../../../compatdata/"
                                + (BL2 ? "49520" : "261640")
                                + "/pfx/drive_c/users/steamuser/My Documents/My Games/"
                                + gamename).getCanonicalPath() + "/";
                        return configDir;
                    } catch (IOException e) {
                        GlobalLogger.log("Attempting to find config folder failed for " + (BL2 ? "BL2" : "TPS"));
                        return null;
                    }
                } else {
                    String home = System.getenv("HOME");
                    return String.format("%s/.local/share/aspyr-media/%s/", home, gamename.toLowerCase());
                }
            }
            default:
                return null;
        }
    }

    private static String[] getWindowsMyDocumentsFolder() {
        String myDocuments = GetRegField("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v personal");
        if (myDocuments != null) {

            //Sometimes, the key we're looking for simply doesn't exist: https://i.imgur.com/aEcfEAj.png
            if (myDocuments.contains("?")) {

                //For some reason, the reg query byte stream does not give us the proper characters  when umlauts (and probably similar things) are in the username.
                //They are given correctly by this statement tho.
                String username = System.getProperty("user.name");
                String[] split = myDocuments.split("\\\\");
                for (String split1 : split) {
                    if (split1.contains("?")) {
                        GlobalLogger.log("Replacing " + split1 + " with " + username);
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

    public static String getPathToINIFiles(boolean BL2) {
        String res = getGameConfigPathWithPostfix(BL2, "WillowGame\\Config\\");
        if (!LOGGED_INI[BL2 ? 0 : 1]) {
            GlobalLogger.log("Path to INI files of " + (BL2 ? "BL2" : "TPS") + ": " + res);
            LOGGED_INI[BL2 ? 0 : 1] = true;
        }
        return res;
    }

    private static String getPathToLogFiles(boolean BL2) {
        String res = getGameConfigPathWithPostfix(BL2, "WillowGame\\Logs\\");
        GlobalLogger.log("Path to log files: " + res);
        return res;
    }

    private static String getGameConfigPathWithPostfix(boolean BL2, String postfix) {
        String gamedir = getPathToGameConfigFiles(BL2);
        if (OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
            postfix = postfix.replace("\\", "/");
        }
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX && !gamedir.contains("/pfx/drive_c/")) {
            postfix = postfix.toLowerCase();
        }
        return gamedir + postfix;
    }

    private static File[] getLogFiles(boolean BL2) {
        File f = new File(getPathToLogFiles(BL2));
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
