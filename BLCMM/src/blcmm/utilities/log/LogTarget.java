/*
 * Copyright (C) 2023 CJ Kucera
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

/**
 * Interface defining a logging target, used by blcmm.utilities.GlobalLogger.
 * This is kind of stupidly overengineered, but whatever.  Note that the
 * majority of these functions don't really apply to console logging.  Other
 * potential log targets like databases or whatever wouldn't fit neatly in
 * here either.
 * 
 * @author apocalyptech
 */

public interface LogTarget {
    
    /**
     * Sets the log folder to send logs to.  Obviously only has an effect for
     * file-based targets.
     * 
     * @param newLogFolder The folder to use.
     */
    public void setLogFolder(String newLogFolder);
    
    /**
     * Logs a single line to the target.
     * 
     * @param line The message to log
     */
    public void singleLine(String line);
    
    /**
     * Flush the target, if appropriate.
     */
    public void flush();
    
    /**
     * Close any open filehandles-or-whatever, if appropriate.
     */
    public void close();
    
    /**
     * Mark the target as permanent -- do not delete on application close.
     */
    public void markAsPermanentLog();
    
    /**
     * Delete the file-or-whatever, if appropriate.  Will not have any effect
     * if markAsPermanentLog has been called, or if the target is otherwise
     * ensuring its permanence.
     */
    public void delete();
}