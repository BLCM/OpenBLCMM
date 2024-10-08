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
