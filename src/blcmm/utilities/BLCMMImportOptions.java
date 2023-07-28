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

import blcmm.gui.theme.Theme;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.options.BooleanOption;
import blcmm.utilities.options.FilenameOption;
import blcmm.utilities.options.IntOption;
import blcmm.utilities.options.SelectionOption;
import blcmm.utilities.options.StringListOption;
import java.io.File;
import java.nio.file.Paths;

/**
 * A class to handle reading in "vanilla" BLCMM options on a first-time
 * OpenBLCMM run, so that the user can get settings imported.
 *
 * There are a few options intentionally omitted from here.  For instance,
 * the app remembers if the user's hidden the OE left-hand panels (Class
 * Browser / Object Browser), but there's been enough changes in OE that I'd
 * prefer having that stay open for new users.
 *
 * @author apocalyptech
 */
public class BLCMMImportOptions extends OptionsBase {

    /**
     * Integer options we're copying over
     */
    private final OptionsBase.OptionNames [] intOptions = new OptionsBase.OptionNames[] {
        // Font size
        OptionsBase.OptionNames.fontsize,
        // Truncate length
        OptionsBase.OptionNames.truncateCommandLength,
        // Remembered geometry for various windows.
        OptionsBase.OptionNames.mainWindowWidth,
        OptionsBase.OptionNames.mainWindowHeight,
        OptionsBase.OptionNames.editWindowWidth,
        OptionsBase.OptionNames.editWindowHeight,
        OptionsBase.OptionNames.oeWindowWidth,
        OptionsBase.OptionNames.oeWindowHeight,
        // The integer storing our 1-time popup messages
        OptionsBase.OptionNames.popupStatus,
    };

    /**
     * Boolean options we're copying over
     */
    private final OptionsBase.OptionNames [] booleanOptions = new OptionsBase.OptionNames[] {
        // Developer Mode
        OptionsBase.OptionNames.developerMode,
        // Truncate commands in tree
        OptionsBase.OptionNames.truncateCommands2,
        // Highlight incomplete BVCs
        OptionsBase.OptionNames.highlightBVCErrors,
        // Drag-and-drop
        OptionsBase.OptionNames.dragAndDroppableCode,
        // Toggle individual statements
        OptionsBase.OptionNames.leafSelectionAllowed,
        // Has the user seen the export warning?
        OptionsBase.OptionNames.hasSeenExportWarning,
        // Remembered geometry for various windows.
        OptionsBase.OptionNames.mainWindowMaximized,
        OptionsBase.OptionNames.oeWindowMaximized,
        // A flag for if we disabled delete messages.
        OptionsBase.OptionNames.showDeleteConfirmation,
    };

    /**
     * StringList options we're copying over
     */
    private final OptionsBase.OptionNames [] stringListOptions = new OptionsBase.OptionNames[] {
        // Remember previously openened files
        OptionsBase.OptionNames.fileHistory,
        // All of our Object Explorer Bookmarks of queries / objects
        OptionsBase.OptionNames.BL2Bookmarks,
        OptionsBase.OptionNames.TPSBookmarks,
    };

    /**
     * Filename options we're copying over
     */
    private final OptionsBase.OptionNames [] filenameOptions = new OptionsBase.OptionNames[] {
        // Remember previous import location
        OptionsBase.OptionNames.lastImport,
    };

    /**
     * Construct a fresh Options object.
     *
     * @param optionsFile The file from which to load options.
     */
    public BLCMMImportOptions(File optionsFile) {
        super(optionsFile);

        // Theme is a special case, just handle it manually
        this.registerOption(new SelectionOption<>(
                this,
                // Name
                OptionNames.theme.toString(),
                // Default
                ThemeManager.getTheme("dark"),
                // Shown Panel
                null,
                // Display description
                null,
                // Callback
                null,
                // Tooltip
                null,
                // options
                ThemeManager.getAllInstalledThemes().toArray(new Theme[0]),
                // converter
                s -> {
                    Theme t = ThemeManager.getTheme(s);
                    return t == null ? ThemeManager.getTheme("dark") : t;
                }
        ));

        // Now all our more-standard ones
        for (OptionsBase.OptionNames optionName : this.intOptions) {
            this.registerOption(new IntOption(this, optionName.toString(), 0));
        }
        for (OptionsBase.OptionNames optionName : this.booleanOptions) {
            this.registerOption(new BooleanOption(this, optionName.toString(), false));
        }
        for (OptionsBase.OptionNames optionName : this.stringListOptions) {
            this.registerOption(new StringListOption(this, optionName.toString(), new String[]{}));
        }
        for (OptionsBase.OptionNames optionName : this.filenameOptions) {
            this.registerOption(new FilenameOption(this, optionName.toString(), "", null));
        }

    }

