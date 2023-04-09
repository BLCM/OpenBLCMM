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
package blcmm.gui.panels;

import blcmm.Meta;
import blcmm.gui.components.BLCMM_FileChooser;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.OSInfo;
import blcmm.utilities.Utilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

/**
 * Base JPanel for dialogs which deal with tweaking game settings -- mostly just
 * used because we split INI Tweaks and Hex Edits into separate dialogs, so
 * this is the trivially-shared code between them.
 *
 * More could be moved into here; the UI setup steps are virtually identical,
 * for instance.  For now I'm just leaving it at all the shared action handlers,
 * though.
 *
 * @author LightChaosman
 */
public abstract class GameTweaksPanel extends JPanel {

    private abstract class ManualSelectionListener implements ActionListener {

        private final File defaultFile;
        private final String executableBaseName;
        private final String name;
        private final String dirName;
        private final Component parentDialog;

        ManualSelectionListener(Component parentDialog, File defaultFile, String executableBaseName, String name, String dirName) {
            this.parentDialog = parentDialog;
            this.defaultFile = defaultFile;
            this.executableBaseName = executableBaseName;
            this.name = name;
            this.dirName = dirName;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {

            String pirateWarningMessage
                    = "<html><b>WARNING:</b> If you are using a pirated version of " + name + ", note that we can provide<br/>"
                    + "<i>NO SUPPORT</i> for " + Meta.NAME + " in the Discord or otherwise.<br/>"
                    + "<br/>"
                    + "There is no guarantee that hex-editing will work, or that mods will work even if the hex-edits do.<br/>"
                    + "A pirated TPS may fail where a pirated BL2 may succeed, etc.  Basically: if you use a pirated version,<br/>"
                    + "you're on your own for support.  We recommend just waiting for a Steam sale and picking it up<br/>"
                    + "legitimately for pennies.<br/>"
                    + "<br/>"
                    + "<i>(Apologies for nagging you, if you've got a legitimate version which we just couldn't figure<br/>"
                    + "out how to autodetect!)</i>";
            JOptionPane.showMessageDialog(this.parentDialog, pirateWarningMessage, "Don't expect support for pirated versions!", JOptionPane.WARNING_MESSAGE);

            JFileChooser fc = new BLCMM_FileChooser(defaultFile);
            fc.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String executable;
                    switch (OSInfo.CURRENT_OS) {
                        case UNIX:
                            executable = executableBaseName;
                            break;
                        case MAC:
                            executable = executableBaseName + ".app";
                            break;
                        default:
                            executable = executableBaseName + ".exe";
                            break;
                    }
                    return file.isDirectory() || file.getName().equalsIgnoreCase(executable);
                }

                @Override
                public String getDescription() {
                    return name + " executable";
                }
            });
            int returnVal = fc.showOpenDialog(parentDialog);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                while (f != null
                        && (!f.isDirectory()
                        || !f.getName().equalsIgnoreCase(dirName))) {
                    f = f.getParentFile();
                }
                if (f == null) {
                    JOptionPane.showMessageDialog(parentDialog, "Invalid file selected, please try again");
                    actionPerformed(evt);
                    return;
                }
                callback(f);
            }
        }

        //called on succesful selection
        public abstract void callback(File f);
    }

    public static interface ComponentProvider {

        public Component[] updateComponents();
    }

    protected static class SeparatorComponentProvider implements ComponentProvider {

        private final Component seperator;

        SeparatorComponentProvider(String title) {
            JLabel label = new JLabel(title);
            label.setBorder(new EmptyBorder(5, 0, 2, 0));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            seperator = label;
        }

        @Override
        public Component[] updateComponents() {
            return new Component[]{seperator};
        }

    }

    public static abstract class SetupAction implements ComponentProvider {

        private static boolean shownErrorDialog = false;

        protected final JLabel namelabel, statuslabel;
        protected final JButton button;
        protected final String name;
        protected boolean available = true;
        protected boolean revertOnly = false;
        private String disableReason;
        private boolean disableClickable = false;
        protected JComponent[] components;
        protected SetupStatus overrideStatus = null;
        private String revertFailMessage = null;

        SetupAction(String name) {
            this.name = name;
            statuslabel = new JLabel();
            button = new JButton();
            namelabel = new JLabel(name);
            components = new JComponent[]{namelabel, statuslabel, button};
        }

        void setRevertOnly(String reason) {
            revertOnly = true;
            this.disableReason = reason;
        }

        void setRevertFailMessage(String message) {
            this.revertFailMessage = message;
        }

        public void disable(String reason, boolean clickable) {
            this.available = false;
            this.disableReason = reason;
            this.disableClickable = clickable;
        }

        protected abstract SetupStatus getRealCurrentStatus();

        public SetupStatus getCurrentStatus() {
            if (overrideStatus != null) {
                return overrideStatus;
            }
            if (revertOnly && (name == null || name.isEmpty())) {
                return SetupStatus.IGNORE;
            }
            if (!available) {
                return SetupStatus.UNAVAILABLE;
            }
            return getRealCurrentStatus();
        }

        public boolean isAvailable() {
            return available;
        }

        public void setDescription(String s) {
            if (s != null && !s.isEmpty()) {
                namelabel.setToolTipText(s);
            }
        }

        public abstract void apply();

        public abstract boolean revert();

        public abstract void fix();

        public void overrideStatus(SetupStatus newStatus) {
            overrideStatus = newStatus;
        }

        @Override
        public Component[] updateComponents() {
            switch (getCurrentStatus()) {
                case ACTIVE:
                    setActive();
                    break;
                case PARTIAL:
                    setPartial();
                    break;
                case UNKNOWN:
                    setUnknown();
                    break;
                case UNAVAILABLE:
                    setUnavailable();
                    break;
                case REFUSEFIX:
                    setRefusefix();
                    break;
                case INACTIVE:
                    setInactive();
                    break;
                case ERROR:
                    setError("");
                    break;
                default:
                    throw new IllegalStateException();
            }
            return components;
        }

        protected void setError(String text) {
            namelabel.setEnabled(false);
            components[2].setEnabled(false);
            statuslabel.setText("Error");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Error");

            if (text != null && !text.isEmpty()) {
                statuslabel.setToolTipText(Utilities.hideUserName(text));
                if (!shownErrorDialog) {
                    JOptionPane.showMessageDialog(null, "One of your selected operations caused an error.\n"
                            + "No changes were made to the file.\n"
                            + "Hover over the red `Error` text for more information.", "Error during setup",
                            JOptionPane.ERROR_MESSAGE);
                    shownErrorDialog = true;//could make this an Option
                }
            }

        }

        protected void setError(Exception e) {
            String s;
            if (e instanceof java.nio.file.AccessDeniedException) {
                s = "Can not save to " + new File(((java.nio.file.AccessDeniedException) e).getFile()).getName() + ", check if it's not set to read-only";
            } else {
                s = e.toString();
            }
            setError(s);
        }

        protected void setUnavailable() {
            statuslabel.setText("Not available");
            statuslabel.setToolTipText(disableReason);
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusDisabledText));
            if (disableClickable) {
                button.setEnabled(true);
                button.setText("More info");
                for (ActionListener l : button.getActionListeners()) {
                    button.removeActionListener(l);
                }
                button.addActionListener(e -> {
                    // html content
                    JEditorPane ep = new JEditorPane("text/html", "<html><body>" //
                            + disableReason //
                            + "</body></html>");

                    //styling
                    JLabel label = new JLabel();
                    Font font = label.getFont();
                    StyleSheet styleSheet = ((HTMLDocument) ep.getDocument()).getStyleSheet();
                    styleSheet.addRule("body{font-size:" + font.getSize() + "pt;}");
                    Color c = ThemeManager.getColor(ThemeManager.ColorType.CodeSingleQuote);
                    styleSheet.addRule("a{color: rgb(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ");}");

                    // handle link events
                    ep.addHyperlinkListener(e1 -> {
                        if (e1.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                            try {
                                Desktop.getDesktop().browse(e1.getURL().toURI()); // roll your own link launcher or use Desktop if J6+
                            } catch (URISyntaxException | IOException ex) {
                                Logger.getLogger(IniTweaksPanel.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    Color bgColor = ThemeManager.getColor(ThemeManager.ColorType.UINimbusBase);
                    UIDefaults defaults = new UIDefaults();
                    defaults.put("EditorPane[Enabled].backgroundPainter", bgColor);
                    ep.putClientProperty("Nimbus.Overrides", defaults);
                    ep.setEditable(false);

                    // show
                    JOptionPane.showMessageDialog(null, ep);
                });
            } else {
                button.setText("-");
                button.setEnabled(false);
            }
            button.setToolTipText(disableReason);
        }

        protected void setUnknown() {
            statuslabel.setText("Unknown");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Fix");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                fix();
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        protected void setRefusefix() {
            statuslabel.setText("Error");
            statuslabel.setToolTipText(disableReason);
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusRed));
            button.setText("Cannot Fix");
            button.setEnabled(false);
        }

        protected void setPartial() {
            statuslabel.setText("Partially installed");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusOrange));
            button.setText("Complete");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                apply();
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        protected void setInactive() {
            statuslabel.setText("Inactive");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UIText));
            button.setText("Apply");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                apply();
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
            if (revertOnly) {
                components[2].setEnabled(false);
                components[2].setToolTipText(disableReason);
            }
        }

        protected void setActive() {
            statuslabel.setText("Installed");
            statuslabel.setForeground(ThemeManager.getColor(ThemeManager.ColorType.UINimbusGreen));
            button.setText("Revert");
            RemoveOldActionListeners();
            button.addActionListener((ae) -> {
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (!revert()) {
                    button.setText("Can't Revert");
                    button.setEnabled(false);
                    if (this.revertFailMessage != null) {
                        button.setToolTipText(this.revertFailMessage);
                    } else {
                        button.setToolTipText("There was a problem reverting to the stock Borderlands value");
                    }
                }
                IniTweaksPanel.INSTANCE.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            });
        }

        private void RemoveOldActionListeners() {
            for (ActionListener al : button.getActionListeners()) {
                button.removeActionListener(al);
            }
        }

    }

    public static class CompoundSetupAction extends SetupAction {

        private final SetupAction[] actions;

        public CompoundSetupAction(String name, SetupAction... actions) {
            super(name);
            this.actions = actions;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            HashMap<SetupStatus, Integer> ress = new HashMap<>();
            for (SetupStatus s : SetupStatus.values()) {
                ress.put(s, 0);
            }
            for (SetupAction action : actions) {
                SetupStatus s = action.getCurrentStatus();
                ress.put(s, ress.getOrDefault(s, 0) + 1);
            }
            int target = actions.length - ress.getOrDefault(SetupStatus.IGNORE, 0);
            if (ress.get(SetupStatus.ERROR) > 0) {
                return SetupStatus.ERROR;
            } else if (ress.get(SetupStatus.UNKNOWN) > 0) {
                return SetupStatus.UNKNOWN;
            } else if (ress.get(SetupStatus.UNAVAILABLE) > 0) {
                return SetupStatus.UNAVAILABLE;
            } else if (ress.get(SetupStatus.ACTIVE) == target) {
                return SetupStatus.ACTIVE;
            } else if (ress.get(SetupStatus.INACTIVE) == target) {
                return SetupStatus.INACTIVE;
            } else if (ress.get(SetupStatus.INACTIVE) + ress.get(SetupStatus.ACTIVE) + ress.get(SetupStatus.PARTIAL) == target) {
                return SetupStatus.PARTIAL;
            }
            return SetupStatus.UNKNOWN;
        }

        @Override
        public void apply() {
            for (SetupAction action : actions) {
                if (action.revertOnly) {
                    action.revert();
                } else {
                    action.apply();
                }
            }
            updateComponents();
        }

        @Override
        public boolean revert() {
            boolean succeeded = true;
            for (int i = actions.length - 1; i >= 0; i--) {
                succeeded = succeeded && actions[i].revert();
            }
            updateComponents();
            return succeeded;
        }

        @Override
        public void fix() {
            for (SetupAction action : actions) {
                action.fix();
                if (action.revertOnly) {
                    action.revert();
                }
            }
            updateComponents();
        }

    }

    public static class SingleFileSetupAction extends SetupAction {

        private final File fileToModify;
        private final File backupOfOriginal;
        private final StreamProvider streamProvider;
        private String shaOfOriginal;
        private String shaOfReplacement;
        private String lastCurrentSha;
        private boolean justExistence = false;
        private boolean allowWeakerCheck = false;
        private StreamProvider sourceForOriginal;
        private boolean neverRevert;

        SingleFileSetupAction(String name, File fileToModify, File backupOfOriginal, StreamProvider stream) {
            super(name);
            this.fileToModify = fileToModify;
            this.backupOfOriginal = backupOfOriginal;
            this.streamProvider = stream;
        }

        @Override
        public void apply() {
            if (backupOfOriginal != null) {
                if (backupOfOriginal.exists()) {
                    fileToModify.delete();
                } else {
                    fileToModify.renameTo(backupOfOriginal);
                }
            }
            if (streamProvider != null) {
                try (InputStream strm = streamProvider.provideStream();) {
                    fileToModify.getParentFile().mkdirs();
                    Files.copy(strm, fileToModify.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    strm.close();
                    setActive();
                } catch (IOException ex) {
                    setError(ex);
                    GlobalLogger.log(ex);
                }
            }
        }

        @Override
        public boolean revert() {
            if (neverRevert) {
                // This is a bit fuzzy, but we'd already be handling user
                // notification of failed reversions elsewhere if this is the
                // case.
                return true;
            }
            if (sourceForOriginal != null) {
                try (InputStream strm = sourceForOriginal.provideStream();) {
                    System.out.println(sourceForOriginal + " " + strm);
                    Files.copy(strm, fileToModify.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    strm.close();
                    backupOfOriginal.delete();
                    setInactive();
                    return true;
                } catch (IOException ex) {
                    setError(ex);
                    return false;
                }
            }
            if (backupOfOriginal == null) {
                fileToModify.delete();
                setInactive();
                return true;
            } else if (backupOfOriginal.exists()) {
                fileToModify.delete();
                backupOfOriginal.renameTo(fileToModify);
                setInactive();
                return true;
            } else if (shaOfOriginal != null) {
                File alternateBackup = findAlternateBackup();
                if (alternateBackup != null && alternateBackup.exists()) {
                    fileToModify.delete();
                    alternateBackup.renameTo(fileToModify);
                    setInactive();
                    return true;
                }
            }
            setUnknown();
            return false;
        }

        @Override
        public void fix() {
            revert();
            apply();
            updateComponents();
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            if (neverRevert) {
                return SetupStatus.IGNORE;
            } else if (justExistence) {
                return fileToModify.exists() ? SetupStatus.ACTIVE : SetupStatus.INACTIVE;
            } else if (streamProvider == null) {
                return fileToModify.exists() ? SetupStatus.INACTIVE : SetupStatus.ACTIVE;

            }
            try {
                String shaCurrent = Utilities.sha256(fileToModify);
                InputStream strm = streamProvider.provideStream();
                lastCurrentSha = Utilities.sha256(strm);
                strm.close();
                if (lastCurrentSha.equals(shaCurrent)) {
                    return SetupStatus.ACTIVE;
                }
                if (shaCurrent.equals(shaOfOriginal)) {
                    return SetupStatus.INACTIVE;
                }
                if (shaOfReplacement != null && !shaOfReplacement.equals(lastCurrentSha)) {
                    return SetupStatus.UNKNOWN;
                }
                if (allowWeakerCheck && backupOfOriginal != null) {
                    return backupOfOriginal.exists() ? SetupStatus.ACTIVE : SetupStatus.INACTIVE;
                }
                return SetupStatus.UNKNOWN;
            } catch (IOException | NoSuchAlgorithmException ex) {
                GlobalLogger.log(ex);
                return SetupStatus.ERROR;
            }
        }

        private void setJustCheckExistence() {
            justExistence = true;
        }

        private void setAllowWeakCheck() {
            this.allowWeakerCheck = true;
        }

        public void setShaOfOriginal(String shaOfOriginal) {
            this.shaOfOriginal = shaOfOriginal;
            if (sourceForOriginal != null) {
                try {
                    String sha = Utilities.sha256(sourceForOriginal.provideStream());
                    if (!sha.equals(shaOfOriginal)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        public void setShaOfReplacement(String shaOfReplacement) {
            this.shaOfReplacement = shaOfReplacement;
            if (streamProvider != null) {
                try {
                    String sha = Utilities.sha256(streamProvider.provideStream());
                    if (!sha.equals(shaOfReplacement)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        private void setSourceForOriginal(StreamProvider sourceForOriginal) {
            this.sourceForOriginal = sourceForOriginal;
            if (shaOfOriginal != null) {
                try {
                    String sha = Utilities.sha256(sourceForOriginal.provideStream());
                    if (!sha.equals(shaOfOriginal)) {
                        throw new IOException();
                    }
                } catch (NoSuchAlgorithmException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        private void neverRevert() {
            this.neverRevert = true;
        }

        private File findAlternateBackup() {
            assert shaOfOriginal != null;
            String filenamefilter = fileToModify.getName();
            if (filenamefilter.contains(".")) {
                filenamefilter = filenamefilter.substring(filenamefilter.indexOf("."));
            }
            final String filenamefilter1 = filenamefilter.toLowerCase();
            for (File f : fileToModify.getAbsoluteFile().getParentFile().listFiles()) {
                if (f.getName().toLowerCase().contains(filenamefilter1)) {
                    try {
                        if (Utilities.sha256(f).equals(shaOfOriginal)) {
                            return f;
                        }
                    } catch (IOException | NoSuchAlgorithmException ex) {

                    }
                }
            }
            return null;
        }

    }

    private static interface StreamProvider {

        public InputStream provideStream() throws IOException;
    }

    public static class FileStreamProvider implements StreamProvider {

        private final File file;

        public FileStreamProvider(File f) {
            this.file = f;
        }

        @Override
        public InputStream provideStream() throws IOException {
            return new FileInputStream(file);
        }

    }

    public static class ClassStreamProvider implements StreamProvider {

        private final String pathInClassPath;

        public ClassStreamProvider(String pathInClassPath) {
            this.pathInClassPath = pathInClassPath;
        }

        @Override
        public InputStream provideStream() throws IOException {
            return ClassLoader.getSystemResourceAsStream(pathInClassPath);
        }

        @Override
        public String toString() {
            return getClass() + " - path: " + pathInClassPath;
        }

    }

    public static abstract class AbstractFileEditSetupAction extends SetupAction {

        protected final File file;
        protected final String preHeaderName;
        protected final String field;
        protected transient String fileContent, relevantChunk;
        private boolean revertReadOnly = false;

        public AbstractFileEditSetupAction(String name, File file, String field, String preHeaderName) {
            super(name);
            this.file = file;
            this.preHeaderName = preHeaderName;
            this.field = field;
        }

        protected final void obtainContents() throws IOException, StringIndexOutOfBoundsException {
            fileContent = Utilities.readFileToString(file);
            int idx = fileContent.indexOf("\n[" + preHeaderName + "]\n");
            if (idx == -1) {
                throw new StringIndexOutOfBoundsException();
            }
            idx += ("\n[" + preHeaderName + "]\n").length();
            int idx2 = fileContent.indexOf("\n[", idx);
            if (idx2 == -1) {
                throw new StringIndexOutOfBoundsException();
            }
            relevantChunk = fileContent.substring(idx, idx2);
        }

        protected final String[] getRelevantLines() throws IOException, StringIndexOutOfBoundsException {
            obtainContents();
            return relevantChunk.split("\n");
        }

        protected final boolean promptForReadOnlyFile() {
            if (!file.canWrite()) {
                final String[] options = {"Yes", "No", "Yes, and revert back to readonly when done"};
                int option = JOptionPane.showOptionDialog(null, "Your " + file.getName() + " is in read-only mode.\n"
                        + "Disregard read-only mode and apply change anyway?",
                        "Read only mode detected", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (option == 1) {
                    return true;
                }
                file.setWritable(true, false);
                System.out.println(option);
                revertReadOnly = option == 2;

            }
            return false;
        }

        protected final void revertReadOnlyIfChosen() {
            if (revertReadOnly) {
                file.setReadOnly();
            }
        }
    }

    public static class FileEditChoiceSetupAction extends AbstractFileEditSetupAction {

        private final String defaultValue;
        private final Map<String, String> choicesToValuesMap;
        private final JComboBox combobox;
        private String current;
        boolean init = false;
        private Object selchoice;

        public FileEditChoiceSetupAction(String name, File file, String field, String preHeaderName, String defaultValue, String[] choices, String[] values) {
            super(name, file, field, preHeaderName);
            this.defaultValue = defaultValue;
            this.choicesToValuesMap = new TreeMap<>();
            for (int i = 0; i < choices.length; i++) {
                choicesToValuesMap.put(choices[i], values[i]);
            }
            combobox = new JComboBox(choices);

            ((JLabel) combobox.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
            combobox.addItemListener(new ItemListener() {
                private Object prevItem = null;

                @Override
                public void itemStateChanged(ItemEvent ie) {
                    if (!init) {
                        return;
                    }
                    if (ie.getStateChange() == ItemEvent.DESELECTED) {
                        prevItem = ie.getItem();
                        return;
                    }
                    try {
                        String choice = (String) combobox.getSelectedItem();
                        String val = choicesToValuesMap.get(choice);
                        StringBuilder sb = new StringBuilder();
                        boolean found = false;
                        for (String line : getRelevantLines()) {
                            if (line.startsWith(field + "=")) {
                                line = field + "=" + val;
                                found = true;
                            }
                            sb.append(line).append("\n");
                        }
                        if (!found) {
                            sb.append(field).append("=").append(val).append("\n");
                        }
                        if (promptForReadOnlyFile()) {
                            init = false;//just abuse the already existent init variable for this
                            combobox.setSelectedItem(prevItem);
                            init = true;
                            return;
                        }
                        Utilities.writeStringToFile(fileContent.replace(relevantChunk, sb).replace("\n", "\r\n"), file);
                        revertReadOnlyIfChosen();
                        current = val;
                        if (val.equals(defaultValue)) {
                            setInactive();
                        } else {
                            setActive();
                        }
                    } catch (IOException ex) {
                        setError(ex);
                        GlobalLogger.log(ex);
                    }
                }
            });
            components[2] = combobox;
        }

        @Override
        protected SetupStatus getRealCurrentStatus() {
            try {
                current = null;
                String lowerField = field.toLowerCase();
                for (String line : getRelevantLines()) {
                    if (line.toLowerCase().startsWith(lowerField + "=")) {
                        int idx = line.indexOf("=");
                        current = line.substring(idx + 1, line.length());
                        break;
                    }
                }
                if (current == null) {
                    current = "Error";
                    selchoice = null;
                    return SetupStatus.UNKNOWN;
                }

                selchoice = null;
                for (String choice : choicesToValuesMap.keySet()) {
                    if (choicesToValuesMap.get(choice).equals(current)) {
                        selchoice = choice;
                        break;
                    }
                }
                if (selchoice != null) {
                    combobox.setSelectedItem(selchoice);
                    if (current.equals(defaultValue)) {
                        return SetupStatus.INACTIVE;
                    } else {
                        return SetupStatus.ACTIVE;
                    }
                } else {
                    return SetupStatus.UNKNOWN;
                }
            } catch (IOException ex) {
                GlobalLogger.log(ex);
                setError(ex);
                return SetupStatus.ERROR;
            } catch (StringIndexOutOfBoundsException ex) {
                setError(file.getName() + "'s content does not match expectations");
                return SetupStatus.ERROR;
            }
        }

        @Override
        public Component[] updateComponents() {
            Component[] components = super.updateComponents();
            components[2] = combobox;
            switch (getCurrentStatus()) {
                case ACTIVE:
                case INACTIVE:
                    combobox.setSelectedItem(selchoice);
                    break;
                case UNKNOWN:
                    choicesToValuesMap.put(current, current);
                    combobox.setModel(new DefaultComboBoxModel(choicesToValuesMap.keySet().toArray()));
                    combobox.setSelectedItem(current);
            }
            init = true;
            return components;
        }

        @Override
        public void apply() {
        }

        @Override
        public boolean revert() {
            return true;
        }

        @Override
        public void fix() {
        }

    }

    public static enum SetupStatus {
        ACTIVE, PARTIAL, INACTIVE, UNKNOWN, UNAVAILABLE, ERROR, IGNORE, REFUSEFIX;
    }

}
