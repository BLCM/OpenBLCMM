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

package blcmm.utilities.log;

/**
 * Console logging for GlobalLogger.  Just a straightforward wrapper around
 * some basic System.out.println(), basically.
 * 
 * @author apocalyptech
 */

public class LogConsole implements LogTarget {

    /**
     * Has no effect.
     * 
     * @param newLogFolder The folder to use.
     */
    @Override
    public void setLogFolder(String newLogFolder) {
    }

    /**
     * Logs a single line.
     * 
     * @param line The message to log
     */
    @Override
    public void singleLine(String line) {
        System.out.println(line);
    }

    /**
     * Flush the console.
     */
    @Override
    public void flush() {
        System.out.flush();
    }

    /**
     * Has no effect.
     */
    @Override
    public void close() {
    }
    
    /**
     * Has no effect.
     */
    @Override
    public void delete() {
    }
    
    /**
     * Has no effect.
     */
    @Override
    public void markAsPermanentLog() {
    }

}