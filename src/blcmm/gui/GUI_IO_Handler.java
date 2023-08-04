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
package blcmm.gui;

import blcmm.gui.components.AdHocDialog;
import blcmm.gui.components.ProgressDialog;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.Category;
import blcmm.model.CompletePatch;
import blcmm.model.ModelElement;
import blcmm.model.PatchIO;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.ImportAnomalyLog;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 *
 * @author LightChaosman
 */
public class GUI_IO_Handler {

    public static MainGUI MASTER_UI;
    public static ProgressDialog progressMeter = null;
    public static FontInfo fontInfo;

    /**
     * Given a "modname" read from the mod itself, and the mod's filename,
     * return an appropriate name for the mod in our tree.
     *
     * @param modname The mod name, read from the root category of the mod
     * itself (which will default to "patch" if there is no root category)
     * @param filename The filename (without path) where the mod was loaded
     * from.
     * @return An appropriate name for the mod in the tree
     */
    private static String getModName(String modname, String filename) {

        // If we were to set the root category name based on the filename,
        // figure out what that name should be (basically just the filename
        // minus extension, though we do some finagling to try and detect
        // things which aren't actually extensions).
        String filenameBasedName = filename;
        if (filename.contains(".")) {
            int idx = filename.lastIndexOf(".");
            String pre = filename.substring(0, idx);
            String post = filename.substring(idx + 1);
            filenameBasedName = post.contains(" ") ? filename : pre;
        }

        // If our loaded root category name was "patch", "hotfixes", or "root",
        // switch to using the filename-based name.
        if (modname.equalsIgnoreCase(Category.DEFAULT_ROOT_NAME)
                || modname.equalsIgnoreCase("hotfixes")
                || modname.equalsIgnoreCase("patch")) {
            modname = filenameBasedName;
        }

        // Now, if our current mod name is different than the filename-
        // based name, do a few extra checks.
        String strippedModname = modname.replaceAll("[^0-9a-zA-Z]", "");
        String strippedFilename = filenameBasedName.replaceAll("[^0-9a-zA-Z]", "");
        if (!strippedModname.equalsIgnoreCase(strippedFilename)) {
            LevenshteinDistance ld = new LevenshteinDistance();
            int dist = ld.apply(strippedModname.toLowerCase(), strippedFilename.toLowerCase());
            if (dist < 4) {
                if (strippedModname.length() < strippedFilename.length()) {//mod name is more or less contained in file name
                    // Use our filenameBasedName
                    modname = filenameBasedName;
                } else {// Keep our modname
                }
            } else {
                modname = String.format("%s (imported from %s)", modname, filename);
            }
        }

        // Finally, return
        return modname;
    }

    static CompletePatch parseFile(File f) {
        CompletePatch newpatch = null;
        try {
            newpatch = PatchIO.parse(f);
        } catch (Exception ex) {
            GlobalLogger.log(ex);
            String message = ex.getMessage();
            if (message == null || message.equals("")) {
                message = ex.toString();
            }
            // Really we probably shouldn't show this at all, because our
            // import results dialog will show us what went wrong anyway.
            // Still, may as well show it anyway while doing a single import.
            if (progressMeter == null) {
                AdHocDialog.run(MASTER_UI,
                        fontInfo,
                        AdHocDialog.IconType.ERROR,
                        "Error opening file",
                        "<html>Unable to open file <tt>" + f.getName() + "</tt><br/><br/>"
                        +"<blockquote>" + message + "</blockquote>");
            }
        }
        return newpatch;
    }