    /**
     * Writes any "set" options of our own into the specified newOptions.
     * Doesn't return any status because who cares?
     *
     * @param newOptions The options to write to.
     */
    public void writeToNewOptions(Options newOptions) {

        GlobalLogger.log("Transferring \"vanilla\" BLCMM settings to OpenBLCMM...");

        // First our custom Theme handling
        SelectionOption themeOption = (SelectionOption) this.getOption(OptionNames.theme);
        if (themeOption.hasSetData()) {
            Theme t = this.getSelectionOptionData(OptionNames.theme, Theme.class);
            GlobalLogger.log("Setting " + OptionNames.theme.name() + " to: " + t.toString());
            newOptions.setSelectionOptionData(OptionNames.theme, t);
        }

        // Now ints
        IntOption intOption;
        for (OptionsBase.OptionNames optionName : this.intOptions) {
            intOption = (IntOption) this.getOption(optionName);
            if (intOption.hasSetData()) {
                GlobalLogger.log("Setting " + optionName.name() + " to: " + intOption.dataToString());
                newOptions.setIntOptionData(optionName, intOption.getData());
            }
        }

        // Now bools
        BooleanOption booleanOption;
        for (OptionsBase.OptionNames optionName : this.booleanOptions) {
            booleanOption = (BooleanOption) this.getOption(optionName);
            if (booleanOption.hasSetData()) {
                GlobalLogger.log("Setting " + optionName.name() + " to: " + booleanOption.dataToString());
                newOptions.setBooleanOptionData(optionName, booleanOption.getData());
            }
        }

        // Now StringLists
        StringListOption stringListOption;
        for (OptionsBase.OptionNames optionName : this.stringListOptions) {
            stringListOption = (StringListOption) this.getOption(optionName);
            if (stringListOption.hasSetData()) {
                GlobalLogger.log("Setting " + optionName.name() + " to: " + stringListOption.dataToString());
                newOptions.setStringListOptionData(optionName, stringListOption.getData());
            }
        }

        // Now filenames
        FilenameOption filenameOption;
        for (OptionsBase.OptionNames optionName : this.filenameOptions) {
            filenameOption = (FilenameOption) this.getOption(optionName);
            if (filenameOption.hasSetData()) {
                GlobalLogger.log("Setting " + optionName.name() + " to: " + filenameOption.dataToString());
                newOptions.setFilenameOptionData(optionName, new File(filenameOption.getData()));
            }
        }

    }


    /**
     * Loads the "vanilla" BLCMM options file, if possible, reads the options
     * from it, and transfers the relevant entries to our new Options object.
     * Doesn't return any status because who cares?  Will log if we have
     * failures, though.
     *
     * @param newOptions The new options to load old settings into.
     */
    public static void transferToNewOptions(Options newOptions) {

        try {

            // Pull from the old BLCMM appdir, if we can find it
            String oldDir = Utilities.getAppDataDir("BLCMM");
            if (oldDir == null) {
                GlobalLogger.log("No old BLCMM data directory could be constructed, to check for old settings");
                return;
            }
            File oldFile = Paths.get(oldDir, "general.options").toFile();
            if (!oldFile.exists()) {
                GlobalLogger.log("No old BLCMM settings file exists, to check for old settings");
                return;
            }

            // If we got here, we may have some settings to look at.
            BLCMMImportOptions oldOptions = new BLCMMImportOptions(oldFile);
            oldOptions.load();
            oldOptions.writeToNewOptions(newOptions);

        } catch (Exception e) {

            // Don't bother reporting to the user except for in the logfile
            GlobalLogger.log("Error while trying to import old BLCMM settings:");
            GlobalLogger.log(e);

        }

    }

}
