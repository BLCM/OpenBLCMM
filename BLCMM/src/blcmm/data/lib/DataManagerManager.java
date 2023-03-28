/*
 * Copyright (C) 2023 CJ Kucera
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
    private PatchType currentPatchType;
    private DataManager currentDataManager;

    /**
     * Default constructor, most likely called by MainGUI, which initializes a
     * new set of DataManager objects, one for each PatchType (or `null` if
     * the data could not be loaded), and sets the specified one as the current
     * active one.
     *
     * @param currentPatchType The currently-active PatchType
     */
    public DataManagerManager(PatchType currentPatchType) {
        this.dataManagers = new HashMap<>();
        for (PatchType type : PatchType.values()) {
            try {
                GlobalLogger.log("Starting initialization of " + type.toString() + " Data Manager");
                this.dataManagers.put(type, new DataManager(type));
                GlobalLogger.log("Initialized " + type.toString() + " Data Manager");
            } catch (DataManager.NoDataException e) {
                this.dataManagers.put(type, null);
                GlobalLogger.log("Error initializing " + type.toString() + " Data Manager: " + e.toString());
            }
        }
        this.updateDataManagersSelectedClasses();
        this.setPatchType(currentPatchType);
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
