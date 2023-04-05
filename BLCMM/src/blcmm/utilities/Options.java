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
import blcmm.gui.theme.Theme;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.PatchType;
import blcmm.utilities.options.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to control dealing with the main application options/settings.
 *
 * @author LightChaosman
 */
public class Options {

    public static Options INSTANCE;

    /**
     * Filename to use.
     */
    private final static String DEFAULT_FILENAME = "general.options";

    /**
     * Enum to store the textual keys for our options. This is probably a bit of
     * overkill, but it prevents us from having to worry about keeping the same
     * string synchronized between a few different places in the code.
     */
    public enum OptionNames {
        theme,
        fontsize,
        truncateCommands2,
        truncateCommandLength,
        highlightBVCErrors,
        leafSelectionAllowed,
        preferFullObjInOE,
        hasSeenExportWarning,
        showConfirmPartialCategory,
        sessionsToKeep,
        backupsPerSession,
        secondsBetweenBackups,
        OELeftPaneVisible,
        mainWindowWidth,
        mainWindowHeight,
        mainWindowMaximized,
        editWindowWidth,
        editWindowHeight,
        oeWindowWidth,
        oeWindowHeight,
        oeWindowMaximized,
        fileHistory,
        lastImport,
        filenameTruncationLength,
        propagateMUTNotification,
        BL2Bookmarks,
        TPSBookmarks,
        popupStatus,
        showHotfixNames,
        dragAndDroppableCode,
        showDeleteConfirmation,
        saveAsOffline,
        onlineServiceNumber,
        checkForNewVersions,
        oeSearchActions,
        oeSearchAI,
        oeSearchAnimations,
        oeSearchBase,
        oeSearchBehaviors,
        oeSearchDialog,
        oeSearchKismets,
        oeSearchMeshes,
        oeSearchMissions,
        oeSearchOthers,
        oeSearchParticles,
        oeSearchPopulations,
        oeSearchSkins,
        oeSearchStaticMeshes,
        oeSearchWillowData,
        oeDataSuccessTimestampDbBL2,
        oeDataSuccessTimestampDbTPS,
        oeDataSuccessTimestampJarBL2,
        oeDataSuccessTimestampJarTPS,
    }

    public enum OESearch {
        Actions(OptionNames.oeSearchActions),
        AI(OptionNames.oeSearchAI),
        Animations(OptionNames.oeSearchAnimations),
        Base(OptionNames.oeSearchBase),
        Behaviors(OptionNames.oeSearchBehaviors),
        Dialog(OptionNames.oeSearchDialog),
        Kismets(OptionNames.oeSearchKismets),
        Meshes(OptionNames.oeSearchMeshes),
        Missions(OptionNames.oeSearchMissions),
        Others(OptionNames.oeSearchOthers),
        Particles(OptionNames.oeSearchParticles),
        Populations(OptionNames.oeSearchPopulations),
        Skins(OptionNames.oeSearchSkins),
        StaticMeshes(OptionNames.oeSearchStaticMeshes),
        WillowData(OptionNames.oeSearchWillowData);

        public OptionNames option;

        private OESearch(OptionNames option) {
            this.option = option;
        }
    }

    private HashSet<OESearch> activeSearchCategories = new HashSet<>();

    /**
     * A list of old option names which shouldn't be re-saved if we encounter
     * them in the options file. This is a bit silly -- at the moment, there's
     * no reason to keep *any* option that we don't explicitly have defined in
     * the main list. If we end up allowing Plugins to store options, though, we
     * may want to hold on to option names which we don't know about, in case a
     * plugin is temporarily unavailable or something.
     */
    private final HashSet<String> IGNORE_OPTIONS = new HashSet(Arrays.asList(
            // Old from the original BLCMM, and no longer wanted
            "contentEdits",
            "truncateCommands",
            "structuralEdits",
            "developerMode",

            // Old from during OpenBLCMM development.  Possibly not worth
            // having in here, but it'll clean out my own configs, so sure.
            // These were replaced by Db/Jar-specific options.
            "oeDataSuccessTimestampBL2",
            "oeDataSuccessTimestampTPS",

            // We don't have a launcher or splash image anymore
            "splashImage"
    ));

    /**
     * HashMap to hold our options
     */
    private final HashMap<String, Option> OPTION_MAP = new HashMap<>();

    /**
     * ArrayList to hold the order in which we should process options
     */
    private final ArrayList<String> optionOrder = new ArrayList<>();

