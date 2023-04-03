/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm;

import blcmm.gui.MainGUI;
import blcmm.gui.panels.IniTweaksPanel;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.AutoBackupper;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.OSInfo;
import blcmm.utilities.Options;
import blcmm.utilities.StringTable;
import blcmm.utilities.Utilities;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

    private static String titlePostfix;
    private static File file;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> vmArguments = runtimeMxBean.getInputArguments();
        System.setProperty("sun.awt.exception.handler", MyExceptionHandler.class.getName());
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());
        if (!confirmIO()) {
            GlobalLogger.log("Closing " + Meta.NAME + " because we can't confirm IO");
            GlobalLogger.markAsPermanentLog();
            return;
        }

        // Process any arguments we have
        file = null;
        for (String arg: args) {
            // Set Creator Mode if we've been told to.  (Note that Creator Mode is
            // also set automatically if Java assertions are active, which is the
            // default behavior when running this project via Netbeans.)
            if (arg.equalsIgnoreCase("-creator")) {
                Utilities.setCreatorMode();
            } else if (arg.startsWith("-f=")) {
                file = new File(arg.substring("-f=".length()));
            }
        }

        GlobalLogger.log("Running " + Meta.NAME + " version " + Meta.VERSION);
        GlobalLogger.log("Running Java version " + System.getProperty("java.version"));
        GlobalLogger.log("Arguments provided; VM arguments: " + Arrays.toString(vmArguments.toArray()) + " - Runtime arguments: " + Arrays.toString(args));
        if (Utilities.isCreatorMode()) {
            GlobalLogger.log("Running in Creator Mode!");
        }
        GlobalLogger.log("Username: " + System.getProperty("user.name"));

        // Total amount of free memory available to the JVM
        GlobalLogger.log("Free Memory: " + humanReadableByteCount(Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
        // Maximum amount of memory the JVM will attempt to use (Long.MAX_VALUE if there is no limit)
        GlobalLogger.log("Maximum Memory: " + (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "No Limit"
                : humanReadableByteCount(Runtime.getRuntime().maxMemory())));

        // Adjust the title
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

        // Load options and set theme
        firstTime();

        // Make tooltips tolerable
        ToolTipManager.sharedInstance().setInitialDelay(500);
        ToolTipManager.sharedInstance().setDismissDelay(10000);

        // Report on some various vars
        GlobalLogger.log(Meta.NAME + " is installed at: " + Utilities.getMainInstallDir());
        GlobalLogger.log("Your user data directory is: " + Utilities.getBLCMMDataDir());
        GlobalLogger.log("Working directory: " + System.getProperty("user.dir").replaceAll("\\\\", "/"));
        GlobalLogger.log("Default file-open location: " + Utilities.getDefaultOpenLocation().toString());

        java.awt.EventQueue.invokeLater(() -> {
            new MainGUI(file, titlePostfix).setVisible(true);
        });
    }

    private static void firstTime() {
        if (isFirstTimeRunning()) {
            GlobalLogger.log("First-time startup detected");
            MainGUI.setTheme(ThemeManager.getDefaultTheme());
            Options.INSTANCE.setShowHotfixNames(false);
        } else {
            Utilities.populateFileHistory(true);
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

    public static void showIniTweaks() {
        IniTweaksPanel panel = new IniTweaksPanel();
        JOptionPane.showMessageDialog(null, panel, "Select options", JOptionPane.PLAIN_MESSAGE);
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
                    + Meta.NAME + " does not seem to be able to write to files.\n"
                    + "Check your file permissions or run it at a different location.\n"
                    + Meta.NAME + " will now shut down, since it requires file I/O.",
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

    public static boolean promptRestart() throws HeadlessException {
        int restart = JOptionPane.showConfirmDialog(null, Meta.NAME + " needs to restart for the selected options to work properly. Restart now?", "Restart?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (restart != JOptionPane.YES_OPTION) {
            return false;
        }
        boolean dispose = MainGUI.INSTANCE.isReadyToSuperDispose();
        if (!dispose) {
            return false;
        }
        startBLCMM();
        MainGUI.INSTANCE.superDispose();
        return true;
    }

    private static void startBLCMM() {
        String path = new File(System.getProperty("user.dir")).getAbsolutePath();
        String path2 = path.replaceAll("\\\\", "/");
        File f = new File(path2 + "/" + Meta.JARFILE);
        String path3 = f.getAbsolutePath().replaceAll("\\\\", "/");
        String[] command4 = new String[]{"java", "-jar", "-XX:MaxHeapFreeRatio=50", path3};
        try {
            Runtime.getRuntime().exec(command4);
        } catch (IOException ex) {
            GlobalLogger.log(ex);
            JOptionPane.showMessageDialog(null,
                    "Unable to launch " + Meta.NAME + ": " + ex.getMessage(),
                    "Error launching " + Meta.NAME, JOptionPane.ERROR_MESSAGE);
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

            // The contents being gathered here used to theoretically be
            // generated by a StringTable.convertTableToSpacedString()
            // method.  IMO this would be a little more useful (also I didn't
            // have an example of how that looked, anyway, and didn't care to
            // try and generate one)
            ArrayList<String> installed = new ArrayList<>();
            String userFriendlyName;
            String version;
            StringBuilder reportStr;
            for (String key : localVersions.keySet()) {
                if (localVersions.get(key, "update").equals("True")) {
                    reportStr = new StringBuilder();
                    reportStr.append("  - ");
                    reportStr.append(key);

                    userFriendlyName = localVersions.get(key, "userFriendlyName");
                    if (!userFriendlyName.isEmpty() && !key.equals(userFriendlyName)) {
                        reportStr.append(" (");
                        reportStr.append(userFriendlyName);
                        reportStr.append(")");
                    }

                    // This always appears to be empty?
                    version = localVersions.get(key, "downloadedVersion").trim();
                    if (!version.isEmpty()) {
                        reportStr.append(" v");
                        reportStr.append(version);
                    }

                    installed.add(reportStr.toString());
                }
            }
            if (installed.isEmpty()) {
                installed.add("(nothing)");
            }
            GlobalLogger.log("\n\n OPTIONS:\n\n" + Options.INSTANCE.toString() + "\n\nInstalled stuff:\n\n" + String.join("\n", installed));
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
                text = "<html><b>" + Meta.NAME + " ran out of memory!</b><br/>"
                        + "<br/>"
                        + "This can happen when trying to load large mods like Randomizers.<br/>"
                        + "<br/>"
                        + "If you're using the Jar version of " + Meta.NAME + ", edit the .bat/.sh<br/>"
                        + "script used to launch the appplication and change the -Xmx parameter found<br/>"
                        + "in the script.  If using the Windows EXE version, please report your problem<br/>"
                        + "to the " + Meta.NAME + " developers, along with the mod(s) you're using, and<br/>"
                        + "we'll try to figure it out for you.<br/>";
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
