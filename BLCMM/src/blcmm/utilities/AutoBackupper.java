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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;

/**
 *
 * @author LightChaosman
 */
public class AutoBackupper {

    //format currenttimemillis - date - name.txt
    public static long BACKUP_INTERVAL = 120000;
    private static int NUMBER_OF_SESSIONS_TO_KEEP = 5;
    private static int NUMBER_OF_BACKUPS_PER_SESSION = 10;
    private static final File DESTINATION = new File("backups/");
    private static final long CURRENT_SESSION = System.currentTimeMillis();
    private static final LinkedList<File> CURRENT_BACKUPS = new LinkedList<File>();
    private static int COUNTER = 0;

    public static void cleanOldBackups() {
        updateSettings();
        HashSet<Long> set = new HashSet<>();
        DESTINATION.mkdirs();
        for (File f : DESTINATION.listFiles()) {
            String name = f.getName();
            long millis = Long.parseLong(name.split("-")[0].trim());
            set.add(millis);
        }
        ArrayList<Long> list = new ArrayList<>();
        list.addAll(set);
        Collections.sort(list);
        int size = list.size();
        for (int i = 0; i < Math.min(size, NUMBER_OF_SESSIONS_TO_KEEP); i++) {
            list.remove(list.size() - 1);
        }
        for (File f : DESTINATION.listFiles()) {
            String name = f.getName();
            long millis = Long.parseLong(name.split("-")[0].trim());
            if (list.contains(millis) && millis != CURRENT_SESSION) {
                f.delete();
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
        File f = new File(DESTINATION.toString() + File.separator + backupFileName);
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