    /**
     * Structure to save information about any option found in the options file
     * which we don't explicitly know about. Possibly useful if, in the future,
     * there are plugins which add options which aren't loaded yet, or
     * something. At the moment this should probably always be empty, but we're
     * keeping track regardless.
     */
    private final HashMap<String, String> UNKNOWN_OPTIONS = new HashMap<>();

    /**
     * List of Strings which contain load errors which should be reported to the
     * user on app startup. Will be populated by loadFromFile() if necessary.
     */
    private final ArrayList<String> loadErrors = new ArrayList<>();

    /**
     * Construct a fresh Options object.
     */
    public Options() {

        // First up, for simplicity's sake: settings shown on the main
        // settings menu.  The order in which these are registered is the
        // order in which they'll show up in the panel.
        this.registerOption(new SelectionOption<>(
                OptionNames.theme.toString(), ThemeManager.getTheme("dark"),
                Option.Shown.SETTINGS, "Theme",
                "setTheme", "Change " + Meta.NAME + "'s color theme",
                "checkThemeSwitchAllowed",
                ThemeManager.getAllInstalledThemes().toArray(new Theme[0]), s -> {
            Theme t = ThemeManager.getTheme(s);
            return t == null ? ThemeManager.getTheme("dark") : t;
        }
        ));

        this.registerOption(new BooleanOption(
                OptionNames.checkForNewVersions.toString(), true,
                Option.Shown.SETTINGS, "Check for new versions on startup",
                "toggleCheckForNewVersions",
                "Check for new versions of " + Meta.NAME + " when the "
                + "app starts up."));

        this.registerOption(new IntOption(
                OptionNames.fontsize.toString(), 12,
                Option.Shown.SETTINGS, "Application font size",
                8, 36, "updateFontSizes",
                "Application font size"));

        this.registerOption(new BooleanOption(
                OptionNames.truncateCommands2.toString(), true,
                Option.Shown.SETTINGS, "Truncate commands in tree",
                "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new IntOption(
                OptionNames.truncateCommandLength.toString(), 100,
                Option.Shown.SETTINGS, "Truncate length",
                20, 900, "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new BooleanOption(
                OptionNames.highlightBVCErrors.toString(), true,
                Option.Shown.SETTINGS, "Highlight Incomplete BVC Statements",
                "toggleHighlightBVCErrors",
                "Toggles highlighting of Incomplete BVC/ID/BVA/BVSC "
                + "tuples.  This is technically valid syntax, but discouraged "
                + "for style reasons."));

        this.registerOption(new BooleanOption(OptionNames.dragAndDroppableCode.toString(), true,
                Option.Shown.SETTINGS, "Enable Dragging & Dropping in Text",
                null,
                "Enables/Disables being able to Drag & Drop"
                + " text into text fields"));

        this.registerOption(new BooleanOption(OptionNames.leafSelectionAllowed.toString(), false,
                Option.Shown.SETTINGS, "Enable Toggling Individual Statements",
                null,
                "Enables/Disables being able to toggle individual statements"));

        this.registerOption(new BooleanOption(OptionNames.preferFullObjInOE.toString(), false,
                Option.Shown.SETTINGS, "Prefer 'full' Object Names in OE Search Field",
                null,
                "<html>When viewing dumps in Object Explorer, this will replace the search field"
                + " with the 'full' object name, including class type.<br/>"
                + "<b>Note:</b> This will limit autocomplete results to the specified type,"
                + " when the class type is present."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchActions.toString(), true,
                Option.Shown.OE, "Actions Data",
                "updateOESearchCategories",
                "Search \"Actions\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchAI.toString(), true,
                Option.Shown.OE, "AI Data",
                "updateOESearchCategories",
                "Search \"AI\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchAnimations.toString(), true,
                Option.Shown.OE, "Animations Data",
                "updateOESearchCategories",
                "Search \"Animations\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchBase.toString(), true,
                Option.Shown.OE, "Base Data",
                "updateOESearchCategories",
                "Search \"Base\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchBehaviors.toString(), true,
                Option.Shown.OE, "Behaviors Data",
                "updateOESearchCategories",
                "Search \"Behaviors\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchDialog.toString(), true,
                Option.Shown.OE, "Dialog Data",
                "updateOESearchCategories",
                "Search \"Dialog\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchKismets.toString(), true,
                Option.Shown.OE, "Kismets Data",
                "updateOESearchCategories",
                "Search \"Kismets\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchMeshes.toString(), true,
                Option.Shown.OE, "Meshes Data",
                "updateOESearchCategories",
                "Search \"Meshes\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchMissions.toString(), true,
                Option.Shown.OE, "Missions Data",
                "updateOESearchCategories",
                "Search \"Missions\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchOthers.toString(), true,
                Option.Shown.OE, "Others Data",
                "updateOESearchCategories",
                "Search \"Others\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchParticles.toString(), true,
                Option.Shown.OE, "Particles Data",
                "updateOESearchCategories",
                "Search \"Particles\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchPopulations.toString(), true,
                Option.Shown.OE, "Populations Data",
                "updateOESearchCategories",
                "Search \"Populations\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchSkins.toString(), true,
                Option.Shown.OE, "Skins Data",
                "updateOESearchCategories",
                "Search \"Skins\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchStaticMeshes.toString(), false,
                Option.Shown.OE, "<html><font color=\"#C86400\">StaticMeshes Data</font>",
                "updateOESearchCategories",
                "Search \"StaticMeshes\" classes during fulltext and refs searches.  "
                + "This package is only useful in specific circumstances, and is pretty big."));

        this.registerOption(new BooleanOption(OptionNames.oeSearchWillowData.toString(), false,
                Option.Shown.OE, "<html><font color=\"#C86400\">WillowData Data</font>",
                "updateOESearchCategories",
                "Search \"WillowData\" classes during fulltext and refs searches.  "
                + "This package is only useful in specific circumstances."));

        this.registerOption(new BooleanOption(OptionNames.saveAsOffline.toString(), true,
                Option.Shown.DANGEROUS, "Save patch files in 'Offline' Mode",
                null,
                "Save patch files in 'Offline' Mode.  This should basically always be selected!"));

        this.registerOption(new IntOption(OptionNames.onlineServiceNumber.toString(), 5,
                Option.Shown.DANGEROUS, "SparkService for 'Online'-saved Hotfixes",
                1, 99,
                null,
                "When saving patchfiles in 'Online' mode, which SparkService index should be used?"));

        // Next: options which don't show up on the settings panel.  Order
        // doesn't really matter here.
        // Has the user seen the export warning?
        this.registerOption(new BooleanOption(OptionNames.hasSeenExportWarning.toString(), false));
        this.registerOption(new BooleanOption(OptionNames.leafSelectionAllowed.toString(), false));

        // Show confirmation when checking partially checked categories?
        this.registerOption(new BooleanOption(OptionNames.showConfirmPartialCategory.toString(), true));

        // Backup session information
        this.registerOption(new IntOption(OptionNames.sessionsToKeep.toString(), 5));
        this.registerOption(new IntOption(OptionNames.backupsPerSession.toString(), 10));
        this.registerOption(new IntOption(OptionNames.secondsBetweenBackups.toString(), 120));

        // Is the left pane in OE visible?
        this.registerOption(new BooleanOption(OptionNames.OELeftPaneVisible.toString(), true));

        // Remembered geometry for various windows.
        this.registerOption(new IntOption(OptionNames.mainWindowWidth.toString(), 900));
        this.registerOption(new IntOption(OptionNames.mainWindowHeight.toString(), 630));
        this.registerOption(new BooleanOption(OptionNames.mainWindowMaximized.toString(), false));
        // Edit window is modal, and thus isn't really allowed to be maximized.
        this.registerOption(new IntOption(OptionNames.editWindowWidth.toString(), 830));
        this.registerOption(new IntOption(OptionNames.editWindowHeight.toString(), 560));
        this.registerOption(new IntOption(OptionNames.oeWindowWidth.toString(), 1150));
        this.registerOption(new IntOption(OptionNames.oeWindowHeight.toString(), 670));
        this.registerOption(new BooleanOption(OptionNames.oeWindowMaximized.toString(), false));

        //Remember previously openened files
        this.registerOption(new StringListOption(OptionNames.fileHistory.toString(), new String[]{}));

        // Remember previous import location
        this.registerOption(new FilenameOption(OptionNames.lastImport.toString(), ""));

        // Filename truncation length ("recent" menu and MainGUI window title)
        this.registerOption(new IntOption(OptionNames.filenameTruncationLength.toString(), 60));

        // Whether or not MUT coloration/notification propagates.  This is just
        // for CJ, who apparently won't shut up about it.  :)
        this.registerOption(new BooleanOption(OptionNames.propagateMUTNotification.toString(), true));

        // All of our Object Explorer Bookmarks of queries / objects
        this.registerOption(new StringListOption(OptionNames.BL2Bookmarks.toString(), new String[]{}));
        this.registerOption(new StringListOption(OptionNames.TPSBookmarks.toString(), new String[]{}));

        // The integer storing our 1-time popup messages
        this.registerOption(new IntOption(OptionNames.popupStatus.toString(), 0));

        // A flag determining if we show the hotfix naming checkbox
        this.registerOption(new BooleanOption(OptionNames.showHotfixNames.toString(), true));

        // A flag for if we disabled delete messages.
        this.registerOption(new BooleanOption(OptionNames.showDeleteConfirmation.toString(), true));

        // Timestamp of the datalib DB/Jar files when they was last successfully verified
        this.registerOption(new LongOption(OptionNames.oeDataSuccessTimestampDbBL2.toString(), 0));
        this.registerOption(new LongOption(OptionNames.oeDataSuccessTimestampDbTPS.toString(), 0));
        this.registerOption(new LongOption(OptionNames.oeDataSuccessTimestampJarBL2.toString(), 0));
        this.registerOption(new LongOption(OptionNames.oeDataSuccessTimestampJarTPS.toString(), 0));

        // Finally: a bit of aggregation housekeeping
        this.updateOESearchCategories();
    }

