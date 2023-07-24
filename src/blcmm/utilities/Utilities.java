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
import blcmm.gui.FontInfo;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.PatchType;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
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
        try {
            Utilities.userDir = new File(newDir).getCanonicalPath();
        } catch (IOException e) {
            Utilities.userDir = new File(newDir).getAbsolutePath();
        }
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
        try {
            Utilities.installDirOverride = new File(newDir).getCanonicalPath();
        } catch (IOException e) {
            Utilities.installDirOverride = new File(newDir).getAbsolutePath();
        }
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
                //GlobalLogger.log("user.home: '" + userHome + "'");
                //GlobalLogger.log("user.name: '" + userName + "'");
                if (!userHome.endsWith(userName) && userName.contains("@")) {
                    // Sometimes a user might have "@outlook" as part of their username
                    // which isn't included in the homedir.  There's probably a better
                    // way to be checking for this.
                    String[] parts = userName.split("@", 2);
                    userName = parts[0];
                }
                // This is sort of a double check of the same values in most cases,
                // but whatever.
                if (userHome.endsWith(userName)) {
                    String replaced = userHome.substring(0, userHome.length() - userName.length()) + "[user]";
                    return text.replace(userHome, replaced);
                } else {
                    // Username wasn't found in the homedir, so just give up
                    // (this logging would be a bit noisy; not bothering with it)
                    //GlobalLogger.log("Could not figure out how to hide username in homedir!");
                    return text;
                }
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
        URL url;
        try {
            url = new URI(fileURL).toURL();
        } catch (URISyntaxException e) {
            System.out.println("Could not parse URL: " + e.getMessage());
            return null;
        }
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

    /**
     * Updates the input masks for the given component to translate "Ctrl"
     * modifier to the default Toolkit's "menu shortcut key."  For mos OSes,
     * this is effectively a NOOP -- converting Ctrl to Ctrl -- but on Mac,
     * this will end up converting any Ctrl shortcut to a "Command" shortcut
     * instead.
     *
     * Long-term I would actually like to get rid of this (and changeMasks)
     * altogether.  When input shortcuts are set up, they should just be
     * grabbing the default toolkit and using its menu shortcut.  However,
     * we still want to support Java 8, and Toolkit.getMenuShortcutKeyMask()
     * is deprecated as of Java 10, in favor of a new
     * Toolkit.getMenuShortcutKeyMaskEx(), which does not exist in Java 8.
     * If we ever do decide to drop Java 8 support (or if we *need* do, in case
     * a future Java version ever gets rid of the deprecated methods) then
     * we should get rid of these two methods altogether and use the "Ex"
     * method in Toolkit when setting shortcuts.  Until then, consolidating
     * them all in here lets us have as few methods as possible with suppressed
     * deprecation warnings.
     *
     * See: https://github.com/BLCM/OpenBLCMM/issues/21
     *
     * @param component The component whose input masks needs updating
     */
    @SuppressWarnings("deprecation")
    public static void changeCTRLMasks(JComponent component) {
        changeMasks(component, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.CTRL_MASK,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    /**
     * This is the workhorse method which actually changes the input masks
     * on a JComponent.  Pass it the old and the new, and it'll update the
     * masks if possible.
     *
     * As mentioned above, I'd like to remove this entirely, though doing so
     * will probably have to wait until we decide to stop supporting Java 8.
     *
     * @param component The component whose input masks need updating
     * @param oldmask The old mask that we'll get rid of
     * @param replacingMask The new mask that we'll replace it with
     */
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

    /**
     * Another inner mask-changing function which I'd like to get rid of.
     * Different somehow to the previous one, I'm sure.
     *
     * @param component
     * @param oldmask
     * @param replacingMask
     */
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

    /**
     * Launches a browser on the user's desktop with the given URL.  The
     * `parent` Component given is used if an error needs to be displayed.
     * This will be the component for which the error should be modal.
     *
     * @param urlString The URL to open
     * @param parent The parent Component which generated the launch request
     * @return True if the launch was (apparenty) successful, false otherwise.
     */
    public static boolean launchBrowser(String urlString, Component parent) {
        try {
            URI uri = new URI(urlString);
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (IOException | URISyntaxException | UnsupportedOperationException ex) {
            GlobalLogger.log(ex);

            //"Unable to launch browser: " + ex.getMessage(),
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints cs = new GridBagConstraints();
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridheight = 1;
            cs.gridwidth = 2;
            cs.weightx = 1;
            cs.weighty = 1;
            cs.ipady = 4;
            cs.fill = GridBagConstraints.HORIZONTAL;

            cs.anchor = GridBagConstraints.CENTER;
            panel.add(new JLabel("<html><b><font size=+1>Unable to launch browser</font></b>"), cs);

            cs.gridy++;
            cs.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel("<html><font color=\""
                    + ThemeManager.getColorHexStringRGB(ThemeManager.ColorType.UINimbusRed)
                    + "\">" + ex.getMessage() + "</font>"), cs);

            cs.gridy++;
            cs.insets = new Insets(10, 0, 0, 0);
            panel.add(new JLabel("<html>You can copy the URL here to open in your browser manually:"), cs);

            cs.gridy++;
            cs.gridwidth = 1;
            cs.weightx = 100;
            cs.insets = new Insets(0, 10, 0, 0);
            JTextField field = new JTextField(urlString);
            field.setEditable(false);
            field.setCaretPosition(0);
            field.moveCaretPosition(urlString.length());
            field.getCaret().setSelectionVisible(true);
            panel.add(field, cs);

            cs.gridx = 1;
            cs.weightx = 1;
            cs.insets = new Insets(0, 0, 0, 10);
            JButton button = new JButton("Copy to Clipboard");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                     StringSelection stringSelection = new StringSelection(urlString);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                }
            });
            panel.add(button, cs);

            JOptionPane.showMessageDialog(parent, panel,
                    "Error launching Browser",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    /**
     * Returns the maximum Dimensions that we'd want a newly-created dialog to
     * consume on the screen where the Component `c` lives.  This will be 90% of
     * the total "usable" area of the current screen (ie: minus OS taskbars and
     * the like).
     *
     * Note that in general it doesn't really make sense to try and cache any
     * of this.  In multi-screen setups, the user could move their windows
     * around at any point, so the proper values here could change at just
     * about any point.
     *
     * @param c The Component which will do the comparison (can be the launching
     *     component or the newly-created one -- doesn't really matter).
     * @return Maximum dimensions for a new dialog
     */
    public static Dimension maxDialogSize(Component c) {
        return Utilities.maxDialogSize(c, 0.9f);
    }

    /**
     * Returns the maximum Dimensions that we'd want a newly-created dialog to
     * consume on the screen where the Component `c` lives, based on a percentage
     * of the screen that we'll be permitted to consume.  The point of the
     * percentage it we typically don't want status dialogs, etc, to take up
     * literally all screen real estate, even if its contents might be able to
     * make use of it.  The "usable area" of the screen is everything minus
     * stuff like OS taskbars and the like.
     *
     * Note that in general it doesn't really make sense to try and cache any
     * of this.  In multi-screen setups, the user could move their windows
     * around at any point, so the proper values here could change at just
     * about any point.
     *
     * @param c The Component which will do the comparison (can be the launching
     *     Component or the newly-created one -- doesn't really matter).
     * @param percent The percentage off the "usable" screen area that we'll
     *     be allowed to consume.
     * @return Maximum dimensions for a new dialog
     */
    public static Dimension maxDialogSize(Component c, float percent) {
        GraphicsConfiguration gc = c.getGraphicsConfiguration();
        Insets insets = c.getToolkit().getScreenInsets(gc);
        Rectangle bounds = gc.getBounds();
        return new Dimension(
                (int)((bounds.width-insets.left-insets.right)*percent),
                (int)((bounds.height-insets.top-insets.bottom)*percent)
        );
    }

    /**
     * Given a proposed new dialog size, which will be rendered into the same
     * GraphicsConfiguration in which the Component `c` belongs, return the
     * actual size of the dialog that we'd want to use, constrained by the
     * available area inside that GraphicsConfiguration.
     *
     * If the clamping results in a shortened width, this will make the dialog
     * taller to compensate, if possible.  That compensation may need some
     * tweaking in the future -- it's probably compenating too much at the
     * moment.
     *
     * @param proposedSize The proposed size of the new dialog.
     * @param c The Component from which the new dialog will be created, or the
     *     dialog itself.
     * @return The dimensions of the dialog that we'll *actually* use.
     */
    public static Dimension clampProposedDialogSize(Dimension proposedSize, Component c) {
        Dimension maxSize = Utilities.maxDialogSize(c);
        int newWidth = Math.min(maxSize.width, proposedSize.width);
        int newHeight = Math.min(maxSize.height, proposedSize.height);
        float widthDiff = (float)newWidth/(float)proposedSize.width;
        if (widthDiff < 1) {
            newHeight = Math.min(maxSize.height, (int)(newHeight/widthDiff));
        }
        return new Dimension(newWidth, newHeight);
    }

    /**
     * Given a proposed new dialog size, using the user-selected font state
     * `fontInfo`, which will be rendered into the same GraphicsConfiguration
     * in which the Component `c` belongs, return the actual size of the dialog
     * we'd want to use, scaled appropriately for the font, and constrained by
     * the available area inside that GraphicsConfiguration.
     *
     * If the clamping results in a shortened width, this will make the dialog
     * taller to compensate, if possible.  That compensation may need some
     * tweaking in the future -- it's probably compenating too much at the
     * moment.
     *
     * @param proposedSize The proposed size of the new dialog.
     * @param fontInfo The FontInfo object describing the current font selection
     * @param c The Component from which the new dialog will be created, or the
     *     dialog itself.
     * @return The dimensions of the dialog that we'll *actually* use.
     */
    public static Dimension scaleAndClampDialogSize(Dimension proposedSize, FontInfo fontInfo, Component c) {
        Dimension scaledSize = new Dimension(
            (int)(proposedSize.width*fontInfo.getScaleWidth()),
            (int)(proposedSize.height*fontInfo.getScaleHeight())
        );
        Dimension dialogSize = Utilities.clampProposedDialogSize(scaledSize, c);
        /*
        if (dialogSize.equals(scaledSize)) {
            GlobalLogger.log("Dimensions are equal: " + scaledSize.toString());
        } else {
            GlobalLogger.log("Dimensions got clamped:");
            GlobalLogger.log("  Proposed: " + scaledSize.toString());
            GlobalLogger.log("  Clamped: " + dialogSize.toString());
        }
        /**/
        return dialogSize;
    }

    /**
     * Given a Component `c`, find the top-level Window which contains the
     * component.  If no Window is found, will fall back to JOptionPane's
     * "root frame," whatever that is.
     *
     * @param c The component whose Window you want to find
     * @return The Window containing the component
     */
    public static Window findWindow(Component c) {
        if (c == null) {
            return JOptionPane.getRootFrame();
        } else if (c instanceof Window) {
            return (Window) c;
        } else {
            return findWindow(c.getParent());
        }
    }

}
