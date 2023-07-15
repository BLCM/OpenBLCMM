/*
 * Copyright (C) 2018-2020  LightChaosman
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A system to automatically create backups of the currently-open file.
 *
 * When OpenBLCMM is started, the CURRENT_SESSION is set to the current system
 * timestamp (to millisecond precision), and that becomes the prefix for the
 * filename for that whole "session."  Backups will be written out every 2
 * minutes so long as there have been unsaved changes made to the patchfile.
 * The number after the timestamp will increment with every one of these.
 *
 * While making the backups, there's an additional NUMBER_OF_BACKUPS_PER_SESSION
 * constant which defines the max number of files which will be saved within
 * a session.  Old files will be removed as those auto-backups happen.
 *
 * Then when the app starts up, the first thing it does is check to see if
 * there are more sessions saved on disk than we want to keep around.  If so,
 * the old ones are removed.  This will remove *all* backups from the old
 * session.
 *
 * In summary: the timestamp (both as a raw number and as a datestamp) currently
 * represents when the app was *started*, not when the backup was actually
 * taken, and it's those sessions which end up getting rotated out.  The
 * convention for the backup filename is:
 *
 *   [session timestamp in millis] - [yyyy.mm.dd] - [counter] - [patchname]
 *
 * @author LightChaosman
 */
public class AutoBackupper {

    //format currenttimemillis - date - name.txt
    public static long BACKUP_INTERVAL = 120000;
    private static int NUMBER_OF_SESSIONS_TO_KEEP = 5;
    private static int NUMBER_OF_BACKUPS_PER_SESSION = 10;
    private static final File DESTINATION = Paths.get(Utilities.getBLCMMDataDir(), "backups").toFile();
    private static final long CURRENT_SESSION = System.currentTimeMillis();
    private static final LinkedList<File> CURRENT_BACKUPS = new LinkedList<>();
    private static int COUNTER = 0;

    /**
     * Removes old backup sessions.  This is called by MainGUI when a new
     * Autobackupper is set up -- it effectively only gets run when the app
     * starts.  The check for NUMBER_OF_BACKUPS_PER_SESSION (which ensures that
     * a single session doesn't store too many backups) is down in the backup
     * routines themselves.
     */
    public static void cleanOldBackupSessions() {
        updateSettings();
        DESTINATION.mkdirs();

        TreeMap<Long, ArrayList<File>> orderedSessions = new TreeMap<>();
        String name;
        String[] nameParts;
        long sessionTimestamp;

        // Loop through files to find things that look like backups.  Store them
        // in a TreeMap with the key being the filename-encoded creation time
        // in milliseconds, so they'll be automatically sorted.  When we loop
        // through below, the oldest ones will be first.  Note that each session
        // can have a number of files associated with it.
        for (File f : DESTINATION.listFiles()) {
            if (f.isFile()) {
                name = f.getName();
                if (name.contains("-")) {
                    nameParts = name.split("-");
                    if (nameParts.length > 1) {
                        try {
                            sessionTimestamp = Long.parseLong(nameParts[0].trim());
                            if (!orderedSessions.containsKey(sessionTimestamp)) {
                                orderedSessions.put(sessionTimestamp, new ArrayList<>());
                            }
                            orderedSessions.get(sessionTimestamp).add(f);
                        } catch (NumberFormatException e) {
                            // Nothing, not a backup file.
                        }
                    }
                }
            }
        }

        // If we have more session than we intend to keep, remove the files
        // belonging to the oldest ones.
        int toRemove = orderedSessions.size() - NUMBER_OF_SESSIONS_TO_KEEP;
        if (toRemove > 0) {
            int counter = 0;
            for (Entry<Long, ArrayList<File>> backupEntry : orderedSessions.entrySet()) {
                if (backupEntry.getKey() != CURRENT_SESSION) {
                    for (File loopFile : backupEntry.getValue()) {
                        loopFile.delete();
                    }
                }
                counter++;
                if (counter >= toRemove) {
                    break;
                }
            }
        }

    }

    public static void backup(File f, Backupable instance) throws IOException {
        backup(f.getName(), instance);
    }

    public static void backup(String filename, Backupable instance) throws IOException {
        if (!instance.inNeedOfBackup()) {
            return;
        }
        while (CURRENT_BACKUPS.size() >= NUMBER_OF_BACKUPS_PER_SESSION) {
            CURRENT_BACKUPS.pop().delete();
        }
        String date = new SimpleDateFormat("YYYY.MM.dd").format(new Date(CURRENT_SESSION));
        String backupFileName = CURRENT_SESSION + " - " + date + " - " + (++COUNTER) + " - " + filename;
        DESTINATION.mkdir();
        File f = Paths.get(DESTINATION.toString(), backupFileName).toFile();
        CURRENT_BACKUPS.add(f);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            instance.write(bw);
        }
        updateSettings();
    }

    private static void updateSettings() {
        BACKUP_INTERVAL = Options.INSTANCE.getSecondsBetweenBackups() * 1000;
        NUMBER_OF_SESSIONS_TO_KEEP = Options.INSTANCE.getSessionsToKeep();
        NUMBER_OF_BACKUPS_PER_SESSION = Options.INSTANCE.getBackupsPerSession();
    }

    public static String getDestination() {
        return DESTINATION.getAbsolutePath();
    }

    public static File getMostRecentBackupFile() {
        DESTINATION.mkdirs();
        File[] options = DESTINATION.listFiles();
        Optional<File> findFirst = Arrays.stream(options).max((o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
        return findFirst.isPresent() ? findFirst.get() : null;
    }

    public static interface Backupable {

        public void write(BufferedWriter writer) throws IOException;

        public boolean inNeedOfBackup();
    }
}
