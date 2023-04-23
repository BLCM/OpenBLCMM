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

import blcmm.Meta;
import blcmm.model.PatchType;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 *
 * @author LightChaosman
 */
public class Utilities {

    private static boolean creatorMode = false;
    private static String userDir = null;
    private static String installDirOverride = null;

    static {
        Utilities.setUserDir(System.getProperty("user.dir"));
    }

    /**
     * Returns true if we're in Creator Mode.  This can be triggered by two
     * things:
     *
     * 1. If Java assertions are enabled (generally via the -ea arg).
     * That's in the default runtime args when in our Netbeans project, so
     * folks hacking on code should get Creator Mode automatically.
     *
     * 2. If the user specifically used the "-creator" argument on the app.
     *
     * @return
     */
    public static boolean isCreatorMode() {
        if (Utilities.creatorMode) {
            return true;
        }
        boolean x = false;
        assert x = true;
        return x;
    }

    /**
     * Sets our Creator Mode flag to True.  Note that Creator Mode will be
     * active even without this flag, if Java assertions are enabled (generally
     * via the -ea argument)
     */
    public static void setCreatorMode() {
        Utilities.creatorMode = true;
        GlobalLogger.resetLogFolder();
    }

    /**
     * Sets our user directory to the specified dir.  Basically just used for
     * the bundled Mac app, so we don't try to write into the .app dir, or
     * look for datapacks inside it.  The default user dir is the directory
     * that Java was launched from (available via the property "user.dir").
     *
     * @param newDir The directory to use as a "user" dir.
     */
    public static void setUserDir(String newDir) {
        Utilities.userDir = new File(newDir).getAbsolutePath();
    }

    /**
     * Returns our current userDir.  Affects a few things, including some
     * button shortcuts on the various file dialogs.  Is also used pretty
     * heavily when in Creator Mode, to know where to write our prefs, logs,
     * etc.
     *
     * @return The current user dir
     */
    public static String getUserDir() {
        return Utilities.userDir;
    }

    /**
     * Sets our install-dir override, mostly used for the Mac bundle where
     * we want to report up a dir from where we'd usually report, since
     * otherwise we'd be pointing folks inside the .app folder.  The primary
     * effect of this is allowing OE datapacks to be found outside that dir.
     *
     * @param newDir
     */
    public static void setInstallDirOverride(String newDir) {
        Utilities.installDirOverride = new File(newDir).getAbsolutePath();
    }

    /**
     * Replaces usernames in paths by '[user]'. Currently English and Dutch
     * Windows only.  This now only replaces "proper" user-dir prefixes, to
     * avoid having false-positive replacements later on in the path, which
     * should cover essentially all cases anyway.
     *
     * @param text
     * @return
     */
    public static String hideUserName(String text) {
        if (text == null) {
            return null;
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null
                && userHome.length() > 0
                && text.startsWith(userHome)) {
            if (OSInfo.CURRENT_OS != OSInfo.OS.WINDOWS) {
                return text.replace(userHome, "~");
            } else {
                String userName = System.getProperty("user.name");
                String replaced = userHome.substring(0, userHome.length() - userName.length()) + "[user]";
                return text.replace(userHome, replaced);
            }
        }
        return text;
    }

