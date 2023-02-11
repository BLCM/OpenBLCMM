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
package blcmm.utilities;

import blcmm.model.CompletePatch;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author LightChaosman
 */
public class ImportAnomalyLog implements Iterable<ImportAnomalyLog.importAnomaly> {

    public static final ImportAnomalyLog INSTANCE = new ImportAnomalyLog();

    // Var to keep track of errors while importing mods, for reporting to
    // the user.
    private final ArrayList<importAnomaly> anomalies = new ArrayList<>();

    /**
     * Error types which we may encounter while trying to import mods.
     */
    public static enum ImportAnomalyType implements Comparable<ImportAnomalyType> {
        ParseError(true),
        AlreadyExists(true),
        NotFound(true),
        HTMLFile(false),
        EmptyFile(false),
        DifferentType(false),
        RARfile(true),
        NoCommands(true),
        IncorrectType(true);

        public final boolean error;

        private ImportAnomalyType(boolean error) {
            this.error = error;
        }

    }

    /**
     * Tiny little class to hold information about an import error. It's a shame
     * Java doesn't just have a native Tuple type (though Apache Commons
     * apparently does).
     */
    public static class importAnomaly implements Comparable<importAnomaly> {

        public final String modName;
        public final ImportAnomalyType anomalyType;
        public final String anomalyString;
        public final CompletePatch patch;

        public importAnomaly(String modName, ImportAnomalyType errorType, String errorString, CompletePatch patch) {
            this.modName = modName;
            this.anomalyType = errorType;
            this.anomalyString = errorString;
            this.patch = patch;
        }

        @Override
        public int compareTo(importAnomaly t) {
            return anomalyType.compareTo(t.anomalyType);
        }
    }

    public void add(importAnomaly anomaly) {
        int i = anomalies.size();
        while (i > 0 && anomaly.compareTo(anomalies.get(i - 1)) < 0) {
            i--;
        }
        anomalies.add(i, anomaly);
    }

    public void clear() {
        anomalies.clear();
    }

    public int size() {
        return anomalies.size();
    }

    @Override
    public Iterator<importAnomaly> iterator() {
        return anomalies.iterator();
    }

}
