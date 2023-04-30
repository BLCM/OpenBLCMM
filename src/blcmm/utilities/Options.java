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
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to control dealing with the main application options/settings.
 *
 * Sets up a static `INSTANCE` variable which a ton of stuff references
 * throughout the app.  I'd sort of like to get rid of that and just pass
 * around references to Options objects, but that's more refactoring than
 * I care to do at the moment.
 *
 * @author LightChaosman
 */
public class Options extends OptionsBase {

    public static Options INSTANCE;

    /**
     * Filename to use.
     */
    private final static String DEFAULT_FILENAME = "general.options";

    /**
     * Conglomerate enum to define the various checkboxes we have available
     * for selecting OE Search/Refs object categories.
     */
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

    /**
     * HashSet which collects the currently-active OE Search/Refs categories.
     */
    private HashSet<OESearch> activeSearchCategories = new HashSet<>();

    /**
     * Construct a fresh Options object.
     *
     * @param optionsFile The file from which to load options.
     */
    public Options(File optionsFile) {
        super(optionsFile);

        IGNORE_OPTIONS = new HashSet(Arrays.asList(
            // Old from the original BLCMM, and no longer wanted
            "contentEdits",
            "truncateCommands",
            "structuralEdits",

            // Old from during OpenBLCMM development.  Possibly not worth
            // having in here, but it'll clean out my own configs, so sure.
            // These were replaced by Db/Jar-specific options.
            "oeDataSuccessTimestampBL2",
            "oeDataSuccessTimestampTPS",

            // We don't have a launcher or splash image anymore
            "splashImage"
        ));

        // First up, for simplicity's sake: settings shown on the main
        // settings menu.  The order in which these are registered is the
        // order in which they'll show up in the panel.
        this.registerOption(new SelectionOption<>(
                this,
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
                this,
                OptionNames.checkForNewVersions.toString(), true,
                Option.Shown.SETTINGS, "Check for new versions on startup",
                "toggleCheckForNewVersions",
                "Check for new versions of " + Meta.NAME + " when the "
                + "app starts up."));

        this.registerOption(new IntOption(
                this,
                OptionNames.fontsize.toString(), 12,
                Option.Shown.SETTINGS, "Application font size",
                8, 36, "updateFontSizes",
                "Application font size"));

        this.registerOption(new BooleanOption(
                this,
                OptionNames.truncateCommands2.toString(), true,
                Option.Shown.SETTINGS, "Truncate commands in tree",
                "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new IntOption(
                this,
                OptionNames.truncateCommandLength.toString(), 100,
                Option.Shown.SETTINGS, "Truncate length",
                20, 900, "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new BooleanOption(
                this,
                OptionNames.developerMode.toString(), false,
                Option.Shown.SETTINGS, "Enable developer mode",
                "toggleDeveloperMode",
                "Enables/Disables changing actual mod code, and authoring "
                + "mods inside " + Meta.NAME + "."));

        this.registerOption(new BooleanOption(
                this,
                OptionNames.highlightBVCErrors.toString(), true,
                Option.Shown.SETTINGS, "Highlight Incomplete BVC Statements",
                "updateMainGUITreeHighlights",
                "Toggles highlighting of Incomplete BVC/ID/BVA/BVSC "
                + "tuples.  This is technically valid syntax, but discouraged "
                + "for style reasons."));

        this.registerOption(new BooleanOption(this, OptionNames.dragAndDroppableCode.toString(), true,
                Option.Shown.SETTINGS, "Enable Dragging & Dropping in Text",
                null,
                "Enables/Disables being able to Drag & Drop"
                + " text into text fields"));

        this.registerOption(new BooleanOption(this, OptionNames.leafSelectionAllowed.toString(), false,
                Option.Shown.SETTINGS, "Enable Toggling Individual Statements",
                null,
                "Enables/Disables being able to toggle individual statements"));

        this.registerOption(new BooleanOption(this, OptionNames.preferFullObjInOE.toString(), false,
                Option.Shown.SETTINGS, "Prefer 'full' Object Names in OE Search Field",
                null,
                "<html>When viewing dumps in Object Explorer, this will replace the search field"
                + " with the 'full' object name, including class type.<br/>"
                + "<b>Note:</b> This will limit autocomplete results to the specified type,"
                + " when the class type is present."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchActions.toString(), true,
                Option.Shown.OE, "Actions Data",
                "updateOESearchCategories",
                "Search \"Actions\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchAI.toString(), true,
                Option.Shown.OE, "AI Data",
                "updateOESearchCategories",
                "Search \"AI\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchAnimations.toString(), true,
                Option.Shown.OE, "Animations Data",
                "updateOESearchCategories",
                "Search \"Animations\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchBase.toString(), true,
                Option.Shown.OE, "Base Data",
                "updateOESearchCategories",
                "Search \"Base\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchBehaviors.toString(), true,
                Option.Shown.OE, "Behaviors Data",
                "updateOESearchCategories",
                "Search \"Behaviors\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchDialog.toString(), true,
                Option.Shown.OE, "Dialog Data",
                "updateOESearchCategories",
                "Search \"Dialog\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchKismets.toString(), true,
                Option.Shown.OE, "Kismets Data",
                "updateOESearchCategories",
                "Search \"Kismets\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchMeshes.toString(), true,
                Option.Shown.OE, "Meshes Data",
                "updateOESearchCategories",
                "Search \"Meshes\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchMissions.toString(), true,
                Option.Shown.OE, "Missions Data",
                "updateOESearchCategories",
                "Search \"Missions\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchOthers.toString(), true,
                Option.Shown.OE, "Others Data",
                "updateOESearchCategories",
                "Search \"Others\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchParticles.toString(), true,
                Option.Shown.OE, "Particles Data",
                "updateOESearchCategories",
                "Search \"Particles\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchPopulations.toString(), true,
                Option.Shown.OE, "Populations Data",
                "updateOESearchCategories",
                "Search \"Populations\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchSkins.toString(), true,
                Option.Shown.OE, "Skins Data",
                "updateOESearchCategories",
                "Search \"Skins\" classes during fulltext and refs searches."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchStaticMeshes.toString(), false,
                Option.Shown.OE, "<html><font color=\"#C86400\">StaticMeshes Data</font>",
                "updateOESearchCategories",
                "Search \"StaticMeshes\" classes during fulltext and refs searches.  "
                + "This package is only useful in specific circumstances, and is pretty big."));

        this.registerOption(new BooleanOption(this, OptionNames.oeSearchWillowData.toString(), false,
                Option.Shown.OE, "<html><font color=\"#C86400\">WillowData Data</font>",
                "updateOESearchCategories",
                "Search \"WillowData\" classes during fulltext and refs searches.  "
                + "This package is only useful in specific circumstances."));

        this.registerOption(new BooleanOption(this, OptionNames.saveAsOffline.toString(), true,
                Option.Shown.DANGEROUS, "Save patch files in 'Offline' Mode",
                null,
                "Save patch files in 'Offline' Mode.  This should basically always be selected!"));

        this.registerOption(new IntOption(this, OptionNames.onlineServiceNumber.toString(), 5,
                Option.Shown.DANGEROUS, "SparkService for 'Online'-saved Hotfixes",
                1, 99,
                null,
                "When saving patchfiles in 'Online' mode, which SparkService index should be used?"));

        // Next: options which don't show up on the settings panel.  Order
        // doesn't really matter here.
        // Has the user seen the export warning?
        this.registerOption(new BooleanOption(this, OptionNames.hasSeenExportWarning.toString(), false));

        // Show confirmation when checking partially checked categories?
        this.registerOption(new BooleanOption(this, OptionNames.showConfirmPartialCategory.toString(), true));

        // Backup session information
        this.registerOption(new IntOption(this, OptionNames.sessionsToKeep.toString(), 5));
        this.registerOption(new IntOption(this, OptionNames.backupsPerSession.toString(), 10));
        this.registerOption(new IntOption(this, OptionNames.secondsBetweenBackups.toString(), 120));

        // Is the left pane in OE visible?
        this.registerOption(new BooleanOption(this, OptionNames.OELeftPaneVisible.toString(), true));

        // Remembered geometry for various windows.
        this.registerOption(new IntOption(this, OptionNames.mainWindowWidth.toString(), 900));
        this.registerOption(new IntOption(this, OptionNames.mainWindowHeight.toString(), 630));
        this.registerOption(new BooleanOption(this, OptionNames.mainWindowMaximized.toString(), false));
        // Edit window is modal, and thus isn't really allowed to be maximized.
        this.registerOption(new IntOption(this, OptionNames.editWindowWidth.toString(), 830));
        this.registerOption(new IntOption(this, OptionNames.editWindowHeight.toString(), 560));
        this.registerOption(new IntOption(this, OptionNames.oeWindowWidth.toString(), 1150));
        this.registerOption(new IntOption(this, OptionNames.oeWindowHeight.toString(), 670));
        this.registerOption(new BooleanOption(this, OptionNames.oeWindowMaximized.toString(), false));

        //Remember previously openened files
        this.registerOption(new StringListOption(this, OptionNames.fileHistory.toString(), new String[]{}));

        // Remember previous import location
        this.registerOption(new FilenameOption(this, OptionNames.lastImport.toString(), ""));

        // Filename truncation length ("recent" menu and MainGUI window title)
        this.registerOption(new IntOption(this, OptionNames.filenameTruncationLength.toString(), 60));

        // Whether or not MUT coloration/notification propagates.  This is just
        // for CJ, who apparently won't shut up about it.  :)
        this.registerOption(new BooleanOption(this, OptionNames.propagateMUTNotification.toString(), true));

        // All of our Object Explorer Bookmarks of queries / objects
        this.registerOption(new StringListOption(this, OptionNames.BL2Bookmarks.toString(), new String[]{}));
        this.registerOption(new StringListOption(this, OptionNames.TPSBookmarks.toString(), new String[]{}));
        this.registerOption(new StringListOption(this, OptionNames.AODKBookmarks.toString(), new String[]{}));

        // The integer storing our 1-time popup messages
        this.registerOption(new IntOption(this, OptionNames.popupStatus.toString(), 0));

        // A flag determining if we show the hotfix naming checkbox
        this.registerOption(new BooleanOption(this, OptionNames.showHotfixNames.toString(), true));

        // A flag for if we disabled delete messages.
        this.registerOption(new BooleanOption(this, OptionNames.showDeleteConfirmation.toString(), true));

        // Timestamp of the datalib DB/Jar files when they was last successfully verified
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampDbBL2.toString(), 0));
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampDbTPS.toString(), 0));
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampDbAODK.toString(), 0));
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampJarBL2.toString(), 0));
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampJarTPS.toString(), 0));
        this.registerOption(new LongOption(this, OptionNames.oeDataSuccessTimestampJarAODK.toString(), 0));

        // Finally: a bit of aggregation housekeeping
        this.updateOESearchCategories();
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
        INSTANCE = new Options(Paths.get(Utilities.getBLCMMDataDir(), Options.DEFAULT_FILENAME).toFile());
        return INSTANCE.loadOrCreate();
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

    public boolean isInDeveloperMode() {
        return this.getBooleanOptionData(OptionNames.developerMode);
    }

    public void setDeveloperMode(boolean selected) {
        this.setBooleanOptionData(OptionNames.developerMode, selected);
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

    public long getOEDataSuccessTimestampDbAODK() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampDbAODK);
    }

    public void setOEDataSuccessTimestampDbAODK(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampDbAODK, newTimestamp);
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

    public long getOEDataSuccessTimestampJarAODK() {
        return this.getLongOptionData(OptionNames.oeDataSuccessTimestampJarAODK);
    }

    public void setOEDataSuccessTimestampJarAODK(long newTimestamp) {
        this.setLongOptionData(Options.OptionNames.oeDataSuccessTimestampJarAODK, newTimestamp);
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
