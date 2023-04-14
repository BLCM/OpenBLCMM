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

import blcmm.utilities.options.BooleanOption;
import blcmm.utilities.options.FilenameOption;
import blcmm.utilities.options.IntOption;
import blcmm.utilities.options.LongOption;
import blcmm.utilities.options.Option;
import blcmm.utilities.options.SelectionOption;
import blcmm.utilities.options.SelectionOptionData;
import blcmm.utilities.options.StringListOption;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for dealing with application options.  This is a *bit* janky,
 * and was split off from the main "Options" class so that I could more
 * easily support importing some preferences from vanilla BLCMM apps, on the
 * first-time run.
 *
 * @author LightChaosman
 */
public abstract class OptionsBase {

    /**
     * A File pointing to the options file we're loading/saving.
     */
    protected File optionsFile;

    /**
     * A boolean which tells us if this options file was created during the
     * initial load.
     */
    protected boolean firstTime;

    /**
     * Enum to store the textual keys for our options. This is probably a bit of
     * overkill, but it prevents us from having to worry about keeping the same
     * string synchronized between a few different places in the code.
     *
     * I'd love to know how I could define this enum only in the *implementing*
     * classes but still be able to use it in function definitions in here,
     * but I'm not sure if that's possible.  So, even if we end up with
     * multiple Options providers, we'll still have to have a conglomerate
     * OptionNames enum in here.  Alas!
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
        AODKBookmarks,
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
        oeDataSuccessTimestampDbAODK,
        oeDataSuccessTimestampJarBL2,
        oeDataSuccessTimestampJarTPS,
        oeDataSuccessTimestampJarAODK,
    }

    /**
     * HashMap to hold our options
     */
    protected final HashMap<String, Option> OPTION_MAP = new HashMap<>();

    /**
     * ArrayList to hold the order in which we should process options
     */
    protected final ArrayList<String> optionOrder = new ArrayList<>();

    /**
     * Structure to save information about any option found in the options file
     * which we don't explicitly know about. Possibly useful if, in the future,
     * there are plugins which add options which aren't loaded yet, or
     * something. At the moment this should probably always be empty, but we're
     * keeping track regardless.
     */
    protected final HashMap<String, String> UNKNOWN_OPTIONS = new HashMap<>();

    /**
     * List of Strings which contain load errors which should be reported to the
     * user on app startup. Will be populated by load() if necessary.
     */
    protected final ArrayList<String> loadErrors = new ArrayList<>();

    /**
     * A list of old option names which shouldn't be re-saved if we encounter
     * them in the options file. This is a bit silly -- at the moment, there's
     * no reason to keep *any* option that we don't explicitly have defined in
     * the main list. If we end up allowing Plugins to store options, though, we
     * may want to hold on to option names which we don't know about, in case a
     * plugin is temporarily unavailable or something.
     */
    protected HashSet<String> IGNORE_OPTIONS = new HashSet<>();

    /**
     * Simple constructor to make sure that we've got some vars set
     *
     * @param optionsFile The file that we're reading options from.
     */
    public OptionsBase(File optionsFile) {
        this.optionsFile = optionsFile;
        this.firstTime = false;
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
     * Loads our options from our file, creating it if one is not already found.
     * Returns true if the options file was created for the first time.
     *
     * @return
     * @throws FileNotFoundException
     */
    public boolean loadOrCreate() throws FileNotFoundException {
        if (this.optionsFile.exists()) {
            // If the file exists already, attempt to load it.  Don't save
            // anything out, even if we encountered errors while trying to
            // load.
            this.load();
            this.firstTime = true;
            return false;
        } else {
            // If the file doesn't exist, create it using our defaults.
            this.save();
            return true;
        }
    }

    /**
     * Attempts to save our options. Returns true if the
     * save was successful, false otherwise.
     *
     * @return True if the save was successful, false otherwise
     */
    public boolean save() {
        try {
            Utilities.writeStringToFile(this.toString(), this.optionsFile);
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
     * @return True if we loaded without errors, or False if errors were
     * detected
     */
    public boolean load() {
        BufferedReader buffered = null;
        this.loadErrors.clear();
        try {
            buffered = new BufferedReader(new FileReader(this.optionsFile));
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

}
