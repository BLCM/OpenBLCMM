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
package blcmm.utilities.options;

/**
 * Interface to be used for Enums which will be used inside SelectionOption
 * options.
 *
 * @author apocalyptech
 */
public interface OptionEnum {

    /**
     * The "description" for the enum value which will be rendered onto the
     * dropdown.
     *
     * @return The user-facing description of the enum.
     */
    String getDescription();

    /**
     * Returns the name of the enum value (should not need to be manually
     * implemented -- merely implementing this interface on an enum class will
     * provide this method).
     *
     * @return The name of the enum value
     */
    String name();

}