    /**
     * Registers an Option with ourselves.
     *
     * @param newOption The new Option to set
     * @return
     */
    public final boolean registerOption(Option newOption) {
        if (OPTION_MAP.containsKey(newOption.getName())) {
            return false;
        } else {
            OPTION_MAP.put(newOption.getName(), newOption);
            optionOrder.add(newOption.getName());
            return true;
        }
    }

    /**
     * Retrieves an Option, given an OptionNames enum entry.
     *
     * @param name The name of the option to return
     * @return The option
     */
    public Option getOption(OptionNames name) {
        return this.getOption(name.toString());
    }

    /**
     * Retrieves an Option, given its name.
     *
     * @param name The name of the option to return
     * @return The option
     */
    public Option getOption(String name) {
        return OPTION_MAP.getOrDefault(name, null);
    }

    /**
     * Returns an ArrayList of Options in the order in which they should be
     * displayed on the settings screen. Will omit any Option objects which are
     * not for display.
     *
     * @param shownPanel The panel to be rendered
     * @return The ArrayList of Options.
     */
    public ArrayList<Option> getDisplayedOptionList(Option.Shown shownPanel) {
        ArrayList<Option> retList = new ArrayList<>();
        Option o;
        for (String key : this.optionOrder) {
            o = this.getOption(key);
            if (o != null && o.isDisplayOnPanel(shownPanel)) {
                retList.add(o);
            }
        }
        return retList;
    }

