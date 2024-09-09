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
package blcmm.data.lib;

import blcmm.model.PatchType;

/**
 * An interface to allow arbitrary components to receive status updates from
 * the DataManager extraction/verification processes, so it can be shown to
 * the user if desired.
 *
 * Event messages are intended to be marked as major or minor, with "major"
 * ones intended to be steps that may take a noticeable amount of time to
 * complete.  That way the app can decide whether or not to display the
 * messages.
 *
 * @author apocalyptech
 */
public interface DataStatusNotifier {

    /**
     * Sets the Game whose data is currently being processed.
     *
     * @param game The game being processed.
     */
    public void setGame(PatchType game);

    /**
     * Respond to an event.  Events may be major or minor.
     *
     * @param message The message to send.
     * @param major True if the event is major, or false if it's minor.
     */
    public void event(String message, boolean major);

    /**
     * Used to indicate that data processing has finished.
     */
    public void finish();

}