    /**
     * Unzips the given .zip file into a temporary folder. Do not forget to
     * delete this temporary folder after you're done with it, or rename it to
     * something permanent. Deletion can be done using the deepDelete method.
     *
     * @param zipped The zip file
     * @return The temporary foldeder containing the unzipped contents.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static File unzip(File zipped) throws FileNotFoundException, IOException {
        File tempdir = new File("temp-" + System.currentTimeMillis());
        return unzip(zipped, tempdir);
    }

    /**
     * Unzips the given .zip file into the provided folder.
     *
     * @param zipFile The zip file
     * @param destDir The
     * @return The temporary foldeder containing the unzipped contents.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static File unzip(File zipFile, File destDir) throws FileNotFoundException, IOException {
        return unzip(new FileInputStream(zipFile), destDir);
    }

    /**
     * Unzips the given stream into the provided folder. Closes the input stream
     * after completion.
     *
     * @param inputStream The inputstream
     * @param destDir The folder to unpack into
     * @return The folder containing the unzipped stream, i.e. destDir
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static File unzip(InputStream inputStream, File destDir) throws IOException {
        destDir.mkdirs();
        try (ZipInputStream zipstream = new ZipInputStream(inputStream)) {
            ZipEntry entry = zipstream.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String filename = entry.getName();
                    File newfile = new File(destDir + File.separator + filename);
                    new File(newfile.getParent()).mkdirs();
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newfile))) {
                        byte[] bs = new byte[4096];
                        int res;
                        while ((res = zipstream.read(bs)) != -1) {
                            bos.write(bs, 0, res);
                        }
                    }
                }
                entry = zipstream.getNextEntry();
            }
        }
        return destDir;
    }

    /**
     * Deletes all files listed under the provided directory recursively
     *
     * @param directory
     */
    public static void deepDelete(File directory) {
        if (directory.isDirectory()) {
            for (File f : directory.listFiles()) {
                deepDelete(f);
            }
        }
        directory.delete();
    }

    /**
     * reads the provided file to memory, and returns a string representation.
     * Use this only to parse small files. Storing big files in RAM for parsing
     * can cause memory issues.
     *
     * @param file The file to read
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static String readFileToString(File file) throws IOException, FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Writes the provided string to the provided file. Returns a boolean
     * indicating if the write was successful.
     *
     * @param string
     * @param file
     * @throws java.io.IOException
     */
    public static void writeStringToFile(CharSequence string, File file) throws IOException {
        BufferedWriter br = new BufferedWriter(new FileWriter(file));
        br.append(string);
        br.close();

    }

    public static String sha256(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        InputStream fis = new FileInputStream(file);
        String sha256 = sha256(fis);
        fis.close();
        return sha256;
    }