    private static CompletePatch parseString(String string) {
        CompletePatch newpatch = null;
        try {
            newpatch = PatchIO.parse(string);
        } catch (Exception ex) {
            Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, null, ex);
            String message = ex.getMessage();
            if (message == null || message.equals("")) {
                message = ex.toString();
            }
            // Really we probably shouldn't show this at all, because our
            // import results dialog will show us what went wrong anyway.
            // Still, may as well show it anyway while doing a single import.
            if (progressMeter == null) {
                AdHocDialog.run(MASTER_UI,
                        fontInfo,
                        AdHocDialog.IconType.ERROR,
                        "Error opening file",
                        "<html>Unable to parse string: <tt>" + string + "</tt><br/><br/>"
                        + "<blockquote>" + message + "</blockquote>");
            }
        }
        return newpatch;
    }

    private static Category getOrCreateModsCategory(CompletePatch patch) {
        Category mods = null;
        for (ModelElement c : patch.getRoot().getElements()) {
            if (c instanceof Category && ((Category) c).getName().equals("mods")) {
                mods = (Category) c;
                break;
            }
        }
        if (mods == null) {
            mods = new Category("mods");
            patch.insertElementInto(mods, patch.getRoot());
            mods.getTransientData().disableStatuses();
        }
        return mods;
    }

    public static int addMods(File mods, CompletePatch patch) {
        return addMods(mods.isDirectory() ? mods.listFiles() : new File[]{mods}, patch, false);
    }

    public static int addMods(File mods, CompletePatch patch, Category parentOfMods) {
        return addMods(mods, patch, parentOfMods, parentOfMods.sizeIncludingHotfixes(), false);
    }

    public static int addMods(File mods, CompletePatch patch, Category parentOfMods, int insertIndex) {
        return addMods(mods.isDirectory() ? mods.listFiles() : new File[]{mods}, patch, parentOfMods, insertIndex, false);
    }

    public static int addMods(File[] mods, CompletePatch patch) {
        Category modsCategory = getOrCreateModsCategory(patch);
        return addMods(mods, patch, modsCategory, false);
    }

    public static int addMods(File[] mods, CompletePatch patch, Category parentOfMods) {
        return addMods(mods, patch, parentOfMods, parentOfMods.sizeIncludingHotfixes(), false);
    }

    public static int addMods(File mods, CompletePatch patch, boolean deselectAll) {
        return addMods(mods.isDirectory() ? mods.listFiles() : new File[]{mods}, patch, deselectAll);
    }

    public static int addMods(File mods, CompletePatch patch, Category parentOfMods, boolean deselectAll) {
        return addMods(mods, patch, parentOfMods, parentOfMods.sizeIncludingHotfixes(), deselectAll);
    }

    public static int addMods(File mods, CompletePatch patch, Category parentOfMods, int insertIndex, boolean deselectAll) {
        return addMods(mods.isDirectory() ? mods.listFiles() : new File[]{mods}, patch, parentOfMods, insertIndex, deselectAll);
    }

    public static int addMods(File[] mods, CompletePatch patch, boolean deselectAll) {
        Category modsCategory = getOrCreateModsCategory(patch);
        return addMods(mods, patch, modsCategory);
    }

    public static int addMods(File[] mods, CompletePatch patch, Category parentOfMods, boolean deselectAll) {
        return addMods(mods, patch, parentOfMods, parentOfMods.sizeIncludingHotfixes(), deselectAll);
    }

    public static int addMods(File[] mods, CompletePatch patch, Category parentOfMods, int insertIndex) {
        return addMods(mods, patch, parentOfMods, insertIndex, false);
    }

    /**
     * This is the final public entry point for adding mods. All the other
     * addMods() definitions bubble up to here. This method will do an initial
     * directory scan to find out how many files we expect to try and process,
     * set up a progress bar to show to the user, and then call out to the
     * private addModsLoop() to actually run the imports.
     *
     * @param mods List of files to import
     * @param patch The patch in which the imported mods should go
     * @param parentOfMods What category inside the patch to put the mods
     * @param insertIndex What index of the category to place the mods
     * @param deselectAll Whether or not to deselect all the imported mods
     * @return The number of mods actually imported
     */
    public static int addMods(File[] mods, CompletePatch patch, Category parentOfMods, int insertIndex, boolean deselectAll) {
        int fileCount = getFileCount(mods);
        if (fileCount > 0) {

            // **********************
            // NOTE/WARNING
            // With these changes to using SwingWorker to load the mods, which
            // is what lets us have a modal dialog progressbar when loading
            // multiple mods, apparently other dialogs *cannot* be shown
            // during the addModsLoop() processing unless the ProgressDialog
            // is present.  It's weird.  If the dialog isn't there, the other
            // dialogs don't actually render; you just get an empty window.
            // Our previous behavior was to only show the dialog when importing
            // more than one file, and I'd planned on restricting that even
            // further, but for NOW, thanks to this behavior, I'm just letting
            // the ProgressDialog show all the time.  This is subpar, of course,
            // and I'd love to figure out why that's happening, 'cause it'd be
            // nice to A) not have to have the ProgressDialog sometimes, and
            // B) I'd like to know why this is the case.
            //
            // To reproduce what I mean, comment out the ProgressDialog
            // creation immediately below and try importing a file which will
            // either trigger an error dialog in parseFile, or a file whose
            // top-level category is named "mods" so it triggers the dialog
            // in addModInternal.
            // NOTE/WARNING
            // **********************
            // In order to display a modal dialog with a progress meter, either
            // the progress dialog has to be in another thread (or something),
            // or the bit which does the work has to be in a SwingWorker (or
            // something.  So, we'll do that.
            progressMeter = new ProgressDialog(MASTER_UI, fileCount,
                    "Processing %s file%s", "", "s",
                    "Processing file %d/%d");

            // We'll import in a SwingWorker even if we only have a single mod
            // to import, so everything's nice and consistent.
            SwingWorker worker = new SwingWorker() {
                @Override
                protected Integer doInBackground() {
                    return addModsLoop(mods, patch, parentOfMods, insertIndex, deselectAll);
                }

                @Override
                protected void done() {
                    disposeProgressMeter();
                }
            };
            worker.execute();

            // If we're using a progress meter, note that the call to
            // setVisible() here will block the application at that point until
            // the dialog returns.  (Which is why we need the SwingWorker,
            // above.)
            if (progressMeter != null) {
                progressMeter.setVisible(true);
            }

            // Now get the result out of the worker.  The call to get() will
            // block until the worker's background tasks are done, though that
            // will only ever happen if we're *not* using a progressMeter,
            // because the dialog visibility will have blocked until the
            // worker's done, anyway.
            try {
                int retval = (Integer) worker.get();
                if (retval > 0) {
                    // Because of how we're threading this, if we make these
                    // two changes with every mod which gets imported (as
                    // happened originally, before SwingWorker), the tree will
                    // get visibly updated with *every* mod import, which is
                    // both slow and annoying.  So we're delaying until now,
                    // if we can.  When only a single mod is importing, this
                    // is probably duplicating effort already done in addMod2.
                    MASTER_UI.SetUIModel(patch);
                    MASTER_UI.getTree().setChanged(true);
                }
                return retval;
            } catch (InterruptedException | ExecutionException ex) {
                disposeProgressMeter();
                // We can't be sure if these steps are needed, but it'll be
                // better to potentially do a NOOP here than have it not update
                // when it should.
                MASTER_UI.SetUIModel(patch);
                MASTER_UI.getTree().setChanged(true);
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * If we're using a progress meter, dispose of it.
     */
    private static void disposeProgressMeter() {
        if (progressMeter != null) {
            progressMeter.dispose();
            progressMeter = null;
        }
    }

    /**
     * Loops through the given File objects and counts how many individual files
     * there are (recursively)
     *
     * @param mods The list of File objects to recurse through
     * @return The number of files found
     */
    private static int getFileCount(File[] mods) {
        int count = 0;
        for (File f : mods) {
            if (f.isDirectory()) {
                count += getFileCount(f.listFiles());
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Main file-based mod import procedure. Will loop back on itself to
     * recursively loop through directories as they're detected.
     *
     * @param mods A list of File objects to import (could be files and/or dirs)
     * @param patch The patch to import everything into
     * @param parentOfMods The Category in which to put all the mods
     * @param insertIndex The index in the category to place the mods
     * @param deselectAll Whether or not to deselect the mods when importing
     * @return
     */
    private static int addModsLoop(File[] mods, CompletePatch patch,
            Category parentOfMods, int insertIndex, boolean deselectAll) {
        int count = 0;
        // For some reason we have to sort these backwards?
        Arrays.sort(mods, (f1, f2) -> f2.getName().compareToIgnoreCase(f1.getName()));
        for (File f : mods) {
            if (f.isDirectory()) {
                Category newLocation = null;
                for (ModelElement c : parentOfMods.getElements()) {
                    if (c instanceof Category && ((Category) c).getName().equals(f.getName())) {
                        newLocation = (Category) c;
                        break;
                    }
                }
                if (newLocation == null) {
                    newLocation = new Category(f.getName());
                    patch.insertElementInto(newLocation, parentOfMods);
                }
                count += addModsLoop(f.listFiles(), patch, newLocation, newLocation.sizeIncludingHotfixes(), deselectAll);
            } else {
                String name = f.getName();
                if (name.endsWith(".py")) {
                    ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(name,
                            ImportAnomalyLog.ImportAnomalyType.PythonFile,
                            name + " is a Python script (possibly an SDK mod), and not managed via BLCMM",
                            null
                    ));
                } else {
                    if (!name.endsWith(".rar") && !name.endsWith(".jar") && !name.endsWith(".exe") && !name.endsWith(".pdf")) {
                        int newCount = addSingleMod(f, patch, parentOfMods, insertIndex, deselectAll);
                        count += newCount;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Adds the single mod at the given File, inside the specified Category.
     * This will modify the imported root category name if necessary to include
     * the filename it was imported from, if the category name is different
     * enough. It will also check to see if the top-level category is named
     * "mods", and if so, will prompt the user whether to import those
     * subcategories individually, or continue with nesting a new "mods" folder.
     *
     * @param string The string containing the to-be-imported mod.
     * @param containingPatch
     * @param whereToPutMod The Category where the imported mod should go
     * @param index
     * @param deselectAll
     * @return The number of mods successfully imported (generally zero or one,
     * though if a multi-mod mod file is detected, it could be more)
     */
    public static int addStringMod(String string, CompletePatch containingPatch, Category whereToPutMod, int index, boolean deselectAll) {
        CompletePatch mod = parseString(string);
        return addModInternal(mod, containingPatch, whereToPutMod, Category.DEFAULT_ROOT_NAME, index, deselectAll);
    }

    /**
     * Adds the single mod at the given File, inside the specified Category.
     * This will modify the imported root category name if necessary to include
     * the filename it was imported from, if the category name is different
     * enough. It will also check to see if the top-level category is named
     * "mods", and if so, will prompt the user whether to import those
     * subcategories individually, or continue with nesting a new "mods" folder.
     *
     * @param file The file to load from
     * @param containingPatch
     * @param whereToPutMod The Category where the imported mod should go
     * @return The number of mods successfully imported (generally zero or one,
     * though if a multi-mod mod file is detected, it could be more)
     */
    private static int addSingleMod(File file, CompletePatch containingPatch, Category whereToPutMod, int insertIndex, boolean deselectAll) {
        if (progressMeter != null) {
            progressMeter.incrementProgress("<html><tt>" + file.getName() + "</tt>");
        }
        CompletePatch mod = parseFile(file);
        return addModInternal(mod, containingPatch, whereToPutMod, file.getName(), insertIndex, deselectAll);
    }

    private static int addModInternal(CompletePatch modPatch, CompletePatch containingPatch, Category whereToPutMod, String filename, int insertIndex, boolean deselectAll) {
        if (insertIndex > whereToPutMod.sizeIncludingHotfixes()) {
            insertIndex = whereToPutMod.sizeIncludingHotfixes();
        }
        if (modPatch != null) {

            // Set our initial modname.
            String modname = modPatch.getRoot().getName();

            // If our initial modname is "mods", then process things a bit
            // differently.
            if (modname.equals("mods")) {

                List<ModelElement> elements = modPatch.getRoot().getElements();
                if (elements.size() == 1 && elements.get(0) instanceof Category) {
                    // If we have exactly one Category in "mods", process just
                    // like that subcategory was our root mod.
                    CompletePatch subMod = new CompletePatch();
                    subMod.setRoot((Category) elements.get(0));
                    modPatch = subMod;
                    modname = modPatch.getRoot().getName();
                } else {
                    int choice = AdHocDialog.run(MASTER_UI,
                            fontInfo,
                            AdHocDialog.IconType.QUESTION,
                            "Import as multiple mods, or a single category?",
                            "<html>The selected mod file looks like it contains multiple mods exported at once.<br/><br/>"
                            + "Import them individually, or into a new \"mods\" category?",
                            new String[] {"Import Individually", "Import as Category", "Cancel"},
                            0);
                    switch (choice) {

                        case 0:
                            // If we've got more than that (or zero), loop through and
                            // import, completely disregarding the filename.
                            int added = 0;
                            // We can't do a fancier "for (ModelElement el : elements)"
                            // loop here because of Reasons?  Get some kind of Concurrent
                            // Update exception, so something ends up modifying the list.
                            for (int i = 0; i < elements.size(); i++) {
                                ModelElement el = elements.get(i);
                                if (el instanceof Category) {
                                    CompletePatch subMod = new CompletePatch();
                                    subMod.setRoot((Category) el);
                                    if (addMod2(subMod,
                                            containingPatch,
                                            whereToPutMod,
                                            subMod.getRoot().getName(),
                                            insertIndex,
                                            deselectAll)) {
                                        added++;
                                    }
                                } else {
                                    containingPatch.insertElementInto(el, whereToPutMod, insertIndex);
                                    MASTER_UI.SetUIModel(containingPatch);
                                    MASTER_UI.getTree().setChanged(true);
                                    added++;
                                }
                            }
                            return added;

                        case 1:
                            // Just continue on, import as a new "mods" folder
                            break;

                        default:
                            // User cancelled import
                            return 0;
                    }
                }

            }

            // Transform it, if needed, based on the filename
            modname = getModName(modname, filename);

            // Set the new mod name if we need to.
            if (!modname.equals(modPatch.getRoot().getName())) {
                modPatch.getRoot().setName(modname);
            }

            // Put this mod into the tree
            if (addMod2(modPatch, containingPatch, whereToPutMod, modname, insertIndex, deselectAll)) {
                return 1;
            } else {
                return 0;
            }
        }
        ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(filename,
                ImportAnomalyLog.ImportAnomalyType.ParseError,
                "Mod could not be parsed.", null));
        return 0;
    }

    private static boolean addMod2(CompletePatch mod, CompletePatch containingPatch, Category whereToPutMod, String modname, int insertIndex, boolean deselectAll) {
        mod.deleteAllProfilesAndReplaceCurrentProfileWith(containingPatch.getCurrentProfile());
        if (deselectAll) {
            mod.deselectAll();
        }
        for (ModelElement c : whereToPutMod.getElements()) {
            if (c instanceof Category && ((Category) c).getName().equals(modname)) {
                ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(((Category) c).getName(),
                        ImportAnomalyLog.ImportAnomalyType.AlreadyExists,
                        "Mod already exists in mods folder", mod));
                return false;
            }

        }
        if (mod.getRoot().getTransientData().getNumberOfOccurences(GlobalListOfProperties.LeafTypeCommandChecker.class) == 0) {
            ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(modname,
                    ImportAnomalyLog.ImportAnomalyType.NoCommands,
                    "Does not contain any actual mod statements", mod));
            return false;
        }
        if (mod.getPatchSource() == CompletePatch.PatchSource.BLCMM
                && mod.getType() != containingPatch.getType()) {
            ImportAnomalyLog.INSTANCE.add(new ImportAnomalyLog.importAnomaly(modname,
                    ImportAnomalyLog.ImportAnomalyType.DifferentType,
                    String.format("Importing a %s mod into a %s patch file", mod.getType(), containingPatch.getType()), mod));
        }
        containingPatch.insertElementInto(mod.getRoot(), whereToPutMod, insertIndex);
        if (progressMeter == null) {
            // Don't do this when we're using a progress meter, because it
            // causes the tree to visibly re-draw with each imported mod.
            MASTER_UI.SetUIModel(containingPatch);
            MASTER_UI.getTree().setChanged(true);
        }
        return true;
    }

    /**
     * Imports a list of mods which have been confirmed by the user -- this can
     * be a mixture of existing mods which will now be overwritten, and mods
     * which don't actually contain any statements (likely to be a README or
     * something, but hey, the user okayed it). Will return the number of mods
     * imported, which should always be the same as the number of mods passed
     * in.
     *
     * @param mods The list of mods to import
     * @param patch The patch in which the mods will be imported
     * @param modCat The category in which to import the mods. If null, this
     * will default to the main "mods" Category.
     * @return The number of mods imported.
     */
    private static int importAfterUserConfirm(ArrayList<CompletePatch> mods,
            CompletePatch patch, Category modCat) {
        int imported = 0;

        // Construct a HashMap of indexes to all the mods we have
        HashMap<String, Integer> indexMap = new HashMap<>();
        HashMap<String, ModelElement> elementMap = new HashMap<>();
        if (modCat == null) {
            modCat = getOrCreateModsCategory(patch);
        }
        List<ModelElement> modElements = modCat.getElements();
        for (int idx = 0; idx < modElements.size(); idx++) {
            ModelElement element = modElements.get(idx);
            String name = element instanceof Category ? ((Category) element).getName() : element.toString();
            indexMap.put(name, idx);
            elementMap.put(name, element);
        }

        // Now loop through the mods that we're importing and replace them.
        for (CompletePatch mod : mods) {
            String modName = mod.getRoot().getName();
            if (indexMap.containsKey(modName)) {
                // We found a mod by the same name, so we're overwriting it.
                int index = indexMap.get(modName);
                patch.removeElementFromParentCategory(elementMap.get(modName));
                patch.insertElementInto(mod.getRoot(), modCat, index);
                imported++;
            } else {
                // If we didn't find the mod already, just import it as usual,
                // at the end of the list.
                patch.insertElementInto(mod.getRoot(), modCat, modCat.sizeIncludingHotfixes());
                imported++;
            }
        }

        // Clean up and return.
        if (imported > 0) {
            MASTER_UI.SetUIModel(patch);
            MASTER_UI.getTree().setChanged(true);
        }
        return imported;
    }

    public static void reportImportResults(int number, boolean open, CompletePatch patch) {
        reportImportResults(number, open, patch, null);
    }

    /**
     * Reports on import results. If a non-null and non-empty message is passed
     * in, it will always be presented to the user. If errors were encountered
     * during the process, these will be shown afterwards. Will clear out the
     * importErrors variable after being run.
     *
     * @param number The number of successfully imported mods
     * @param open true iff this was called from opening a file, rather than
     * importing mods.
     * @param patch the master patch that has been imported to
     * @param parent the category in which the mods were placed (will get passed
     * on, if the user chooses to import after being given an option here)
     */
    public static void reportImportResults(int number, boolean open,
            CompletePatch patch, Category parent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        String dialogTitle = "Mod " + (open ? "Open" : "Import") + " Results";
        AdHocDialog.IconType dialogIcon = AdHocDialog.IconType.INFORMATION;
        boolean showDialog = false;
        ArrayList<CompletePatch> canOverwrite = new ArrayList<>();
        ArrayList<CompletePatch> canImportNoCommands = new ArrayList<>();

        // If we have a message, always show the dialog (and the message)
        if (number > 1) {
            showDialog = true;
            sb.append("Imported " + number + " mods");
            MASTER_UI.getTimedLabel().showTemporary("Successfully imported " + number + " mods", ThemeManager.getColor(ThemeManager.ColorType.UIText));
        } else if (number == 1) {
            if (ImportAnomalyLog.INSTANCE.size() > 0 && !open) {
                sb.append("Imported 1 mod");
            }
            if (!open) {
                MASTER_UI.getTimedLabel().showTemporary("Mod successfully imported", ThemeManager.getColor(ThemeManager.ColorType.UIText));
            }
        }

        // Append any errors we might have.
        if (ImportAnomalyLog.INSTANCE.size() > 0) {
            showDialog = true;
            dialogIcon = AdHocDialog.IconType.WARNING;
            if (number > 1 || (number == 1 && !open)) {
                sb.append("<br/><br/>");
            }
            if (ImportAnomalyLog.INSTANCE.size() == 1) {
                ImportAnomalyLog.importAnomaly error = ImportAnomalyLog.INSTANCE.iterator().next();
                if (error.anomalyType.error) {
                    sb.append(String.format(
                            "The mod '<b>%s</b>' was not %s:<br/>%s",
                            error.modName, open ? "opened" : "imported", error.anomalyString));
                } else {
                    sb.append(String.format(
                            "The mod '<b>%s</b>' was %s but with a warning:<br/>%s",
                            error.modName, open ? "opened" : "imported", error.anomalyString));
                }
                switch (error.anomalyType) {
                    case AlreadyExists:
                        canOverwrite.add(error.patch);
                        break;
                    case NoCommands:
                        canImportNoCommands.add(error.patch);
                        break;
                }
                if (canOverwrite.size() > 0 || canImportNoCommands.size() > 0) {
                    sb.append("<br/><br/>");
                }
            } else {
                int idx = 0;
                int max_reported = 10;
                boolean errors = true;
                sb.append("The following mods were not imported: ");
                sb.append("<ul>");
                // We're looping through the list twice - the first time we're
                // building a display to the user (so it'll only show about
                // 10 entries), and the second time is when we're constructing
                // the actual lists that we'll use after the fact to import
                // if the user tells us to.
                for (ImportAnomalyLog.importAnomaly error : ImportAnomalyLog.INSTANCE) {
                    if (idx == max_reported) {
                        sb.append(String.format("<li><i>+%d more</i></li>",
                                ImportAnomalyLog.INSTANCE.size() - max_reported));
                        break;
                    } else {
                        if (!error.anomalyType.error && errors) {
                            sb.append("</ul>");
                            sb.append("The following mods were imported, but with warnings: ");
                            sb.append("<ul>");
                            errors = false;
                        }
                        if (idx < max_reported) {
                            sb.append(String.format("<li><b>%s</b>: %s</li>",
                                    error.modName, error.anomalyString));
                        }
                    }
                    idx++;
                }
                for (ImportAnomalyLog.importAnomaly error : ImportAnomalyLog.INSTANCE) {
                    if (error.patch != null) {
                        switch (error.anomalyType) {
                            case AlreadyExists:
                                canOverwrite.add(error.patch);
                                break;
                            case NoCommands:
                                canImportNoCommands.add(error.patch);
                                break;
                        }
                    }
                }
                sb.append("</ul>");
            }
            ImportAnomalyLog.INSTANCE.clear();
        }
        if (showDialog) {
            if (canOverwrite.size() > 0 || canImportNoCommands.size() > 0) {
                ArrayList<String> questions = new ArrayList<>();
                if (canOverwrite.size() == 1) {
                    questions.add("overwrite with the new version");
                } else if (canOverwrite.size() > 1) {
                    questions.add("overwrite existing mods with the new versions");
                }
                if (canImportNoCommands.size() == 1) {
                    questions.add("import the empty mod anyway");
                } else if (canImportNoCommands.size() > 1) {
                    questions.add("import the empty mods anyway");
                }
                String question = String.join(" and ", questions);
                sb.append(question.substring(0, 1).toUpperCase());
                sb.append(question.substring(1));
                sb.append("?  ");
                if (canOverwrite.size() > 0) {
                    sb.append("Note that overwriting a mod will discard any <br/>");
                    sb.append("changes you have made to the mod, including checking/unchecking folders.");
                }

                // The previous JOptionPane version of this dialog used custom
                // buttons to make "no" the default -- our AdHocDialog doesn't
                // set a default for Yes/No dialogs, though, so we're just using
                // the standard now.
                AdHocDialog.Button choice = AdHocDialog.run(MASTER_UI,
                        fontInfo,
                        AdHocDialog.IconType.QUESTION,
                        dialogTitle,
                        sb.toString(),
                        AdHocDialog.ButtonSet.YES_NO);
                if (choice == AdHocDialog.Button.YES) {
                    // A bit improper, but whatever.
                    canOverwrite.addAll(canImportNoCommands);
                    reportImportResults(importAfterUserConfirm(canOverwrite, patch, parent), open, patch);
                }
            } else {
                AdHocDialog.run(MASTER_UI,
                        fontInfo,
                        dialogIcon,
                        dialogTitle,
                        sb.toString());
            }
        }
    }

}
