/*
 * Testing 123
 * abc
 */package blcmm.plugins;

import blcmm.plugins.pseudo_model.PCategory;
import blcmm.utilities.Utilities;
import general.utilities.GlobalLogger;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

public class PluginLoader {

    public static URLClassLoader MY_CLASS_LOADER;

    public static void setModelSupplier(BiFunction<Boolean, Boolean, PCategory> sup) {
        BLCMMPlugin.setModelSupplier(sup);
    }

    // the directory where we keep the plugin classes
    public static final File PLUGINS_DIR = new File("plugins/");
    public static final File PLUGINS_DIR_TO_UPDATE = new File("plugins/update");
    private static final File PLUGINS_TO_DELETE = new File("plugins/delete/list.txt");

    // a list where we keep an initialized object of each plugin class
    public final static Map<BLCMMPlugin, PluginInfo> PLUGINS = new LinkedHashMap<>();//Maps plugin to compile time
    public final static Map<String, String> FAILED_TO_LOAD = new HashMap<String, String>();//Maps jar name to error message

    public static void loadPlugins() {
        List<JarFile> jarfiles = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
        if (PLUGINS_DIR_TO_UPDATE.exists()) {
            for (File f : PLUGINS_DIR_TO_UPDATE.listFiles()) {
                try {
                    Files.move(f.toPath(), new File(PLUGINS_DIR.getAbsolutePath() + File.separator + f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    GlobalLogger.log("Updated plugins contained in " + f.getName());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to update plugins contained in " + f.getName());
                }
            }
        }
        if (PLUGINS_TO_DELETE.exists()) {
            try {
                List<String> failed = new ArrayList<>();
                for (String name : Utilities.readFileToString(PLUGINS_TO_DELETE).split("\n")) {
                    if (name.isEmpty()) {
                        continue;
                    }
                    boolean delete = new File(PLUGINS_DIR.getAbsolutePath() + File.separator + name).delete();
                    if (delete) {
                        GlobalLogger.log("Deleted plugins in " + name);
                    } else {
                        GlobalLogger.log("Failed to delete plugins in " + name);
                        failed.add(name);
                    }
                }
                Utilities.writeStringToFile(failed.stream().collect(Collectors.joining("\n")), PLUGINS_TO_DELETE);
            } catch (IOException ex) {
                GlobalLogger.log("Failed to read plugin deletion file");
            }
        }
        if (PLUGINS_DIR.exists() && PLUGINS_DIR.isDirectory()) {
            File[] files = PLUGINS_DIR.listFiles();
            for (File file : files) {
                // only consider files ending in ".jar"
                if (!file.getName().endsWith(".jar")) {
                    continue;
                }
                addFile(jarfiles, file, urls);
            }
        }
        MY_CLASS_LOADER = URLClassLoader.newInstance(urls.toArray(new URL[0]));
        loadAllFiles(MY_CLASS_LOADER, jarfiles);
    }

    public static void markForDeletion(BLCMMPlugin plugin) {
        markForDeletion(PLUGINS.get(plugin).file.getName());
    }

    public static void markForDeletion(String jarName) {
        try {
            if (!PLUGINS_TO_DELETE.exists()) {
                PLUGINS_TO_DELETE.getParentFile().mkdirs();
                PLUGINS_TO_DELETE.createNewFile();
            }
            Utilities.writeStringToFile(Utilities.readFileToString(PLUGINS_TO_DELETE) + "\n" + jarName, PLUGINS_TO_DELETE);
        } catch (IOException ex) {
            Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void addFile(final List<JarFile> jarfiles, final File f, final List<URL> urls) {
        try {
            jarfiles.add(new JarFile(f));
            urls.add(new URL("jar:file:" + f.getPath() + "!/"));
        } catch (MalformedURLException ex) {
            Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (urls.size() > jarfiles.size()) {
            urls.remove(urls.size() - 1);
        }
        while (urls.size() < jarfiles.size()) {
            jarfiles.remove(jarfiles.size() - 1);
        }
    }

    private static void loadAllFiles(URLClassLoader cl, List<JarFile> jarfiles) {
        HashMap<String, String> usedNames = new HashMap<>();
        for (JarFile jarFile : jarfiles) {
            try {
                Enumeration<JarEntry> e = jarFile.entries();
                while (e.hasMoreElements()) {
                    JarEntry je = e.nextElement();
                    if (je.isDirectory() || !je.getName().endsWith(".class")) {
                        continue;
                    }

                    // -6 because of .class
                    String className = je.getName().substring(0, je.getName().length() - 6);
                    className = className.replace('/', '.');
                    if (className.endsWith("test")) {
                        continue;
                    }
                    if (usedNames.containsKey(className)) {
                        GlobalLogger.log("Plugin class clash while loading " + jarFile.getName() + ": " + className + " was already loaded from " + usedNames.get(className));
                    } else {
                        usedNames.put(className, jarFile.getName());
                    }
                    Class c = cl.loadClass(className);
                    Class superclass = c.getSuperclass();
                    if (superclass != null && BLCMMPlugin.class.isAssignableFrom(superclass)) {
                        // the following line assumes that BLCMMPlugin has a no-argument constructor
                        BLCMMPlugin plugin = (BLCMMPlugin) c.newInstance();
                        GlobalLogger.log(String.format("plugin loaded from %s%s, class name: %s%s, plugin name: %s",
                                jarFile.getName(), pad(40 - jarFile.getName().length()),
                                className, pad(50 - className.length()),
                                plugin.getName()));
                        PLUGINS.put(plugin, new PluginInfo(jarFile, je.getLastModifiedTime()));
                    }
                }

            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Error e) {
                GlobalLogger.log("Error Occured while loading plugin (" + jarFile.getName() + "): \n");
                //e.printStackTrace();
                FAILED_TO_LOAD.put(jarFile.getName().replaceFirst("plugins\\\\", ""), e.toString());
            }
        }
    }

    private static String pad(int x) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < x; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public static class PluginInfo {

        private final JarFile file;
        private final FileTime compileTime;

        public PluginInfo(JarFile file, FileTime compileTime) {
            this.file = file;
            this.compileTime = compileTime;
        }

        public FileTime getCompileTime() {
            return compileTime;
        }

        public JarFile getFile() {
            return file;
        }

    }
}