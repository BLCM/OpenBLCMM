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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small class to provide some miscellaneous BLCMM-specific utilities which
 * don't really belong in the main Utilities class. Could probably bear to have
 * various things moved over here.
 */
public class BLCMMUtilities {

    /**
     * Holds the path to the launcher used to launch BLCMM, if it was launched
     * that way.
     */
    private static File launcher = null;

    /**
     * A boolean so we know whether we were launched with the launcher or not.
     */
    private static boolean usedLauncher = false;

    public static File getLauncher() {
        return launcher;
    }

    public static void setLauncher(File launcher) {
        BLCMMUtilities.launcher = launcher;
    }

    public static boolean isUsedLauncher() {
        return usedLauncher;
    }

    public static void setUsedLauncher(boolean usedLauncher) {
        BLCMMUtilities.usedLauncher = usedLauncher;
    }

    /**
     * Returns a directory which we can use to store data, should we want to. If
     * we couldn't find existing app data directories via the detection in
     * Utilities, we'll try using the launcher's directory (if that exists), or
     * failing that just our current working directory. Note that the actual
     * directory returned here is not guaranteed to exist, but all its parent
     * dirs should exist.
     *
     * @return A path to a directory we should be able to use to store data,
     * writable by the user.
     */
    public static String getBLCMMDataDir() {
        String appDirName = "BLCMM";
        String detectedDir = Utilities.getAppDataDir(appDirName);
        if (detectedDir == null) {
            // If we couldn't find a dir, check to see if we can use the
            // launcher's dir.
            if (isUsedLauncher()) {
                detectedDir = launcher.getParent();
                if (new File(detectedDir).exists()) {
                    return detectedDir + "/" + appDirName;
                }
            }

            // If we got here, there's no launcher dir and nothing could be
            // autodetected.  Fall back to the current working directory,
            // I guess.  Let's just assume that this exists.  To remain
            // compatible with the way the app has always worked when run
            // without a launcher, we're going to NOT add appDirName here.
            return System.getProperty("user.dir");

        } else {
            return detectedDir;
        }
    }

    /**
     * Populates our file history based on GameDetection, if we don't already
     * have data in the file history.
     *
     * @param bl2First Whether to sort BL2-discovered patch files first. True
     * for BL2, False for TPS.
     */
    public static void populateFileHistory(boolean bl2First) {
        if (Options.INSTANCE.getFileHistory().length == -1) {
            String[] bl2Patches = new String[]{"patch.txt"};
            String[] tpsPatches = new String[]{"patch.txt", "patchtps.txt"};
            final int maxDistance = 4;//arbitrary
            List<String> res = new ArrayList<>();
            populate(bl2Patches, true, maxDistance, res);
            populate(tpsPatches, false, maxDistance, res);
            if (!bl2First) {
                Collections.reverse(res);
            }
            Options.INSTANCE.setFileHistory(res.toArray(new String[0]));
        }
    }

    private static void populate(String[] patches, boolean BL2, final int maxDistance, List<String> res) {
        if (GameDetection.getBinariesDir(BL2) != null) {
            Map<File, Integer> map = new HashMap<>();

            String binaries = GameDetection.getBinariesDir(BL2).replace("\\", "/");
            for (File f : new File(binaries).listFiles()) {
                for (String patch : patches) {
                    int dist = GeneralUtilities.Strings.levenshteinDistance(f.getName().toLowerCase(), patch);
                    if (dist <= maxDistance && f.isFile()) {
                        map.put(f, java.lang.Math.min(map.getOrDefault(f, dist), dist));
                    }
                }
            }
            List<File> fs = new ArrayList<>();
            fs.addAll(map.keySet());
            fs.sort((t, t1) -> Integer.compare(map.get(t), map.get(t1)));
            if (fs.size() > 0) {
                res.add(fs.get(0).getAbsolutePath());
            }
        }
    }

    /**
     * Called when starting up the GUI to make sure that, if we have files in
     * our file history, they exist. Will purge the list of any nonexistent
     * files. If this results in an empty history, we will call our detection
     * routines to attempt to populate it.
     */
    public static void cleanFileHistory() {
        String[] fileHistory = Options.INSTANCE.getFileHistory();
        ArrayList<String> newHistory = new ArrayList<>();
        File curFile;
        for (String filename : fileHistory) {
            curFile = new File(filename);
            if (curFile.exists()) {
                newHistory.add(filename);
            }
        }
        if (fileHistory.length != newHistory.size()) {
            Options.INSTANCE.setFileHistory(newHistory.toArray(new String[0]));
            populateFileHistory(true);
        }
    }

}
