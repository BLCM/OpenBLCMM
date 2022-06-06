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
package blcmm.utilities.options;

/**
 * Simple interface for any data type used in a SelectionOption.
 *
 * @author apocalyptech
 */
public interface SelectionOptionData {

    /**
     * Returns a string suitable for saving this object into a text-based
     * preference file. Needed because .toString() might be something sent to
     * the user.
     *
     * @return A string identifying this option
     */
    public String toSaveString();

}
