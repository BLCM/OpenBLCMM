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

package blcmm.utilities.log;

import blcmm.utilities.GlobalLogger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Logging to a logfile, for GlobalLogger.
 * 
 * BLCMM historically deleted its logfile when the app closed, unless there
 * was a properly-logged exception.  We're now logging to *two* files at once,
 * though -- one a timestamped file which is deleted on close just like before,
 * and the other a "latest" file which is never deleted.  The constructor for
 * this target supports marking a logfile as permanent right at instantiation,
 * to support that.
 * 
 * @author apocalyptech
 */

public class LogFile implements LogTarget {

    private final String filenameBase;
    private File logFile = null;
    private BufferedWriter writer = null;
    private boolean persistent;
    private static String NEWLINE = System.lineSeparator();
    
    public LogFile(String filenameBase) {
        this(filenameBase, false);
    }

    public LogFile(String filenameBase, boolean persistent) {
        this.filenameBase = filenameBase;
        this.persistent = persistent;
    }
    
    /**
     * Get the path to the actual logfile
     * 
     * @return The logfile
     */
    public File getLogFile() {
        return this.logFile;
    }
    
    /**
     * What to do if we encounter an Exception (most likely an IOException)
     * while attempting to interact with the on-disk logfile.  Will close
     * the file so no further writes are attempted, and send yet another
     * log entry to GlobalLogger to report on the failure (which should
     * hopefully at least make it to the console).
     * 
     * @param report String prefix to report along with the Exception
     * @param e The thrown Exception
     */
    private void handleLoggingException(String report, Exception e) {
        this.close();
        GlobalLogger.log(report + ", file " + logFile.toString() + ": " + e.toString());
    }

    /**
     * Sets the log folder to send logs to.
     * 
     * @param newLogFolder The folder to use.
     */
   @Override
    public void setLogFolder(String newLogFolder) {
        this.close();
        Path curLogFilePath = Paths.get(newLogFolder, this.filenameBase);
        this.logFile = curLogFilePath.toFile();
        try {
            this.writer = new BufferedWriter(new FileWriter(this.logFile));
        } catch (IOException e) {
            this.handleLoggingException("Error opening logfile", e);
        }
    }

    /**
     * Logs a single line.
     * 
     * @param line The message to log
     */
    @Override
    public void singleLine(String line) {
        if (writer != null) {
            try {
                writer.write(line + NEWLINE);
            } catch (IOException e) {
                this.handleLoggingException("Error writing line to logfile: \"" + line + "\"", e);
            }
        }
    }

    /**
     * Flush our filehandle.
     */
    @Override
    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException e) {
                this.handleLoggingException("Error flushing logfile", e);
            }
        }
    }

    /**
     * Close our open filehandle, if needed.
     */
    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                this.handleLoggingException("Error closing logfile", e);
            }
        }
        writer = null;
    }
    
    /**
     * Deletes the logfile, unless we've been marked as persistent, or if
     * markAsPermanentLog has been called.
     */
    @Override
    public void delete() {
        this.close();
        if (persistent) {
            return;
        }
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }
    
    /**
     * Marks this logfile as permanent, so it won't be deleted.
     */
    @Override
    public void markAsPermanentLog() {
        this.persistent = true;
    }
    
}
