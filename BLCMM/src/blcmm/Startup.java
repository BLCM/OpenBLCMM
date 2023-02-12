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
package blcmm;

import blcmm.gui.MainGUI;
import blcmm.gui.panels.FirstTimeActionsPanel;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.AutoBackupper;
import blcmm.utilities.BLCMMUtilities;
import blcmm.utilities.IconManager;
import blcmm.utilities.Options;
import blcmm.utilities.Utilities;
import general.utilities.GlobalLogger;
import general.utilities.OSInfo;
import general.utilities.OSInfo.OS;
import general.utilities.StringTable;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;

/**
 *
 * @author LightChaosman
 */
public class Startup {

    private static final File CRASH_FLAG_FILE = new File("crash.boom");
    private static final boolean CRASHED_LAST_TIME;

    static {
        CRASHED_LAST_TIME = CRASH_FLAG_FILE.exists();
        CRASH_FLAG_FILE.delete();
    }

    private static File LAUNCHER;
    private static String titlePostfix;
    private static File file;
    private static boolean usedLauncher;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> vmArguments = runtimeMxBean.getInputArguments();
        System.setProperty("sun.awt.exception.handler", MyExceptionHandler.class.getName());
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());
        if (!FileVerifier.verifyFiles()) {
            return;
        }
        if (!confirmIO()) {
            GlobalLogger.log("Closing BLCMM because we can't confirm IO");
            GlobalLogger.markAsPermanentLog();
            return;
        }

        usedLauncher = false;
        file = null;
        for (String arg : args) {
            if (arg.startsWith("-f=")) {
                file = new File(arg.substring("-f=".length()));
            } else if (arg.startsWith("-launcher=")) {
                LAUNCHER = new File(arg.substring("-launcher=".length()));
                usedLauncher = true;
            }
        }
        String message = null;
        if (LAUNCHER == null || !LAUNCHER.exists() || !LAUNCHER.isFile()) {
            message = "Changing launcher back to the one in the parent folder";
            if (LAUNCHER != null) {
                message += " - because: " + LAUNCHER.exists() + " " + LAUNCHER.isFile();
            }
            LAUNCHER = new File(new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath() + "/BLCMM_Launcher.jar");//The location of the main launcher
        }
        if (message != null) {//dev enviroment
            GlobalLogger.log(message);
        } else {
            GlobalLogger.setLogFolder(LAUNCHER.getParent() + "/blcmm_logs");
        }
        GlobalLogger.log("Running BLCMM version " + MainGUI.VERSION);
        GlobalLogger.log("Running Java version " + System.getProperty("java.version"));
        GlobalLogger.log("Arguments provided; VM arguments: " + Arrays.toString(vmArguments.toArray()) + " - Runtime arguments: " + Arrays.toString(args));
        GlobalLogger.log("Username: " + System.getProperty("user.name"));

        // Total amount of free memory available to the JVM
        GlobalLogger.log("Free Memory: " + humanReadableByteCount(Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
        // Maximum amount of memory the JVM will attempt to use (Long.MAX_VALUE if there is no limit)
        GlobalLogger.log("Maximum Memory: " + (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "No Limit"
                : humanReadableByteCount(Runtime.getRuntime().maxMemory())));

        BLCMMUtilities.setUsedLauncher(usedLauncher);
        BLCMMUtilities.setLauncher(LAUNCHER);

        titlePostfix = "";
        for (String vmarg : vmArguments) {
            if (vmarg.startsWith("-Xmx")) {
                int a = Integer.parseInt(vmarg.substring("-Xmx".length(), vmarg.length() - 1));
                if (vmarg.endsWith("g")) {
                    titlePostfix = " | RAM: " + (a * 1024);
                } else if (vmarg.endsWith("m")) {
                    titlePostfix = " | RAM: " + a;
                }
            }
        }
        if (!usedLauncher && !Utilities.isCreatorMode()) {
            File f2 = new File("one_time_blcmm_startup.blcm");
            if (!f2.exists()) {
                MainGUI.setTheme(ThemeManager.getDefaultTheme());
                JOptionPane.showMessageDialog(null, "Please use the launcher to run BLCMM.", "No launcher used", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
            f2.delete();
        }
        firstTime();//load options and set theme
        generateHintsFile();
        updateLauncher();

        ToolTipManager.sharedInstance().setInitialDelay(500);//make tooltips tolerable
        ToolTipManager.sharedInstance().setDismissDelay(10000);

        // TESTING!  This is a new function which maybe we'll start using to store data,
        // rather than always being alongside the launcher.  Don't want to actually use
        // this anywhere until it's seen some action on various platforms, and under
        // various conditions.
        GlobalLogger.log("Your BLCMM installation can be found here: " + BLCMMUtilities.getBLCMMDataDir());
        GlobalLogger.log("Working directory: " + System.getProperty("user.dir").replaceAll("\\\\", "/"));

        java.awt.EventQueue.invokeLater(() -> {
            new MainGUI(usedLauncher, file, titlePostfix).setVisible(true);
        });
    }

    private static void ShortcutCreator(OS OperatingSystem) throws IOException {
        if (OperatingSystem == OS.UNIX) {
            String launcherPath = new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath().replaceAll("\\\\", "/") + "/BLCMM_Launcher.jar";
            String iconPath = new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath().replaceAll("\\\\", "/") + "/BLCMM/launcher/icon.png";
            File desktop = new File("/home/" + System.getProperty("user.name") + "/Desktop/BLCMM_Launcher.desktop");
            if (desktop.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(desktop.getAbsolutePath())), StandardCharsets.UTF_8);
                if (content.contains(launcherPath)) {
                    return;
                }
            } else {
                desktop.getParentFile().mkdirs();
                desktop.createNewFile();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("#!/usr/bin/env xdg-open\n"
                    + "[Desktop Entry]\n"
                    + "Version=1.0\n"
                    + "Type=Application\n"
                    + "Terminal=false\n"
                    + "Icon=" + iconPath + "\n"
                    + "Name=BLCMM\n"
                    + "StartupNotify=true\n"
                    + "Comment=Edit Borderlands 2 and Pre-Sequel mods");
            sb.append("Exec=/usr/bin/java -jar \"" + launcherPath + "\"\n");
            String DesktopString = sb.toString();
            PrintWriter writer = new PrintWriter(new FileWriter(desktop, true));
            writer.println(DesktopString);
            writer.close();
        } else {
            return;
        }
    }

    private static void firstTime() {
        if (isFirstTimeRunning()) {
            MainGUI.setTheme(ThemeManager.getDefaultTheme());
            showFirstTimeMessage();
            showFirstTimeActions(false);
            /*BLCMMUtilities.populateFileHistory(true);
            showFirstTimeOSSpecificMessages();
            String bl2Path = GameDetection.getBinariesDir(true);
            String tpsPath = GameDetection.getBinariesDir(false);
            if ((bl2Path != null && GameDetection.getBL2Exe() != null)
                    && (tpsPath != null && GameDetection.getTPSExe() != null)
                    && (new File(bl2Path + "\\Patch.txt").exists())
                    && (new File(tpsPath + "\\Patch.txt").exists() || new File(tpsPath + "\\Patch").exists())) {
                int gameChoice = JOptionPane.showOptionDialog(null, "We noticed you have two patch files.\nWhat file do you want to load?",
                        "Multiple Patches Detected", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        new Object[]{"Borderlands 2", "Borderlands: The Pre-Sequel"}, "Borderlands 2");

                // BL2 was chosen
                if (gameChoice == 0) {
                    file = new File(bl2Path + "\\Patch.txt");
                } // TPS was chosen.
                else {
                    file = new File(tpsPath + "\\Patch");
                    if (!file.exists()) {
                        file = new File(tpsPath + "\\Patch.txt");
                    }
                }
            }*/
            Options.INSTANCE.setShowHotfixNames(false);
        } else {
            BLCMMUtilities.populateFileHistory(true);
            MainGUI.setTheme(Options.INSTANCE.getTheme());
        }
    }

    /**
     *
     * @param bytes
     * @return A more human friendly version of bytes into the SI unit
     * measurement like: "1024 bytes to 1.0kB"
     */
    private static String humanReadableByteCount(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1000));
        String prefix = "kMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1000, exp), prefix);
    }

    /**
     *
     * @return true if this is the first time running the tool
     */
    private static boolean isFirstTimeRunning() {
        try {
            boolean firstTime = Options.loadOptions();
            if (Options.INSTANCE.hasLoadErrors()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html>The following errors were encountered while loading options:");
                sb.append("<ul>");
                for (String s : Options.INSTANCE.getLoadErrors()) {
                    sb.append(String.format("<li>%s</li>", s));
                }
                sb.append("</ul>");
                sb.append("Any unread options have been reset to their default values.");
                JOptionPane.showMessageDialog(MainGUI.INSTANCE, sb.toString(),
                        "Option Load Errors", JOptionPane.ERROR_MESSAGE);
            }
            return firstTime;
        } catch (IOException ex) {
            Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(null,
                    "Error loading options - you may have trouble saving options: " + ex.getMessage(),
                    "Option Load Errors", JOptionPane.WARNING_MESSAGE);
        }
        return true;
    }

    private static void showFirstTimeMessage() {
        int size = 25;
        String p = ClassLoader.getSystemClassLoader().getResource("resources/Qmark.png").toString();
        String p2 = "<img src=\"" + p + "\" height=\"" + size + "\" width=\"" + size + "\">";
        String s = "<html><center>Welcome to LightChaosman's BLCMM or Borderlands Community Mod Manager!</center><br/>"
                + "BLCMM can be used for: <br/>"
                + "<ul style=\"margin-left: 15px;\">"
                + "<li>Toggling mods</li>"
                + "<li>Exploring objects</li>"
                + "<li>Making mods</li>"
                + "<li>Merging mods for hotfixes</li></ul>"
                + "<table><tr><td>Note: You will need to open a TPS mod file to be in the TPS mode and vice versa.</td><tr></table>"
                + "<table><tr><td>Hover over the</td><td>" + p2 + "</td><td>icons troughout the tool for tips & tricks.</td></tr></table>";
        JOptionPane.showMessageDialog(null, s, "Welcome", JOptionPane.PLAIN_MESSAGE, new ImageIcon(IconManager.getBLCMMIcon(64)));
    }

    private static void showFirstTimeOSSpecificMessages() {
        ArrayList<String> messages = new ArrayList<>();
        if (OSInfo.CURRENT_OS == OSInfo.OS.MAC || OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            messages.add("On Mac and Linux systems, patches must be run from the 'Press any Key'<br/>"
                    + "screen, not the main menu.  To correctly execute patches from the console,<br/>"
                    + "click through to the main menu, hit Escape to go back out to the title<br/>"
                    + "screen, and execute the patch from there.");
        }
        if (OSInfo.CURRENT_OS == OSInfo.OS.UNIX) {
            messages.add("On Linux, Borderlands can only execute patches with all-lowercase filenames.<br/>"
                    + "Either save your patch directly with an all-lowercase filename, or use a<br/>"
                    + "symlink to provide a lowercase version.");
        }
        if (messages.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append("There are some considerations when running Borderlands mods on non-Windows platforms.<br/>");
            sb.append("Here are some things to keep in mind:<br/>");
            sb.append("<ul>");
            for (String message : messages) {
                sb.append(String.format("<li>%s<br/><br/></li>", message));
            }
            sb.append("</ul>");
            JOptionPane.showMessageDialog(null, sb.toString(),
                    "Things To Note", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void showFirstTimeActions(boolean advanced) {
        FirstTimeActionsPanel panel = new FirstTimeActionsPanel(advanced);
        do {
            JOptionPane.showMessageDialog(null, panel, "Select options", JOptionPane.PLAIN_MESSAGE);
        } while (panel.showResultString(true));
    }

    private static void generateHintsFile() {
        try {
            File f = new File("hints.txt");
            String hints
                    = "Use the middle mouse button in the Object Explorer to open a new tab\n"
                    + "Press CTRL+Space while editing for auto-complete\n"
                    + "Is something unavailable? Hover your mouse over the button for info\n"
                    + "Hotfixes work mostly the same as regular set commands with BLCMM\n"
                    + "Extra features for BLCMM can be downloaded in the settings menu\n"
                    + "Use 'Format code' or 'Deformat code' to make dumped objects or code more readable\n"
                    + "Most tools to reorganize mods are found in the right-mouse menu\n"
                    + "Did BLCMM crash? a backup of your current file is made every 2 minutes\n"
                    + "Hide the left side of the object explorer by double-clicking the divider";

            Utilities.writeStringToFile(hints, f);
        } catch (IOException ex) {
            GlobalLogger.log(ex);
        }
    }

    private static void updateLauncher() {
        File newLauncher = new File("launcher/" + LAUNCHER.getName());//if it exists, it's here
        boolean needsupdate = false;
        if (newLauncher.exists() && LAUNCHER.exists()) {
            try {
                String sha1 = Utilities.sha256(LAUNCHER);
                String sha2 = Utilities.sha256(newLauncher);
                if (!sha1.equals(sha2)) {
                    needsupdate = true;
                    GlobalLogger.log("Replacing the launcher (" + LAUNCHER.getAbsolutePath() + ")");
                }
            } catch (IOException | NoSuchAlgorithmException ex) {
                GlobalLogger.log("Error while getting hashes for launcher: " + ex.getMessage());
            }
        } else {
            if (!LAUNCHER.exists()) {
                GlobalLogger.log("Could not find the old launcher at: " + Utilities.hideUserName(LAUNCHER.getAbsolutePath()));
            }
            if (!newLauncher.exists()) {
                GlobalLogger.log("Could not find the new launcher at: " + Utilities.hideUserName(newLauncher.getAbsolutePath()));
            }
        }
        if (needsupdate) {
            SwingWorker sw = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    boolean updated = false;
                    IOException ex = null;
                    for (int i = 0; i < 60; i++) {
                        try {
                            Files.copy(newLauncher.toPath(), LAUNCHER.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            GlobalLogger.log("Succesfully updated launcher");
                            updated = true;
                            break;
                        } catch (IOException e) {
                            ex = e;
                            Thread.sleep(1000);
                        }
                    }
                    if (!updated && ex != null) {
                        JOptionPane.showMessageDialog(null,
                                "Error updating launcher: " + ex.getMessage(),
                                "Error updating launcher", JOptionPane.ERROR_MESSAGE);
                        GlobalLogger.log("Error while trying to update launcher:" + ex.getMessage());
                    }
                    return null;
                }
            };
            sw.execute();
        }

    }

    private static boolean confirmIO() {
        File f = new File("io.temp");
        boolean b = true;
        try {
            f.createNewFile();
            if (!f.canWrite()) {
                throw new IOException();
            }
            Utilities.writeStringToFile("test", f);
        } catch (IOException e) {
            b = false;
            JOptionPane.showMessageDialog(null, ""
                    + "BLCMM does not seem to be able to write to files.\n"
                    + "Check your file permissions / run BLCMM as an administrator / run it at a different location.\n"
                    + "BLCMM will now shut down, since it requires file I/O.",
                    "No I/O permissions", JOptionPane.ERROR_MESSAGE);

        } finally {
            f.delete();
            f.deleteOnExit();
        }
        return b;
    }

    public static FileTime getUtilitiesCompiletime() {
        try {
            return getCompileTime(new JarFile(new File("lib/BLCMM_Utilities.jar")));
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            return null;
        }
    }

    public static FileTime getDataLibraryCompiletime() {
        try {
            return getCompileTime(new JarFile(new File("lib/BLCMM_Data_Interaction_Library.jar")));
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            return null;
        }
    }

    private static FileTime getCompileTime(JarFile jarFile) {
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry el = e.nextElement();
            if (!el.isDirectory()) {
                return el.getLastModifiedTime();
            }
        }
        return null;
    }

    public static boolean isFirstBootAfterCrash() {
        return CRASHED_LAST_TIME;
    }

    public static boolean promptRestart(boolean launcher) throws HeadlessException {
        int restart = JOptionPane.showConfirmDialog(null, "BLCMM needs to restart for the selected options to work properly. Restart now?", "Restart?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (restart != JOptionPane.YES_OPTION) {
            return false;
        }
        boolean dispose = MainGUI.INSTANCE.isReadyToSuperDispose();
        if (!dispose) {
            return false;
        }
        if (launcher) {
            startLauncher();
        } else {
            startBLCMM();
        }
        MainGUI.INSTANCE.superDispose();
        return true;
    }

    private static void startBLCMM() {
        String path = new File(System.getProperty("user.dir")).getAbsolutePath();
        String path2 = path.replaceAll("\\\\", "/");
        File f = new File(path2 + "/BLCMM.jar");
        String path3 = f.getAbsolutePath().replaceAll("\\\\", "/");
        String[] command4 = new String[]{"java", "-jar", "-XX:MaxHeapFreeRatio=50", path3, "-launcher=" + LAUNCHER.getAbsolutePath()};
        try {
            Runtime.getRuntime().exec(command4);
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(null,
                    "Unable to launch BLCMM: " + ex.getMessage(),
                    "Error launching BLCMM", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void startLauncher() {
        String path3 = LAUNCHER.getAbsolutePath().replaceAll("\\\\", "/");
        String[] command4 = new String[]{"java", "-jar", path3, "-fromblcmm"};//This also works with the .exe version of the launcher
        try {
            Runtime.getRuntime().exec(command4, null, LAUNCHER.getParentFile());
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(null,
                    "Unable to start launcher: " + ex.getMessage(),
                    "Error starting launcher", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Startup() {
    }

    private static class MyExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable thrwbl) {
            logError(thrwbl);
            showErrorMessage(thrwbl);
        }

        private void logError(Throwable thrwbl) {
            File options = new File("versions.options");
            StringTable localVersions = null;
            if (options.exists()) {
                try {
                    String optionsString = Utilities.readFileToString(options);
                    localVersions = StringTable.generateTable(optionsString);
                } catch (FileNotFoundException notfounde) {
                    //never happens
                } catch (IOException ex) {
                    //??
                }
            }
            if (localVersions == null) {//No options file or IO exception
                localVersions = new StringTable(); // default settings are stored serverside
            }
            GlobalLogger.log(thrwbl);
            GlobalLogger.log("\n\n OPTIONS:\n\n" + Options.INSTANCE.toString() + "\n\nInstalled stuff:\n\n" + localVersions.convertTableToSpacedString());
        }

        private void showErrorMessage(Throwable thrwbl) {
            if (MainGUI.INSTANCE.getTree().isChanged()) {//No need to offer opening a backup on next boot if we didn't make any changestry {
                try {
                    CRASH_FLAG_FILE.createNewFile();
                } catch (IOException ex) {//If we fail, we fail
                }
            }
            JDialog dialog = new JDialog(MainGUI.INSTANCE, Dialog.ModalityType.APPLICATION_MODAL);
            boolean memError = thrwbl instanceof java.lang.OutOfMemoryError;
            dialog.setTitle(memError ? "Out of Memory Error!" : "Unknown error");

            String text;
            if (memError) {
                text = "<html><b>BLCMM ran out of memory!</b><br/>"
                        + "<br/>"
                        + "This can happen when trying to load large mods like Randomizers.<br/>"
                        + "<br/>"
                        + "To prevent this error from happening in the future, assign BLCMM more<br/>"
                        + "RAM on the launcher screen.  At the launcher, click the checkbox<br/>"
                        + "in the top left, and adjust the memory usage there!";
            } else {
                text = "<html>An unknown error has ocurred, please contact the developer.<br/>"
                        + "A log file with the error has been generated.<br/>"
                        + "Backup files of your changes are made while editing, you shouldn't have lost too much progress.<br/><br/>"
                        + "Log file: " + GlobalLogger.getLOG().getAbsolutePath() + "<br/>"
                        + "Backup directory: " + AutoBackupper.getDestination();
            }
            text = Utilities.hideUserName(text);
            Icon icon = javax.swing.UIManager.getIcon("OptionPane.errorIcon");
            JLabel label = new JLabel(text);
            label.setIconTextGap(10);
            label.setIcon(icon);
            JButton ok = new JButton("OK");

            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = c.gridy = 0;
            c.gridwidth = 5;
            c.gridheight = 1;
            c.insets = new Insets(10, 10, 10, 10);
            panel.add(label, c);
            c.gridy = c.gridwidth = 1;
            c.weightx = 100;
            panel.add(new JPanel(), c);
            c.weightx = 1;
            c.gridx++;
            c.insets.left = 0;
            c.insets.right = 5;
            if (!memError) {
                JButton openFile = new JButton("Open log");
                JButton openFolder = new JButton("Open log folder");
                JButton openBFolder = new JButton("Open backup folder");
                panel.add(openBFolder, c);
                c.gridx++;
                panel.add(openFolder, c);
                c.gridx++;
                panel.add(openFile, c);
                c.gridx++;
                c.insets.right = 10;

                openFile.addActionListener((ActionEvent ae) -> {
                    try {
                        Desktop.getDesktop().open(GlobalLogger.getLOG());
                    } catch (IOException ex) {
                        Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dialog.dispose();
                });
                openFolder.addActionListener((ActionEvent ae) -> {
                    try {
                        Desktop.getDesktop().open(GlobalLogger.getLOG_FOLDER());
                    } catch (IOException ex) {
                        Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dialog.dispose();
                });
                openBFolder.addActionListener((ActionEvent ae) -> {
                    try {
                        Desktop.getDesktop().open(new File(AutoBackupper.getDestination()));
                    } catch (IOException ex) {
                        Logger.getLogger(Startup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dialog.dispose();
                });
            }
            panel.add(ok, c);
            ok.addActionListener((ActionEvent ae) -> {
                dialog.dispose();
            });
            dialog.add(panel);
            dialog.setIconImages(MainGUI.INSTANCE.getIconImages());
            dialog.pack();
            dialog.setLocationRelativeTo(MainGUI.INSTANCE);
            ok.requestFocus();//so just mashing enter causes OK to be pressed, and not a folder to be opened as well.
            dialog.setVisible(true);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                //we're crashed at this point anyway, the 1 seconds is for any off-thread logging to properly finish.
            }

            thrwbl.printStackTrace();
            System.exit(-1);
            //We don't know where the crash came from, so we close everything.
            //All threads, all windows. We don't want to  risk corrupting files.
        }

    }

}
