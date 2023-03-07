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
package blcmm.gui.panels;

import blcmm.utilities.Utilities;
import blcmm.utilities.GlobalLogger;
import general.utilities.OSInfo;
import general.utilities.StringTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 *
 * @author LightChaosman - The general basics
 * @author FromDarkHell - The plugin updating
 */
public class UpdateSettingsPanel extends JPanel {

    private static UpdateSettingsPanel instance;
    private static final File VERSION_FILE = new File("versions.options");
    private static final File DATA_EXCLUDES_FILE_BL2 = new File("data/BL2/excludes.txt");
    private static final File DATA_EXCLUDES_FILE_TPS = new File("data/TPS/excludes.txt");

    private final StringTable settings;
    private boolean canAskForEagerDownloader;
    private static boolean needsLauncherReset = false;
    private final Folder f;

    public UpdateSettingsPanel() {
        settings = obtainTable();
        this.canAskForEagerDownloader = !alreadyHasDataPackages(settings);
        instance = this;
        super.setLayout(new GridBagLayout());

        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 0;
        constr.gridy = 0;
        constr.gridwidth = 1;
        constr.gridheight = 1;
        constr.ipadx = 15;
        constr.ipady = 2;
        constr.weightx = 1;
        constr.weighty = 1;
        constr.insets = new Insets(5, 10, 0, 0);
        constr.anchor = GridBagConstraints.WEST;
        super.add(new JLabel("Updatable content"), constr);
        constr.gridx = 1;
        super.add(new JLabel("Current version"), constr);
        constr.gridx = 2;

        super.add(new JLabel("Auto update enabled"), constr);
        constr.gridx = 0;
        constr.insets = new Insets(0, 10, 0, 0);
        constr.gridy = 1;
        constr.gridwidth = 3;
        constr.fill = GridBagConstraints.HORIZONTAL;
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setPreferredSize(new Dimension(1, 5));

        constr.ipadx = 1;
        super.add(sep, constr);

        constr.fill = GridBagConstraints.NONE;
        constr.gridwidth = 1;
        int y = 2;
        //y = old(constr, y);
        constr.gridy++;
        constr.ipady = 0;
        f = convertOptionsToFolder(settings);

        for (Row r : f) {
            if (r instanceof Folder) {
                ((Folder) r).collapse();
            }
        }
        addFolderToLayout(f, constr, 0);
        constr.gridx = 3;
        constr.gridwidth = 3;
        constr.weightx = 1000;
        constr.weighty = 1000;
        constr.fill = GridBagConstraints.BOTH;
        super.add(new JPanel(), constr);

        constr.gridx = 0;
        constr.gridy++;
        constr.weightx = 1;
        constr.weighty = 1;
        constr.fill = GridBagConstraints.NONE;
        constr.insets.bottom = 5;
        super.add(new JLabel("Restart BLCMM after selecting more content"), constr);
    }

    private StringTable obtainTable() {
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
        return localVersions;
    }

    public void saveTable() {
        try {
            Utilities.writeStringToFile(settings.convertTableToString(), VERSION_FILE);

            //Yeah, we're using hardcoded knowledge about:
            //A) which file exactly the library uses to stor its excludes and
            //B) hardcoded settings file headers
            //But really, properly abstracting for this one case, not worth
            StringBuilder bl2 = new StringBuilder(), tps = new StringBuilder();
            for (String key : settings.keySet()) {
                if (!Boolean.parseBoolean(settings.get(key, "update")) && key.startsWith("BLCMM_Data")) {
                    (key.contains("TPS") ? tps : bl2).append(settings.get(key, "fileName") + "\n");
                }
            }
            DATA_EXCLUDES_FILE_BL2.getParentFile().mkdirs();
            DATA_EXCLUDES_FILE_TPS.getParentFile().mkdirs();
            Utilities.writeStringToFile(bl2, DATA_EXCLUDES_FILE_BL2);
            Utilities.writeStringToFile(tps, DATA_EXCLUDES_FILE_TPS);
        } catch (IOException ex) {
            GlobalLogger.log(ex);
        }
    }

    public boolean needsLauncherReset() {
        return needsLauncherReset;
    }

    private void addFolderToLayout(Row f, GridBagConstraints constr, int depth) {
        if (depth != 0) {
            Component[] cs = f.getRowComponents();
            constr.gridx = 0;
            constr.anchor = GridBagConstraints.WEST;
            int extradepth = f instanceof Folder ? 0 : (depth > 1 ? 1 : 0);
            constr.insets.left = (depth + extradepth) * 10;
            this.add(cs[0], constr);
            constr.insets.left = 10;
            constr.gridx = 1;
            if (cs.length > 1) {
                this.add(cs[1], constr);
            }
            constr.gridx = 2;
            constr.anchor = GridBagConstraints.CENTER;
            if (cs.length > 2) {
                this.add(cs[2], constr);
            }
            constr.gridy++;
        }
        if (f instanceof Folder) {
            for (Row r : (Folder) f) {
                addFolderToLayout(r, constr, depth + 1);
            }
        }
    }