    /**
     * Loads our options from the main options file, creating a new options file
     * if one is not already found. Returns true if the options file was created
     * for the first time.
     *
     * @return
     * @throws FileNotFoundException
     */
    public static boolean loadOptions() throws FileNotFoundException {
        INSTANCE = new Options();
        File f = Paths.get(Utilities.getBLCMMDataDir(), Options.DEFAULT_FILENAME).toFile();
        if (f.exists()) {
            // If the file exists already, attempt to load it.  Don't save
            // anything out, even if we encountered errors while trying to
            // load.
            INSTANCE.loadFromFile(f);
            return false;
        } else {
            // If the file doesn't exist, create it using our defaults.
            INSTANCE.save();
            return true;
        }
    }

    /**
     * Attempts to save our options to the default filename. Returns true if the
     * save was successful, false otherwise.
     *
     * @return True if the save was successful, false otherwise.
     */
    public boolean save() {
        return this.saveToFilename(Paths.get(Utilities.getBLCMMDataDir(), Options.DEFAULT_FILENAME).toString());
    }

    /**
     * Attempts to save our options to the given filename. Returns true if the
     * save was successful, false otherwise.
     *
     * @param filename The filename to save to
     * @return True if the save was successful, false otherwise
     */
    public boolean saveToFilename(String filename) {
        try {
            Utilities.writeStringToFile(this.toString(), new File(filename));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Attempts to load our options from the given File object. If this returns
     * False, errors occurred while loading. Use getLoadErrors() to retrieve the
     * list of errors, for reporting to the user, in that case.
     *
     * @param f The File object to load from
     * @return True if we loaded without errors, or False if errors were
     * detected
     */
    public boolean loadFromFile(File f) {
        BufferedReader buffered = null;
        this.loadErrors.clear();
        try {
            buffered = new BufferedReader(new FileReader(f));
            String line = buffered.readLine();

            // If our header isn't "key,value", it's an old version of the
            // options file.  Don't bother parsing it, just cancel out.
            if (line != null && line.equals("key,value")) {
                String[] lineComponents;
                Option option;
                while ((line = buffered.readLine()) != null) {
                    lineComponents = line.split(",", 2);
                    if (lineComponents.length == 2) {
                        option = this.getOption(lineComponents[0]);
                        if (option == null) {
                            if (!IGNORE_OPTIONS.contains(lineComponents[0])) {
                                UNKNOWN_OPTIONS.put(lineComponents[0],
                                        lineComponents[1].trim());
                            }
                        } else {
                            try {
                                option.setData(option.stringToData(lineComponents[1].trim()));
                            } catch (Exception ex) {
                                this.loadErrors.add(String.format("Option '%s' could not be parsed: %s",
                                        option.getName(), ex.getMessage()));
                                Logger.getLogger(Options.class.getName()).log(Level.SEVERE,
                                        String.format("Error loading option '%s'", option.getName()),
                                        ex);
                            }
                        }
                    } else {
                        this.loadErrors.add(String.format(
                                "An invalid option line was skipped: <tt>%s</tt>",
                                line));
                    }
                }
            } else {
                if (line == null) {
                    this.loadErrors.add("Empty options file detected, restoring defaults.");
                } else {
                    this.loadErrors.add("Older-style options file detected, not reading any values.");
                }
            }
            buffered.close();
        } catch (FileNotFoundException ex) {
            this.loadErrors.add(String.format("The options file could not be found: %s",
                    ex.getMessage()));
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            this.loadErrors.add(String.format("The options file could not be loaded: %s",
                    ex.getMessage()));
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (buffered != null) {
                try {
                    buffered.close();
                } catch (IOException e) {

                }
            }
        }
        return (this.loadErrors.isEmpty());
    }

    /**
     * Determine if there were load errors during our attempt to load options.
     *
     * @return True if there were errors, False otherwise
     */
    public boolean hasLoadErrors() {
        return (!this.loadErrors.isEmpty());
    }

    /**
     * Returns an ArrayList of errors which occurred while loading options.
     *
     * @return The list of errors, for reporting to the user.
     */
    public ArrayList<String> getLoadErrors() {
        return this.loadErrors;
    }

    /**
     * Restores all our options to their default values, and saves out the
     * options file.
     *
     * @param shownPanel The panel whose values we should be resetting
     */
    public void restoreDefaults(Option.Shown shownPanel) {
        for (Option o : OPTION_MAP.values()) {
            if (o.isDisplayOnPanel(shownPanel)) {
                o.restoreDefault();
            }
        }
        this.save();
    }

    /**
     * Returns a string representation of our options.
     *
     * @return A string representing the options.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key,value\n");
        for (Option o : OPTION_MAP.values()) {
            sb.append(String.format("%s,%s\n", o.getName(), o.dataToString()));
        }
        for (Map.Entry<String, String> e : UNKNOWN_OPTIONS.entrySet()) {
            sb.append(String.format("%s,%s\n", e.getKey(), e.getValue()));
        }
        return sb.toString();
    }

    /**
     * Convenience function to get a boolean option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public boolean getBooleanOptionData(OptionNames optionName) {
        return this.getBooleanOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a boolean option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setBooleanOptionData(OptionNames optionName, boolean optionValue) {
        this.setBooleanOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get an integer option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public int getIntOptionData(OptionNames optionName) {
        return this.getIntOptionData(optionName.toString());
    }

    /**
     * Convenience function to set an integer option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setIntOptionData(OptionNames optionName, int optionValue) {
        this.setIntOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get a long option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public long getLongOptionData(OptionNames optionName) {
        return this.getLongOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a long option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setLongOptionData(OptionNames optionName, long optionValue) {
        this.setLongOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get a filename option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public String getFilenameOptionData(OptionNames optionName) {
        return this.getFilenameOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a filename option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param newFile A File object describing the new filename
     */
    public void setFilenameOptionData(OptionNames optionName, File newFile) {
        this.setFilenameOptionData(optionName.toString(), newFile);
    }

    /**
     * Convenience function to get an selection option by name.
     *
     * @param <O>
     * @param optionName The option to retrieve
     * @param c
     * @return The current option data
     */
    public <O extends SelectionOptionData> O getSelectionOptionData(String optionName, Class<O> c) {
        return ((SelectionOption<O>) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set an selection option by name.
     *
     * @param <O> The class of the option value
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public <O extends SelectionOptionData> void setSelectionOptionData(String optionName, O optionValue) {
        ((SelectionOption<O>) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get an selection option by OptionNames enum
     * entry.
     *
     * @param <O>
     * @param optionName The option to retrieve
     * @param c
     * @return The current option data
     */
    public <O extends SelectionOptionData> O getSelectionOptionData(OptionNames optionName, Class<O> c) {
        return this.getSelectionOptionData(optionName.toString(), c);
    }

    /**
     * Convenience function to set an selection option by OptionNames enum
     * entry.
     *
     * @param <O>
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public <O extends SelectionOptionData> void setSelectionOptionData(OptionNames optionName, O optionValue) {
        this.setSelectionOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get a boolean option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public boolean getBooleanOptionData(String optionName) {
        return ((BooleanOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a boolean option by name.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setBooleanOptionData(String optionName, boolean optionValue) {
        ((BooleanOption) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get an integer option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public int getIntOptionData(String optionName) {
        return ((IntOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set an integer option by name.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setIntOptionData(String optionName, int optionValue) {
        ((IntOption) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get a long option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public long getLongOptionData(String optionName) {
        return ((LongOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a long option by name.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setLongOptionData(String optionName, long optionValue) {
        ((LongOption) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get a filename option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public String getFilenameOptionData(String optionName) {
        return ((FilenameOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a filename option by name.
     *
     * @param optionName The option whose value to set
     * @param newFile A File object describing the new filename
     */
    public void setFilenameOptionData(String optionName, File newFile) {
        ((FilenameOption) this.getOption(optionName)).setData(newFile.getAbsolutePath());
        this.save();
    }

    /**
     * Convenience function to get a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to retrieve
     * @return The list of strings stored under this option
     */
    public String[] getStringListOptionData(OptionNames optionName) {
        return this.getStringListOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to retrieve
     * @return The list of strings stored under this option name
     */
    public String[] getStringListOptionData(String optionName) {
        return ((StringListOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to set
     * @param list A list of strings
     */
    public void setStringListOptionData(OptionNames optionName, String[] list) {
        this.setStringListOptionData(optionName.toString(), list);
    }

    /**
     * Convenience function to set a Stringlist option by name.
     *
     * @param optionName The option to set
     * @param list A list of strings
     */
    public void setStringListOptionData(String optionName, String[] list) {
        ((StringListOption) this.getOption(optionName)).setData(list);
        this.save();
    }

    // What follows are convenience functions to allow the rest of the app
    // to use some more well-defined functions for accessing/setting our
    // options.  First up: user-settable options.  Note that the "set"
    // functions all save out the file as well.
    public Theme getTheme() {
        return this.getSelectionOptionData(OptionNames.theme, Theme.class);
    }

    public void setTheme(Theme theme) {
        this.setSelectionOptionData(OptionNames.theme, theme);
    }

    public int getFontsize() {
        return this.getIntOptionData(OptionNames.fontsize);
    }

    public void setFontSize(int fontsize) {
        this.setIntOptionData(OptionNames.fontsize, fontsize);
    }

    public boolean getCheckForNewVersions() {
        return this.getBooleanOptionData(OptionNames.checkForNewVersions);
    }

    public void setCheckForNewVersions(boolean checkVal) {
        this.setBooleanOptionData(OptionNames.checkForNewVersions, checkVal);
    }

    public boolean getHighlightBVCErrors() {
        return this.getBooleanOptionData(OptionNames.highlightBVCErrors);
    }

    public void setHighlightBVCErrors(boolean highlightErrors) {
        this.setBooleanOptionData(OptionNames.highlightBVCErrors, highlightErrors);
    }

    public boolean getTruncateCommands() {
        return this.getBooleanOptionData(OptionNames.truncateCommands2);
    }

    public void setTruncateCommands(boolean truncateCommands) {
        this.setBooleanOptionData(OptionNames.truncateCommands2, truncateCommands);
    }

    public int getTruncateCommandLength() {
        return this.getIntOptionData(OptionNames.truncateCommandLength);
    }

    public void setTruncateCommandLength(int truncateCommandLength) {
        this.setIntOptionData(OptionNames.truncateCommandLength, truncateCommandLength);
    }

    public boolean getLeafSelectionAllowed() {
        return this.getBooleanOptionData(OptionNames.leafSelectionAllowed);
    }

    public void setLeafSelectionAllowed(boolean selected) {
        this.setBooleanOptionData(OptionNames.leafSelectionAllowed, selected);
    }

    public boolean getPreferFullObjInOE() {
        return this.getBooleanOptionData(OptionNames.preferFullObjInOE);
    }

    public void setPreferFullObjInOE(boolean newPref) {
        this.setBooleanOptionData(OptionNames.preferFullObjInOE, newPref);
    }

    // Next up: non-user-settable options.  Doing gets/sets for these even
    // though only getters make sense for most of them.
    public boolean getHasSeenExportWarning() {
        return this.getBooleanOptionData(OptionNames.hasSeenExportWarning);
    }

    public void setHasSeenExportWarning(boolean hasSeenExportWarning) {
        this.setBooleanOptionData(OptionNames.hasSeenExportWarning, hasSeenExportWarning);
    }

    public boolean getShowConfirmPartiaclCategory() {
        return this.getBooleanOptionData(OptionNames.showConfirmPartialCategory);
    }

    public void setShowConfirmPartiaclCategory(boolean b) {
        this.setBooleanOptionData(OptionNames.showConfirmPartialCategory, b);
    }

    public int getSessionsToKeep() {
        return this.getIntOptionData(OptionNames.sessionsToKeep);
    }

    public void setSessionsToKeep(int newValue) {
        this.setIntOptionData(OptionNames.sessionsToKeep, newValue);
    }

    public int getBackupsPerSession() {
        return this.getIntOptionData(OptionNames.backupsPerSession);
    }

    public void setBackupsPerSession(int newValue) {
        this.setIntOptionData(OptionNames.backupsPerSession, newValue);
    }

    public int getSecondsBetweenBackups() {
        return this.getIntOptionData(OptionNames.secondsBetweenBackups);
    }

    public void setSecondsBetweenBackups(int newValue) {
        this.setIntOptionData(OptionNames.secondsBetweenBackups, newValue);
    }

    public boolean getOELeftPaneVisible() {
        return this.getBooleanOptionData(OptionNames.OELeftPaneVisible);
    }

    public void setOELeftPaneVisible(boolean leftVisible) {
        this.setBooleanOptionData(OptionNames.OELeftPaneVisible, leftVisible);
    }

    public int getMainWindowWidth() {
        return this.getIntOptionData(OptionNames.mainWindowWidth);
    }

    public void setMainWindowWidth(int w) {
        this.setIntOptionData(OptionNames.mainWindowWidth, w);
    }

    public int getMainWindowHeight() {
        return this.getIntOptionData(OptionNames.mainWindowHeight);
    }

    public void setMainWindowHeight(int h) {
        this.setIntOptionData(OptionNames.mainWindowHeight, h);
    }

    public boolean getMainWindowMaximized() {
        return this.getBooleanOptionData(OptionNames.mainWindowMaximized);
    }

    public void setMainWindowMaximized(boolean maximized) {
        this.setBooleanOptionData(OptionNames.mainWindowMaximized, maximized);
    }

    public int getEditWindowWidth() {
        return this.getIntOptionData(OptionNames.editWindowWidth);
    }

    public void setEditWindowWidth(int w) {
        this.setIntOptionData(OptionNames.editWindowWidth, w);
    }

    public int getEditWindowHeight() {
        return this.getIntOptionData(OptionNames.editWindowHeight);
    }

    public void setEditWindowHeight(int h) {
        this.setIntOptionData(OptionNames.editWindowHeight, h);
    }

    public int getOEWindowWidth() {
        return this.getIntOptionData(OptionNames.oeWindowWidth);
    }

    public void setOEWindowWidth(int w) {
        this.setIntOptionData(OptionNames.oeWindowWidth, w);
    }

    public int getOEWindowHeight() {
        return this.getIntOptionData(OptionNames.oeWindowHeight);
    }

    public void setOEWindowHeight(int h) {
        this.setIntOptionData(OptionNames.oeWindowHeight, h);
    }

    public boolean getOEWindowMaximized() {
        return this.getBooleanOptionData(OptionNames.oeWindowMaximized);
    }

    public void setOEWindowMaximized(boolean maximized) {
        this.setBooleanOptionData(OptionNames.oeWindowMaximized, maximized);
    }

    public String[] getFileHistory() {
        return this.getStringListOptionData(OptionNames.fileHistory);
    }

    public void setFileHistory(String[] history) {
        this.setStringListOptionData(OptionNames.fileHistory, history);
    }

    public String getLastImport() {
        return this.getFilenameOptionData(OptionNames.lastImport);
    }

    public void setLastImport(File newImport) {
        this.setFilenameOptionData(OptionNames.lastImport, newImport);
    }

    public int getFilenameTruncationLength() {
        return this.getIntOptionData(Options.OptionNames.filenameTruncationLength);
    }

    public void setFilenameTruncationLength(int newLength) {
        this.setIntOptionData(Options.OptionNames.filenameTruncationLength, newLength);
    }

    public boolean getPropagateMUTNotification() {
        return this.getBooleanOptionData(Options.OptionNames.propagateMUTNotification);
    }

    public void setPropagateMUTNotification(boolean propagate) {
        this.setBooleanOptionData(Options.OptionNames.propagateMUTNotification, propagate);
    }

    /**
     * Returns our Object Explorer bookmarks for the specified PatchType
     *
     * @param patch The game for which to get bookmarks
     * @return The bookmarks
     */
    public String[] getOEBookmarks(PatchType patch) {
        return this.getStringListOptionData(OptionNames.valueOf(patch.toString() + "Bookmarks"));
    }

    /**
     * Sets the bookmarks for the given PatchType
     *
     * @param bookmarks The bookmarks to set
     * @param patch The game for which to set bookmarks
     */
    public void setOEBookmarks(String[] bookmarks, PatchType patch) {
        this.setStringListOptionData(OptionNames.valueOf(patch.toString() + "Bookmarks"), bookmarks);
    }

    public int getPopupStatus() {
        return this.getIntOptionData(OptionNames.popupStatus);
    }

    public void setPopupStatus(int status) {
        this.setIntOptionData(Options.OptionNames.popupStatus, status);
    }

    public boolean getShowHotfixNames() {
        return this.getBooleanOptionData(OptionNames.showHotfixNames);
    }

    public void setShowHotfixNames(boolean status) {
        this.setBooleanOptionData(Options.OptionNames.showHotfixNames, status);
    }

    public boolean getDragAndDropEnabled() {
        return this.getBooleanOptionData(OptionNames.dragAndDroppableCode);
    }

    public void setDragAndDroppableCode(boolean status) {
        this.setBooleanOptionData(OptionNames.dragAndDroppableCode, status);
    }

    public boolean getShowDeletionConfirm() {
        return this.getBooleanOptionData(OptionNames.showDeleteConfirmation);
    }

    public void setShowDeleteConfirmation(boolean status) {
        this.setBooleanOptionData(OptionNames.showDeleteConfirmation, status);
    }

    public boolean getSaveAsOffline() {
        return this.getBooleanOptionData(OptionNames.saveAsOffline);
    }

    public void setSaveAsOffline(boolean saveAsOffline) {
        this.setBooleanOptionData(OptionNames.saveAsOffline, saveAsOffline);
    }

    public int getOnlineServiceNumber() {
        return this.getIntOptionData(OptionNames.onlineServiceNumber);
    }

    public void setOnlineServiceNumber(int serviceNumber) {
        this.setIntOptionData(Options.OptionNames.onlineServiceNumber, serviceNumber);
    }

    public long getOEDataSuccessTimestampDbBL2() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampDbBL2);
    }

    public void setOEDataSuccessTimestampDbBL2(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampDbBL2, newTimestamp);
    }

    public long getOEDataSuccessTimestampDbTPS() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampDbTPS);
    }

    public void setOEDataSuccessTimestampDbTPS(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampDbTPS, newTimestamp);
    }

    public long getOEDataSuccessTimestampJarBL2() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampJarBL2);
    }

    public void setOEDataSuccessTimestampJarBL2(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampJarBL2, newTimestamp);
    }

    public long getOEDataSuccessTimestampJarTPS() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampJarTPS);
    }

    public void setOEDataSuccessTimestampJarTPS(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampJarTPS, newTimestamp);
    }

    public final void updateOESearchCategories() {
        this.activeSearchCategories.clear();
        for (OESearch oeSearch : OESearch.values()) {
            if (this.getBooleanOptionData(oeSearch.option)) {
                this.activeSearchCategories.add(oeSearch);
            }
        }
    }

    public Set<OESearch> getOESearchCategories() {
        return this.activeSearchCategories;
    }
}