    public static String sha256(InputStream stream) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] data = new byte[1024];
        int read;
        while ((read = stream.read(data)) != -1) {
            sha256.update(data, 0, read);
        }
        byte[] hashBytes = sha256.digest();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        String fileHash = sb.toString();
        return fileHash;
    }

    public static String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Downloads a file from an URL to a temporary file.
     *
     * @param fileURL the URL to the file to download
     * @return the downloaded File
     * @throws IOException
     */
    public static File downloadFile(String fileURL) throws IOException {
        return downloadFile(fileURL, File.createTempFile("download", ".temp").getAbsoluteFile().getParentFile());
    }

    /**
     * Downloads a file from an URL to the provided directory
     *
     * @param fileURL the URL to the file to download
     * @param directory the directory in which to place the downloaded file
     * @return the downloaded File
     * @throws IOException
     */
    public static File downloadFile(String fileURL, File directory) throws IOException {
        File f = new File(directory.getAbsolutePath() + File.separator + "download.temp");
        String n = downloadFile(fileURL, new FileOutputStream(f));
        if (n != null) {
            File f2 = new File(directory.getAbsolutePath() + File.separator + n);
            f.renameTo(f2);
            return f2;
        }
        return null;
    }

    /**
     * Downloads a file from an URL and returns its contents as a String object.
     *
     * @param fileURL the URL of the file to download
     * @return The content of the downloaded file
     * @throws IOException
     */
    public static String downloadFileToString(String fileURL)
            throws IOException {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        String res = downloadFile(fileURL, boas);//closes boas
        return res != null ? new String(boas.toByteArray(), StandardCharsets.UTF_8) : null;
    }

    /**
     * Downloads a file from an URL
     *
     * @param fileURL the URL to the file which needs to be downloaded
     * @param outputStream the stream to which to write the downloaded file
     * @return the filename of the downloaded file
     * @throws IOException
     */
    private static String downloadFile(String fileURL, OutputStream outputStream)
            throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();
        String res = null;
        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "";
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }

            System.out.println("Content-Type = " + contentType);
            System.out.println("Content-Disposition = " + disposition);
            System.out.println("Content-Length = " + contentLength);
            System.out.println("fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();

            // opens an output stream to save into file
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            res = fileName;
            System.out.println("File downloaded");
        } else {
            System.out.println("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
        return res;
    }

    /**
     * Makes the window containing the provided component resizable
     *
     * @param component
     */
    public static void makeWindowOfComponentResizable(final Component component) {
        component.addHierarchyListener((HierarchyEvent e) -> {
            Window window = SwingUtilities.getWindowAncestor(component);
            if (window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialog.isResizable()) {
                    dialog.setResizable(true);
                }
                for (KeyListener kl : window.getKeyListeners()) {
                    window.removeKeyListener(kl);
                }
            }
        });
    }

    public static void changeCTRLMasks(Collection<JComponent> components) {
        for (JComponent component : components) {
            changeCTRLMasks(component);
        }
    }

    public static void changeCTRLMasks(JComponent component) {
        changeMasks(component, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.CTRL_MASK,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    private static void changeMasks(JComponent component, final int oldmask, final int replacingMask) {
        if (component instanceof JMenuBar) {
            for (int i = 0; i < ((JMenuBar) component).getMenuCount(); i++) {
                changeMasks(((JMenuBar) component).getMenu(i), oldmask, replacingMask);
            }
        } else if (component instanceof JMenu) {
            for (Component c2 : ((JMenu) component).getMenuComponents()) {
                changeMasks((JComponent) c2, oldmask, replacingMask);
            }
        } else if (component instanceof JMenuItem) {
            JMenuItem item = (JMenuItem) component;
            KeyStroke accelerator = item.getAccelerator();
            if (accelerator == null) {
                return;
            }
            int currentmask = accelerator.getModifiers();
            if ((currentmask & oldmask) == oldmask) {
                int newmask = (currentmask & (~oldmask)) | replacingMask;
                item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(accelerator.getKeyCode(), newmask));
            }
        } else if (component instanceof Container) {
            for (Component c2 : ((Container) component).getComponents()) {
                if (c2 instanceof JComponent) {
                    changeMasks((JComponent) c2, oldmask, replacingMask);
                }
            }
            changeMasksOfInputMap(component, oldmask, replacingMask);
        } else {
            changeMasksOfInputMap(component, oldmask, replacingMask);
        }
    }

    private static void changeMasksOfInputMap(JComponent component, final int oldmask, final int replacingMask) {
        InputMap inputMap = component.getInputMap();
        if (inputMap == null || inputMap.allKeys() == null) {
            return;
        }
        HashMap<KeyStroke, KeyStroke> replacements = new HashMap<>();
        for (KeyStroke stroke : inputMap.allKeys()) {
            if ((stroke.getModifiers() & oldmask) == oldmask) {
                int newmask = (stroke.getModifiers() & (~oldmask)) | replacingMask;
                replacements.put(stroke, javax.swing.KeyStroke.getKeyStroke(stroke.getKeyCode(), newmask));
            }
        }
        for (KeyStroke stroke : replacements.keySet()) {
            Object get = inputMap.get(stroke);
            inputMap.remove(stroke);
            inputMap.put(replacements.get(stroke), get);
        }
    }

    /**
     * Returns a data directory in a user-writeable location which the app can
     * use to store data, should it want to. The directory itself is not
     * guaranteed to exist, though all of its parents should exist. May return a
     * null value if we couldn't find any valid parents for our directory to
     * live. This may differ from the prefix used by Steam in getSteamDataDir,
     * which is why we don't use a common code base for there.
     *
     * @param appDirName The directory name for the application itself
     * @return The path to a data directory, or null
     */
    public static String getAppDataDir(String appDirName) {

        // Grab our homedir, and do some basic sanity checks on it.
        String homeDir = System.getProperty("user.home");
        if (homeDir != null) {
            if (homeDir.isEmpty() || !new File(homeDir).exists()) {
                homeDir = null;
            }
        }

        // A handy string to use, should we need it.
        String dirTest;

        switch (OSInfo.CURRENT_OS) {

            case MAC:

                // If we don't have a homedir, we're sunk.
                if (homeDir == null) {
                    return null;
                }

                // Just a single hardcoded option here
                String prefDir = homeDir + "/Library/Application Support";
                if (new File(prefDir).exists()) {
                    return prefDir + "/" + appDirName;
                }

                // Just return null, not sure what else to do here.
                return null;

            case UNIX:

                File dataHome;

                // Check for $XDG_DATA_HOME, first
                dirTest = System.getenv("XDG_DATA_HOME");
                if (dirTest != null
                        && !dirTest.isEmpty()) {
                    // On Linux, at least, it would be appropriate to create
                    // the missing directory here.  I know I could test the
                    // return value of .mkdirs() here but this makes the code
                    // a bit flatter.
                    try {
                        if (dirTest.substring(0, 2).equals("~/") && homeDir != null) {
                            // I'm not actually sure if tildes are actually
                            // allowable, but we'll pretend that they are.
                            dirTest = dirTest.replaceFirst("~", homeDir);
                        }
                    } catch (StringIndexOutOfBoundsException x) {
                    }
                    // Only allow absolute paths.  Technically we should probably
                    // use anything that's given to us, but the intention for
                    // these is definitely absolute paths.
                    if (dirTest.substring(0, 1).equals("/")) {
                        dataHome = new File(dirTest);
                        if (!dataHome.exists()) {
                            dataHome.mkdirs();
                        }
                        if (dataHome.exists()) {
                            return dirTest + "/" + appDirName;
                        }
                    }
                }

                // In the absence of that, make sure we have a homedir...
                if (homeDir == null) {
                    return null;
                }

                // And now default to ~/.local/share
                dirTest = homeDir + "/.local/share";

                // As with XDG_DATA_HOME, on Linux it is appropriate for an
                // app to create this dir if it doesn't exist.  I know I could
                // test the return of .mkdirs() here, but this makes the code
                // a bit flatter.
                dataHome = new File(dirTest);
                if (!dataHome.exists()) {
                    dataHome.mkdirs();
                }
                if (dataHome.exists()) {
                    return dirTest + "/" + appDirName;
                }

                // If that wasn't found, just give up?
                return null;

            case WINDOWS:
            default:

                // First up, %LOCALAPPDATA% should be the easiest way.
                dirTest = System.getenv("LOCALAPPDATA");
                if (dirTest != null
                        && !dirTest.isEmpty()
                        && new File(dirTest).exists()) {
                    return dirTest + "\\" + appDirName;
                }

                // AFAIK, if we get here, something's really weird - that
                // LOCALAPPDATA dir is supposed to exist.  Still, we'll try
                // to be clever, so long as we have a homedir.
                if (homeDir == null) {
                    return null;
                }

                // Look for a few alternatives.  Any even vaguely-modern
                // Windows should succeed at AppData/Local; the other tests may
                // match for ancient versions of Windows which may or may nott
                // even be able to run Java 8.
                for (String appdata : new String[]{"AppData", "Application Data"}) {
                    for (String homeSuffix : new String[]{
                        appdata + "\\Local",
                        "Local Settings\\" + appdata
                    }) {
                        dirTest = homeDir + "\\" + homeSuffix;
                        if (new File(dirTest).exists()) {
                            return dirTest + "\\" + appDirName;
                        }
                    }
                }

                // Yes, now would be an excellent time to give up.
                return null;
        }
    }

    // Migrated from BLCMMUtilities as part of the OpenBLCMM dev process
    private static void populate(String[] patches, PatchType type, final int maxDistance, List<String> res) {
        if (GameDetection.getBinariesDir(type) != null) {
            Map<File, Integer> map = new HashMap<>();
            String binaries = GameDetection.getBinariesDir(type).replace("\\", "/");
            LevenshteinDistance ld = new LevenshteinDistance();
            for (File f : new File(binaries).listFiles()) {
                for (String patch : patches) {
                    int dist = ld.apply(f.getName().toLowerCase(), patch);
                    if (dist <= maxDistance && f.isFile()) {
                        map.put(f, Math.min(map.getOrDefault(f, dist), dist));
                    }
                }
            }
            List<File> fs = new ArrayList<>();
            fs.addAll(map.keySet());
            fs.sort((t, t1) -> Integer.compare(map.get(t), map.get(t1)));
            if (fs.size() > 0) {
                res.add(fs.get(0).getAbsolutePath());
            }
        }
    }

    /**
     * Called when starting up the GUI to make sure that, if we have files in
     * our file history, they exist. Will purge the list of any nonexistent
     * files. If this results in an empty history, we will call our detection
     * routines to attempt to populate it.
     *
     * Migrated from BLCMMUtilities as part of the OpenBLCMM dev process.
     *
     * @param curType The currently-active patch type
     */
    public static void cleanFileHistory(PatchType curType) {
        String[] fileHistory = Options.INSTANCE.getFileHistory();
        ArrayList<String> newHistory = new ArrayList<>();
        File curFile;
        for (String filename : fileHistory) {
            curFile = new File(filename);
            if (curFile.exists()) {
                newHistory.add(filename);
            }
        }
        if (fileHistory.length != newHistory.size()) {
            Options.INSTANCE.setFileHistory(newHistory.toArray(new String[0]));
            populateFileHistory(curType);
        }
    }

    /**
     * Returns a directory which we can use to store data, should we want to. If
     * we couldn't find existing app data directories via the detection in
     * Utilities, we'll default to our current working directory. Note that the
     * actual directory returned here is not guaranteed to exist, but all its
     * parent dirs should exist.  Note that Creative Mode will always choose
     * the current working directory!
     *
     * Migrated from BLCMMUtilities as part of the OpenBLCMM dev process.
     *
     * @return A path to a directory we should be able to use to store data,
     * writable by the user.
     */
    public static String getBLCMMDataDir() {
        if (Utilities.isCreatorMode()) {
            // In creator mode, we always store stuff in the CWD
            return Utilities.userDir;
        } else {
            // In regular mode, we should be going into an appdir, if we can.
            String detectedDir = Utilities.getAppDataDir(Meta.APP_DATA_DIR_NAME);
            if (detectedDir == null) {
                // If we got here, nothing could be autodetected.  Fall back to the
                // current working directory, I guess.  Let's just assume that this
                // exists.  To remain compatible with the way the app has always
                // worked when run without a launcher, we're going to NOT add
                // appDirName here.
                return Utilities.userDir;
            } else {
                return detectedDir;
            }
        }
    }

    /**
     * Populates our file history based on GameDetection, if we don't already
     * have data in the file history.
     *
     * Migrated from BLCMMUtilities as part of the OpenBLCMM dev process.
     *
     * @param showFirst The game type whose patch files we should show first
     * (basically just putting the current game higher in the list)
     */
    public static void populateFileHistory(PatchType showFirst) {
        if (Options.INSTANCE.getFileHistory().length == -1) {
            final int maxDistance = 4; //arbitrary
            List<String> res = new ArrayList<>();
            if (showFirst != null) {
                populate(Utilities.getDefaultPatchNames(showFirst), showFirst, maxDistance, res);
            }
            for (PatchType loopType : PatchType.values()) {
                if (loopType != showFirst) {
                    populate(Utilities.getDefaultPatchNames(loopType), loopType, maxDistance, res);
                }
            }
            Options.INSTANCE.setFileHistory(res.toArray(new String[0]));
        }
    }

    /**
     * Given a PatchType, return the list of "main" patch names that we'll
     * look for first.
     *
     * @param type The type to look up
     * @return A default list of patch names to look for
     */
    private static String[] getDefaultPatchNames(PatchType type) {
        if (type == PatchType.TPS) {
            return new String[]{"patch.txt", "patchtps.txt"};
        } else {
            return new String[]{"patch.txt"};
        }
    }

    public static class CodeFormatter {

        private final static String INDENTATION = "    ";

        public static String formatCode(String original) {

            StringBuilder sb = new StringBuilder();
            original = original.replaceAll("\n", "   ");
            int depth = 0;
            original = original.trim();
            for (int i = 0; i < original.length(); i++) {
                char c = original.charAt(i);

                switch (c) {
                    case '(': {
                        boolean b = true;
                        boolean d = false;
                        int l = 1;
                        while (i + l < original.length()) {
                            char c2 = original.charAt(i + l);
                            if (Character.isDigit(c2) || c2 == ' ' || c2 == ',') {
                                l++;
                            } else if (c2 == ')') {
                                d = true;
                                break;
                            } else {
                                b = false;
                                break;
                            }
                        }
                        if (b && d) {
                            sb.append(original.substring(i, i + l + 1));
                            i = i + l;
                        } else {
                            boolean lastIsPlus = sb.length() > 0 && sb.charAt(sb.length() - 1) == '+';
                            if (lastIsPlus) {
                                sb.setLength(sb.length() - 1);
                            }
                            removeTrailingWhiteSpace(sb);
                            sb.append("\n");
                            addIndentation(sb, depth);
                            if (lastIsPlus) {
                                sb.append("+");
                            }
                            sb.append(c);
                            depth++;
                            int j = 1;
                            while (i + j < original.length() && original.charAt(i + j) == ' ') {
                                j++;
                            }
                            if (i + j < original.length() && original.charAt(i + j) != '(') {
                                sb.append("\n");
                                addIndentation(sb, depth);
                            }
                        }
                        break;
                    }
                    case ')':
                        removeTrailingWhiteSpace(sb);
                        sb.append("\n");
                        depth--;
                        addIndentation(sb, depth);
                        sb.append(c);
                        if (depth == 0) {
                            sb.append("\n\n");
                        }
                        break;
                    case '=':
                        boolean skipSpacingForEquals = shouldSkipSpacingOnEqualSignInsert(sb);
                        if (!skipSpacingForEquals && (i == 0 || original.charAt(i - 1) != ' ')) {
                            sb.append(" ");
                        }
                        sb.append(c);
                        if (!skipSpacingForEquals && (i + 1 >= original.length() || original.charAt(i + 1) != ' ')) {
                            sb.append(" ");
                        }
                        break;
                    case ',': {
                        sb.append(c);
                        int j = 1;
                        while (i + j < original.length() && original.charAt(i + j) == ' ') {
                            j++;
                        }
                        if (depth == 0) {
                            //do nothing
                        } else if (i + j < original.length() && original.charAt(i + j) != '(') {
                            sb.append("\n");
                            addIndentation(sb, depth);
                        }
                        break;
                    }
                    case ' ': {
                        int j = 1;
                        while (sb.charAt(sb.length() - j) == ' ') {
                            j++;
                        }
                        if (sb.charAt(sb.length() - j) != '\n') {
                            sb.append(c);
                        }
                        break;
                    }
                    case '"': {
                        sb.append(c);
                        while (i < original.length() - 1 && (c = original.charAt(++i)) != '"') {
                            sb.append(c);
                        }
                        if (i < original.length() - 1 || (i == original.length() - 1 && original.charAt(i) == '"')) {
                            sb.append(c);
                        }
                        break;
                    }
                    default:
                        sb.append(c);
                        break;
                }

            }
            putSetCommandsOnNewlines(sb);
            return sb.toString();
        }

        private static void removeTrailingWhiteSpace(StringBuilder sb) {
            while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                sb.setLength(sb.length() - 1);
            }
        }

        private static void addIndentation(StringBuilder sb, int depth) {
            for (int j = 0; j < depth; j++) {
                sb.append(INDENTATION);
            }
        }

        private static boolean shouldSkipSpacingOnEqualSignInsert(StringBuilder sb) {//concise naming ftw
            int idx = sb.length() - 1;
            while (idx > 0 && sb.charAt(idx) != '\n') {
                idx--;
            }
            if (StringUtilities.substringStartsWith(sb, idx, "set")) {
                if (sb.charAt(idx + 3) == ' ') {
                    return true;
                } else if (StringUtilities.substringStartsWith(sb, idx + 3, "_cmp") && sb.charAt(idx + 7) == ' ') {
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("empty-statement")
        private static void putSetCommandsOnNewlines(StringBuilder sb) {
            int idx = 0;
            while (idx < sb.length()) {
                if (sb.charAt(idx) == '"') {
                    while (++idx < sb.length() && sb.charAt(idx) != '"');
                    //idx is now at the end of the string, or at the index of the closing quote
                } else if (StringUtilities.substringStartsWith(sb, idx, "set")) {
                    boolean prevIsWhitespace = idx == 0 || Character.isWhitespace(sb.charAt(idx - 1));
                    boolean cmp = StringUtilities.substringStartsWith(sb, idx + 3, "_cmp");
                    int nextIdx = idx + (cmp ? 7 : 3);
                    boolean nextIsWhiteSpace = nextIdx < sb.length() && Character.isWhitespace(sb.charAt(nextIdx));
                    if (prevIsWhitespace && nextIsWhiteSpace && idx > 0) {
                        sb.replace(idx - 1, idx, "\n\n");
                        idx = nextIdx + 1;
                    }
                }
                idx++;
            }
        }

        public static String deFormatCode(String original) {
            return removeNonQuotedSpaces(original.replaceAll("\n", " "));
        }

        public static String removeNonQuotedSpaces(String s) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '"') {
                    sb.append(s.charAt(i++));
                    while (i < s.length() && s.charAt(i) != '"') {
                        sb.append(s.charAt(i++));
                    }
                    if (i < s.length()) {
                        sb.append('"');
                    }
                } else if (c != ' ') {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        public static String deFormatCodeInnerNBrackets(String original, final int n) {
            List<Integer> stack = new ArrayList<>();
            Map<Integer, Integer> depthMap = new TreeMap<>();
            Map<Integer, Integer> closingMap = new TreeMap<>();
            int depth = 0;
            int maxdepth = 0;
            original = formatCode(original);
            for (int i = 0; i < original.length(); i++) {
                char c = original.charAt(i);
                if (c == '(') {
                    depth++;
                    maxdepth = java.lang.Math.max(maxdepth, depth);
                    for (int j = 0; j < stack.size(); j++) {
                        Integer start = stack.get(j);
                        int curDeepest = depthMap.get(start);
                        depthMap.put(start, java.lang.Math.max(curDeepest, depth - j));
                    }
                    stack.add(i);
                    depthMap.put(i, 1);
                } else if (c == ')' && depth != 0) {//
                    depth--;
                    int start = stack.remove(stack.size() - 1);
                    closingMap.put(start, i);
                }
            }
            TreeMap<Integer, String> replacements = new TreeMap<>();
            for (Integer start : depthMap.keySet()) {
                int d = depthMap.get(start);
                if (d <= n || (d == maxdepth && maxdepth < n)) {
                    int endIdx = closingMap.getOrDefault(start, original.length());
                    String val = deFormatCode(original.substring(start, endIdx));
                    replacements.put(start, val);
                }
            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < original.length()) {
                if (replacements.containsKey(i)) {
                    int k = sb.length() - 1;
                    while (k >= 0 && Character.isWhitespace(sb.charAt(k))) {
                        k--;
                    }
                    if (k == '=') {
                        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                    }
                    sb.append(replacements.get(i).trim());
                    i = closingMap.getOrDefault(i, original.length());
                } else {
                    sb.append(original.charAt(i));
                    i++;
                }
            }
            return sb.toString();
        }

        public static String deFormatCodeForUserDialog(String code) {
            return deFormatCodeForUserDialog(code, 1);
        }

        public static String deFormatCodeForUserDialog(String code, int n) {
            return deFormatCodeForUserDialog(code, n, 15, 120);
        }

        public static String deFormatCodeForUserDialog(String code, int n,
                int maxLines, int maxLineLength) {

            String formattedCommand = Utilities.CodeFormatter.deFormatCodeInnerNBrackets(code, n);
            StringBuilder sb = new StringBuilder();
            sb.append("<pre>");
            int linecount = 0;
            for (String line : formattedCommand.split("\n")) {
                linecount++;
                if (linecount > maxLines) {
                    sb.append("...");
                    break;
                }
                if (line.length() > maxLineLength) {
                    sb.append(line.substring(0, maxLineLength - 3));
                    sb.append("...");
                } else {
                    sb.append(line);
                }
                sb.append("<br/>");
            }
            sb.append("</pre>");
            return sb.toString();
        }
    }

    /**
     * Returns the main Jar/EXE file path for OpenBLCMM, or null if we can't.
     *
     * @return The location of the main Jar/EXE
     */
    public static File getMainJarFile() {
        try {
            return new File(Utilities.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns the main install dir where OpenBLCMM.jar/.exe currently lives,
     * or null if we can't.  Could potentially have been overriden with CLI
     * args, mostly for use in the Mac app bundle.
     *
     * @return The directory containing our "installation"
     */
    public static File getMainInstallDir() {
        if (Utilities.installDirOverride != null) {
            // This will already be an absolute location
            return new File(Utilities.installDirOverride);
        }
        File jarFile = Utilities.getMainJarFile();
        if (jarFile == null) {
            return null;
        } else {
            if (jarFile.isDirectory()) {
                // This is most likely to be the case when being run from
                // inside NetBeans itself, 'cause this'll just point to the
                // top-level classpath where the compiled classes can be
                // found.
                return jarFile.getAbsoluteFile();
            } else {
                return jarFile.getAbsoluteFile().getParentFile();
            }
        }
    }

    /**
     * Gets the default location for file-open type interactions, assuming we
     * don't have any better place to direct the user to.  For Creator Mode,
     * this should probably just be the current directory (which'll place it
     * inside the working project); for other modes, maybe the location of
     * the Jar/EXE is appropriate?
     *
     * @return Where file-open type behaviors should default to, in the absence
     * of better ideas.
     */
    public static File getDefaultOpenLocation() {
        if (Utilities.isCreatorMode()) {
            return new File(Utilities.userDir).getAbsoluteFile();
        } else {
            File mainJarDir = Utilities.getMainInstallDir();
            if (mainJarDir == null) {
                return new File(Utilities.userDir).getAbsoluteFile();
            } else {
                return mainJarDir;
            }
        }
    }

    /**
     * Gets the location where our data pack files should be contained.  This
     * is effectively identical to getDefaultOpenLocation, but I didn't want
     * to literally combine them into one function.
     *
     * @return The directory where datapacks can be found, or null
     */
    public static File getDataPackDirectory() {
        if (Utilities.isCreatorMode()) {
            return new File(Utilities.userDir);
        } else {
            return Utilities.getMainInstallDir();
        }
    }

    /**
     * Given a size in bytes, return a human-readable string suffixed by
     * the most appropriate units.  This implementation taken from blcmm.Startup.
     *
     * @param bytes The size in bytes.
     * @return A string describing the size.
     */
    public static String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1000));
        String prefix = "kMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1000, exp), prefix);
    }

}
