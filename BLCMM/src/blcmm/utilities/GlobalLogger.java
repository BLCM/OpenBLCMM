/*
 * Copyright (C) 2023 CJ Kucera
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

import blcmm.utilities.log.LogConsole;
import blcmm.utilities.log.LogFile;
import blcmm.utilities.log.LogTarget;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Global logging class for BLCMM.  This class was reimplemented based on the
 * calls BLCMM makes into BLCMM_Utilities.jar, without reference to the original
 * sourcecode.
 * 
 * This class used to live under general.utilities, but it's been moved
 * under blcmm.utilities as part of its opensourcing.  This also got pretty
 * overengineered; honestly I probably should've just bitten the bullet and
 * spun up a proper Log4j.
 * 
 * @author apocalyptech
 */
public class GlobalLogger {
    
    private static String logFolder;
    private static final LogTarget[] targets;
        
    /**
     * Define where we're logging:
     *   1. To the console
     *   2. A datetimestamped logfile which will be removed if there are no errors
     *   3. A "latest" logfile which will always remain on disk (and get overwritten
     *      with each run)
     * 
     * Also set a default log dir
     */
    static {
        targets = new LogTarget[] {
            new LogConsole(),
            new LogFile("log-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HHmmss")) + ".log"),
            new LogFile("log-latest.log", true),
        };
        setLogFolder(Paths.get(System.getProperty("user.dir"), "blcmm_logs").toString());
    }

    /**
     * Set the log folder for all our targets.  Will close any existing files,
     * if that's something that makes sense for the target, and will also
     * ensure that the directory exists.
     * 
     * @param newLogFolder The new log folder
     */
    public static void setLogFolder(String newLogFolder) {
        close();
        new File(newLogFolder).mkdirs();
        logFolder = newLogFolder;
        for (LogTarget t : targets) {
            t.setLogFolder(logFolder);
        }
    }
    
    /**
     * Close all log targets, if appropriate.
     */
    private static void close() {
        for (LogTarget t : targets) {
            t.close();
        }
    }
    
    /**
     * Log a single line to all log targets
     * 
     * @param line The line to log
     */
    private static void singleLine(String line) {
        for (LogTarget t : targets) {
            t.singleLine(line);
        }
    }
    
    /**
     * Flush filehandles-or-whatever on all log targets, if appropriate.
     */
    private static void flush() {
        for (LogTarget t : targets) {
            t.flush();
        }
    }
    
    /**
     * Mark all log targets as "permanent" -- don't delete them when exiting.
     */
    public static void markAsPermanentLog() {
        for (LogTarget t : targets) {
            t.markAsPermanentLog();
        }
    }
    
    /**
     * Delete all log targets, if appropriate.  Will do nothing if you've
     * already called markAsPermanentLog, or if the target otherwise ensures
     * its own preservation.
     */
    public static void deleteLog() {
        for (LogTarget t : targets) {
            t.delete();
        }
    }
    
    /**
     * Returns the log folder where file-based logs are being written.
     * 
     * @return The directory
     */
    public static File getLOG_FOLDER() {
        return new File(logFolder);
    }
    
    /**
     * Returns the first file-based logfile we're logging to.  In our default
     * configuration, this'll be the timestamped one.
     * 
     * @return The path to the logfile
     */
    public static File getLOG() {
        for (LogTarget t : targets) {
            if (t instanceof LogFile) {
                return ((LogFile) t).getLogFile();
            }
        }
        return null;
    }
    
    /**
     * Gets a standardized log prefix to put in front of each line.  Will
     * include a useful traceback step, if we can.
     * 
     * @return The prefix
     */
    private static String getLogPrefix() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        StackTraceElement before = null;
        boolean found_log = false;
        for (StackTraceElement element : trace) {
            if (found_log) {
                before = element;
                break;
            }
            if (element.getFileName().equals("GlobalLogger.java") && element.getMethodName().equals("log")) {
                found_log = true;
            }
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"));
        if (before == null) {
            return timestamp + " (unknown) -> ";
        } else {
            return timestamp + " "
                    + before.getClassName() + "." + before.getMethodName()
                    + "(" + before.getFileName() + ":"
                    + Integer.toString(before.getLineNumber()) + ")"
                    + " -> ";
        }
    }

    /**
     * Log the given message to all targets
     * 
     * @param message The message to log
     */
    public static void log(String message) {
        singleLine(getLogPrefix() + message);
        flush();
    }
    
    /**
     * Log the given Throwable to all targets.  Will include a traceback.
     * 
     * @param throwable The Throwable to log
     */
    public static void log(Throwable throwable) {
        singleLine(getLogPrefix() + throwable.getClass().toString() + ": " + throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
            singleLine("    " + element.toString());
        }
        flush();
    }
    
}
