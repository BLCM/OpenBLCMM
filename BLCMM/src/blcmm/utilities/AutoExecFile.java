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
 */
package blcmm.utilities;

import blcmm.model.PatchType;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * A specialized class to manage the autoexec.txt files for c0dycode's AutoExec
 * plugin.
 *
 * @author LightChaosman
 */
public class AutoExecFile {

    public static final Function<String, String> FILENAME_ADJUSTER = s -> s + ".mirror";

    private static final String FILENAME = "autoexec.txt";

    private static final int SHOULD_ADAPT_AFTER_EACH_SAVE_MASK = 1 /*<< 0*/;//commented to get rid of compiler warning about useless statements
    private static final int SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK = 1 << 1;
    private static final int SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE = 1 << 2;
    private static final int DEFAULT_BLCMM_SETTING = 0
            + 1 * SHOULD_ADAPT_AFTER_EACH_SAVE_MASK//On by default
            + 0 * SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK//Off by default
            + 0 * SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE//Off by default
            ;

    private final PatchType type;
    private String onlinePatch;
    private String offlinePatch;
    private boolean fastMode = true;
    private boolean forceOffline = false;
    private int BLCMM_Settings = DEFAULT_BLCMM_SETTING;

    private AutoExecFile(PatchType type) {
        this.type = type;
    }

    public PatchType getType() {
        return type;
    }

    public String getOnlinePatch() {
        return onlinePatch;
    }

    public String getOfflinePatch() {
        return offlinePatch;
    }

    public boolean isFastMode() {
        return fastMode;
    }

    public boolean isForceOffline() {
        return forceOffline;
    }

    public boolean shouldAdaptAfterEachSave() {
        return (BLCMM_Settings & SHOULD_ADAPT_AFTER_EACH_SAVE_MASK) == SHOULD_ADAPT_AFTER_EACH_SAVE_MASK;
    }

    public boolean shouldSaveOnlineAndOfflineSeperately() {
        return (BLCMM_Settings & SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK) == SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK;
    }

    public boolean shouldSaveBothVersionsOnEachSave() {
        return (BLCMM_Settings & SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE) == SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE;
    }

    public void setOnlinePatch(String onlinePatch) {
        this.onlinePatch = onlinePatch;
    }

    public void setOfflinePatch(String offlinePatch) {
        this.offlinePatch = offlinePatch;
    }

    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;
    }

    public void setForceOffline(boolean forceOffline) {
        this.forceOffline = forceOffline;
    }

    public void setShouldAdaptAfterEachSave(boolean flag) {
        if (flag) {
            BLCMM_Settings = BLCMM_Settings | SHOULD_ADAPT_AFTER_EACH_SAVE_MASK;
        } else {
            BLCMM_Settings = BLCMM_Settings & (~SHOULD_ADAPT_AFTER_EACH_SAVE_MASK);
        }
    }

    public void setShouldSaveOnlineAndOfflineSeperately(boolean flag) {
        if (flag) {
            BLCMM_Settings = BLCMM_Settings | SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK;
        } else {
            BLCMM_Settings = BLCMM_Settings & (~SHOULD_SAVE_ONLINE_AND_OFFLINE_SEPERATELY_MASK);
        }
    }

    public void setShouldSaveBothVersionsOnEachSave(boolean flag) {
        if (flag) {
            BLCMM_Settings = BLCMM_Settings | SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE;
        } else {
            BLCMM_Settings = BLCMM_Settings & (~SHOULD_SAVE_BOTH_VERSIONS_ON_EACH_SAVE);
        }
    }

    /**
     * Saves the current configuration to the file it was loaded from
     *
     * @throws IOException
     */
    public void save() throws IOException {
        String binariesDir = GameDetection.getBinariesDir(type == PatchType.BL2);
        File f = new File(binariesDir + File.separator + FILENAME);
        Utilities.writeStringToFile(toString(), f);
    }

    @Override
    public String toString() {
        if (onlinePatch != null && offlinePatch != null) {
            return String.format(""
                    + "Online=exec %s\n"
                    + "Offline=exec %s\n"
                    + "Fastmode=%s\n"
                    + "ForceOfflineHotfixes=%s\n"
                    + "BLCMM_Settings=%s",
                    onlinePatch, offlinePatch, fastMode, forceOffline, BLCMM_Settings);
        }
        return String.format(""
                + "exec %s\n"
                + "Fastmode=%s\n"
                + "ForceOfflineHotfixes=%s\n"
                + "BLCMM_Settings=%s",
                onlinePatch == null ? offlinePatch : onlinePatch, fastMode, forceOffline, BLCMM_Settings);
    }

    /**
     * Returns wethet the autoexec.txt file of the given game exists. If it
     * does, we just assume autoexec is installed.
     *
     * @param type The game to check for
     * @return true iff the autoexec file is in place
     */
    public static boolean hasAutoExecInstalled(PatchType type) {
        return new File(GameDetection.getBinariesDir(type == PatchType.BL2) + File.separator + FILENAME).exists();
    }

    /**
     * Parses the file in the binaries dir of the provided game. Returns a
     * default configuration if no such file exists
     *
     * @param type The game to get the file from
     * @return An OE version of the file
     * @throws IOException if an error occurs while reading the file
     */
    public static AutoExecFile read(PatchType type) throws IOException {
        String binariesDir = GameDetection.getBinariesDir(type == PatchType.BL2);
        File f = new File(binariesDir + File.separator + FILENAME);
        AutoExecFile res = new AutoExecFile(type);
        if (!f.exists()) {
            return res;
        }

        String[] split = Utilities.readFileToString(f).split("\n");
        for (String line : split) {
            line = line.trim();
            String field, value;
            if (line.contains("=")) {
                String[] split2 = line.split("=", 2);
                field = split2[0].toLowerCase();
                value = split2[1];
                if (startsWithIgnoreCase(value, "exec")) {
                    value = value.substring(value.indexOf(" ") + 1);
                }
            } else if (startsWithIgnoreCase(line, "exec")) {
                field = "online";
                value = line.substring(line.indexOf(" ") + 1);
            } else {
                continue;//be lenient, just ignore the noise
            }
            value = value.trim();//Remove any spaces after an =
            switch (field) {
                case "online":
                    res.onlinePatch = value;
                    break;
                case "offline":
                    res.offlinePatch = value;
                    break;
                case "fastmode":
                    res.fastMode = Boolean.parseBoolean(value);
                    break;
                case "forceofflinehotfixes":
                    res.forceOffline = Boolean.parseBoolean(value);
                    break;
                case "blcmm_settings":
                    res.BLCMM_Settings = Integer.parseInt(value);
                    break;
                default:
                    continue;//be lenient
            }
        }
        return res;
    }

    public static AutoExecFile getDefaultFile(PatchType type) {
        AutoExecFile file = new AutoExecFile(type);
        file.onlinePatch = type == PatchType.BL2 ? "Patch.txt" : "PatchTPS.txt";
        file.forceOffline = false;
        file.fastMode = true;
        return file;
    }

    private static boolean startsWithIgnoreCase(String toTest, String prefix) {
        return toTest.substring(0, Math.min(toTest.length(), prefix.length())).toLowerCase().equals(prefix.toLowerCase());
    }

}
