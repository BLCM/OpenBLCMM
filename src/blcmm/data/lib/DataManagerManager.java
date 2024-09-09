/*
 * Copyright (C) 2023 Christopher J. Kucera
 * <cj@apocalyptech.com>
 * <https://apocalyptech.com/contact.php>
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
 */

package blcmm.data.lib;

import blcmm.model.PatchType;
import blcmm.utilities.GlobalLogger;
import blcmm.utilities.Options;
import java.util.HashMap;

/**
 * This absurdly-named class manages the individual DataManager instances
 * which talk to game data.  The original BLCMM DataManager class was
 * effectively static, and could be toggled inbetween game data at runtime.
 * Various components could just call in to static methods in the class to
 * retrieve game data, and the data returned would depend on that state.
 *
 * In the new model, though, a DataManager is instantiated for a specific
 * game, and it will only be bound to that game.  OpenBLCMM will keep multiple
 * DataManager objects alive (one per game we have data for), and keeps track
 * of which one is "current."  This is easy enough when all components of
 * the application are acting on the same dataset, which has historically
 * been the case -- the only game dropdown available was in BLCMM itself, and
 * when that was toggled, OE starts pulling from the new game data as well.
 *
 * Well, with the rewrite, we're aiming to decouple that a bit, so that OE
 * could be querying different data than the OpenBLCMM-configured patch type,
 * etc.  So, this class was born to provide both OpenBLCMM and OE a common way
 * to manage which game data's currently in use.
 *
 * @author apocalyptech
 */
public class DataManagerManager {

    private HashMap <PatchType, DataManager> dataManagers;
    private HashMap <PatchType, String> dataManagerStatus;
    private PatchType currentPatchType;
    private DataManager currentDataManager;

    /**
     * Default constructor, most likely called by MainGUI, which initializes a
     * new set of DataManager objects, one for each PatchType (or `null` if
     * the data could not be loaded), and sets the specified one as the current
     * active one.
     *
     * @param currentPatchType The currently-active PatchType.
     * @param dataStatusNotifier A DataStatus object to send updates to.
     */
    public DataManagerManager(PatchType currentPatchType, DataStatusNotifier dataStatusNotifier) {

        this.dataManagers = new HashMap<>();
        this.dataManagerStatus = new HashMap<>();
        for (PatchType type : PatchType.values()) {
            dataStatusNotifier.setGame(type);
            try {
                GlobalLogger.log("Starting initialization of " + type.toString() + " Data Manager");
                this.dataManagers.put(type, new DataManager(type, dataStatusNotifier));
                GlobalLogger.log("Initialized " + type.toString() + " Data Manager");
                dataStatusNotifier.event("Data initialization successful!", false);
                this.dataManagerStatus.put(type, "Loaded, v" + this.dataManagers.get(type).getDumpVersion());
            } catch (DataManager.NoDataException e) {
                this.dataManagers.put(type, null);
                dataStatusNotifier.event("Error initializing: " + e.getMessage(), false);
                GlobalLogger.log("Error initializing " + type.toString() + " Data Manager: " + e.toString());
                this.dataManagerStatus.put(type, "Not loaded: " + e.getMessage());
            } catch (Exception e) {
                // So someone testing OpenBLCMM on Mac reported an NPE on startup which indicated that
                // MainGUI's `dmm` was null.  I'm not super sure how that could happen -- I'd have throught
                // that we'd get an exception in *here*, but perhaps because we're initializing this inside
                // a SwingWorker, it'd "hide" uncaught Exceptions?  Anyway, if that *is* the case, maybe
                // this will at least let the app start up, though it would mean that those folks wouldn't
                // have access to the datalib.  We'll see.
                this.dataManagers.put(type, null);
                dataStatusNotifier.event("Error initializing: " + e.getMessage(), false);
                GlobalLogger.log("Error initializing " + type.toString() + " Data Manager: " + e.toString());
                GlobalLogger.log(e);
                this.dataManagerStatus.put(type, "Not loaded: " + e.getMessage());
            }
        }
        this.updateDataManagersSelectedClasses();
        this.setPatchType(currentPatchType);
        dataStatusNotifier.finish();
    }

    /**
     * Alternate constructor, most likely called by ObjectExplorer, which
     * essentially copies an existing DataManagerManager object, so that another
     * component which wants to manage its current data state can do so without
     * instantiating completely separate DataManager objects.
     *
     * Keep in mind that even though this creates separate DataManagerManager
     * objects, the individual DataManager objects are the same, so updates
     * to the DataManagers in one DMM would end up applying to the other
     * (such as with the call to updateDataManagersSelectedClasses, below).
     *
     * @param dmm The DataManagerManager object to copy
     */
    public DataManagerManager(DataManagerManager dmm) {
        this.dataManagers = dmm.dataManagers;
        this.currentPatchType = dmm.currentPatchType;
        this.currentDataManager = dmm.currentDataManager;
    }

    /**
     * Returns an English status describing the state of game data for the
     * given PatchType.  At the moment just used for the About panel.
     *
     * @param type The PatchType to query
     * @return A string describing if the data is loaded or not.
     */
    public String getStatus(PatchType type) {
        if (this.dataManagerStatus.containsKey(type)) {
            return this.dataManagerStatus.get(type);
        } else {
            return "Unknown!";
        }
    }

    /**
     * Updates all our DataManager objects to reflect the user-chosen categories
     * for use in fulltext/refs searches.  Note that in our intended operation,
     * even though the OE window maintains a separate DMM, calling this method
     * on the MainGUI's DMM object will also apply over there.
     */
    public final void updateDataManagersSelectedClasses() {
        for (DataManager dm : this.dataManagers.values()) {
            if (dm != null) {
                dm.updateClassesByEnabledCategory(Options.INSTANCE.getOESearchCategories());
            }
        }
    }

    /**
     * Returns the current PatchType we're using
     *
     * @return The PatchType of the current data
     */
    public PatchType getCurrentPatchType() {
        return this.currentPatchType;
    }

    /**
     * Returns the current DataManager we're using
     *
     * @return The DataManager for the current PatchType
     */
    public DataManager getCurrentDataManager() {
        return this.currentDataManager;
    }

    /**
     * Returns the specified DataManager (which might be null).
     *
     * @param pt The PatchType to retrieve
     * @return The DataManager, or null
     */
    public DataManager getDataManager(PatchType pt) {
        if (this.dataManagers.containsKey(pt)) {
            return this.dataManagers.get(pt);
        } else {
            return null;
        }
    }

    /**
     * Sets the current PatchType (ie: swap over to using different data)
     *
     * @param newPatchType The new PatchType to get data for
     * @return The newly-pointed-at DataManager object (or null, if there's no data)
     */
    public final DataManager setPatchType(PatchType newPatchType) {
        this.currentPatchType = newPatchType;
        this.currentDataManager = this.dataManagers.get(newPatchType);
        return this.currentDataManager;
    }

}
