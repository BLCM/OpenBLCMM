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
import blcmm.utilities.Utilities;
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
 * Note that the file isn't actually opened for writing until we actually
 * receive a log line.  This is so that we can set up our logging info in
 * the static context but redirect the log directory based on CLI args,
 * without having something opened up for writing in a location we don't
 * intend to.  If the file is explicitly closed, or encounters an error while
 * writing, it'll be closed and rewrites will not be attempted unless we
 * receive another update to our base log dir, in which case the next log
 * attempt will try to re-open the file for writing.
 *
 * @author apocalyptech
 */

public class LogFile implements LogTarget {

    private enum LogState {
        NEW,
        WRITING,
        CLOSED
    };

    private final String filenameBase;
    private File logFile = null;
    private BufferedWriter writer = null;
    private boolean persistent;
    private static String NEWLINE = System.lineSeparator();
    private LogState state;

    public LogFile(String filenameBase) {
        this(filenameBase, false);
    }

    public LogFile(String filenameBase, boolean persistent) {
        this.filenameBase = filenameBase;
        this.persistent = persistent;
        this.state = LogState.NEW;
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
        GlobalLogger.log(report + ", file " + Utilities.hideUserName(logFile.toString()) + ": " + e.toString());
    }

    /**
     * Sets the log folder to send logs to.
     *
     * @param newLogFolder The folder to use.
     */
    @Override
    public void setLogFolder(String newLogFolder) {
        this.close();
        this.state = LogState.NEW;
        Path curLogFilePath = Paths.get(newLogFolder, this.filenameBase);
        this.logFile = curLogFilePath.toFile();
    }

    /**
     * Open ourself for writing; returns True if we're now open, or False
     * otherwise.  Will refuse to do anything if we've been explicitly
     * closed.  This is basically used so that we don't actually try opening
     * the file for writing until a log entry is attempted.
     *
     * @return True if we're now open, false otherwise.
     */
    private boolean startWriting() {
        if (this.state == LogState.CLOSED) {
            return false;
        }
        try {
            this.writer = new BufferedWriter(new FileWriter(this.logFile));
            this.state = LogState.WRITING;
            return true;
        } catch (IOException e) {
            this.handleLoggingException("Error opening logfile", e);
            this.state = LogState.CLOSED;
            return false;
        }
    }

    /**
     * Logs a single line.
     *
     * @param line The message to log
     */
    @Override
    public void singleLine(String line) {
        if (this.state == LogState.NEW) {
            if (!this.startWriting()) {
                return;
            }
        }
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
        this.state = LogState.CLOSED;
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