    private static boolean alreadyHasDataPackages(StringTable settings) {
        for (String key : settings.keySet()) {
            String update = settings.get(key, "update");
            String enablable = settings.get(key, "enablable");
            if (key.startsWith("BLCMM_Data_") && "true".equalsIgnoreCase(update) && "true".equalsIgnoreCase(enablable)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkForEagerDownloader(String key) {
        if (!canAskForEagerDownloader) {
            return false;
        }
        if (key.startsWith("BLCMM_Data_") && !key.endsWith("_Base")) {
            int result = JOptionPane.showConfirmDialog(instance, "<html>"
                    + "It seems like you're selecting more than just the basic data package right off the bat.<br/>"
                    + "In most cases you do not need any more data packages than the basic data package.<br/>"
                    + "Having more data than you need slows down searches in the Object explorer, and increases memory consumption.<br/>"
                    + "<br/>"
                    + "<b>If you try to dump data from an uninstalled package, BLCMM will inform you which one you need</b><br/>"
                    + "<br/>"
                    + "Continue downloading more packages anyway?",
                    "Are you sure you need this data?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return true;
            }
            canAskForEagerDownloader = false;
        }
        return false;
    }

    private HashMap<String, String> fileNamesToReadableNames(String gameType) {
        HashMap<String, String> fileNamesToReadableNames = new HashMap<>();
        settings.keySet().forEach((updatable) -> {
            String object = settings.get(updatable, "fileName");
            if (!(!object.contains(gameType) || object.contains("Indices"))) {
                String packageName = object.split(gameType + "_")[1].replace(".jar", "");
                fileNamesToReadableNames.put(packageName, settings.get(updatable, "userFriendlyName"));
            }
        });
        return fileNamesToReadableNames;
    }

    public void checkAllNeededPackages(List<String> packagesToCheck, boolean[] gameTypeToCheck) {
        // General fix to make sure all of our checking goes fine.
        if (canAskForEagerDownloader) {
            canAskForEagerDownloader = false;
        }
        for (int i = 0; i < gameTypeToCheck.length; i++) {
            boolean game = gameTypeToCheck[i];
            if (game == false) {
                // The plugin doesn't support BL2 / TPS.
                continue;
            }
            String gameType = i == 1 ? "BL2" : "TPS";

            HashMap<String, String> fileNamesToReadableNames = fileNamesToReadableNames(gameType);
            Folder properRow = null;
            for (Row r : f) {
                if (!r.name.trim().equalsIgnoreCase("Data")) {
                    continue;
                }
                f.expand();
                ((Folder) r).expand();
                for (Row rows : (Folder) r) {
                    if (rows.name.trim().equalsIgnoreCase(gameType)) {
                        properRow = (Folder) rows;
                        properRow.expand();
                    }
                }
            }

            for (String packageToCheck : packagesToCheck) {
                // Our package somehow isn't in our file names.
                if (!fileNamesToReadableNames.containsKey(packageToCheck)) {
                    continue;
                }
                for (Row r : properRow) {
                    if (r.name != fileNamesToReadableNames.get(packageToCheck)) {
                        continue;
                    }
                    JCheckBox box = (JCheckBox) ((Updatable) r).getRowComponents()[2];
                    if (box.isSelected()) {
                        continue;
                    }
                    box.setSelected(true);
                    for (ActionListener a : box.getActionListeners()) {
                        a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                    }
                }
            }
        }

    }

    //Some auxilary classes to add structure to the GUI generation below.
    private static abstract class Row implements Comparable<Row> {

        final String name;
        final Folder parent;

        public Row(String name, Folder parent) {
            this.name = name;
            this.parent = parent;
        }

        @Override
        public int compareTo(Row t) {
            String n = (this instanceof Updatable ? "AA" : "") + name;
            String n2 = (t instanceof Updatable ? "AA" : "") + t.name;
            return n.compareToIgnoreCase(n2);
        }

        public abstract Component[] getRowComponents();

    }

    private static class Folder extends Row implements Iterable<Row> {

        final Set<Row> children = new TreeSet<>();
        boolean expanded = false;
        final Component comp;
        private final JButton button;

        public Folder(String name, Folder parent) {
            super(name, parent);

            JPanel panel = new JPanel();
            comp = panel;
            button = new JButton("+");
            JLabel label = new JLabel(name);
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constr = new GridBagConstraints();
            constr.gridx = 0;
            constr.gridy = 0;
            constr.gridwidth = 1;
            constr.gridheight = 1;
            constr.ipadx = 0;
            constr.ipady = 0;
            constr.weightx = 1;
            constr.weighty = 1;
            panel.add(button, constr);
            constr.gridx++;
            panel.add(label, constr);

            button.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 3));
            button.setMargin(new Insets(0, 0, 0, 0));
            Dimension d = new Dimension(17, 17);
            button.setMinimumSize(d);
            button.setPreferredSize(d);
            button.setMaximumSize(d);
            button.addActionListener(ae -> {
                expanded = !expanded;
                if (expanded) {
                    expand();
                } else {
                    collapse();
                }
            });
        }

        @Override
        public Component[] getRowComponents() {
            return new Component[]{comp};
        }

        @Override
        public Iterator<Row> iterator() {
            return children.iterator();
        }

        private void collapse() {
            button.setText("+");
            for (Row r : this) {
                if (r instanceof Folder) {
                    ((Folder) r).collapse();
                }
                for (Component component : r.getRowComponents()) {
                    component.setVisible(false);
                }
            }
        }

        private void expand() {
            button.setText("-");
            for (Row r : this) {
                for (Component component : r.getRowComponents()) {
                    component.setVisible(true);
                }
            }
        }
    }

    private static class Updatable extends Row {

        final Component[] comps;

        Updatable(StringTable settings, String key, Folder parent) {
            super(settings.get(key, "userFriendlyName"), parent);
            JLabel label1 = new JLabel(name);
            JLabel label2 = new JLabel(settings.get(key, "currentVersion").trim().equals("") ? "Not installed" : settings.get(key, "currentVersion"));
            JCheckBox check = new JCheckBox();
            check.setSelected(settings.get(key, "update").equalsIgnoreCase("True"));
            check.addActionListener((ActionEvent ae) -> {
                if (instance.checkForEagerDownloader(key)) {
                    check.setSelected(false);
                    return;
                }
                settings.put(key, "update", check.isSelected() + "");
                instance.saveTable();
                needsLauncherReset = true;
            });
            String dep = settings.get(key, "dependencies");
            String[] deps = dep.trim().isEmpty() ? new String[]{} : (dep.contains(";") ? dep.split(";") : new String[]{dep});
            StringBuilder tooltipBuilder = new StringBuilder();
            Color newForeground = null;
            if (deps.length > 0) {
                List<String> uninstalled = new ArrayList<>();
                for (String d : deps) {
                    if (settings.get(d, "currentVersion").trim().equals("")) {
                        uninstalled.add(settings.get(d, "userFriendlyName"));
                    }
                }
                if (uninstalled.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("The following required packages are not installed: ");
                    for (String un : uninstalled) {
                        sb.append(un + ", ");
                    }
                    tooltipBuilder.append(sb.substring(0, sb.length() - 2));
                    newForeground = new Color(200, 0, 0);
                }
            }
            String tooltipFromSettings = settings.get(key, "tooltip");
            if (tooltipFromSettings != null && !tooltipFromSettings.isEmpty()) {
                if (tooltipBuilder.length() == 0) {
                    tooltipBuilder.append(tooltipFromSettings);
                    newForeground = new Color(200, 100, 0);
                } else {
                    tooltipBuilder.append(" -- " + tooltipFromSettings);
                }
            }
            if (tooltipBuilder.length() > 0) {
                label1.setForeground(newForeground);
                label1.setToolTipText(tooltipBuilder.toString());
            }
            comps = new Component[]{label1, label2, check};
        }

        @Override
        public Component[] getRowComponents() {
            return comps;
        }
    }

    private static Folder convertOptionsToFolder(StringTable settings) {
        Folder main = new Folder("", null);
        for (final String updatable : settings.keySet()) {
            OSInfo.OS os = OSInfo.CURRENT_OS;
            if (!Boolean.parseBoolean(settings.get(updatable, "enablable")) || ((os == OSInfo.OS.MAC || os == OSInfo.OS.UNIX) && settings.get(updatable, "userFriendlyName").contains("Multitool"))) {
                continue;
            }
            String serverpath = settings.get(updatable, "serverPath");
            Folder current = main;
            while (!serverpath.isEmpty()) {
                String nextFolder = serverpath.substring(0, serverpath.indexOf("/"));
                serverpath = serverpath.substring(serverpath.indexOf("/") + 1);
                Folder search = null;
                for (Row r : current) {
                    if (r instanceof Folder && r.name.equalsIgnoreCase(nextFolder)) {
                        search = (Folder) r;
                        break;
                    }
                }
                if (search == null) {
                    search = new Folder(nextFolder, current);
                    current.children.add(search);
                }
                current = search;
            }
            current.children.add(new Updatable(settings, updatable, current));
        }
        return main;

    }
}
